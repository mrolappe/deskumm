package deskumm

import java.io.DataInput
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.RandomAccessFile

fun DataInput.readShortLittleEndian(): Short {
    return java.lang.Short.reverseBytes(readShort())
}

fun DataInput.readIntLittleEndian(): Int {
    return Integer.reverseBytes(readInt())
}

fun DataOutput.writeShortLittleEndian(value: Short) {
    writeShort(java.lang.Short.reverseBytes(value).toInt())
}

fun DataOutput.writeIntLittleEndian(value: Int) {
    writeInt(Integer.reverseBytes(value))
}

fun DataInput.readIntLittleEndianXorEncoded(): Int {
    return readIntLittleEndian().xor(0x69696969)
}
