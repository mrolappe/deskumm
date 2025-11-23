package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.*
import kotlin.experimental.xor

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = DumpDirFileCommand().main(args)

class DumpDirFileCommand : CliktCommand() {
    val sourcePath by argument(name = "source-path", help = "Source path").file(mustExist = true, canBeDir = false)
    val dumpAsJson by option("--dump-as-json", help = "Dump as JSON").flag(default = false)
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
            val varCount = data.readShortLittleEndian().toInt()
            val unknown1 = data.readShortLittleEndian().toInt()
            val bitVarCount = data.readShortLittleEndian().toInt()
            val localObjCount = data.readShortLittleEndian().toInt()
            val unknown2 = data.readShortLittleEndian().toInt()
            val charsetCount = data.readShortLittleEndian().toInt()
            val unknown3 = data.readShortLittleEndian().toInt()
            val unknown4 = data.readShortLittleEndian().toInt()
            val inventoryCount = data.readShortLittleEndian().toInt()

            val maximums = Maximums(
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

            builder.maximums(maximums)
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
        val dumper = if (dumpAsJson) { DirectoryDumper.JsonDirectoryDumper } else { DirectoryDumper.TextDirectoryDumper }

        dumper.dump(
            directory, DirectoryDumper.Options(
                dumpRoomNames,
                dumpMaximums,
                dumpRoomEntries,
                dumpScriptEntries,
                dumpSoundEntries,
                dumpCostumeEntries,
                dumpCharsetEntries,
                dumpObjectEntries
            )
        )
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

    fun dump(directory: DirectoryV5, options: Options)

    object TextDirectoryDumper : DirectoryDumper {
        override fun dump(directory: DirectoryV5, options: Options) {
            if (options.dumpRoomNames) {
                println("Room names (#: ${directory.roomNames.value.size}):")
                directory.roomNames.value.forEach { (room, name) -> println("$room: $name") }
            }

            if (options.dumpMaximums) {
                // TODO
            }

            if (options.dumpRoomEntries) {
                println("Room entries (#: ${directory.roomEntries.value.size}):")
                directory.roomEntries.value.forEachIndexed { idx, (container, offset) -> println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpScriptEntries) {
                println("Script entries (#: ${directory.scriptEntries.value.size}):")
                directory.scriptEntries.value.forEachIndexed { idx, (container, offset) -> println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpSoundEntries) {
                println("Sound entries (#: ${directory.soundEntries.value.size}):")
                directory.soundEntries.value.forEachIndexed { idx, (container, offset) -> println("$idx Container: $container, Offset: $offset") }
            }
            if (options.dumpCostumeEntries) {
                println("Costume entries (#: ${directory.costumeEntries.value.size}):")
                directory.costumeEntries.value.forEachIndexed { idx, (container, offset) -> println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpCharsetEntries) {
                println("Charset entries (#: ${directory.charsetEntries.value.size}):")
                directory.charsetEntries.value.forEachIndexed { idx, (container, offset) -> println("$idx Container: $container, Offset: $offset") }
            }

            if (options.dumpObjectEntries) {
                println("Object entries (#: ${directory.objectEntries.value.size}):")
                directory.objectEntries.value.forEachIndexed { idx, (ownerState, klass) -> println("$idx Owner/State: $ownerState, Class: 0x${klass.toHexString()}") }
            }
        }
    }

    object JsonDirectoryDumper : DirectoryDumper {
        override fun dump(directory: DirectoryV5, options: Options) {
            TODO("Not yet implemented")
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

    abstract class BlockIdBase(override val blockId4: BlockId4) : DirectoryBlockId
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
    val maximums: Maximums,
    val roomEntries: RoomEntries,
    val scriptEntries: ScriptEntries,
    val soundEntries: SoundEntries,
    val costumeEntries: CostumeEntries,
    val charsetEntries: CharsetEntries,
    val objectEntries: ObjectEntries
)

@JvmInline
value class ResourceCount(val value: Int) {
    init {
        require(value >= 0) { "Resource count must be >= 0" }
    }
}

data class Maximums(
    val varCount: ResourceCount,
    val unknown1: ResourceCount,
    val bitVarCount: ResourceCount,
    val localObjCount: ResourceCount,
    val unknown2: ResourceCount,
    val charsetCount: ResourceCount,
    val unknown3: ResourceCount,
    val unknown4: ResourceCount,
    val inventoryCount: ResourceCount
)

@JvmInline
value class RoomNames(val value: Map<Int, String>)

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
    var maximums: Maximums? = null
    val roomNames = mutableMapOf<Int, String>()
    var roomEntries: List<ContainerAndOffset> = emptyList()
    var scriptEntries: List<ContainerAndOffset> = emptyList()
    var soundEntries: List<ContainerAndOffset> = emptyList()
    var costumeEntries: List<ContainerAndOffset> = emptyList()
    var charsetEntries: List<ContainerAndOffset> = emptyList()
    var objectEntries: List<OwnerStateAndClass> = emptyList()

    fun maximums(maximums: Maximums) {
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

data class ContainerAndOffset(val container: Int, val offset: Int)

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
