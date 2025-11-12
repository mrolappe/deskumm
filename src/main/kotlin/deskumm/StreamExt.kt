package deskumm

import java.io.DataInput
import java.io.DataOutputStream

fun DataInput.readShortLittleEndian(): Short {
    return java.lang.Short.reverseBytes(readShort())
}

fun DataInput.readIntLittleEndian(): Int {
    return Integer.reverseBytes(readInt())
}

fun DataOutputStream.writeShortLittleEndian(value: Short) {
    writeShort(java.lang.Short.reverseBytes(value).toInt())
}

fun DataOutputStream.writeIntLittleEndian(value: Int) {
    writeInt(Integer.reverseBytes(value))
}
