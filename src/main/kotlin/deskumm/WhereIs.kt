package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path

fun main(args: Array<String>) = WhereIsCommand().main(args)

class WhereIsCommand : CliktCommand("where-is") {
    val dirFile by argument(name = "dir-file").path(mustExist = true)
    val type by argument(name = "type")
    val number by argument(name = "number")

    override fun run() {
        DirectoryFileV5.parse(dirFile)
    }
}