package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import java.io.Closeable
import java.io.DataInput
import java.io.File
import java.io.RandomAccessFile
import kotlin.experimental.xor

fun main(args: Array<String>) = DumpRoomFileCommand().main(args)

enum class RoomFileBlockId(val id: String) {
    BOXD("BOXD"),
    BOXM("BOXM"),
    CLUT("CLUT"),   // standard room palette
    COST("COST"),
    CYCL("CYCL"),
    EPAL("EPAL"),   // EGA palette
    ENCD("ENCD"),
    EXCD("EXCD"),
    LSCR("LSCR"),
    NLSC("NLSC"),
    OBCD("OBCD"),
    OBIM("OBIM"),
    ROOM("ROOM"),
    RMHD("RMHD"),
    RMIM("RMIM"),
    SCAL("SCAL"),
    SCRP("SCRP"),
    TRNS("TRNS"),
    ;

    init {
        require(id.length == 4) { "Block ID must be exactly 4 characters long" }
    }

    fun matches(blockId4: BlockId4): Boolean = blockId4.matches(id)
}

data class ScriptBytesV5(val bytes: ByteArray) {
    companion object {
        fun fromScriptBytes(bytes: ByteArray): ScriptBytesV5 {
            return ScriptBytesV5(bytes)
        }
    }
}

data class EntryCodeDataV5(val scriptBytes: ScriptBytesV5) {
    companion object Companion {
        fun fromScriptBytes(bytes: ByteArray): EntryCodeDataV5 {
            return EntryCodeDataV5(ScriptBytesV5(bytes))
        }
    }
}

data class ExitCodeDataV5(val scriptBytes: ScriptBytesV5) {
    companion object Companion {
        fun fromScriptBytes(bytes: ByteArray): ExitCodeDataV5 {
            return ExitCodeDataV5(ScriptBytesV5(bytes))
        }
    }
}

class ScummDataFileV5(val file: RandomAccessFile, val xorCode: Byte = 0x69) : Closeable {
    val xorInt = xorCode.toInt().shl(24)
        .or(xorCode.toInt().shl(16))
        .or(xorCode.toInt().shl(8))
        .or(xorCode.toInt())

    fun readBlockId4(): BlockId4 {
        val idBytes = ByteArray(4)
        file.readFully(idBytes)
        idBytes.sliceArray(0..< 4).mapIndexed { index, byte -> idBytes[index] = byte.xor(xorCode) }
        return BlockId4(idBytes)
    }

    override fun close() {
        file.close()
    }


    fun readBlockLength(): BlockLengthV5 {
        val length = file.readInt()
        return BlockLengthV5(length.xor(xorInt))
    }

    fun readBlockHeader(): BlockHeaderV5 {
        val blockId4 = readBlockId4()
        val blockLength = readBlockLength()

        return BlockHeaderV5(blockId4, blockLength)
    }

    fun readRoomHeaderBlock(): RoomHeaderBlockV5 {
        val blockHeader = readBlockHeader()
        blockHeader.expectBlockId(RoomFileBlockId.RMHD)
        return RoomHeaderBlockV5.readDataFrom(file)
    }

    fun expectBlockHeaderWithId(blockId: RoomFileBlockId): BlockHeaderV5 {
        val blockHeader = readBlockHeader()
        blockHeader.expectBlockId(blockId)
        return blockHeader
    }

    fun expectAndSeekToEndOfBlock(blockId: RoomFileBlockId) {
        val blockStart = file.filePointer
        val blockHeader = readBlockHeader()
        println(blockHeader)
        blockHeader.expectBlockId(blockId)
        file.seek(blockStart + blockHeader.blockLength.value)
    }

    fun readRoomEEntryCodeData(): EntryCodeDataV5 {
        val blockHeader = readBlockHeader()
        blockHeader.expectBlockId(RoomFileBlockId.ENCD)
        val entryCodeBytes = ByteArray(blockHeader.blockLength.value - 8)
        file.readFully(entryCodeBytes)
        return EntryCodeDataV5.fromScriptBytes(entryCodeBytes)
    }

