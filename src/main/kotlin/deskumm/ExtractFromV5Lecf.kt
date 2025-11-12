package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.io.RandomAccessFile

fun main(args: Array<String>) {
    ExtractFromLecfCommand().main(args)
}

class ExtractFromLecfCommand : CliktCommand() {
    val lecfFile by argument(name = "lecf-file", help = "LECF file to extract from")
        .file(mustExist = true)
    val offset by argument(help = "file offset of block to extract")
        .int()
    val blockId by argument(help = "ID of block to extract")
        .convert { DataFileBlockId.valueOf(it) }
    val withBlockHeader by option("--with-block-header", help = "include block header")
        .boolean().default(true)

    override fun run() {
        RandomAccessFile(lecfFile, "r").use { file ->
            val lecfBlockHeader = file.readBlockHeader()
            println(lecfBlockHeader)

            file.seek(offset.toLong())
            val blockHeaderAtOffset = file.readBlockHeader()
            println("$blockHeaderAtOffset, ok: ${blockId == blockHeaderAtOffset.blockId}")

            RandomAccessFile("extract.out", "rw").use { outFile ->
                if (withBlockHeader == true) {
                    outFile.write(blockHeaderAtOffset.blockId.name.toByteArray())
                    outFile.writeInt(blockHeaderAtOffset.blockLength)
                }

                val blockData = ByteArray(blockHeaderAtOffset.blockLength - 8)
                file.readXorEncoded(blockData, 0x69)
                outFile.write(blockData)
            }
        }
    }
}