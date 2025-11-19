package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = WriteDummyRoomCommand().main(args)

class WriteDummyRoomCommand : CliktCommand() {
    val outFile by argument(name = "out-file", help = "output file").path()
    val roomImage by option("--room-image", help = "Use room image from this file for the output")
        .path(mustExist = true)

    override fun run() {
        writeDummyRoom(outFile, roomImage)
    }
}

data class RoomData(
    val entryCode: ScriptBytesV5,
    val exitCode: ScriptBytesV5,
    val localScripts: Map<Int, ScriptBytesV5> = emptyMap(),
)

@JvmInline
value class ColorCycleIndex(val value: Int) {
    init {
        require(value in 1..16) { "Illegal index: $value, must be between 1 ..= 16" }
    }
}

@JvmInline
value class ColorCycleDelay(val value: Int)

@JvmInline
value class ColorCycleFlags(val value: Int)

data class ColorCycleEntry(val index: ColorCycleIndex, val dunno: Int, val delay: ColorCycleDelay, val flags: ColorCycleFlags) {
}

data class ColorCycleBlockV5(val entries: List<ColorCycleEntry>) : BlockV5 {
    val contentLength: Int
        get() = 1 /* \0 */ +  entries.size * 7

    override fun writeTo(out: DataOutput) {
        writeBlockHeader(out, blockId, contentLength)

        entries.forEach { entry ->
            out.writeByte(entry.index.value)
            out.writeShort(entry.dunno)
            out.writeShort(entry.delay.value)
            out.writeShort(entry.flags.value)
        }

        out.writeByte(0)
    }

    override val blockId: BlockId4
        get() = BlockId4("CYCL")
    override val blockLength: BlockLengthV5
        get() = BlockLengthV5(headerLength + contentLength)
}

fun writeDummyRoom(path: Path, roomImageFile: Path?) {
    DataOutputStream(FileOutputStream(path.toFile())).use { out ->
        val objectCount = 0

        val blocksInRoom = mutableListOf<BlockV5>()

        // RMHD
        val rmhdBlock = RoomHeaderBlockV5(11, 77, objectCount)
            .also { blocksInRoom.add(it) }

        // CYCL
        val cyclBlock = ColorCycleBlockV5(emptyList())
            .also { blocksInRoom.add(it) }

        // TRNS
        val trnsBlock = RawBlockV5(BlockId4("TRNS"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // EPAL
        val epalBlock = RawBlockV5(BlockId4("EPAL"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // BOXD
        val boxdBlock = RawBlockV5(BlockId4("BOXD"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // BOXM
        val boxmBlock = RawBlockV5(BlockId4("BOXM"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // CLUT
        val clutBlock = RawBlockV5(BlockId4("CLUT"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // SCAL
        val scalBlock = RawBlockV5(BlockId4("SCAL"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // RMIM
        val rmimBlock = if (roomImageFile == null) {
            RawBlockV5(BlockId4("RMIM"), ByteArray(0))
        } else {
            DataInputStream(roomImageFile.toFile().inputStream()).use {
                logger.info { "Copying room image from $roomImageFile" }

                val blockHeader = BlockHeaderV5.readFrom(it)
                blockHeader.expectBlockId(RoomFileBlockId.RMIM)

                val rmimBytes = ByteArray(blockHeader.contentLength.value)
                it.readFully(rmimBytes)

                RawBlockV5(BlockId4("RMIM"), rmimBytes)
            }
        }

        blocksInRoom.add(rmimBlock)

        repeat(objectCount) {
            // OBIM
            val obimBlock = RawBlockV5(BlockId4("OBIM"), ByteArray(0))
                .also { blocksInRoom.add(it) }

            // OBCD
            val obcdBlock = RawBlockV5(BlockId4("OBCD"), ByteArray(0))
                .also { blocksInRoom.add(it) }
        }

        // EXCD
        val excdBlock = RawBlockV5(BlockId4("EXCD"), ByteArray(0))
            .also { blocksInRoom.add(it) }

        // ENCD
        val encdBlock = RawBlockV5(BlockId4("ENCD"), dummyEntryCodeBytes())
            .also { blocksInRoom.add(it) }

        val localScriptCount = 0
        // NLSC
        val nlscBlock = RawBlockV5(BlockId4("NLSC"), byteArrayOf(0, 0))
            .also { blocksInRoom.add(it) }

        // LSCR
        repeat(localScriptCount) {
            val lscrBlock = RawBlockV5(BlockId4("LSCR"), ByteArray(0))
                .also { blocksInRoom.add(it) }
        }

/*
        val blocksInRoom = listOf(
            rmhdBlock,
            cyclBlock,
            trnsBlock,
            epalBlock,
            boxdBlock,
            boxmBlock,
            clutBlock,
            scalBlock,
            rmimBlock,
            obimBlock,
            obcdBlock,
            excdBlock,
            encdBlock,
            nlscBlock,
            lscrBlock
        )
*/

        val contentLength = blocksInRoom.sumOf { it.blockLength.value }
        writeBlockHeader(out, BlockId4("ROOM"), contentLength)
        blocksInRoom.forEach { it.writeTo(out) }
    }
}

fun dummyEntryCodeBytes(): ByteArray {
    val baos = ByteArrayOutputStream()

//    val printInstr = PrintInstr(
//        ImmediateByteParam(0xfd),
//        listOf(PrintInstr.Text(ScummStringBytesV5("entry code in da house".toByteArray() + byteArrayOf(0))))
//    )

    DataOutputStream(baos).use { dataOut ->
       emitDummyScriptBytes(dataOut)

//        val drawBoxBytes = DrawBoxInstr(
//            ImmediateWordParam(10),
//            ImmediateWordParam(10),
//            ImmediateWordParam(150),
//            ImmediateWordParam(300),
//            ImmediateByteParam(2)
//        ).emitBytes()
//        dataOut.write(drawBoxBytes)
    }

    return baos.toByteArray()
}
