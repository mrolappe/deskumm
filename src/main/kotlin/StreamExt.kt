package jmj.deskumm

import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutputStream

inline fun DataInput.readShortLittleEndian(): Short {
    return java.lang.Short.reverseBytes(readShort())
}

inline fun DataInput.readIntLittleEndian(): Int {
    return Integer.reverseBytes(readInt())
}

inline fun DataOutputStream.writeShortLittleEndian(value: Short) {
    writeShort(java.lang.Short.reverseBytes(value).toInt())
}

inline fun DataOutputStream.writeIntLittleEndian(value: Int) {
    writeInt(Integer.reverseBytes(value))
}
