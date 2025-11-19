package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.*
import java.nio.charset.Charset

fun main(args: Array<String>) = DumpDirFileCommand().main(args)

class DumpDirFileCommand : CliktCommand() {
    val sourcePath by argument(name = "source-path", help = "Source path")

    override fun run() {
        val inputStream = File(sourcePath).xorInputStream()

        inputStream.use {
            val blockHandlers = hashMapOf<DirectoryBlockId, DirBlockHandler>(
                DirectoryBlockId.RNAM to defaultBlockHandler,
                DirectoryBlockId.MAXS to maxsBlockHandler,
                DirectoryBlockId.DROO to drooBlockHandler,
                DirectoryBlockId.DSCR to dscrBlockHandler,
                DirectoryBlockId.DSOU to defaultBlockHandler,
                DirectoryBlockId.DCOS to defaultGlobDirBlockHandler,
                DirectoryBlockId.DCHR to defaultGlobDirBlockHandler,
                DirectoryBlockId.DOBJ to dobjBlockHandler)

            parseDirectory(it, blockHandlers)
        }
    }
}


enum class DirectoryBlockId {
    RNAM,
    MAXS,
    DROO,
    DSCR,
    DSOU,
    DCOS,
    DCHR,
    DOBJ
}

typealias DirBlockHandler = (DirectoryBlockId, DataFileBlockLength, DataInput) -> Unit

val defaultBlockHandler = { blockId: DirectoryBlockId, blockLength: DataFileBlockLength, data: DataInput ->
    println("Block: $blockId, Länge: $blockLength")
}

val maxsBlockHandler = { blockId: DirectoryBlockId, blockLength: DataFileBlockLength, data: DataInput ->
    println("Block: $blockId, Länge: $blockLength")

    val varCount = data.readShortLittleEndian().toUInt()
    val unknown = data.readShortLittleEndian().toUInt()
    val bitVarCount = data.readShortLittleEndian().toUInt()
    val localObjCount = data.readShortLittleEndian().toUInt()
    val unknown2 = data.readShortLittleEndian().toUInt()
    val numCharsets = data.readShortLittleEndian().toUInt()
    val unknown3 = data.readShortLittleEndian().toUInt()
    val unknown4 = data.readShortLittleEndian().toUInt()
    val inventoryCount = data.readShortLittleEndian().toUInt()

    println("varCount: $varCount")
    println("unknown: $unknown")
    println("bitVarCount: $bitVarCount")
    println("localObjCount: $localObjCount")
    println("unknown2: $unknown2")
    println("numCharsets: $numCharsets")
    println("unknown3: $unknown3")
    println("unknown4: $unknown4")
    println("inventoryCount: $inventoryCount")
}

val drooBlockHandler = { blockId: DirectoryBlockId, blockLength: DataFileBlockLength, data: DataInput ->
    println("Block: $blockId, Länge: $blockLength")

    val roomCount = data.readShortLittleEndian().toInt()
    println("Anzahl Räume: ${roomCount}")

    repeat(roomCount) {
        val roomNumber = data.readByte().toInt()
        println("Raumnummer: $roomNumber")
    }

    repeat(roomCount) {
        val roomOffset = data.readIntLittleEndian()
        println("Raumoffset: $roomOffset")
    }
}

val dscrBlockHandler = { blockId: DirectoryBlockId, blockLength: DataFileBlockLength, data: DataInput ->
    println("Block: $blockId, Länge: $blockLength")

    val scriptCount = data.readShortLittleEndian().toInt()
    println("Anzahl Scripts: ${scriptCount}")

    repeat(scriptCount) {
        val roomNumber = data.readByte().toInt()
        println("Raumnummer: $roomNumber")
    }

    repeat(scriptCount) {
        val roomOffset = data.readIntLittleEndian()
        println("Raumoffset: $roomOffset")
    }
}

val defaultGlobDirBlockHandler = { blockId: DirectoryBlockId, blockLength: DataFileBlockLength, data: DataInput ->
    println("Block: $blockId, Länge: $blockLength")

    val itemCount = data.readShortLittleEndian().toInt()
    println("Anzahl: ${itemCount}")

    repeat(itemCount) {
        val roomNumber = data.readByte().toInt()
        println("Raumnummer: $roomNumber")
    }

    repeat(itemCount) {
        val roomOffset = data.readIntLittleEndian()
        println("Raumoffset: $roomOffset")
    }
}

val dobjBlockHandler = { blockId: DirectoryBlockId, blockLength: DataFileBlockLength, data: DataInput ->
    println("Block: $blockId, Länge: $blockLength")

    val itemCount = data.readShortLittleEndian().toInt()
    println("Anzahl: ${itemCount}")

    repeat(itemCount) {
        val objectOwnerState = data.readByte().toInt()
        println("object owner/state: $objectOwnerState")
    }

    repeat(itemCount) {
        val objectClass = data.readIntLittleEndian()
        println("Raumoffset: $objectClass")
    }
}



fun parseDirectory(stream: InputStream, blockHandlers: Map<DirectoryBlockId, DirBlockHandler>): Unit {
    var done = false

    while (!done) {
        var blockId = stream.readBlockId()

        if (blockId == null) {
            break
        }

        var blockLength = stream.readBlockLength()

        if (!blockHandlers.containsKey(blockId)) {
            System.err.println("unhandled block ID: $blockId")
            return
        }

        val blockData = ByteArray(blockLength - 8)
        stream.read(blockData)

        DataInputStream(ByteArrayInputStream(blockData)).use {
            blockHandlers[blockId]?.invoke(blockId, blockLength, it)
        }
    }
}

fun InputStream.readBlockId(): DirectoryBlockId? {
    val blockIdBytes = ByteArray(4)

    val numBytesRead = this.read(blockIdBytes)

    if (numBytesRead < 4) {
        return null
    } else {
        return DirectoryBlockId.valueOf(blockIdBytes.toString(Charset.defaultCharset()))
    }
}

fun InputStream.readBlockLength(): DataFileBlockLength {
    val data = DataInputStream(this)
    return data.readInt()
}


const val MINIMUM_BLOCK_SIZE: Int = 512
const val DEFAULT_BLOCK_SIZE: Int = 4096

fun File.xorInputStream(code: Byte = 0x69): XorInputStream {
    return XorInputStream(FileInputStream(this), code)
}

fun InputStream.xor(code: Byte = 0x69): XorInputStream {
    return XorInputStream(this, code)
}

fun InputStream.forEachBlock(blockSize: Int, action: (buffer: ByteArray, bytesRead: Int) -> Unit): Unit {
    val arr = ByteArray(blockSize.coerceAtLeast(MINIMUM_BLOCK_SIZE))

    this.use { input ->
        do {
            val size = input.read(arr)
            if (size <= 0) {
                break
            } else {
                action(arr, size)
            }
        } while (true)
    }
}
