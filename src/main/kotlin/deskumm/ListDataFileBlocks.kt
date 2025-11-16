package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import java.io.DataInput
import java.io.RandomAccessFile
import kotlin.experimental.xor

fun main(args: Array<String>) = ListDataFileBlocksCommand().main(args)

class ListDataFileBlocksCommand : CliktCommand() {
    val dataFile by argument(name = "data-file").file(mustExist = true)

    override fun run() {
        val dataFile = RandomAccessFile(dataFile, "r")
        listDataFileBlocks(dataFile)
    }
}

enum class DataFileBlockId {
    LECF, LOFF, LFLF, ROOM, RMHD, CYCL, TRNS, EPAL, BOXD, BOXM, CLUT, SCAL, RMIM, OBIM, OBCD, EXCD,
    ENCD, NLSC, LSCR, COST, SCRP, CHAR, SOUN
}

typealias BlockHandler = () -> Unit

sealed class BlockInfo(val hasChildren: Boolean)
class LECFBlockInfo : BlockInfo(hasChildren = true)
class LOFFBlockInfo : BlockInfo(hasChildren = false)
class LFLFBlockInfo : BlockInfo(hasChildren = true)
class ROOMBlockInfo : BlockInfo(hasChildren = true)
class RMHDBlockInfo : BlockInfo(hasChildren = false)
class CYCLBlockInfo : BlockInfo(hasChildren = false)
class TRNSBlockInfo : BlockInfo(hasChildren = false)
class EPALBlockInfo : BlockInfo(hasChildren = false)
class BOXDBlockInfo : BlockInfo(hasChildren = false)
class BOXMBlockInfo : BlockInfo(hasChildren = false)
class CLUTBlockInfo : BlockInfo(hasChildren = false)
class SCALBlockInfo : BlockInfo(hasChildren = false)
class RMIMBlockInfo : BlockInfo(hasChildren = false)
class OBIMBlockInfo : BlockInfo(hasChildren = false)
class OBCDBlockInfo : BlockInfo(hasChildren = false)
class EXCDBlockInfo : BlockInfo(hasChildren = false)
class ENCDBlockInfo : BlockInfo(hasChildren = false)
class NLSCBlockInfo : BlockInfo(hasChildren = false)
class LSCRBlockInfo : BlockInfo(hasChildren = false)
class COSTBlockInfo : BlockInfo(hasChildren = false)
class SCRPBlockInfo : BlockInfo(hasChildren = false)
class CHARBlockInfo : BlockInfo(hasChildren = false)
class SOUNBlockInfo : BlockInfo(hasChildren = false)

fun listDataFileBlocks(dataFile: RandomAccessFile) {
    val blockHandlers = hashMapOf(
            DataFileBlockId.LECF to LECFBlockInfo(),
            DataFileBlockId.LOFF to LOFFBlockInfo(),
            DataFileBlockId.LFLF to LFLFBlockInfo(),
            DataFileBlockId.ROOM to ROOMBlockInfo(),
            DataFileBlockId.RMHD to RMHDBlockInfo(),
            DataFileBlockId.CYCL to CYCLBlockInfo(),
            DataFileBlockId.TRNS to TRNSBlockInfo(),
            DataFileBlockId.EPAL to EPALBlockInfo(),
            DataFileBlockId.BOXD to BOXDBlockInfo(),
            DataFileBlockId.BOXM to BOXMBlockInfo(),
            DataFileBlockId.CLUT to CLUTBlockInfo(),
            DataFileBlockId.SCAL to SCALBlockInfo(),
            DataFileBlockId.RMIM to RMIMBlockInfo(),
            DataFileBlockId.OBIM to OBIMBlockInfo(),
            DataFileBlockId.OBCD to OBCDBlockInfo(),
            DataFileBlockId.EXCD to EXCDBlockInfo(),
            DataFileBlockId.ENCD to ENCDBlockInfo(),
            DataFileBlockId.NLSC to NLSCBlockInfo(),
            DataFileBlockId.LSCR to LSCRBlockInfo(),
            DataFileBlockId.COST to COSTBlockInfo(),
            DataFileBlockId.SCRP to SCRPBlockInfo(),
            DataFileBlockId.CHAR to CHARBlockInfo(),
        DataFileBlockId.SOUN to SOUNBlockInfo()
    )

    dataFile.use {
        while (dataFile.filePointer < dataFile.length()) {
            val absoluteOffsetOfBlock = dataFile.filePointer

            val blockHeader = dataFile.readBlockHeader()

            println("@$absoluteOffsetOfBlock: ${blockHeader.blockId}, len: ${blockHeader.blockLength} (end: ${absoluteOffsetOfBlock + blockHeader.blockLength})")

            if (blockHeader.blockId == DataFileBlockId.LOFF) {
                val numRooms = dataFile.readXorEncoded()
                println("loff, numRooms: $numRooms")
                for (i in 0 until numRooms) {
                    val room = dataFile.readXorEncoded()
                    val offset = dataFile.readIntLittleEndianXorEncoded()
                    println("room: $room, offset: $offset")
                }
            }

            val blockInfo = blockHandlers[blockHeader.blockId]

            if (blockInfo == null) {
                System.err.println("unhandled block id: ${blockHeader.blockId}")
                break
            }
            if (!blockInfo.hasChildren) {
                dataFile.seek(absoluteOffsetOfBlock + blockHeader.blockLength)
            } else {
                dataFile.seek(absoluteOffsetOfBlock + 8)
            }
        }
    }
}

typealias DataFileBlockLength = Int

data class DataFileBlockHeader(val blockId: DataFileBlockId, val blockLength: DataFileBlockLength)

fun DataInput.readBlockHeader(): DataFileBlockHeader {
//    println("readBlockHeader @ $filePointer")
    val blockId = readBlockId()
    val blockLength = readInt().xor(0x69696969)
    return DataFileBlockHeader(blockId, blockLength)
}

fun DataInput.readBlockId(): DataFileBlockId {
    val idBytes = ByteArray(4)
    readXorEncoded(idBytes, 0, 4)
    return DataFileBlockId.valueOf(String(idBytes))
}

fun DataInput.readXorEncoded(code: Byte = 0x69): Byte {
    return readByte().xor(code)
}

fun DataInput.readXorEncoded(buffer: ByteArray, code: Byte) {
   readXorEncoded(buffer, 0, buffer.size, code)
}

fun DataInput.readXorEncoded(buffer: ByteArray, offset: Int, length: Int, code: Byte = 0x69) {
    readFully(buffer, offset, length)

    buffer.sliceArray(0..< length).mapIndexed { index, byte -> buffer[index] = byte.xor(code) }
}