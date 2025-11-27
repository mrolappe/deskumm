package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.*
import java.nio.file.Path
import java.nio.file.Paths


fun main(args: Array<String>) = WriteEmptyProjectCommand().main(args)

class WriteEmptyProjectCommand : CliktCommand() {
    val baseName by argument(name = "base-name", help = "Base name of project")

    override fun run() {
//        writeEmptyDirFile(File("$baseName.000"))

//        writeDummyLecfFileForCharset(Paths.get("$baseName.002"))
        writeDummyLecfFileForRoomAndScript(Paths.get("$baseName.004"))
//        writeDummyLecfFileForScript(Paths.get("$baseName.005"))
    }
}

fun writeDummyLecfFileForRoomAndScript(path: Path) {
    val lecfBlock = LecfBlock(listOf(
        RoomAndOffset(1, 27),
        RoomAndOffset(113, 27)
    ))

    DataOutputStream(XorOutputStream(FileOutputStream(path.toFile()))).use { out ->
        lecfBlock.writeTo(out)

        FileInputStream(Paths.get("data/ROOM/dummy.ROOM").toFile()).use { it.copyTo(out) }
//        FileInputStream(Paths.get("data/ROOM/mi2_7_397860.ROOM").toFile()).use { it.copyTo(out) }

        FileInputStream(Paths.get("data/CHAR/2.CHAR").toFile()).use { it.copyTo(out) }

        FileInputStream(Paths.get("data/COST/mi2_142_room7_590264.COST").toFile()).use { it.copyTo(out) }

        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { emitDummyScriptBytes(it) }
        val scrpBlock = ScrpBlockV5(bytes.toByteArray())
        scrpBlock.writeTo(out)
        DataOutputStream(FileOutputStream(Paths.get("data/SCRP/dummy.SCRP").toFile())).use { scrpBlock.writeTo(it) }
    }
}

fun bannerBytes(count: Int): ByteArray {
    return ByteArray(count, { i -> ' '.toByte() })
}


fun writeDummyLecfFileForScript(path: Path) {
//    val scrpBlock = readScrpBlock(Paths.get("data/SCRP/1.SCRP"))
    val lecfBlock = LecfBlock(listOf(RoomAndOffset(2, 0)))

    DataOutputStream(XorOutputStream(FileOutputStream(path.toFile()))).use { out ->
        lecfBlock.writeTo(out)

        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { emitDummyScriptBytes(it) }
        val scrpBlock = ScrpBlockV5(bytes.toByteArray())
        scrpBlock.writeTo(out)
    }
}

