package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.*


fun main(args: Array<String>) = DeskummCommand().main(args)

class DeskummCommand : CliktCommand() {
    val scriptPath by argument(name = "script-path")

    override fun run() {
        val script = loadScript(File(scriptPath))

        if (script == null) {
            System.err.println("Scriptdatei $scriptPath konnte nicht geladen werden")
        } else {
            val opcodes = decodeInstructionsFromScriptBytes(script)
            opcodes.forEach {
                val (offset, opcode) = it
                println("%6d %s".format(offset, opcode.toSource()))
            }
        }
    }
}

sealed interface Instruction {
    fun toSource(): String
    val length: Int
}

abstract class Opcode(val name: String, override val length: Int) : Instruction {
    override fun toSource(): String = name
}

object EndObject : Opcode("end-object", 1)

class InvalidInstruction(val opcode: ByteArray) : Opcode("invalid-opcode", 1) {
    constructor(opcode: Byte) : this(byteArrayOf(opcode))

    override fun toSource(): String {
        val opcodeHex = opcode.joinToString(" ") { "%02x".format(it) }
        return "invalid-opcode $opcodeHex"
    }
}

class DrawObjectAtOpcode(val obj: Int, val x: Int, val y: Int) : Opcode("draw-object at", 7) {
    override fun toSource(): String = "draw-object $obj at $x, $y"
}

class DrawObjectImageOpcode(val obj: Int, val img: Int) : Opcode("draw-object image", 4) {
    override fun toSource(): String = "draw-object $obj image $img"
}

class StartMusicInstr(val sound: ByteParam) : Opcode("start-music", 3)  {

}

// 0x04
class IfVarLessOrEqualOpcode(val varSpec: VarSpec, val value: Int, val skipOffset: Int)
    : Opcode("if (@var <= @wert)", 7) {
    override fun toSource(): String {
        return "if ${nameForVar(varSpec)} <= $value (else jump $skipOffset)"
    }
}

class ActorId(val id: Int) {
    fun toSourceName(): String = "actor$id"
}

class ObjId(val id: Int) {
    fun toSourceName(): String = "obj$id"
}

// 0x09
class ActorFaceTowardsOpcode(val actor: ByteParam, val obj: WordParam)
    : Opcode("actor @actor face-towards @obj", 4) {     // TODO ausprägung mit varparam
    override fun toSource(): String {
        return "actor ${actor} face-towards ${obj}"
    }
}

// 0x0c01
class LoadScriptOpcode(val script: Int) : Opcode("load-script", 3) {
    override fun toSource(): String = "load-script $script"
}

// 0x0c02
class LoadSoundOpcode(val sound: Int) : Opcode("load-sound", 3) {
    override fun toSource(): String = "load-sound $sound"
}

// 0x0c03
class LoadCostumeOpcode(val costume: Int) : Opcode("load-costume", 3) {
    override fun toSource(): String = "load-costume $costume"
}

// 0x0c04
class LoadRoomOpcode(val room: Int) : Opcode("load-room", 3) {
    override fun toSource(): String {
        return "load-room $room"
    }
}

// 0x0c05
class NukeScriptOpcode(val script: Int) : Opcode("nuke-script", 3) {
    override fun toSource(): String {
        return "nuke-script $script"
    }
}

// 0x0c06
class NukeSoundOpcode(val sound: Int) : Opcode("nuke-sound", 3) {
    override fun toSource(): String {
        return "nuke-sound $sound"
    }
}

// 0x0c07
class NukeCostumeOpcode(val costume: Int) : Opcode("nuke-costume", 3) {
    override fun toSource(): String {
        return "nuke-costume ${costume}"
    }
}

// 0x0c08
class NukeRoomOpcode(val room: Int) : Opcode("nuke-room", 3) {
    override fun toSource(): String {
        return "nuke-room $room"
    }
}

// 0x0c09
class LockScriptOpcode(val script: Int) : Opcode("lock-script", 3) {
    override fun toSource(): String {
        return "lock-script $script"
    }
}

// 0x0c0a
class LockSoundOpcode(val sound: Int) : Opcode("lock-sound", 3) {
    override fun toSource(): String {
        return "lock-sound $sound"
    }
}

