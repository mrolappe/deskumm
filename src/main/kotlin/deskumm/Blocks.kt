package deskumm

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.DataInput
import java.io.DataOutput
import kotlin.experimental.xor

private val logger = KotlinLogging.logger {}

data class BlockId4(val name: String) {
    constructor(bytes: ByteArray) : this(bytes.decodeToString())

    init {
        require(name.length == 4)
    }

    fun matches(other: BlockId4) = name == other.name

    fun matches(other: String) = name == other

    fun asString(): String = name

    override fun toString(): String = name

    fun writeTo(out: DataOutput) {
        out.write(name.toByteArray())
    }

    companion object {
        fun readFrom(input: DataInput): BlockId4 {
            val buffer = ByteArray(4)
            input.readFully(buffer)
            return BlockId4(buffer)
        }

        fun readFrom(input: DataInput, xorCode: Byte): BlockId4 {
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
        blockId.writeTo(out)
        out.writeInt(blockLength.value)
    }

    val contentLength: ContentLengthV5
        get() = ContentLengthV5(blockLength.value - BLOCK_HEADER_BYTE_COUNT)

    companion object {
        const val BLOCK_HEADER_BYTE_COUNT = 8

        fun readFrom(input: DataInput): BlockHeaderV5 {
            val blockId = BlockId4.readFrom(input)
            val blockLength = BlockLengthV5(input.readInt())
            return BlockHeaderV5(blockId, blockLength)
        }

        fun readFrom(input: DataInput, xorCode: Byte): BlockHeaderV5 {
            val blockId = BlockId4.readFrom(input, xorCode)
            val blockLength = BlockLengthV5(input.readInt().xor(xorInt(xorCode)))
            return BlockHeaderV5(blockId, blockLength)
        }
    }
}

infix fun BlockId4.isExpectedToBe(expectedBlockId: BlockId4) {
    logger.debug { "expecting block ID $expectedBlockId, got $this" }

    if (!this.matches(expectedBlockId)) {
        throw IllegalArgumentException("Expected block ID $expectedBlockId, but got ${this.asString()}")
    }
}

fun BlockHeaderV5.expectBlockId(expectedBlockId: BlockId4) {
    this.blockId isExpectedToBe expectedBlockId
}

fun expectAndSeekToEndOfBlock(input: DataInput, expectedBlockId: BlockId4) {
    val blockHeader = BlockHeaderV5.readFrom(input)
    blockHeader.expectBlockId(expectedBlockId)
    input.skipBytes(blockHeader.contentLength.value)
}

class RawBlockV5(override val blockId: BlockId4, val content: ByteArray) : BlockV5 {
    companion object {
        fun readFrom(input: DataInput, xorCode: Byte = 0): RawBlockV5 {
            val blockHeader = BlockHeaderV5.readFrom(input, xorCode)
            val content = ByteArray(blockHeader.contentLength.value)
            input.readFully(content)
            return RawBlockV5(blockHeader.blockId, content)
        }
    }
    override fun writeTo(out: DataOutput) {
        val length = 8 + content.size
        blockId.writeTo(out)
        out.writeInt(length)
        out.write(content)
    }

    override val blockLength: BlockLengthV5
        get() = BlockLengthV5(8 + content.size)
}