import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) = SkreplCommand().main(args)

class SkreplCommand : CliktCommand() {
    override fun run() {
        val tmpBla = TmpBla("bla", 123)
        val json = Json.encodeToString(tmpBla)
        println("tmp bla: $json")
        val skrepl = Skrepl
        skrepl.repl()
    }
}

object Skrepl {
    private var done = false

    fun println(string: String) = kotlin.io.println(string)

    fun repl() {
        while (!done) {
            print(">> ")
            val line = readLine() ?: continue

            val command = parseCommand(line)
            command.execute(this)
        }
    }

    fun quit() {
        done = true
    }
}

@Serializable
data class TmpBla(val kling: String, val klang: Int)

sealed class Command(val help: String) {
    abstract fun execute(skrepl: Skrepl)
}

object QuitCommand : Command(help = "quit skrepl") {
    override fun execute(skrepl: Skrepl) {
        skrepl.println("quitting")
        skrepl.quit()
    }
}

object HelpCommand : Command(help = "list commands") {
    val commands = arrayOf("ene", "mene", "mu")

    override fun execute(skrepl: Skrepl) {
        commands.forEach { skrepl.println(it) }
    }
}

fun parseCommand(line: String): Command =
    if (line.startsWith("help")) {
        HelpCommand
    } else {
        QuitCommand
    }
