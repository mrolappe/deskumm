package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.Closeable
import java.io.DataInput
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.io.RandomAccessFile

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = DumpRoomFileCommand().main(args)

enum class RoomFileBlockId(val id: String) {
    BOXD("BOXD"),
    BOXM("BOXM"),
    CDHD("CDHD"),
    CLUT("CLUT"),   // standard room palette
    COST("COST"),
    CYCL("CYCL"),
    EPAL("EPAL"),   // EGA palette
    ENCD("ENCD"),
    IMHD("IMHD"),
    IM00("IM00"),
    IM01("IM01"),
    IM02("IM02"),
    IM03("IM03"),
    IM04("IM04"),
    EXCD("EXCD"),
    LSCR("LSCR"),
    NLSC("NLSC"),
    OBCD("OBCD"),
    OBIM("OBIM"),
    ROOM("ROOM"),
    RMHD("RMHD"),
    RMIH("RMIH"),
    RMIM("RMIM"),
    SCAL("SCAL"),
    SCRP("SCRP"),
    SMAP("SMAP"),
    TRNS("TRNS"),
    ZP01("ZP01"),
    ZP02("ZP02"),
    ;

    init {
        require(id.length == 4) { "Block ID must be exactly 4 characters long" }
    }

    fun matches(blockId4: BlockId4): Boolean = blockId4.matches(id)

    fun toBlockId4(): BlockId4 = BlockId4(id)

    companion object {
        fun from(blockId4: BlockId4): RoomFileBlockId = valueOf(blockId4.asString())
    }
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

data class ObjectCodeDataV5(val scriptBytes: ScriptBytesV5) {
    companion object Companion {
        fun fromScriptBytes(bytes: ByteArray): ObjectCodeDataV5 {
            return ObjectCodeDataV5(ScriptBytesV5(bytes))
        }
    }
}

data class LocalScriptBlockV5(val scriptId: Int, val scriptBytes: ScriptBytesV5) {
    companion object Companion {
        fun fromScriptBytes(scriptId: Int, bytes: ByteArray): LocalScriptBlockV5 {
            return LocalScriptBlockV5(scriptId, ScriptBytesV5(bytes))
        }
    }
}

class ScummDataFileV5(val file: RandomAccessFile, val xorCode: Byte = 0x69) : Closeable {
    val xorInt = xorInt(xorCode)

    fun readBlockId4(): BlockId4 = BlockId4.readFrom(file, xorCode)

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
        blockHeader.expectBlockId(blockId)
        file.seek(blockStart + blockHeader.blockLength.value)
    }

    fun readRoomEntryCodeData(): EntryCodeDataV5 {
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

    fun readObjectCodeData(): ObjectCodeDataV5 {
        val blockHeader = readBlockHeader()
        blockHeader.expectBlockId(RoomFileBlockId.OBCD)

        readBlockHeader().expectBlockId(RoomFileBlockId.CDHD)
        val objectCodeBytes = ByteArray(blockHeader.blockLength.value - 8)
        file.readFully(objectCodeBytes)
        return ObjectCodeDataV5.fromScriptBytes(objectCodeBytes)
    }

    fun peekBlockId(): RoomFileBlockId {
        val blockStart = file.filePointer
        val blockId4 = readBlockId4()
        file.seek(blockStart)
        val blockId = RoomFileBlockId.from(blockId4)
        logger.trace { "peeked block ID $blockId" }
        return blockId
    }

    fun readLocalScriptBlock(): LocalScriptBlockV5 {
        val blockHeader = readBlockHeader()
        blockHeader.expectBlockId(RoomFileBlockId.LSCR)
        val scriptId = file.readUnsignedByte()
        val scriptBytes = ByteArray(blockHeader.blockLength.value - 9)
        file.readFully(scriptBytes)
        return LocalScriptBlockV5.fromScriptBytes(scriptId, scriptBytes)
    }
}

fun xorInt(xorCode: Byte): Int = xorCode.toInt().shl(24)
    .or(xorCode.toInt().shl(16))
    .or(xorCode.toInt().shl(8))
    .or(xorCode.toInt())

interface BlockV5 {
    fun writeTo(out: DataOutput)

    val blockId: BlockId4
    val blockLength: BlockLengthV5

    val headerLength: Int
        get() = BlockHeaderV5.BLOCK_HEADER_BYTE_COUNT
}

data class RoomHeaderBlockV5(val width: Int, val height: Int, val objectCount: Int) : BlockV5 {
    companion object {
        fun readDataFrom(data: DataInput): RoomHeaderBlockV5 {
            val width = data.readShortLittleEndian().toInt()
            val height = data.readShortLittleEndian().toInt()
            val objectCount = data.readShortLittleEndian().toInt()

            return RoomHeaderBlockV5(width, height, objectCount)
        }
    }

    override fun writeTo(out: DataOutput) {
        writeBlockHeader(out, BlockId4("RMHD"), 6)
        out.writeShortLittleEndian(width.toShort())
        out.writeShortLittleEndian(height.toShort())
        out.writeShortLittleEndian(objectCount.toShort())
    }

    override val blockId: BlockId4
        get() = BlockId4("RMHD")

