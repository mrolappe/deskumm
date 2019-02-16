package jmj.deskumm

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
        buffer.forEachIndexed { index, byte -> buffer[index] = byte.xor(0x69) }
        outputStream.write(buffer, 0, bytesRead)
        println("block, $bytesRead")
    }
}

fun showUsage() {
    println("Aufruf: DecodeFileXOR69 <Quelldatei> <Zieldatei>")
}