package jmj.deskumm.decodefile.xor96

import java.io.File
import kotlin.experimental.xor
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        showUsage()
        exitProcess(5)
    }

    val sourcePath = args[0]
    val destPath = args[1]

    val outputStream = File(destPath).outputStream()

    File(sourcePath).forEachBlock { buffer, bytesRead ->
        buffer.forEachIndexed { index, byte -> buffer[index] = byte.xor(0x96.toByte()) }
        outputStream.write(buffer, 0, bytesRead)
        println("block, $bytesRead")
    }
}

fun showUsage() {
    println("Aufruf: DecodeFileXOR96 <Quelldatei> <Zieldatei>")
}