// 0x0c0b
class LockCostumeOpcode(val costume: Int) : Opcode("lock-costume", 3) {
    override fun toSource(): String {
        return "lock-costume $costume"
    }
}

// 0x0c0c
class LockRoomOpcode(val room: Int) : Opcode("lock-room", 3) {
    override fun toSource(): String {
        return "lock-room $room"
    }
}

// 0x0c0d
class UnlockScriptOpcode(val script: Int) : Opcode("lock-room", 3) {
    override fun toSource(): String {
        return "unlock-script $script"
    }
}

// 0x0c0e
class UnlockSoundOpcode(val sound: Int) : Opcode("unlock-sound", 3) {
    override fun toSource(): String {
        return "unlock-sound $sound"
    }
}

// 0x0c0f
class UnlockCostumeOpcode(val costume: Int) : Opcode("unlock-costume", 3) {
    override fun toSource(): String {
        return "unlock-costume $costume"
    }
}

// 0x0c10
class UnlockRoomOpcode(val room: Int) : Opcode("unlock-room", 3) {
    override fun toSource(): String {
        return "unlock-room $room"
    }
}

// 0x0c11
class ClearHeapOpcode : Opcode("clear-heap", 2)

// 0x0c12
class LoadCharsetOpcode(val charset: Int) : Opcode("load-charset", 3) {
    override fun toSource(): String {
        return "load-charset $charset"
    }
}

// 0x0c13
class NukeCharsetOpcode(val charset: Int) : Opcode("nuke-charset", 3) {
    override fun toSource(): String {
        return "nuke-charset $charset"
    }
}

// 0x18
class JumpOpcode(val offset: Int) : Opcode("jump", 3) {
    override fun toSource(): String = "jump $offset"
}

// 0x20
object StopMusicOpcode : Opcode("stop-music", 1)

// 27 01	*@string = @zeichenkette
class AssignLiteralToStringOpcode(val str: Int, val strVal: String, val opcodeLength: Int)
    : Opcode("assign string", opcodeLength){
    override fun toSource() = "str$str = \"$strVal\""
}

// 27 04	@var = *@str [@idx]
class AssignStringCharAtToVarOpcode(val varSpec: VarSpec, val str: Int, index: Int)
    : Opcode("assign string char at idx to var", 6) {
    override fun toSource() = "$varSpec = str${str}[index]"
}

// 27 02	*@str1 = *@str2
class AssignStringToStringOpcode(val destStr: Int, val srcStr: Int) : Opcode("str1 = str2", 4) {
    override fun toSource() = "str$destStr = str$srcStr"
}

// 27 03	*@str1 [@idx] = @wert
class SetStringCharAtOpcode(val destStr: Int, val index: Int, val char: Int) : Opcode("str[idx] = char", 5) {
    override fun toSource() = "str$destStr[$index] = $char"
}

// 27 05	*@str [@idx]
class PushStringCharAtIdxToVarOpcode(val destStr: Int, val index: Int) : Opcode("push str[idx]", 4) {
    override fun toSource() = "push str${destStr}[$index]"
}

class AssignOpcode(val varSpec: VarSpec, val value: Int) : Opcode("assign", 5) {
    override fun toSource(): String = "${varSpec.toSourceName()} = $value"
}

object CursorOn : Opcode("cursor on", 2)
object CursorOff : Opcode("cursor off", 2)
object UserPutOn : Opcode("userput on", 2)
object UserPutOff : Opcode("userput off", 2)
object CursorSoftOn : Opcode("cursor soft-on", 2)
object CursorSoftOff : Opcode("cursor soft-off", 2)
object UserPutSoftOn : Opcode("userput soft-on", 2)
object UserPutSoftOff : Opcode("userput soft-off", 2)
class CursorImageOpcode(cursor: Int, image: Int) : Opcode("cursor image", 4)
class CursorHotspotOpcode(cursor: Int, hotspotX: Int, hotspotY: Int) : Opcode("cursor hotspot", 4)
class CursorOpcode(cursor: Int) : Opcode("cursor", 3)
class CharsetOpcode(val charset: Int) : Opcode("charset", 3) {
    override fun toSource(): String = "charset $charset"
}

// 44	if (@var > @wert)
class IfVarGreaterThanOpcode(val varSpec: VarSpec, val value: Int, val skipOffset: Int)
    : Opcode("if (@var > @wert)", 7) {
    override fun toSource() = "if ${nameForVar(varSpec)} > $value else jump $skipOffset"
}