    override val blockLength: BlockLengthV5
        get() = BlockLengthV5(8 /* header */ + 6 /* data */)
}

fun BlockHeaderV5.expectBlockId(expectedBlockId: RoomFileBlockId) {
    expectBlockId(expectedBlockId.toBlockId4())
}

class DumpRoomFileCommand : CliktCommand() {
    val roomFile by argument(name = "room-file", help = "Room file to dump").file(mustExist = true)
    val outputFile by option("--output-file", "-o", help = "Output file name").file(mustExist = false)
    val encoded by option("--encoded").boolean().default(false)
    val dumpEntryCode by option("--dump-entry-code").boolean().default(false)
    val dumpExitCode by option("--dump-exit-code").boolean().default(false)
    val dumpObjectCode by option("--dump-object-code").boolean().default(false)
    val dumpLocalScripts by option("--dump-local-scripts").boolean().default(false)
    val extractRoomImage by option("--extract-room-image").boolean().default(false)
    val extractClut by option("--extract-clut").boolean().default(false)
    val extractBoxm by option("--extract-boxm").boolean().default(false)
    val extractBoxd by option("--extract-boxd").boolean().default(false)

    override fun run() {
        dumpRoomFile(roomFile, encoded)
    }

    private fun dumpRoomFile(path: File, encoded: Boolean) {
        val xorCode = if (encoded) 0x69.toByte() else 0x00.toByte()

        val printStream: PrintStream = outputFile?.let { PrintStream(it) } ?: System.out

        ScummDataFileV5(RandomAccessFile(path, "r"), xorCode).use { file ->
            val blockHeader = file.readBlockHeader()
            blockHeader.expectBlockId(RoomFileBlockId.ROOM)

            val roomHeaderBlock = file.readRoomHeaderBlock()
            printStream.println("Room header block: $roomHeaderBlock")

            file.expectAndSeekToEndOfBlock(RoomFileBlockId.CYCL)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.TRNS)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.EPAL)
            dumpBoxDefinition(file, path.nameWithoutExtension)
            dumpBoxMatrix(file, path.nameWithoutExtension)
            dumpPalette(file, path.nameWithoutExtension)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.SCAL)

            dumpRoomImage(file, roomHeaderBlock.objectCount, path.nameWithoutExtension)

//            repeat(roomHeaderBlock.objectCount.toInt()) {
//                file.expectAndSeekToEndOfBlock(RoomFileBlockId.OBIM)
//            }

            var numberOfLocalScripts = 0

            while (file.file.filePointer < file.file.length()) {
                logger.debug { "file pointer: ${file.file.filePointer}, length: ${file.file.length()}" }
                when (file.peekBlockId()) {
                    RoomFileBlockId.OBCD -> processObjectCode(
                        file,
                        roomHeaderBlock.objectCount,
                        dump = dumpObjectCode,
                        printStream
                    )

                    RoomFileBlockId.EXCD -> processRoomExitCode(file, dump = dumpExitCode, printStream)
                    RoomFileBlockId.ENCD -> processRoomEntryCode(file, dump = dumpEntryCode, printStream)

                    RoomFileBlockId.NLSC -> {
                        file.expectBlockHeaderWithId(RoomFileBlockId.NLSC)
                        numberOfLocalScripts = file.file.readShortLittleEndian().toInt()
                    }

                    RoomFileBlockId.LSCR -> processLocalScript(numberOfLocalScripts, file, dump=dumpLocalScripts, printStream)
                    else -> throw IllegalArgumentException("Unexpected block id ${file.peekBlockId()}")
                }
            }