    fun readRoomExitCodeData(): ExitCodeDataV5 {
        val blockHeader = readBlockHeader()
        blockHeader.expectBlockId(RoomFileBlockId.EXCD)
        val exitCodeBytes = ByteArray(blockHeader.blockLength.value - 8)
        file.readFully(exitCodeBytes)
        return ExitCodeDataV5.fromScriptBytes(exitCodeBytes)
    }
}

data class RoomHeaderBlockV5(val width: UInt, val height: UInt, val objectCount: UInt) {
    companion object {
        fun readDataFrom(data: DataInput): RoomHeaderBlockV5 {
            val width = data.readShortLittleEndian().toUInt()
            val height = data.readShortLittleEndian().toUInt()
            val objectCount = data.readShortLittleEndian().toUInt()

            return RoomHeaderBlockV5(width, height, objectCount)
        }
    }
}

fun BlockHeaderV5.expectBlockId(expectedBlockId: RoomFileBlockId) {
    if (!expectedBlockId.matches(blockId)) {
        throw IllegalArgumentException("Expected block ID $expectedBlockId, but got ${blockId.asString()}")
    }
}

class DumpRoomFileCommand : CliktCommand() {
    val roomFile by argument(name = "room-file", help = "Room file to dump").file(mustExist = true)
    val encoded by option("--encoded").boolean().default(false)
    val dumpEntryCode by option("--dump-entry-code").boolean().default(false)
    val dumpExitCode by option("--dump-exit-code").boolean().default(false)

    override fun run() {
        dumpRoomFile(roomFile, encoded)
    }

    private fun dumpRoomFile(path: File, encoded: Boolean) {
        val xorCode = if (encoded) 0x69.toByte() else 0x00.toByte()

        ScummDataFileV5(RandomAccessFile(path, "r"), xorCode).use { file ->
            val blockHeader = file.readBlockHeader()
            blockHeader.expectBlockId(RoomFileBlockId.ROOM)

            val roomHeaderBlock = file.readRoomHeaderBlock()
            println(roomHeaderBlock)

            file.expectAndSeekToEndOfBlock(RoomFileBlockId.CYCL)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.TRNS)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.EPAL)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.BOXD)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.BOXM)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.CLUT)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.SCAL)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.RMIM)

            repeat(roomHeaderBlock.objectCount.toInt()) {
                file.expectAndSeekToEndOfBlock(RoomFileBlockId.OBIM)
            }

            repeat(roomHeaderBlock.objectCount.toInt()) {
                file.expectAndSeekToEndOfBlock(RoomFileBlockId.OBCD)
            }

            processRoomExitCode(file, dump=dumpExitCode)
            processRoomEntryCode(file, dump=dumpEntryCode)
            file.expectBlockHeaderWithId(RoomFileBlockId.NLSC)
            val nlsc = file.file.readShortLittleEndian()
            repeat(nlsc.toInt()) {
                file.expectAndSeekToEndOfBlock(RoomFileBlockId.LSCR)
            }
            println("file length: ${file.file.length()}, file pointer: ${file.file.filePointer}")
        }
    }

    private fun processRoomEntryCode(file: ScummDataFileV5, dump: Boolean) {
        if (!dump) {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.ENCD)
            return
        }

        val entryCodeData = file.readRoomEEntryCodeData()
        val instructions = decodeInstructionsFromScriptBytes(entryCodeData.scriptBytes.bytes)

        println("room entry code:")

        dumpInstructions(instructions, entryCodeData.scriptBytes)
    }

    private fun dumpInstructions(
        instructions: List<Pair<Int, Instruction>>,
        scriptBytes: ScriptBytesV5
    ) {
        instructions.forEach {
            val (offset, instruction) = it
            val bytesString = scriptBytes.bytes.sliceArray(offset..<offset + instruction.length)
                .joinToString(" ") { "%02x".format(it) }
            println("%6d %s\n       %s".format(offset, instruction.toSource(), bytesString))
        }
    }

    private fun processRoomExitCode(file: ScummDataFileV5, dump: Boolean) {
        if (!dump) {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.EXCD)
            return
        }

        val exitCodeData = file.readRoomExitCodeData()
        val instructions = decodeInstructionsFromScriptBytes(exitCodeData.scriptBytes.bytes)

        println("room exit code:")

        dumpInstructions(instructions, exitCodeData.scriptBytes)
    }
}