// 46 ++@var
class IncrementVarOpcode(val varSpec: VarSpec) : Opcode("++var", 3) {
    override fun toSource() = "++${nameForVar(varSpec)}"
}

typealias ObjSpec = Int

// 67	@var = width @object
class AssignObjectWidthToVar(val varSpec: VarSpec, val objSpec: ObjSpec) : Opcode("<var> = width <object>", 4) {
}

object BreakHereInstr : Opcode("break-here", 1)
object EndScriptInstr : Opcode("end-script", 1)

// 0xa8 if (@var)
class IfVarOpcode(val varSpec: VarSpec, val skipOffset: Int) : Opcode("if (@var)", 3) {
    override fun toSource(): String {
        return "if ${nameForVar(varSpec)} (else jump $skipOffset)"
    }
}

fun objSpecToString(objSpec: ObjSpec) = "obj$objSpec"


sealed class VarSpec {
    abstract fun toSourceName(): String
}

class GlobalVarSpec(val varNum: Int) : VarSpec() {
    override fun toSourceName() = nameForGlobalVar(this)
}

class BitVarSpec(val varNum: Int, val bitNum: Int) : VarSpec() {
    override fun toSourceName() = "bit$varNum:$bitNum"
}

class IfVarEqualOpcode(val varSpec: VarSpec, val value: Int, val skipOffset: Int) : Opcode("if var == val", 7) {
    override fun toSource(): String = "if ${nameForVar(varSpec)} == $value (else skip $skipOffset)"
}

class IfVarNotEqualOpcode(val varSpec: VarSpec, val value: Int, val skipOffset: Int) : Opcode("if var != val", 7) {
    override fun toSource(): String = "if ${nameForVar(varSpec)} != $value (else skip $skipOffset)"
}


val globalVarNames = mapOf(
        1 to "main-actor",
        8 to "actor-count",
        39 to "debug-level",
        44 to "mouse-x",
        45 to "mouse-y",
        49 to "run-mode",
        51 to "running-from",
        68 to "machine-speed",
        69 to "video-spéed"
)

fun nameForVar(varSpec: VarSpec) = when (varSpec) {
    is GlobalVarSpec -> nameForGlobalVar(varSpec)
    is BitVarSpec -> "bit${varSpec.varNum}:${varSpec.bitNum}"
}

fun nameForGlobalVar(varSpec: GlobalVarSpec): String =
        globalVarNames.getOrDefault(varSpec.varNum, "global${varSpec.varNum}")

fun loadScript(path: File, xorDecode: Boolean = false): ByteArray? {
    var stream: InputStream = FileInputStream(path)

    if (xorDecode) {
        stream = XorInputStream(stream)
    }

    DataInputStream(stream).use {
        val blockIdBytes = ByteArray(4)
        it.read(blockIdBytes)

        if (String(blockIdBytes) != "SCRP") {
            return null
        } else {
            val blockLength = it.readInt()
            val scriptBuffer = ByteArray(blockLength - 8)

            it.read(scriptBuffer)
            return scriptBuffer
        }
    }

}


fun decodeInstructionsFromScriptBytes(scriptBytes: ByteArray): List<Pair<Int, Opcode>> {
    var done = false
    var offset = 0
    val opcodes = mutableListOf<Pair<Int, Opcode>>()

    while (offset < scriptBytes.size) {
        val opcode = decompileInstruction(scriptBytes, offset)

        if (opcode != null) {
            opcodes.add(Pair(offset, opcode))
        } else {
            println("@$offset: unbekannter opcode: 0x${Integer.toHexString(scriptBytes[offset].toInt().and(0xff))}")
        }

        offset += opcode?.length ?: 1
    }

    return opcodes
}

sealed class ByteParam
data class ImmediateByteParam(val byte: Int) : ByteParam()
data class ByteVarParam(val varSpec: VarSpec) : ByteParam()

fun DataInput.readByteParam() : ByteParam { // TODO ByteVarParam
    return ImmediateByteParam(readByte().toInt().and(0xff))
}

sealed class WordParam
data class ImmediateWordParam(val word: Int) : WordParam()
data class WordVarParam(val varSpec: VarSpec) : WordParam()

