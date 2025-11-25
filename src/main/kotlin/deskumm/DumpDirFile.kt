package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValueLazy
import com.github.ajalt.clikt.parameters.types.file
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.nio.file.Paths
import kotlin.experimental.xor

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = DumpDirFileCommand().main(args)

class DumpDirFileCommand : CliktCommand() {
    val sourcePath by argument(name = "source-path", help = "Source path").file(mustExist = true, canBeDir = false)
    val dumpAsJson by option("--dump-as-json", help = "Dump as JSON").file(mustExist = false, canBeDir = false)
        .optionalValueLazy { Paths.get(sourcePath.parent, "${sourcePath.nameWithoutExtension}.json").toFile() }
    val dumpRoomNames by option("--dump-room-names", help = "Dump room names").flag(default = false)
    val dumpMaximums by option("--dump-maximums", help = "Dump maximums").flag(default = false)
    val dumpRoomEntries by option("--dump-room-entries", help = "Dump room entries").flag(default = false)
    val dumpScriptEntries by option("--dump-script-entries", help = "Dump script entries").flag(default = false)
    val dumpObjectEntries by option("--dump-object-entries", help = "Dump object entries").flag(default = false)
    val dumpSoundEntries by option("--dump-sound-entries", help = "Dump sound entries").flag(default = false)
    val dumpCostumeEntries by option("--dump-costume-entries", help = "Dump costume entries").flag(default = false)
    val dumpCharsetEntries by option("--dump-charset-entries", help = "Dump charset entries").flag(default = false)

    override fun run() {
        val builder = DirectoryBuilder()

        val rnamBlockHandler = { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
            RnamHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
        }

        val drooBlockHandler = { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
            DrooHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
        }

        val dscrBlockHandler = { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
            DscrHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
        }

        val dobjBlockHandler = { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
            DobjHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
        }

        val maxsBlockHandler = { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
            builder.maximums(MaximumsV5.readFrom(data))
        }

        ScummDirFileV5(sourcePath).use {
            val blockHandlers = hashMapOf(
                DirectoryBlockId.RNAM.blockId4 to rnamBlockHandler,
                DirectoryBlockId.MAXS.blockId4 to maxsBlockHandler,
                DirectoryBlockId.DROO.blockId4 to drooBlockHandler,
                DirectoryBlockId.DSCR.blockId4 to dscrBlockHandler,
                DirectoryBlockId.DSOU.blockId4 to { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
                    DsouHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
                },
                DirectoryBlockId.DCOS.blockId4 to { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
                    DcosHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
                },
                DirectoryBlockId.DCHR.blockId4 to { blockId: BlockId4, blockLength: DataFileBlockLength, data: DataInput ->
                    DchrHandler.handle(BlockHeaderV5(blockId, BlockLengthV5(blockLength)), data, builder)
                },
                DirectoryBlockId.DOBJ.blockId4 to dobjBlockHandler)

            parseDirectory(it.inputStream, blockHandlers, builder)
        }

        val directory = builder.build()

        val options = DirectoryDumper.Options(
            dumpRoomNames,
            dumpMaximums,
            dumpRoomEntries,
            dumpScriptEntries,
            dumpSoundEntries,
            dumpCostumeEntries,
            dumpCharsetEntries,
            dumpObjectEntries
        )

        if (dumpAsJson != null) {
            log.debug { "Dumping directory as JSON to ${dumpAsJson!!.absolutePath}" }

            PrintStream(dumpAsJson!!.outputStream()).use { printStream ->
                DirectoryDumper.JsonDirectoryDumper.dump(directory, options, printStream)
            }
        } else {
            DirectoryDumper.TextDirectoryDumper.dump(directory, options, System.out)
        }
    }

}

sealed interface DirectoryDumper {
    data class Options(
        val dumpRoomNames: Boolean = false,
        val dumpMaximums: Boolean = false,
        val dumpRoomEntries: Boolean = false,
        val dumpScriptEntries: Boolean = false,
        val dumpSoundEntries: Boolean = false,
        val dumpCostumeEntries: Boolean = false,
        val dumpCharsetEntries: Boolean = false,
        val dumpObjectEntries: Boolean = false
    )

