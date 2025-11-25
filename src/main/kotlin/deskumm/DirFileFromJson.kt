package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import deskumm.DirectoryDumper.JsonDirectoryDumper.DirectoryModel
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream

fun main(args: Array<String>) = DirFileFromJsonCommand().main(args)

class DirFileFromJsonCommand : CliktCommand() {
    val jsonFile by argument(name = "json-file").file(mustExist = true)
    val dirFile by argument(name = "dir-file").file(mustExist = false, canBeDir = false)

    override fun run() {
        val json = Json {}
        val directoryModel = json.decodeFromString<DirectoryModel>(jsonFile.readText())
        val directory = directoryModel.toDirectory()

        DataOutputStream(XorOutputStream(dirFile.outputStream(), 0x69)).use { outputStream ->
            writeTo(directory, outputStream)
        }

        println("Directory file written to ${dirFile.absolutePath}")
    }

    private fun writeTo(
        directory: DirectoryV5,
        dataOutput: DataOutput
    ) {
        if (directory.roomNames.hasAnyRoomNames) {
            val baos = ByteArrayOutputStream()
            DataOutputStream(baos).use { bytesOutput ->
                directory.roomNames.writeXorEncodedTo(bytesOutput, 0xff.toByte())
            }
            val dataBytes = baos.toByteArray()

            RawBlockV5(DirectoryBlockId.RNAM.blockId4, dataBytes).writeTo(dataOutput)
        }

        val maxsBytes = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { directory.maximums.writeTo(it) }
            baos.toByteArray()
        }

        RawBlockV5(DirectoryBlockId.MAXS.blockId4, maxsBytes).writeTo(dataOutput)

        mapOf(
            DirectoryBlockId.DROO.blockId4 to directory.roomEntries.value,
            DirectoryBlockId.DSCR.blockId4 to directory.scriptEntries.value,
            DirectoryBlockId.DSOU.blockId4 to directory.soundEntries.value,
            DirectoryBlockId.DCOS.blockId4 to directory.costumeEntries.value,
            DirectoryBlockId.DCHR.blockId4 to directory.charsetEntries.value,
        ).forEach { (blockId, entries) ->
            if (entries.isNotEmpty()) {
                val dataBytes = dataBytesFromDirectoryEntries(entries)
                RawBlockV5(blockId, dataBytes).writeTo(dataOutput)
            }
        }

        if (directory.objectEntries.value.isNotEmpty()) {
            val dataBytes = ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { bytesOutput ->
                    val objectCount = directory.objectEntries.value.size
                    bytesOutput.writeShortLittleEndian(objectCount.toShort())
                    repeat(objectCount) { bytesOutput.writeByte(directory.objectEntries.value[it].ownerState) }
                    repeat(objectCount) { bytesOutput.writeIntLittleEndian(directory.objectEntries.value[it].klass) }
                }

                baos.toByteArray()
            }

            RawBlockV5(DirectoryBlockId.DOBJ.blockId4, dataBytes).writeTo(dataOutput)
        }
    }

    private fun dataBytesFromDirectoryEntries(entries: List<ContainerAndOffset>): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dataOutput -> writeDirectoryEntries(entries, dataOutput) }
            baos.toByteArray()
        }
    }

    private fun writeDirectoryEntries(
        entries: List<ContainerAndOffset>,
        dataOutput: DataOutput
    ) {
        dataOutput.writeShortLittleEndian(entries.size.toShort())
        repeat(entries.size) { dataOutput.writeByte(entries[it].container) }
        repeat(entries.size) { dataOutput.writeIntLittleEndian(entries[it].offset) }
    }
}