fun DataInput.readWordParam(): WordParam {  // TODO WordVarParam
    return ImmediateWordParam(readShortLittleEndian().toInt().and(0xffff))
}

fun decompileInstruction(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readByte().toInt().and(0xff)
    return when (opcode) {
        0 -> EndObject

//        0x1, 0x21, 0x41, 0x61, 0x81, 0xa1, 0xc1, 0xe1 -> {
//            PutActorAtInstruction()
//        }

//        0x2, 0x82 -> {
//            if (opcode == 0x2) {
//                StartMusicInstr(ImmediateByteParam(data.readByte().toInt()))
//
//            } else {
//                StartMusicInstr(ByteVarParam())
//            }
//        }

        0x4, 0x84 -> {
            val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

            val opcode = data.readByte()
            val varSpec = data.readShortLittleEndian().toInt()
            val value = data.readShortLittleEndian().toInt()
            val skipOffset = data.readShortLittleEndian().toInt()

            IfVarLessOrEqualOpcode(toVarSpec(varSpec), value, skipOffset)
        }

        5 -> {
            when (data.readByte().toInt()) {
                1 -> {
                    val obj = data.readByte()
                    val x = data.readShortLittleEndian()
                    val y = data.readShortLittleEndian()
                    DrawObjectAtOpcode(obj.toInt(), x.toInt(), y.toInt())
                }
                2 -> {
                    val obj = data.readByte()
                    val img = data.readByte()
                    DrawObjectImageOpcode(obj.toInt(), img.toInt())
                }
                else -> null
            }
        }

        8 -> decompileIfNotEqualOpcode(bytes, offset)

        9 -> {
            val actor = data.readByteParam()
            val obj = data.readWordParam()
            ActorFaceTowardsOpcode(actor, obj)
        }

        0xc -> decompileHeapStuffOpcode(bytes, offset)
        0x18 -> JumpOpcode(data.readShortLittleEndian().toInt())
        0x1a -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val value = data.readShortLittleEndian().toInt()
            AssignOpcode(varSpec, value)
        }
        0x20 -> StopMusicOpcode

        0x27 -> decompileStringAssignOpcode(bytes, offset)
        0x2c -> decompileCursorOpcode(bytes, offset)
        0x44 -> decompileIfVarGreaterThanOpcode(bytes, offset)
        0x46 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            IncrementVarOpcode(varSpec)
        }
        0x48, 0xc8 -> decompileIfVarEqualOpcode(bytes, offset)
        0x67 -> {
            var varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            var objSpec = toObjSpec(data.readByte().toInt())
            AssignObjectWidthToVar(varSpec, objSpec)
        }

        0x80 -> BreakHereInstr
        0xa0 -> EndScriptInstr

        0xa8 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val skipOffset = data.readShortLittleEndian().toInt()

            return IfVarOpcode(varSpec, skipOffset)
        }

        else -> InvalidInstruction(opcode.toByte())
    }
}

fun toObjSpec(objSpec: Int) = objSpec

fun DataInput.readOpcodeAsInt() = readByte().toInt()

fun DataInput.readString(): String {    // TODO control code
    val byteBuffer = ByteArrayOutputStream()
    var strByte = readByte().toInt()

    while (strByte.and(0xff) != 0) {
        byteBuffer.write(strByte)
        strByte = readByte().toInt()
    }

    return byteBuffer.toString()
}

// 0xc xx
fun decompileHeapStuffOpcode(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    data.readByte() // 0xc
    val byte2 = data.readOpcodeAsInt()

    val param = if (byte2 != 0x11) data.readByte().toInt() else 0  // TODO byteVar

    return when (byte2) {
        1 -> LoadScriptOpcode(param)
        2 -> LoadSoundOpcode(param)
        3 -> LoadCostumeOpcode(param)
        4 -> LoadRoomOpcode(param)
        5 -> NukeScriptOpcode(param)
        6 -> NukeSoundOpcode(param)
        7 -> NukeCostumeOpcode(param)
        8 -> NukeRoomOpcode(param)
        9 -> LockScriptOpcode(param)
        0xa -> LockSoundOpcode(param)
        0xb -> LockCostumeOpcode(param)
        0xc -> LockRoomOpcode(param)
        0xd -> UnlockScriptOpcode(param)
        0xe -> UnlockSoundOpcode(param)
        0xf -> UnlockCostumeOpcode(param)
        0x10 -> UnlockRoomOpcode(param)
        0x11 -> ClearHeapOpcode()
        0x12 -> LoadCharsetOpcode(param)
        0x13 -> NukeCharsetOpcode(param)
        else -> InvalidInstruction(byteArrayOf(0xc, byte2.toByte()))
    }
}