    fun dump(directory: DirectoryV5, options: Options, printStream: PrintStream)

    object TextDirectoryDumper : DirectoryDumper {
        override fun dump(directory: DirectoryV5, options: Options, printStream: PrintStream) {
            if (options.dumpRoomNames) {
                printStream.println("Room names (#: ${directory.roomNames.value.size}):")
                directory.roomNames.value.forEach { (room, name) -> printStream.println("$room: $name") }
            }

            if (options.dumpMaximums) {
                printStream.println("Maximums:")
                printStream.println("  Var count: ${directory.maximums.varCount.value}")
                printStream.println("  Unknown 1: ${directory.maximums.unknown1.value}")
                printStream.println("  Bit var count: ${directory.maximums.bitVarCount.value}")
                printStream.println("  Local obj count: ${directory.maximums.localObjCount.value}")
                printStream.println("  Unknown 2: ${directory.maximums.unknown2.value}")
                printStream.println("  Charset count: ${directory.maximums.charsetCount.value}")
                printStream.println("  Unknown 3: ${directory.maximums.unknown3.value}")
                printStream.println("  Unknown 4: ${directory.maximums.unknown4.value}")
                printStream.println("  Inventory count: ${directory.maximums.inventoryCount.value}")
            }

            if (options.dumpRoomEntries) {
                printStream.println("Room entries (#: ${directory.roomEntries.value.size}):")
                directory.roomEntries.value.forEachIndexed { idx, (container, offset) -> printStream.println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpScriptEntries) {
                printStream.println("Script entries (#: ${directory.scriptEntries.value.size}):")
                directory.scriptEntries.value.forEachIndexed { idx, (container, offset) -> printStream.println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpSoundEntries) {
                printStream.println("Sound entries (#: ${directory.soundEntries.value.size}):")
                directory.soundEntries.value.forEachIndexed { idx, (container, offset) -> printStream.println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpCostumeEntries) {
                printStream.println("Costume entries (#: ${directory.costumeEntries.value.size}):")
                directory.costumeEntries.value.forEachIndexed { idx, (container, offset) -> printStream.println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpCharsetEntries) {
                printStream.println("Charset entries (#: ${directory.charsetEntries.value.size}):")
                directory.charsetEntries.value.forEachIndexed { idx, (container, offset) -> printStream.println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpObjectEntries) {
                printStream.println("Object entries (#: ${directory.objectEntries.value.size}):")
                directory.objectEntries.value.forEachIndexed { idx, (ownerState, klass) -> printStream.println("$idx Owner/State: $ownerState, Class: 0x${klass.toHexString()}") }
            }
        }
    }

    object JsonDirectoryDumper : DirectoryDumper {
        override fun dump(directory: DirectoryV5, options: Options, printStream: PrintStream) {
            val model = DirectoryModel.from(directory)
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(model)
            printStream.println(jsonString)
        }

        @Serializable
        data class DirectoryModel(
            val roomNames: Map<Int, String>,
            val maximums: MaximumsV5,
            val rooms: Map<Int, GenericEntry>,
            val scripts: Map<Int, GenericEntry>,
            val sounds: Map<Int, GenericEntry>,
            val costumes: Map<Int, GenericEntry>,
            val charsets: Map<Int, GenericEntry>,
            val objects: Map<Int, ObjectEntry>
        ) {
            fun toDirectory(): DirectoryV5 {
                return DirectoryV5(
                    RoomNames(roomNames),
                    maximums,
                    RoomEntries(toListOfContainerAndOffset(rooms)),
                    ScriptEntries(toListOfContainerAndOffset(scripts)),
                    SoundEntries(toListOfContainerAndOffset(sounds)),
                    CostumeEntries(toListOfContainerAndOffset(costumes)),
                    CharsetEntries(toListOfContainerAndOffset(charsets)),
                    ObjectEntries(toListOfOwnerStateAndClass(objects))
                )
            }

            private fun toListOfOwnerStateAndClass(entries: Map<Int, ObjectEntry>): List<OwnerStateAndClass> {
                val maxId = entries.maxOf { it.key }
                val dummy = OwnerStateAndClass(0, 0)
                val list = MutableList(maxId + 1) { dummy }
                entries.forEach { (idx, entry) -> list[idx] = entry.toOnwerStateAndClass() }
                return list
            }

            private fun toListOfContainerAndOffset(entries: Map<Int, GenericEntry>): List<ContainerAndOffset> {
                val maxId = entries.maxOf { it.key }
                val dummy = ContainerAndOffset(0, 0)
                val list = MutableList(maxId + 1) { dummy }
                entries.forEach { (idx, entry) -> list[idx] = entry.toContainerAndOffset() }
                return list
            }

            @Serializable
            data class GenericEntry(val container: Int, val offset: Int) {
                companion object {
                    fun from(containerAndOffset: ContainerAndOffset): GenericEntry {
                        return GenericEntry(containerAndOffset.container, containerAndOffset.offset)
                    }
                }

                fun toContainerAndOffset(): ContainerAndOffset {
                    return ContainerAndOffset(container, offset)
                }
            }

            @Serializable
            data class ObjectEntry(val ownerState: Int, val klass: Int) {
                fun toOnwerStateAndClass(): OwnerStateAndClass {
                    return OwnerStateAndClass(ownerState, klass)
                }

                companion object {
                    fun from(ownerStateAndClass: OwnerStateAndClass): ObjectEntry {
                        return ObjectEntry(ownerStateAndClass.ownerState, ownerStateAndClass.klass)
                    }
                }
            }

            companion object {
                fun mapGenericEntryByIndex(entries: List<ContainerAndOffset>): Map<Int, GenericEntry> {
                    return entries.mapIndexed { idx, containerAndOffset ->
                        idx to GenericEntry.from(containerAndOffset)
                    }.toMap()
                }

                fun from(directory: DirectoryV5): DirectoryModel {
                    val roomEntryByIndex = mapGenericEntryByIndex(directory.roomEntries.value)
                    val scriptEntryByIndex = mapGenericEntryByIndex(directory.scriptEntries.value)
                    val soundEntryByIndex = mapGenericEntryByIndex(directory.soundEntries.value)
                    val costumeEntryByIndex = mapGenericEntryByIndex(directory.costumeEntries.value)
                    val charsetEntryByIndex = mapGenericEntryByIndex(directory.charsetEntries.value)
                    val objectEntryByIndex = directory.objectEntries.value.mapIndexed { idx, ownerStateAndClass -> idx to ObjectEntry.from(ownerStateAndClass) }.toMap()

                    return DirectoryModel(
                        directory.roomNames.value,
                        directory.maximums,
                        roomEntryByIndex,
                        scriptEntryByIndex,
                        soundEntryByIndex,
                        costumeEntryByIndex,
                        charsetEntryByIndex,
                        objectEntryByIndex
                    )
                }
            }
        }
    }
}

class ScummDirFileV5(path: File) : Closeable {
    val inputStream: InputStream = XorInputStream(BufferedInputStream(FileInputStream(path)))

    override fun close() {
        inputStream.close()
    }
}

sealed interface DirectoryBlockId {
    val blockId4: BlockId4

    abstract class BlockIdBase(override val blockId4: BlockId4) : DirectoryBlockId {
        fun writeTo(dataOutput: DataOutput) {
            blockId4.writeTo(dataOutput)
        }
    }

    object RNAM : BlockIdBase(BlockId4("RNAM"))
    object MAXS : BlockIdBase(BlockId4("MAXS"))
    object DROO : BlockIdBase(BlockId4("DROO"))
    object DSCR : BlockIdBase(BlockId4("DSCR"))
    object DSOU : BlockIdBase(BlockId4("DSOU"))
    object DCOS : BlockIdBase(BlockId4("DCOS"))
    object DCHR : BlockIdBase(BlockId4("DCHR"))
    object DOBJ : BlockIdBase(BlockId4("DOBJ"))
}


sealed interface NewDirBlockHandler {
    fun handle(blockHeader: BlockHeaderV5, blockData: DataInput, builder: DirectoryBuilder)
}

abstract class NewDirBlockHandlerBase(
    private val read: (DataInput) -> List<ContainerAndOffset>,
    private val add: (List<ContainerAndOffset>, DirectoryBuilder) -> Unit
) : NewDirBlockHandler {
    override fun handle(
        blockHeader: BlockHeaderV5,
        blockData: DataInput,
        builder: DirectoryBuilder
    ) {
        val entries = read(blockData)
        add(entries, builder)
    }
}

object RnamHandler : NewDirBlockHandler {
    override fun handle(
        blockHeader: BlockHeaderV5,
        blockData: DataInput,
        builder: DirectoryBuilder
    ) {
        var room = blockData.readUnsignedByte()

        val roomNames = buildMap {
            while (room != 0) {
                val buffer = ByteArray(10)
                blockData.readFully(buffer, 0, 9)
                buffer.forEachIndexed { idx, byte -> if (idx < 9) buffer[idx] = byte.xor(0xff.toByte()) }
                val nullIndex = buffer.indexOf(0)
                val endIndex = if (nullIndex >= 0) nullIndex else buffer.size
                val roomName = buffer.decodeToString(0, endIndex)
                put(room, roomName)

                room = blockData.readUnsignedByte()
            }
        }

        builder.roomNames(roomNames)
    }
}

object DscrHandler : NewDirBlockHandlerBase(
    ::defaultReadDirectoryEntries,
    { entries, builder -> builder.scriptEntries(entries) })

object DrooHandler : NewDirBlockHandler {
    override fun handle(
        blockHeader: BlockHeaderV5,
        blockData: DataInput,
        builder: DirectoryBuilder
    ) {
        val entries = defaultReadDirectoryEntries(blockData)
        builder.roomEntries(entries)
    }

}

object DcosHandler : NewDirBlockHandlerBase(
    ::defaultReadDirectoryEntries,
    { entries, builder -> builder.costumeEntries(entries) }
)

object DchrHandler : NewDirBlockHandlerBase(
    ::defaultReadDirectoryEntries,
    { entries, builder -> builder.charsetEntries(entries) }
)

object DsouHandler : NewDirBlockHandlerBase(
    ::defaultReadDirectoryEntries,
    { entries, builder -> builder.soundEntries(entries) }
)

object DobjHandler : NewDirBlockHandler {
    override fun handle(
        blockHeader: BlockHeaderV5,
        blockData: DataInput,
        builder: DirectoryBuilder
    ) {
        val entries = readObjectDirectoryEntries(blockData)
        builder.objectEntries(entries)
    }
}

data class DirectoryV5(
    val roomNames: RoomNames,
    val maximums: MaximumsV5,
    val roomEntries: RoomEntries,
    val scriptEntries: ScriptEntries,
    val soundEntries: SoundEntries,
    val costumeEntries: CostumeEntries,
    val charsetEntries: CharsetEntries,
    val objectEntries: ObjectEntries
) {
    init {
        require(roomNames.value.size <= roomEntries.value.size) {
            val roomNamesCount = roomNames.value.size
            val roomEntriesCount = roomEntries.value.size
            "Room names count ($roomNamesCount) must not exceed room entries count ($roomEntriesCount)"
        }

        require(maximums.charsetCount.value == charsetEntries.value.size) {
            val charsetCountMaximum = maximums.charsetCount.value
            val charsetEntriesCount = charsetEntries.value.size
            "Charset count maximum ($charsetCountMaximum) must match charset entries count ($charsetEntriesCount)" }
    }
}

@JvmInline
@Serializable
value class ResourceCount(val value: Int) {
    init {
        require(value >= 0) { "Resource count must be >= 0" }
    }

    fun writeTo(dataOutput: DataOutput) {
        dataOutput.writeShortLittleEndian(value.toShort())
    }

    companion object {
        fun readFrom(dataInput: DataInput): ResourceCount {
            return ResourceCount(dataInput.readShortLittleEndian().toInt())
        }
    }
}

@Serializable
data class MaximumsV5(
    val varCount: ResourceCount,
    val unknown1: ResourceCount,
    val bitVarCount: ResourceCount,
    val localObjCount: ResourceCount,
    val unknown2: ResourceCount,
    val charsetCount: ResourceCount,
    val unknown3: ResourceCount,
    val unknown4: ResourceCount,
    val inventoryCount: ResourceCount
) {
    fun writeTo(dataOutput: DataOutput) {
        varCount.writeTo(dataOutput)
        unknown1.writeTo(dataOutput)
        bitVarCount.writeTo(dataOutput)
        localObjCount.writeTo(dataOutput)
        unknown2.writeTo(dataOutput)
        charsetCount.writeTo(dataOutput)
        unknown3.writeTo(dataOutput)
        unknown4.writeTo(dataOutput)
        inventoryCount.writeTo(dataOutput)
    }

    companion object {
        fun readFrom(dataInput: DataInput): MaximumsV5 {
            val varCount = dataInput.readShortLittleEndian().toInt()
            val unknown1 = dataInput.readShortLittleEndian().toInt()
            val bitVarCount = dataInput.readShortLittleEndian().toInt()
            val localObjCount = dataInput.readShortLittleEndian().toInt()
            val unknown2 = dataInput.readShortLittleEndian().toInt()
            val charsetCount = dataInput.readShortLittleEndian().toInt()
            val unknown3 = dataInput.readShortLittleEndian().toInt()
            val unknown4 = dataInput.readShortLittleEndian().toInt()
            val inventoryCount = dataInput.readShortLittleEndian().toInt()

            val maximums = MaximumsV5(
                ResourceCount(varCount),
                ResourceCount(unknown1),
                ResourceCount(bitVarCount),
                ResourceCount(localObjCount),
                ResourceCount(unknown2),
                ResourceCount(charsetCount),
                ResourceCount(unknown3),
                ResourceCount(unknown4),
                ResourceCount(inventoryCount)
            )

            return maximums
        }
    }
}

@JvmInline
value class RoomNames(val value: Map<Int, String>) {
    val hasAnyRoomNames: Boolean get() = value.isNotEmpty()

    fun writeXorEncodedTo(dataOutput: DataOutput, xorCode: Byte) {
        value.forEach { (room, name) ->
            dataOutput.writeByte(room)

            val buffer = ByteArray(9) { _ -> 0 }
            val endIndex = minOf(name.length, 9)
            name.toByteArray(charset = Charsets.US_ASCII).copyInto(buffer, 0, 0, endIndex)
            for (i in buffer.indices) { buffer[i] = buffer[i].xor(xorCode) }
            dataOutput.write(buffer, 0, 9)
        }

        dataOutput.writeByte(0)
    }
}

@JvmInline
value class RoomEntries(val value: List<ContainerAndOffset>)

@JvmInline
value class ScriptEntries(val value: List<ContainerAndOffset>)

@JvmInline
value class SoundEntries(val value: List<ContainerAndOffset>)

@JvmInline
value class CostumeEntries(val value: List<ContainerAndOffset>)

@JvmInline
value class CharsetEntries(val value: List<ContainerAndOffset>)

@JvmInline
value class ObjectEntries(val value: List<OwnerStateAndClass>)

class DirectoryBuilder {
    var maximums: MaximumsV5? = null
    val roomNames = mutableMapOf<Int, String>()
    var roomEntries: List<ContainerAndOffset> = emptyList()
    var scriptEntries: List<ContainerAndOffset> = emptyList()
    var soundEntries: List<ContainerAndOffset> = emptyList()
    var costumeEntries: List<ContainerAndOffset> = emptyList()
    var charsetEntries: List<ContainerAndOffset> = emptyList()
    var objectEntries: List<OwnerStateAndClass> = emptyList()

    fun maximums(maximums: MaximumsV5) {
        this.maximums = maximums
    }

    fun roomNames(roomNames: Map<Int, String>) {
        this.roomNames.putAll(roomNames)
    }

    fun objectEntries(entries: List<OwnerStateAndClass>) {
        this.objectEntries = entries
    }

    fun roomEntries(entries: List<ContainerAndOffset>) {
        this.roomEntries = entries
    }

    fun scriptEntries(entries: List<ContainerAndOffset>) {
        this.scriptEntries = entries
    }

    fun soundEntries(entries: List<ContainerAndOffset>) {
        this.soundEntries = entries
    }

    fun costumeEntries(entries: List<ContainerAndOffset>) {
        this.costumeEntries = entries
    }

    fun charsetEntries(entries: List<ContainerAndOffset>) {
        this.charsetEntries = entries
    }

    fun build(): DirectoryV5 {
        require(this.maximums != null) { "Maximums must be initialized" }

        return DirectoryV5(
            RoomNames(roomNames),
            maximums!!,
            RoomEntries(roomEntries),
            ScriptEntries(scriptEntries),
            SoundEntries(soundEntries),
            CostumeEntries(costumeEntries),
            CharsetEntries(charsetEntries),
            ObjectEntries(objectEntries)
        )
    }
}

typealias DirBlockHandler = (BlockId4, DataFileBlockLength, DataInput) -> Unit


fun defaultDumpDirectoryBlock(
    blockId: DirectoryBlockId,
    blockLength: DataFileBlockLength,
    data: DataInput,
    formatEntry: (ContainerAndOffset) -> String = { (container, offset) -> "Container: $container, Offset: $offset" }
) {
    log.debug { "block ID: $blockId, length: $blockLength" }

    val entries = defaultReadDirectoryEntries(data)
    entries.forEachIndexed { idx, entry -> println("$idx ${formatEntry(entry)}") }
}

data class ContainerAndOffset(val container: Int, val offset: Int) {
    fun writeTo(dataOutput: DataOutput) {
        TODO("Not yet implemented")
    }
}

data class OwnerStateAndClass(val ownerState: Int, val klass: Int)

private fun defaultReadDirectoryEntries(data: DataInput): List<ContainerAndOffset> {
    val itemCount = data.readShortLittleEndian().toInt()
    require(itemCount >= 0) { "itemCount must be >= 0" }

    log.debug { "# entries: $itemCount" }

    val containers = buildList(itemCount) {
        repeat(itemCount) { add(data.readUnsignedByte()) }
    }

    val offsets = buildList(itemCount) {
        repeat(itemCount) { add(data.readIntLittleEndian()) }
    }

    return containers.zip(offsets).map { (container, offset) -> ContainerAndOffset(container, offset) }
}

private fun readObjectDirectoryEntries(data: DataInput): List<OwnerStateAndClass> {
    return defaultReadDirectoryEntries(data).map { OwnerStateAndClass(it.container, it.offset) }
}





fun parseDirectory(
    input: InputStream,
    blockHandlers: Map<BlockId4, DirBlockHandler>,
    builder: DirectoryBuilder
) {
    val pushbackInput = PushbackInputStream(input)
    val dataInput = DataInputStream(pushbackInput)

    var readByte = dataInput.read()

    while (readByte != -1) {
        pushbackInput.unread(readByte)

        val blockHeader = BlockHeaderV5.readFrom(dataInput)
        val blockId = blockHeader.blockId

        log.debug { "parseDirectory, block ID: $blockId, length: ${blockHeader.blockLength}" }

        val blockLength = blockHeader.blockLength.value

        if (!blockHandlers.containsKey(blockId)) {
            System.err.println("unhandled block ID: $blockId")
            return
        }

        val blockData = ByteArray(blockLength - 8)
        dataInput.readFully(blockData)

        DataInputStream(ByteArrayInputStream(blockData)).use {
            blockHandlers[blockId]?.invoke(blockId, blockLength, it)
        }

        readByte = dataInput.read()
    }
}
