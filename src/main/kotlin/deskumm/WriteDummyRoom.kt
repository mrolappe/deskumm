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
    val clutFile by option("--clut-file", help = "Use palette from this file for the output")
        .path(mustExist = true)
    val boxdFile by option("--boxd-file", help = "Use box definitions from this file for the output")
        .path(mustExist = true)
    val boxmFile by option("--boxm-file", help = "Use box matrices from this file for the output")
        .path(mustExist = true)

    override fun run() {
        writeDummyRoom(outFile, roomImage, clutFile, boxdFile, boxmFile)
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

fun writeDummyRoom(path: Path, roomImageFile: Path?, clutFile: Path?, boxdFile: Path?, boxmFile: Path?) {
    DataOutputStream(FileOutputStream(path.toFile())).use { out ->
        val objectCount = 0

        val blocksInRoom = mutableListOf<BlockV5>()

        // RMHD
        val rmhdBlock = RoomHeaderBlockV5(784, 144, objectCount)    // mi2 room7
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
        val boxdBlock = if (boxdFile == null) {
            RawBlockV5(BlockId4("BOXD"), ByteArray(0))
        } else {
            rawBlockFromFile(BlockId4("BOXD"), "box definitions", boxdFile)
        }
        blocksInRoom.add(boxdBlock)

        // BOXM
        val boxmBlock = if (boxmFile == null) {
            RawBlockV5(BlockId4("BOXM"), ByteArray(0))
        } else {
            rawBlockFromFile(BlockId4("BOXM"), "box matrix", boxmFile)
        }
        blocksInRoom.add(boxmBlock)

        // CLUT
        clutBlock(clutFile).let { blocksInRoom.add(it) }

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
        RawBlockV5(BlockId4("EXCD"), exitCodeBytes()).let { blocksInRoom.add(it) }

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

        println("Dummy room written to $path")
    }
}

private fun rawBlockFromFile(
    blockId: BlockId4,
    description: String,
    fromFile: Path
): RawBlockV5 =
    DataInputStream(fromFile.toFile().inputStream()).use {
        logger.info { "Copying $description from $fromFile" }

        val blockHeader = BlockHeaderV5.readFrom(it)
        blockHeader.expectBlockId(blockId)

        val contentBytes = ByteArray(blockHeader.contentLength.value)
        it.readFully(contentBytes)

        RawBlockV5(blockId, contentBytes)
    }

private fun exitCodeBytes(): ByteArray {
    return ByteArrayOutputStream().use { baos ->
        DataOutputStream(baos).use { out ->
            emitBytesForBannerColorString(out)

            listOf(0xfc, 0xfd).forEach { who ->
                // who == 0xfe||0xff -> load charset 0
                DebugInstr(ImmediateWordParam(who)).emitBytes(out)

                val printInstr = PrintInstr(
                    ImmediateByteParam(who),
                    listOf(PrintInstr.Text(ScummStringBytesV5("exit code in da house".toByteArray() + byteArrayOf(0))))
                )

                printInstr.emitBytes(out)
            }

            EndScriptInstr.emitBytes(out)
        }

        baos.toByteArray()
    }
}

private fun clutBlock(clutFile: Path?): RawBlockV5 {
    val clutBlock = if (clutFile == null) {
        RawBlockV5(BlockId4("CLUT"), ByteArray(0))

    } else {
        DataInputStream(clutFile.toFile().inputStream()).use {
            logger.info { "Copying palette from $clutFile" }

            val blockHeader = BlockHeaderV5.readFrom(it)
            blockHeader.expectBlockId(RoomFileBlockId.CLUT)

            val clutBytes = ByteArray(blockHeader.contentLength.value)
            it.readFully(clutBytes)
            RawBlockV5(BlockId4("CLUT"), clutBytes)
        }
    }
    return clutBlock
}

fun dummyEntryCodeBytes(): ByteArray {
    val baos = ByteArrayOutputStream()

//    val printInstr = PrintInstr(
//        ImmediateByteParam(0xfd),
//        listOf(PrintInstr.Text(ScummStringBytesV5("entry code in da house".toByteArray() + byteArrayOf(0))))
//    )

    DataOutputStream(baos).use { out ->
//       emitDummyScriptBytes(dataOut)

//        CursonOnEmit.bytes().let { dataOut.write(it) }
//        CursorSoftOnEmit.bytes().let { dataOut.write(it) }
//        UserPutOnEmit.bytes().let { dataOut.write(it) }

        DebugInstr(ImmediateWordParam(1177)).emitBytes(out)

        AssignValueInstr(ResultVar(GlobalVarSpec(43)), ImmediateWordParam(32))  // pause-key

        emitString44(out)

        AssignLiteralToStringInstr(ImmediateByteParam(6), ScummStringBytesV5.from("string 6")).emitBytes(out)
        AssignLiteralToStringInstr(ImmediateByteParam(4), ScummStringBytesV5.from("pause-text: leertaste etc. pp.")).emitBytes(out)

        emitBytesForBannerColorString(out)

        val charsetParam = ImmediateByteParam(2)
        LoadCharsetInstr(charsetParam).emitBytes(out)
        CharsetInstr(charsetParam).emitBytes(out)

        listOf(0xfc, 0xfd).forEach { who ->
            // who == 0xfe||0xff -> load charset 0
            DebugInstr(ImmediateWordParam(who)).emitBytes(out)

            val subs =
                listOf(
                    PrintInstr.At(ImmediateWordParam(10), ImmediateWordParam(10)),
                    PrintInstr.Center,
                    PrintInstr.Color(ImmediateByteParam(10)),
                    PrintInstr.Text(ScummStringBytesV5("entry code in da house".toByteArray() + byteArrayOf(0))))
            val printInstr = PrintInstr(
                ImmediateByteParam(who),
                subs
            )

            printInstr.emitBytes(out)
        }

        val actorParam = ImmediateByteParam(11)

        DoAnimationInstr(actorParam, ImmediateByteParam(250)).emitBytes(out)

        val costumeParam = ImmediateByteParam(1)

        LockCostumeInstr(costumeParam).emitBytes(out)
        LoadCostumeInstr(costumeParam).emitBytes(out)

        val actorSubs = listOf(
            ActorInstr.Default,
            ActorInstr.Costume(costumeParam),
            ActorInstr.TalkColor(ImmediateByteParam(15)),
            ActorInstr.IgnoreBoxes,
            ActorInstr.NeverZClip
        )
        ActorInstr(actorParam, actorSubs).emitBytes(out)

        PutActorInRoomInstr(actorParam, ImmediateByteParam(1)).emitBytes(out)
        PutActorAtInstr(actorParam, ImmediateWordParam(50), ImmediateWordParam(50)).emitBytes(out)

        SayLineInstr(listOf(PrintInstr.Text(ScummStringBytesV5.from("funzt dat getz?")))).emitBytes(out)

        val drawBoxBytes = DrawBoxInstr(
            ImmediateWordParam(10),
            ImmediateWordParam(10),
            ImmediateWordParam(150),
            ImmediateWordParam(300),
            ImmediateByteParam(2)
        ).emitBytes()
        out.write(drawBoxBytes)

        DebugInstr(ImmediateWordParam(1337)).emitBytes(out)

        EndObjectInstr.emitBytes(out)
//        EndScriptInstr.emitBytes(out)
    }

    return baos.toByteArray()
}