fun decompileStringAssignOpcode(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    data.readByte() // 0x27
    val byte2 = data.readOpcodeAsInt()

    return when (byte2) {
        1 -> {
            val str = data.readByte().toInt()   // TODO byte korrekt?
            val strVal = data.readString()
            val opcodeLength = 4 + strVal.length
            AssignLiteralToStringOpcode(str, strVal, opcodeLength)
        }

        2 -> {
            val destStr = data.readByte().toInt()
            val srcStr = data.readByte().toInt()
            AssignStringToStringOpcode(destStr, srcStr)
        }

        3 -> {
            val strVar = data.readByte().toInt()
            val strIdx = data.readByte().toInt()
            val char = data.readByte().toInt()
            SetStringCharAtOpcode(strVar, strIdx, char)
        }

        4 -> {  // @var = *@str [@idx]
            val destVar = data.readShortLittleEndian().toInt()
            val str = data.readByte().toInt()
            val strIdx = data.readByte().toInt()
            AssignStringCharAtToVarOpcode(toVarSpec(destVar), str, strIdx)
        }

        5 -> {  // *@str [@idx]
            val strVar = data.readByte().toInt()
            val strIdx = data.readByte().toInt()
            PushStringCharAtIdxToVarOpcode(strVar, strIdx)
        }

        else -> InvalidInstruction(byteArrayOf(0x27, byte2.toByte()))
    }
}

fun toVarSpec(varSpec: Int): VarSpec = when (varSpec) {
    else -> GlobalVarSpec(varSpec)
}

// 44	if (@var > @wert)
fun decompileIfVarGreaterThanOpcode(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readByte()
    val varSpec = data.readShortLittleEndian().toInt()
    val value = data.readShortLittleEndian().toInt()
    val skipOffset = data.readShortLittleEndian().toInt()

    return IfVarGreaterThanOpcode(toVarSpec(varSpec), value, skipOffset)
}

// 48	if (<var> == <wert>)
fun decompileIfVarEqualOpcode(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readByte()
    val varSpec = data.readShortLittleEndian().toInt()
    val value = data.readShortLittleEndian().toInt()
    val skipOffset = data.readShortLittleEndian().toInt()

    return IfVarEqualOpcode(toVarSpec(varSpec), value, skipOffset)
}

fun decompileIfNotEqualOpcode(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readByte()
    val varSpec = data.readShortLittleEndian().toInt()
    val value = data.readShortLittleEndian().toInt()
    val skipOffset = data.readShortLittleEndian().toInt()

    return IfVarNotEqualOpcode(toVarSpec(varSpec), value, skipOffset)
}

fun decompileCursorOpcode(bytes: ByteArray, offset: Int): Opcode {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    data.readByte() // 0x2c

    val instruction = when (val byte2 = data.readByte().toInt()) {
        1 -> CursorOn
        2 -> CursorOff
        3 -> UserPutOn
        4 -> UserPutOff
        5 -> CursorSoftOn
        6 -> CursorSoftOff
        7 -> UserPutSoftOn
        8 -> UserPutSoftOff
        0xa -> CursorImageOpcode(data.readByte().toInt(), data.readByte().toInt())      // TODO parameter cursor, img
        0xb -> {
            val cursor = data.readByte().toInt()
            val hotspotX = data.readByte().toInt()
            val hotspotY = data.readByte().toInt()
            CursorHotspotOpcode(cursor, hotspotX, hotspotY)    // TODO parameter cursor, x, y
        }
        0xc -> CursorOpcode(data.readByte().toInt())     // TODO parameter cursor
        0xd -> CharsetOpcode(data.readByte().toInt())          // TODO parameter charset

        else -> InvalidInstruction(byteArrayOf(0x2c, byte2.toByte()))
    }

    return instruction
}
