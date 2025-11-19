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
        writeEmptyDirFile(File("$baseName.000"))
//    writeEmptyDataFile(File("$baseName.001"))

        writeDummyLecfFileForCharset(Paths.get("$baseName.001"))
        writeDummyLecfFileForRoomAndScript(Paths.get("$baseName.004"))
        writeDummyLecfFileForScript(Paths.get("$baseName.005"))
    }
}

fun writeDummyLecfFileForRoomAndScript(path: Path) {
    val lecfBlock = LecfBlock(listOf(
        RoomAndOffset(1, 397),
        RoomAndOffset(7, 397)
    ))

    DataOutputStream(XorOutputStream(FileOutputStream(path.toFile()))).use { out ->
        lecfBlock.writeTo(out)

        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { emitDummyScriptBytes(it) }
        val scrpBlock = ScrpBlockV5(bytes.toByteArray())
        scrpBlock.writeTo(out)

        FileInputStream(Paths.get("data/ROOM/dummy.ROOM").toFile()).use { it.copyTo(out) }
//        FileInputStream(Paths.get("data/ROOM/mi2_7_397860.ROOM").toFile()).use { it.copyTo(out) }

        FileInputStream(Paths.get("data/CHAR/2.CHAR").toFile()).use { it.copyTo(out) }

        FileInputStream(Paths.get("data/COST/mi2_142_room7_590264.COST").toFile()).use { it.copyTo(out) }
    }
}

fun bannerBytes(count: Int): ByteArray {
    return ByteArray(count, { i -> ' '.toByte() })
}

object CursonOnEmit {
    fun bytes(): ByteArray = byteArrayOf(0x27, 1)
}

object CursorOffEmit {
    fun bytes(): ByteArray = byteArrayOf(0x27, 2)
}
object UserPutOnEmit {
    fun bytes(): ByteArray = byteArrayOf(0x27, 3)
}

object UserPutOffEmit {
    fun bytes(): ByteArray = byteArrayOf(0x27, 4)
}

object CursorSoftOnEmit {
    fun bytes(): ByteArray = byteArrayOf(0x27, 5)
}

object CursorSoftOffEmit {
    fun bytes(): ByteArray = byteArrayOf(0x27, 6)
}

class LoadCharsetInstrEmit(val charset: Int) {
    init {
        require(charset in 1..<256) { "Illegal charset: $charset" }
    }

    fun bytes(): ByteArray = byteArrayOf(0x0c, 0x12, charset.toByte())
}

class CursorSetCharsetInstr(val charset: Int) {
    init {
        require(charset in 1..<256) { "Illegal charset: $charset" }
    }

    fun bytes(): ByteArray = byteArrayOf(0x2c, 13, charset.toByte())
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
    listOf(1, 1).map { ImmediateByteParam(it) }.forEach { roomParam ->
        LockRoomInstr(roomParam).emitBytes(dataOut)
        LoadRoomInstr(roomParam).emitBytes(dataOut)
        CurrentRoomInstr(roomParam).emitBytes(dataOut)
    }

    // set-screen 16 to 144
    dataOut.write(byteArrayOf(0x33, 0x03))
    ImmediateWordParam(16).emitBytes(dataOut)
    ImmediateWordParam(144).emitBytes(dataOut)

    // load-charset 1
    dataOut.write(byteArrayOf(0x0c, 0x12))
    ImmediateByteParam(1).emitBytes(dataOut)

    // charset 1
    dataOut.write(byteArrayOf(0x2c, 0xd))
    ImmediateByteParam(1).emitBytes(dataOut)

        emitBytesForBannerColorString(dataOut)

    EndScriptInstr.emit(dataOut)
//        dataOut.write(CursonOnEmit.bytes())
//        dataOut.write(CursorSoftOnEmit.bytes())

    StartMusicInstr(ImmediateByteParam(0)).emit(dataOut)

    val drawBoxBytes = DrawBoxInstr(
        ImmediateWordParam(10),
        ImmediateWordParam(10),
        ImmediateWordParam(150),
        ImmediateWordParam(300),
        ImmediateByteParam(2)
    ).emitBytes()
    dataOut.write(drawBoxBytes)

    dataOut.write(
        byteArrayOf(
            0x14.toByte(), 0xfc.toByte(),
            0, 0x10, 0, 0x10, 0,    // at x, y
            1, 3,   // color 3
            4,  // center
            //            0xfc.toByte(),
            0xf, 'A'.toByte(), 'B'.toByte(), 'C'.toByte(), 'D'.toByte(), 0,
//                0xff.toByte(),
        )
    )

//    PutActorInRoomInstr(ImmediateByteParam(1), roomParam).emitBytes(dataOut)

    dataOut.write(LoadCharsetInstrEmit(2).bytes())
    dataOut.write(CursorSetCharsetInstr(2).bytes())

