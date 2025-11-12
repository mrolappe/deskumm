package deskumm

import java.io.OutputStream

class XorOutputStream(private val destination: OutputStream, private val code: Byte = 0x69) : OutputStream() {
    override fun write(byte: Int) {
        destination.write(byte.xor(code.toInt()))
    }
}