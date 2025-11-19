package deskumm

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.RandomAccessFile
import java.nio.file.Path

data class DirectoryEntryV5(val container: Int, val offset: Int) {
    init {
        require(container in 0..<256) { "Illegal container: $container" }
        require(offset >= 0) { "Illegal offset: $offset" }
    }
}

class DirectoryFileV5(val file: RandomAccessFile, val xorCode: Byte = 0x69) : Closeable {
    override fun close() {
        file.close()
    }

    companion object {
        fun parse(path: Path) : GlobDirectory {
            DataInputStream(XorInputStream(path.toFile().inputStream(), 0x69)).use { dataIn ->
                processRealNames(dataIn)
                expectAndSeekToEndOfBlock(dataIn, BlockId4("MAXS"))
                expectAndSeekToEndOfBlock(dataIn, BlockId4("DROO"))
                expectAndSeekToEndOfBlock(dataIn, BlockId4("DSCR"))
                expectAndSeekToEndOfBlock(dataIn, BlockId4("DSOU"))
                val costumeEntries = processCostumeDirectory(dataIn)
                costumeEntries.forEachIndexed { idx, entry -> println("Costume $idx: ${entry.container} ${entry.offset}") }
                expectAndSeekToEndOfBlock(dataIn, BlockId4("DCHR"))
                expectAndSeekToEndOfBlock(dataIn, BlockId4("DOBJ"))
                
                return GlobDirectory()
            }
        }

        private fun processCostumeDirectory(dataIn: DataInputStream): List<DirectoryEntryV5> {
            val block = RawBlockV5.readFrom(dataIn)
            block.blockId isExpectedToBe BlockId4("DCOS")

            val entries = DataInputStream(ByteArrayInputStream(block.content)).use { blockData ->
                val itemCount = blockData.readShortLittleEndian()
                println("itemCount: $itemCount")

                val containers = buildList(itemCount.toInt()) {
                    repeat(itemCount.toInt()) { add(blockData.readUnsignedByte()) }
                }

                val offsets = buildList(itemCount.toInt()) {
                    repeat(itemCount.toInt()) { add(blockData.readIntLittleEndian()) }
                }

                containers.zip(offsets) { container, offset -> DirectoryEntryV5(container, offset) }
            }

            return entries
        }

        private fun processRealNames(dataIn: DataInputStream) {
            expectAndSeekToEndOfBlock(dataIn, BlockId4("RNAM"))
        }
    }
}

class GlobDirectory