fun emitDummyScriptBytes(dataOut: DataOutput) {
    SetScreenInstr.emit(dataOut, 0, 150)

//    CurrentRoomInstr(ByteVarParam(LocalVarSpec(1))).emitBytes(dataOut)
//    LoadCharsetInstr(ByteVarParam(LocalVarSpec(0))).emitBytes(dataOut)

    val charset = 4
    LoadCharsetInstr.emit(dataOut, charset)
    CharsetInstr.emit(dataOut, charset)

        emitBytesForBannerColorString(dataOut)

    // pause-key
    AssignValueToVarInstr.emit(dataOut, ResultVar(GlobalVarSpec(43)), 32)
    AssignLiteralToStringInstr(ImmediateByteParam(4), ScummStringBytesV5.from("pause-text: leertaste etc. pp.")).emitBytes(dataOut)

    val theRoom = 113
    val roomParam = ImmediateByteParam(theRoom)
    LockRoomInstr(roomParam).emitBytes(dataOut)
    LoadRoomInstr(roomParam).emitBytes(dataOut)
    CurrentRoomInstr(roomParam).emitBytes(dataOut)

/*
    listOf(*/
/*1,*//*
 5).map { ImmediateByteParam(it) }.forEach { roomParam ->
        LockRoomInstr(roomParam).emitBytes(dataOut)
        LoadRoomInstr(roomParam).emitBytes(dataOut)
        CurrentRoomInstr(roomParam).emitBytes(dataOut)
    }
*/

    (1..12).forEach { costume ->
        LockCostumeInstr(ImmediateByteParam(costume)).emitBytes(dataOut)
        LoadCostumeInstr(ImmediateByteParam(costume)).emitBytes(dataOut)
    }

    // selected-actor
    AssignValueToVarInstr(ResultVar(GlobalVarSpec(1)), ImmediateWordParam(1)).emitBytes(dataOut)

    // machine-speed
    AssignValueToVarInstr(ResultVar(GlobalVarSpec(68)), ImmediateWordParam(2)).emitBytes(dataOut)

    ActorInstr(
        ImmediateByteParam(1),
        listOf(
//                ActorInstr.Default,
            ActorInstr.Costume(ImmediateByteParam(1)),
            ActorInstr.IgnoreBoxes,
            ActorInstr.TalkColor(ImmediateByteParam(1)),
            ActorInstr.StepDist(ImmediateByteParam(3), ImmediateByteParam(3)),
            ActorInstr.Name(ScummStringBytesV5.from("walker"))
        )
    ).emitBytes(dataOut)
    PutActorInRoomInstr(ImmediateByteParam(1), roomParam).emitBytes(dataOut)
    PutActorAtInstr(ImmediateByteParam(1), ImmediateWordParam(100), ImmediateWordParam(100)).emitBytes(dataOut)

    WalkActorToXYInstr(ImmediateByteParam(1), ImmediateWordParam(300), ImmediateWordParam(150)).emitBytes(dataOut)
    PrintInstr(ImmediateByteParam(1), listOf(PrintInstr.Text(ScummStringBytesV5.from("ich gehe!")))).emitBytes(dataOut)

    SleepForJiffiesInstr(1000).emitBytes(dataOut)

    EndScriptInstr.emitBytes(dataOut)
    (1..24).forEach { idx ->
        ActorInstr(
            ImmediateByteParam(1),
            listOf(
//                ActorInstr.Default,
                ActorInstr.Costume(ImmediateByteParam(idx)),
                ActorInstr.IgnoreBoxes,
                ActorInstr.TalkColor(ImmediateByteParam(idx)),
                ActorInstr.Name(ScummStringBytesV5.from("le guy costume $idx"))
            )
        ).emitBytes(dataOut)

        PutActorInRoomInstr(ImmediateByteParam(1), roomParam).emitBytes(dataOut)
        PutActorAtInstr(ImmediateByteParam(1), ImmediateWordParam(100), ImmediateWordParam(100)).emitBytes(dataOut)
        SayLineInstr(listOf(PrintInstr.Text(ScummStringBytesV5.from("tach!")))).emitBytes(dataOut)
        WaitForMessageInstr.emitBytes(dataOut)

        SleepForJiffiesInstr(60).emitBytes(dataOut)
    }

    (3..20).forEach { sound ->
        emitPrintText(
            dataOut,
            "sound $sound",
            listOf(
                PrintInstr.At(ImmediateWordParam(100), ImmediateWordParam(10 * sound)),
                PrintInstr.Color(ImmediateByteParam(3)),
                PrintInstr.Overhead
            )
        )

        StartMusicInstr(ImmediateByteParam(sound)).emit(dataOut)
        SleepForJiffiesInstr(120).emitBytes(dataOut)
    }

    (50..100 step 5).forEach { x ->
        PutActorAtInstr(ImmediateByteParam(1), ImmediateWordParam(x), ImmediateWordParam(100)).emitBytes(dataOut)
        SleepForJiffiesInstr(10).emitBytes(dataOut)
    }

    SayLineInstr(listOf(PrintInstr.Text(ScummStringBytesV5.from("le guy sagt was")))).emitBytes(dataOut)
    WaitForMessageInstr.emitBytes(dataOut)

    CursorOnInstr.emitBytes(dataOut)
    CursorSoftOn.emitBytes(dataOut)


    (0..8).forEach { idx ->
        DrawBoxInstr.emit(dataOut, idx * 10, 10, 320, 50, idx)
        SleepForJiffiesInstr(10).emitBytes(dataOut)
    }

    emitPrintSystem(
        dataOut,
        "tach",
        listOf(
            PrintInstr.At(ImmediateWordParam(10), ImmediateWordParam(10)),
            PrintInstr.Color(ImmediateByteParam(3)),
            PrintInstr.Center)
    )

    emitPrintText(dataOut, "hinter dir^ein dreiköpfiger affe",
        listOf(PrintInstr.At(ImmediateWordParam(100), ImmediateWordParam(150))))
//    emitPrintLine(dataOut, "print-line; mal schauen, wie es aussieht")
//    emitSayLine(dataOut, "say-line; mal schauen, wie es aussieht")

    PutActorInRoomInstr(ImmediateByteParam(12), roomParam).emitBytes(dataOut)
    PutActorAtInstr(ImmediateByteParam(12), ImmediateWordParam(0), ImmediateWordParam(0)).emitBytes(dataOut)

    LoadCharsetInstr(ImmediateByteParam(3)).emitBytes(dataOut)
    CharsetInstr(ImmediateByteParam(3)).emitBytes(dataOut)

    AssignLiteralToStringInstr(ImmediateByteParam(5), ScummStringBytesV5.from("neu starten?")).emitBytes(dataOut)
    AssignLiteralToStringInstr(ImmediateByteParam(6), ScummStringBytesV5.from("string 6")).emitBytes(dataOut)

    EndScriptInstr.emitBytes(dataOut)
    emitPrintDebug(dataOut, "> sleep jiffies")
    SleepForJiffiesInstr(1111).emitBytes(dataOut)
    emitPrintDebug(dataOut, "< sleep jiffies")

    StopMusicInstr.emitBytes(dataOut)

    // print 252 text "Hard disk"
    dataOut.write(byteArrayOf(0x14, 0xfc.toByte(), 0x0f, 0x48, 0x61, 0x72, 0x64, 0x20, 0x64, 0x69, 0x73, 0x6b, 0x00))

    // say-line text "..."
//    dataOut.write(byteArrayOf(0xd8.toByte(), 0xf) + "tach!".toByteArray() + byteArrayOf(0x00))
}

