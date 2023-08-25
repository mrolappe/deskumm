package jmj.deskumm.writeemptyproject

import jmj.deskumm.dumpdirfile.DirectoryBlockId
import jmj.deskumm.writeIntLittleEndian
import jmj.deskumm.writeShortLittleEndian
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(10)
    }

    val baseName = args[0]

    writeEmptyDirFile(File("$baseName.000"))
    writeEmptyDataFile(File("$baseName.001"))
}

fun writeEmptyDirFile(dirFile: File) {
    val maximums = Maximums(charsetCount = 2u)

    DataOutputStream(XorOutputStream(FileOutputStream(dirFile))).use {
        it.writeDummyRnamBlock()
        it.writeMaxsBlock(maximums)
        it.writeDummyDrooBlock()
        it.writeDummyDscrBlock()
        it.writeEmptyBlock(DirectoryBlockId.DSOU)
        it.writeEmptyBlock(DirectoryBlockId.DCOS)
        it.writeDummyDchrBlock()
        it.writeEmptyBlock(DirectoryBlockId.DOBJ)
    }
}

private fun DataOutputStream.writeDummyRnamBlock() {
    val blockIdBytes = DirectoryBlockId.RNAM.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(9)     // total length including block ID, length itself and end marker
    writeByte(0)   // end marker
}

private fun DataOutputStream.writeDummyDchrBlock() {
    val blockIdBytes = DirectoryBlockId.DCHR.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(20)    // total length including block ID, length itself and end marker

    writeShortLittleEndian(2)       // itemCount

    // in welchem ROOM befindet sich das charset; muss eine raumnummer sind, für die im DROO ein
    // entsprechender eintrag existiert, damit das LECF ermittelt werden kann
    writeByte(0)   // room (first dummy entry)
    writeByte(1)   // room (first dummy entry)

    // offset relativ zum ROOM
    writeIntLittleEndian(0)  // offs (first dummy entry)
    writeIntLittleEndian(151789)  // offs
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
    val blockIdBytes = DirectoryBlockId.MAXS.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(26)

    writeShortLittleEndian(200)                            // vars
    writeShortLittleEndian(0x8001.toShort())                    // unknown
    writeShortLittleEndian(200)                           // bit vars
    writeShortLittleEndian(200)                           // local objs
    writeShortLittleEndian(0x8002.toShort())                    // unknown
    writeShortLittleEndian(maximums.charsetCount.toShort())     // charsets
    writeShortLittleEndian(0x8003.toShort())                    // unknown
    writeShortLittleEndian(0x8004.toShort())                    // unknown
    writeShortLittleEndian(1)                    // inventory
}

fun DataOutputStream.writeDummyDrooBlock() {
    val blockIdBytes = DirectoryBlockId.DROO.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(35)
    writeShortLittleEndian(5)   // 2 ROOMs, erster ist dummy

    // gibt LECF an, in dem ROOM zu finden ist
    writeByte(0)
    writeByte(1)
    writeByte(1)
    writeByte(1)
    writeByte(1)

    writeIntLittleEndian(0)  // off
    writeIntLittleEndian(0)  // off
    writeIntLittleEndian(0)  // off
    writeIntLittleEndian(0)  // off
    writeIntLittleEndian(0)  // off
}

fun DataOutputStream.writeDummyDscrBlock() {
    val blockIdBytes = DirectoryBlockId.DSCR.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(20)    // 8 Bytes Header und 1 Word für Anzahl

    writeShortLittleEndian(2)   // count

    writeByte(0)    // room
    writeByte(4)    // room

    writeIntLittleEndian(0)     // offs
    writeIntLittleEndian(76461)     // offs
}


fun writeEmptyDataFile(dataFile: File) {
    FileOutputStream(dataFile).writer().use {
        it.write("DATA")
    }
}

fun printUsage() {
    System.err.println("Aufruf: WriteEmptyProject <Basis-Pfad>")
}