            logger.debug { "file length: ${file.file.length()}, file pointer: ${file.file.filePointer}" }
        }
    }

    private fun dumpBoxMatrix(file: ScummDataFileV5, basename: String) {
        if (extractBoxm) {
            val boxmHeader = file.expectBlockHeaderWithId(RoomFileBlockId.BOXM)
            DataOutputStream(FileOutputStream("data/$basename.BOXM")).use { out ->
                boxmHeader.writeTo(out)
                val boxmBytes = ByteArray(boxmHeader.contentLength.value)
                file.file.readFully(boxmBytes)
                out.write(boxmBytes)
            }
        } else {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.BOXM)
        }
    }

    private fun dumpBoxDefinition(file: ScummDataFileV5, basename: String) {
        if (extractBoxd) {
            val boxdHeader = file.expectBlockHeaderWithId(RoomFileBlockId.BOXD)
            DataOutputStream(FileOutputStream("data/$basename.BOXD")).use { out ->
                boxdHeader.writeTo(out)
                val boxdBytes = ByteArray(boxdHeader.contentLength.value)
                file.file.readFully(boxdBytes)
                out.write(boxdBytes)
            }
        } else {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.BOXD)
        }
    }

    private fun dumpPalette(file: ScummDataFileV5, basename: String) {
        if (extractClut) {
            val clutHeader = file.expectBlockHeaderWithId(RoomFileBlockId.CLUT)

            DataOutputStream(FileOutputStream("data/$basename.CLUT")).use { out ->
                clutHeader.writeTo(out)
                val clutBytes = ByteArray(clutHeader.contentLength.value)
                file.file.readFully(clutBytes)
                out.write(clutBytes)
            }
        } else {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.CLUT)
        }
    }

    private fun dumpRoomImage(file: ScummDataFileV5, objectCount: Int, baseName: String) {
        val rmimHeader = file.expectBlockHeaderWithId(RoomFileBlockId.RMIM)

        if (extractRoomImage) {
            // TODO better specification of target file name
            DataOutputStream(FileOutputStream("data/$baseName.RMIM")).use { out ->
                rmimHeader.writeTo(out)
                val rmimBytes = ByteArray(rmimHeader.contentLength.value)
                file.file.readFully(rmimBytes)
                out.write(rmimBytes)
            }
        } else {
            file.expectBlockHeaderWithId(RoomFileBlockId.RMIH)

            val numZBuffers = file.file.readShortLittleEndian().toInt()
            logger.debug { "# zBuffers: $numZBuffers" }
            file.expectBlockHeaderWithId(RoomFileBlockId.IM00)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.SMAP)

            if (file.peekBlockId() == RoomFileBlockId.ZP01) {
                file.expectAndSeekToEndOfBlock(RoomFileBlockId.ZP01)
            }

            if (file.peekBlockId() == RoomFileBlockId.ZP02) {
                file.expectAndSeekToEndOfBlock(RoomFileBlockId.ZP02)
            }
        }


        repeat(objectCount) {
            file.expectBlockHeaderWithId(RoomFileBlockId.OBIM)
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.IMHD)

            val peekBlockId = file.peekBlockId()
            logger.debug { "peek block id: $peekBlockId" }
            skipBlocksWithId(file, RoomFileBlockId.IM00, RoomFileBlockId.IM01, RoomFileBlockId.IM02, RoomFileBlockId.IM03, RoomFileBlockId.IM04)
            when(peekBlockId) {
                else -> {}
            }
        }
    }

    private fun skipBlocksWithId(
        file: ScummDataFileV5,
        vararg blockId: RoomFileBlockId,
    ) {
        logger.debug { "skipping blocks with ids ${blockId.joinToString(", ") { it.id }}" }

        while (file.peekBlockId() in blockId) {
            file.expectAndSeekToEndOfBlock(file.peekBlockId())
        }
    }

    private fun processLocalScript(
        numberOfLocalScripts: Int,
        file: ScummDataFileV5,
        dump: Boolean,
        printStream: PrintStream
    ) {
        if (!dump) {
            repeat(numberOfLocalScripts) { file.expectAndSeekToEndOfBlock(RoomFileBlockId.LSCR) }
            return
        }

        repeat(numberOfLocalScripts) {
            val localScriptBlock = file.readLocalScriptBlock()
            val instructions = decodeInstructionsFromScriptBytes(localScriptBlock.scriptBytes.bytes)

            printStream.println("local script ${localScriptBlock.scriptId}:")

            dumpInstructions(instructions, localScriptBlock.scriptBytes, printStream)
        }
    }

    private fun processObjectCode(file: ScummDataFileV5, objectCount: Int, dump: Boolean, printStream: PrintStream) {
        if (!dump) {
            repeat(objectCount) {
                file.expectAndSeekToEndOfBlock(RoomFileBlockId.OBCD)
                return
            }
        }

        repeat(objectCount) {
            val objectCodeData = file.readObjectCodeData()
            val instructions = decodeInstructionsFromScriptBytes(objectCodeData.scriptBytes.bytes)

            printStream.println("object code:")

            dumpInstructions(instructions, objectCodeData.scriptBytes, printStream)
        }
    }

    private fun processRoomEntryCode(file: ScummDataFileV5, dump: Boolean, printStream: PrintStream) {
        if (!dump) {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.ENCD)
            return
        }

        val entryCodeData = file.readRoomEntryCodeData()
        val instructions = decodeInstructionsFromScriptBytes(entryCodeData.scriptBytes.bytes)

        printStream.println("room entry code:")

        dumpInstructions(instructions, entryCodeData.scriptBytes, printStream)
    }

    private fun dumpInstructions(
        instructions: List<Pair<Int, Instruction>>,
        scriptBytes: ScriptBytesV5,
        printStream: PrintStream
    ) {
        instructions.forEach { offsetAndInstruction ->
            val (offset, instruction) = offsetAndInstruction
            val bytesString = scriptBytes.bytes.sliceArray(offset..<offset + instruction.length)
                .joinToString(" ") { "%02x".format(it) }
            printStream.println("%6d %s\n       %s".format(offset, instruction.toSource(), bytesString))
        }
    }

    private fun processRoomExitCode(file: ScummDataFileV5, dump: Boolean, printStream: PrintStream) {
        if (!dump) {
            file.expectAndSeekToEndOfBlock(RoomFileBlockId.EXCD)
            return
        }

        val exitCodeData = file.readRoomExitCodeData()
        val instructions = decodeInstructionsFromScriptBytes(exitCodeData.scriptBytes.bytes)

        printStream.println("room exit code:")

        dumpInstructions(instructions, exitCodeData.scriptBytes, printStream)
    }
}