fun emitPrintLine(dataOut: DataOutput, string: String, subs: List<PrintInstr.Sub> = emptyList()) {
    PrintInstr(ImmediateByteParam(0xff), subs + PrintInstr.Text(ScummStringBytesV5.from(string))).emitBytes(dataOut)
}

fun emitPrintText(dataOut: DataOutput, string: String, subs: List<PrintInstr.Sub> = emptyList()) {
    PrintInstr(ImmediateByteParam(0xfe), subs + PrintInstr.Text(ScummStringBytesV5.from(string))).emitBytes(dataOut)
}

fun emitSayLine(dataOut: DataOutput, string: String, subs: List<PrintInstr.Sub> = emptyList()) {
    SayLineInstr(subs + PrintInstr.Text(ScummStringBytesV5.from(string))).emitBytes(dataOut)
}

fun emitPrintSystem(
    dataOut: DataOutput,
    string: String,
    subs: List<PrintInstr.Sub>
) {
    PrintInstr(ImmediateByteParam(0xfc), subs + PrintInstr.Text(ScummStringBytesV5.from(string))).emitBytes(dataOut)
}

fun emitPrintDebug(dataOut: DataOutput, string: String) {
    PrintInstr(ImmediateByteParam(0xfd), listOf(PrintInstr.Text(ScummStringBytesV5.from(string)))).emitBytes(dataOut)
}

