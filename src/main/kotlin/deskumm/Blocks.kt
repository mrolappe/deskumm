package deskumm

import java.io.DataInput

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
        fun readFrom(input: DataInput): BlockId4 {
            val buffer = ByteArray(4)
            input.readFully(buffer)
            return BlockId4(buffer)
        }
    }
}

@JvmInline
value class BlockLengthV5(val value: Int) {
    init {
        require(value > 0) { "Block length must be positive" }
    }
}

data class BlockHeaderV5(val blockId: BlockId4, val blockLength: BlockLengthV5)