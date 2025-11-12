package deskumm

import java.io.InputStream

class XorInputStream(private val source: InputStream, private val code: Byte = 0x69) : InputStream() {
    override fun read(): Int {
        val byteRead = source.read()

        return when (byteRead) {
            -1 ->
                -1
            else ->
                byteRead.xor(code.toInt()).and(0xff)
        }

    }
}