fun emitBytesForBannerColorString(byteStream: DataOutput) {
//    byteStream.write(byteArrayOf(0x27, 1, 21) + bannerBytes(32))      // str21 = banner color bytes

    byteStream.write(byteArrayOf(0x27, 5, 21, 32))      // dim str21[32]; str21[:] = 0

    val colors = listOf(5, 2, 4)

   // str21[i] = 3
    for (i in 0..42) {
        byteStream.write(byteArrayOf(0x27, 3, 21, i.toByte(), colors[i % 3].toByte()))
    }
/*
    byteStream.write(byteArrayOf(0x27, 3, 21, 25, 4))    // str21[25] = ?
    byteStream.write(byteArrayOf(0x27, 3, 21, 26, 1))    // str21[26] = ?
    byteStream.write(byteArrayOf(0x27, 3, 21, 27, 5))
    byteStream.write(byteArrayOf(0x27, 3, 21, 28, 2))
    byteStream.write(byteArrayOf(0x27, 3, 21, 29, 5))
    byteStream.write(byteArrayOf(0x27, 3, 21, 30, 2))
*/
}

fun emitString44(out: DataOutput) {
    val stringParam = ImmediateByteParam(44)
    NewStringInstr(stringParam, ImmediateByteParam(105)).emitBytes(out)

    (0..105).forEach {
        SetStringCharAtInstr(stringParam, ImmediateByteParam(it), ImmediateByteParam(50)).emitBytes(out)
    }
}

data class RoomAndOffset(val room: Int, val offset: Int) {
    init {
        require(room in 1..<256) { "Illegal room: $room" }
        require(offset >= 0) { "Illegal offset: $offset" }
    }
}

class LoffBlock(val roomOffsets: List<RoomAndOffset>) {
    val blockId = BlockId4("LOFF")

    companion object {
        const val HEADER_LENGTH = 8
    }

    private val numberOfRooms = roomOffsets.size

    val contentLength: Int
        get() = 1 + 5 * numberOfRooms

    val totalLength: Int
        get() = HEADER_LENGTH + contentLength

    fun writeTo(stream: DataOutputStream) {
        writeBlockHeader(stream, blockId, contentLength)

        stream.write(roomOffsets.size)
        roomOffsets.forEach { roomOffset ->
            stream.write(roomOffset.room)
            stream.writeIntLittleEndian(roomOffset.offset)
        }
    }
}

fun writeBlockHeader(stream: DataOutput, blockId: BlockId4, contentLength: Int) {
    blockId.writeTo(stream)
    stream.writeInt(8 + contentLength)
}

class LecfBlock(val roomOffsets: List<RoomAndOffset>) {
    val blockId = BlockId4("LECF")

    fun writeTo(stream: DataOutputStream) {

        val loffBlock = LoffBlock(roomOffsets)

        val contentLength = loffBlock.totalLength
        writeBlockHeader(stream, blockId, contentLength)

        loffBlock.writeTo(stream)
    }
}

private fun writeDummyLecfFileForCharset(path: Path) {
    val charBlock = readCharBlock(Paths.get("data/CHAR/2.CHAR"))
    val lecfBlock = LecfBlock(listOf(RoomAndOffset(1, 0)))

    DataOutputStream(XorOutputStream(FileOutputStream(path.toFile()))).use { out ->
        lecfBlock.writeTo(out)
        charBlock?.writeTo(out)
    }
}

private fun CharBlock.writeToFile(path: Path) {
    DataOutputStream(XorOutputStream(FileOutputStream(path.toFile()))).use {
        it.writeBlockId(BlockId4("CHAR"))
        it.writeIntLittleEndian(data.size)
        it.write(data)
    }
}

//private fun DataOutputStream.writeBlockId()
data class CharBlock(val data: ByteArray) {
    fun writeTo(stream: DataOutputStream) {
        val blockId = BlockId4("CHAR")
        println("char block write, data size: ${data.size}")
        writeBlockHeader(stream, blockId, data.size)
        stream.write(data)
    }
}