    dataOut.write(byteArrayOf(0x27, 1, 5) + "neu starten?0".toByteArray())
    dataOut.write(0)

    dataOut.write(byteArrayOf(0x27, 1, 6) + "string 6".toByteArray())
    dataOut.write(0)

    // print 255 color 15 at 160, 8 center overhead
    dataOut.write(
        byteArrayOf(
            0x14,
            0xff.toByte(),
            0x01,
            0x0f,
            0x00,
            0xa0.toByte(),
            0x00,
            0x08,
            0x00,
            0x04,
            0x07,
            0xff.toByte()
        )
    )
    dataOut.write(
        byteArrayOf(
            0x14,
            0xfc.toByte(),
            0x01,
            0x0c,
            0x0f,
            0x48,
            0x61,
            0x72,
            0x64,
            0x20,
            0x64,
            0x69,
            0x73,
            0x6b,
            0
        )
    )


    // print 252 text "Hard disk"
    dataOut.write(byteArrayOf(0x14, 0xfc.toByte(), 0x0f, 0x48, 0x61, 0x72, 0x64, 0x20, 0x64, 0x69, 0x73, 0x6b, 0x00))

    // say-line text "..."
    dataOut.write(byteArrayOf(0xd8.toByte(), 0xf) + "tach!".toByteArray() + byteArrayOf(0x00))

    // wait-for-message
    dataOut.write(byteArrayOf(0xae.toByte(), 0x02))
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
    val maximums = Maximums(charsetCount = 3u)

    DataOutputStream(XorOutputStream(FileOutputStream(dirFile))).use {
//        it.writeDummyRnamBlock()
        it.writeMaxsBlock(maximums)
        it.writeDummyDrooBlock()
        it.writeDummyDscrBlock()
//        it.writeEmptyBlock(DirectoryBlockId.DSOU)

//        it.writeEmptyBlock(DirectoryBlockId.DCOS)
        it.writeGenericDirectoryBlock(DirectoryBlockId.DCOS, costumeInfos())

        val charsetInfo = listOf(
            // erster eintrag ist dummy
            DummyRoomNumberAndOffset,

            RoomNumberAndOffset(2, 22),
            RoomNumberAndOffset(1, 82979),
        )
        it.writeGenericDirectoryBlock(DirectoryBlockId.DCHR, charsetInfo)
        it.writeDummyDobjBlock()
    }
}

fun costumeInfos(): List<RoomNumberAndOffset> {
    return List(143) { idx ->
        if (idx == 142) {
            RoomNumberAndOffset(7, 87605)
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
    val blockIdBytes = DirectoryBlockId.RNAM.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(9)     // total length including block ID, length itself and end marker
    writeByte(0)   // end marker
}

private fun DataOutput.writeBlockId(blockId: BlockId4) {
    blockId.writeTo(this)
}

private fun DataOutput.writeBlockId(blockId: DirectoryBlockId) {
    val blockIdBytes = blockId.name.toByteArray()
    write(blockIdBytes, 0, 4)
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
    val blockIdBytes = blockId.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(10)
    writeShort(0)
}

data class Maximums(val charsetCount: UInt)

/**
 * Für MI2 scheinen 9 Words Daten erwartet zu werden
 */
fun DataOutputStream.writeMaxsBlock(maximums: Maximums) {
    writeBlockId(DirectoryBlockId.MAXS)
    writeInt(26)

    writeShortLittleEndian(600)                            // vars
    writeShortLittleEndian(0x8001.toShort())                    // unknown
    writeShortLittleEndian(777)                           // bit vars
    writeShortLittleEndian(200)                           // local objs
    writeShortLittleEndian(0x8002.toShort())                    // unknown
    writeShortLittleEndian(maximums.charsetCount.toShort())     // charsets
    writeShortLittleEndian(0x8003.toShort())                    // unknown
    writeShortLittleEndian(0x8004.toShort())                    // unknown
    writeShortLittleEndian(1)                    // inventory
}

fun DataOutputStream.writeDummyDrooBlock() {
    writeBlockId(DirectoryBlockId.DROO)

    val entries = listOf(
        DummyRoomNumberAndOffset,
        RoomNumberAndOffset(4, 22),
        RoomNumberAndOffset(1, 0),
        RoomNumberAndOffset(4, 22),
        RoomNumberAndOffset(1, 0),
        RoomNumberAndOffset(4, 22),
        RoomNumberAndOffset(4, 22),
        RoomNumberAndOffset(4, 22),
        RoomNumberAndOffset(4, 22),
    )

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
        else if (it == 1) return@List RoomNumberAndOffset(3, 27)
        else return@List RoomNumberAndOffset(it.toByte(), it)
    })
//        val infos = listOf(
//        DummyRoomNumberAndOffset,
//        RoomNumberAndOffset(3, 22)
//    )

    val blockIdBytes = DirectoryBlockId.DSCR.name.toByteArray()
    write(blockIdBytes, 0, 4)

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
