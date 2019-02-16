package jmj.deskumm

enum class DecompiledInstruction(val stringRepresentation: String, val length: UInt) {
    END_OBJECT("end-object", 1u)
}

fun main(args: Array<String>) {
    println("deskumm")
}


fun decompile(data: ByteArray): DecompiledInstruction? {
    if (data[0].toUInt() == 0u) {
        return DecompiledInstruction.END_OBJECT
    } else {
        return null
    }
}