data class ScrpBlockV5(val data: ByteArray) {
    fun writeTo(stream: DataOutputStream) {
        val blockId = BlockId4("SCRP")
        println("scrp block write, data size: ${data.size}")
        writeBlockHeader(stream, blockId, data.size)
        stream.write(data)
    }
}

fun readScrpBlock(path: Path): ScrpBlockV5? {
    return RandomAccessFile(path.toFile(), "r").use { file ->
        val blockId = BlockId4.readFrom(file)

        if (!blockId.matches("SCRP")) {
            return null
        }

        val blockLength = file.readInt()
        println("scrp ($path) block length: $blockLength")
        val buffer = ByteArray(blockLength - 8)
        file.read(buffer)
        ScrpBlockV5(buffer)
    }
}

fun readCharBlock(path: Path): CharBlock? {
    return RandomAccessFile(path.toFile(), "r").use { file ->
        val blockId = BlockId4.readFrom(file)

        if (!blockId.matches("CHAR")) {
            return null
        }

        val blockLength = file.readInt()
        println("char ($path) block length: $blockLength")
        val buffer = ByteArray(blockLength - 8)
        file.read(buffer)
        CharBlock(buffer)
    }
}

fun writeEmptyDirFile(dirFile: File) {

    DataOutputStream(XorOutputStream(FileOutputStream(dirFile))).use {
//        it.writeDummyRnamBlock()
        it.writeMaxsBlock()
        it.writeDummyDrooBlock()
        it.writeDummyDscrBlock()
//        it.writeEmptyBlock(DirectoryBlockId.DSOU)

//        it.writeEmptyBlock(DirectoryBlockId.DCOS)
        it.writeGenericDirectoryBlock(DirectoryBlockId.DCOS, costumeInfos())

        val charsetInfo = listOf(
            // erster eintrag ist dummy
            DummyRoomNumberAndOffset,

            RoomNumberAndOffset(4, 151789),
            RoomNumberAndOffset(1, 75723 - 397),
        )
        it.writeGenericDirectoryBlock(DirectoryBlockId.DCHR, charsetInfo)
        it.writeDummyDobjBlock()
    }
}

fun costumeInfos(): List<RoomNumberAndOffset> {
    return List(143) { idx ->
        if (idx == 1) {
            RoomNumberAndOffset(4, 117285)
        }
        else if (idx == 27) {
            // 27 Container: 4, Offset: 145250
            RoomNumberAndOffset(4, 145250)
        }
        else if (idx == 32) {
            // 32 Container: 5, Offset: 37818
            RoomNumberAndOffset(5, 37818)
        }
        else if (idx == 142 || idx == 20 || idx == 12) {
            RoomNumberAndOffset(7, 192404)
        } else {
            RoomNumberAndOffset(0, 0)
        }
    }
}

private fun DataOutputStream.writeDummyDobjBlock() {
    writeBlockId(DirectoryBlockId.DOBJ)

    val entries = List(137) { RoomNumberAndOffset(it.toByte(), 0)}

    val numEntries = entries.size

    writeInt(8 /* header */ + 2 /* num entries */ + 5 * numEntries)

    writeShortLittleEndian(numEntries.toShort())

    entries.map { it.roomNumber }.forEach { writeByte(it.toInt()) }
    entries.map { it.offset }.forEach { writeIntLittleEndian(it) }
}

private fun DataOutputStream.writeDummyRnamBlock() {
    DirectoryBlockId.RNAM.blockId4.writeTo(this)
    writeInt(9)     // total length including block ID, length itself and end marker
    writeByte(0)   // end marker
}

private fun DataOutput.writeBlockId(blockId: BlockId4) {
    blockId.writeTo(this)
}

private fun DataOutput.writeBlockId(blockId: DirectoryBlockId) {
    blockId.blockId4.writeTo(this)
}

data class RoomNumberAndOffset(val roomNumber: Byte, val offset: Int)

val DummyRoomNumberAndOffset = RoomNumberAndOffset(0, 0)

