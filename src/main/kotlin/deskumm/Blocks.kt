package deskumm

import java.io.DataInput
import java.io.DataOutput
import kotlin.experimental.xor

data class BlockId4(val bytes: ByteArray) {
    constructor(name: String) : this(name.toByteArray())

    init {
        require(bytes.size == 4)
    }

    fun matches(other: String) = this.bytes.contentEquals(other.toByteArray())

    fun asString(): String {
        return bytes.decodeToString()
    }

    override fun toString(): String {
        return bytes.decodeToString()
    }

    companion object {
        fun readFrom(input: DataInput, xorCode: Byte = 0): BlockId4 {
            val buffer = ByteArray(4)
            input.readFully(buffer)

            if (xorCode != 0.toByte()) {
                buffer.sliceArray(0..< 4).mapIndexed { index, byte -> buffer[index] = byte.xor(xorCode) }
            }

            return BlockId4(buffer)
        }
    }
}

@JvmInline
value class BlockLengthV5(val value: Int) {
    init {
        require(value > 0) { "Block length must be positive" }
    }

    companion object {
        fun readFrom(input: DataInput): BlockLengthV5 = BlockLengthV5(input.readInt())
    }
}

@JvmInline
value class ContentLengthV5(val value: Int) {
    init {
        require(value > 0) { "Content length must be positive" }
    }
}

data class BlockHeaderV5(val blockId: BlockId4, val blockLength: BlockLengthV5) {
    fun writeTo(out: DataOutput) {
        out.write(blockId.bytes)
        out.writeInt(blockLength.value)
    }

    val contentLength: ContentLengthV5
        get() = ContentLengthV5(blockLength.value - BLOCK_HEADER_BYTE_COUNT)

    companion object {
        const val BLOCK_HEADER_BYTE_COUNT = 8

        fun readFrom(input: DataInput, xorCode: Byte = 0): BlockHeaderV5 {
            val blockId = BlockId4.readFrom(input, xorCode)
            val blockLength = BlockLengthV5(input.readInt())
            return BlockHeaderV5(blockId, blockLength)
        }
    }
}

class RawBlockV5(override val blockId: BlockId4, val content: ByteArray) : BlockV5 {
    override fun writeTo(out: DataOutput) {
        val length = 8 + content.size
        out.write(blockId.bytes)
        out.writeInt(length)
        out.write(content)
    }

    override val blockLength: BlockLengthV5
        get() = BlockLengthV5(8 + content.size)
}