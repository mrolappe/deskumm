fun main(args: Array<String>) {
    val skrepl = Skrepl
    skrepl.repl()
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