private fun DataOutput.writeGenericDirectoryBlock(
    blockId: DirectoryBlockId, infos: List<RoomNumberAndOffset>
) {

    writeBlockId(blockId)
    writeInt(8 + 2 + 5 * infos.size)    // total length including block ID, length itself and end marker

    writeShortLittleEndian(infos.size.toShort())       // itemCount

    infos.map { it.roomNumber }.forEach { writeByte(it.toInt()) }
    infos.map { it.offset }.forEach { writeIntLittleEndian(it) }
}

fun DataOutputStream.writeEmptyBlock(blockId: DirectoryBlockId) {
    val blockIdBytes = blockId.blockId4.writeTo(this)
    writeInt(10)
    writeShort(0)
}

//data class Maximums(val charsetCount: UInt)

fun DataOutput.writeMaxsBlock() {
    val maximums = MaximumsV5(
        varCount = ResourceCount(600),
        unknown1 = ResourceCount(0x8001),
        bitVarCount = ResourceCount(777),
        localObjCount = ResourceCount(200),
        unknown2 = ResourceCount(0x8002),
        charsetCount = ResourceCount(3),
        unknown3 = ResourceCount(0x8003),
        unknown4 = ResourceCount(0x8004),
        inventoryCount = ResourceCount(1)
    )

    writeBlockId(DirectoryBlockId.MAXS)
    writeInt(26)

    writeShortLittleEndian(maximums.varCount.value.toShort())
    writeShortLittleEndian(maximums.unknown1.value.toShort())
    writeShortLittleEndian(maximums.bitVarCount.value.toShort())
    writeShortLittleEndian(maximums.localObjCount.value.toShort())
    writeShortLittleEndian(maximums.unknown2.value.toShort())
    writeShortLittleEndian(maximums.charsetCount.value.toShort())
    writeShortLittleEndian(maximums.unknown3.value.toShort())
    writeShortLittleEndian(maximums.unknown4.value.toShort())
    writeShortLittleEndian(maximums.inventoryCount.value.toShort())
}

fun DataOutputStream.writeDummyDrooBlock() {
    writeBlockId(DirectoryBlockId.DROO)

    val entries = List(114) {
        if (it == 0) return@List DummyRoomNumberAndOffset
        else if (it == 113) return@List RoomNumberAndOffset(4, 0)
        else return@List RoomNumberAndOffset(1, 0)
    }

    val numEntries = entries.size

    writeInt(8 /* header */ + 2 /* num entries */ + 5 * numEntries)

    writeShortLittleEndian(numEntries.toShort())

    // gibt LECF an, in dem ROOM zu finden ist
    entries.map { it.roomNumber }.forEach { writeByte(it.toInt()) }
    entries.map { it.offset }.forEach { writeIntLittleEndian(it) }
}

fun DataOutputStream.writeDummyDscrBlock() {
    val infos = List(17, {
        if (it == 0) return@List DummyRoomNumberAndOffset
        else if (it == 1) return@List RoomNumberAndOffset(113, 81488 - 27)
        else if (it == 111) return@List RoomNumberAndOffset(82, 57103)
        else return@List RoomNumberAndOffset(it.toByte(), it)
    })
//        val infos = listOf(
//        DummyRoomNumberAndOffset,
//        RoomNumberAndOffset(3, 22)
//    )

    DirectoryBlockId.DSCR.blockId4.writeTo(this)

    writeInt(8 + 2 + 5 * infos.size)    // 8 Bytes Header und 1 Word für Anzahl

    writeShortLittleEndian(infos.size.toShort())   // count

    infos.map { it.roomNumber }.forEach { writeByte(it.toInt()) }
    infos.map { it.offset }.forEach { writeIntLittleEndian(it) }
}


fun writeEmptyDataFile(dataFile: File) {
    DataOutputStream(XorOutputStream(FileOutputStream(dataFile))).writer().use {
        it.write("CHARBEEFDATA")
    }
}
