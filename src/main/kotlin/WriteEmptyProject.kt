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
    writeEmptyDataFile(File("$baseName.017"))
}

fun writeEmptyDirFile(dirFile: File) {
    DataOutputStream(XorOutputStream(FileOutputStream(dirFile))).use {
        it.writeDummyRnamBlock()
        it.writeEmptyMaxsBlock()
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
    writeInt(9)
    writeByte(0)   // end marker
}

private fun DataOutputStream.writeDummyDchrBlock() {
    val blockIdBytes = DirectoryBlockId.DCHR.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(20)

    writeShortLittleEndian(2)

    // in welchem ROOM befindet sich das charset; muss eine raumnummer sind, für die im DROO ein
    // entsprechender eintrag existiert, damit das LECF ermittelt werden kann
    writeByte(0)   // room
    writeByte(4)   // room

    // offset relativ zum ROOM
    writeIntLittleEndian(0)  // offs
    writeIntLittleEndian(151789)  // offs
}

fun DataOutputStream.writeEmptyBlock(blockId: DirectoryBlockId) {
    val blockIdBytes = blockId.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(10)
    writeShort(0)
}

/**
 * Für MI2 scheinen 9 Words Daten erwartet zu werden
 */
fun DataOutputStream.writeEmptyMaxsBlock() {
    val blockIdBytes = DirectoryBlockId.MAXS.name.toByteArray()
    write(blockIdBytes, 0, 4)
    writeInt(26)

    repeat(9) {
        writeShortLittleEndian(200)
    }
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

