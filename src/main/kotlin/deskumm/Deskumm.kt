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
            val instructions = decodeInstructionsFromScriptBytes(script)

            instructions.forEach {
                val (offset, opcode) = it
                val bytesString = script.sliceArray(offset ..< offset + opcode.length).joinToString(" ") { "%02x".format(it) }
                println("%6d %s\n       %s".format(offset, opcode.toSource(), bytesString))
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

class InvalidInstruction(val opcode: ByteArray) : Instruction {
    constructor(opcode: Byte) : this(byteArrayOf(opcode))

    override fun toSource(): String {
        val opcodeHex = opcode.joinToString(" ") { "%02x".format(it) }
        return "invalid-opcode 0x$opcodeHex"
    }

    override val length: Int
        get() = opcode.size
}

class DrawObjectAtOpcode(val obj: Int, val x: Int, val y: Int) : Opcode("draw-object at", 7) {
    override fun toSource(): String = "draw-object $obj at $x, $y"
}

class DrawObjectImageOpcode(val obj: Int, val img: Int) : Opcode("draw-object image", 4) {
    override fun toSource(): String = "draw-object $obj image $img"
}

class StartMusicInstr(val sound: ByteParam) : Opcode("start-music", 3)  {

}

// 0x1, 0x21, 0x41, 0x61, 0x81, 0xa1, 0xc1, 0xe1
class PutActorAtInstr(val actorParam: ByteParam, val xParam: WordParam, val yParam: WordParam) : Instruction {
    override fun toSource(): String = "put-actor ${actorParam.toSource()} at ${xParam.toSource()}, ${yParam.toSource()}"
    override val length: Int
        get() = 1 + actorParam.byteCount + xParam.byteCount + yParam.byteCount
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

// 0x07/0x47/0x87/0xc7
class StateOfObjectIsInstr(val objectParam: WordParam, val stateParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return "state-of ${objectParam.toSource()} is ${stateParam.toSource()}"
    }

    override val length: Int
        get() = 1 + objectParam.byteCount + stateParam.byteCount
}

// 0x09
class ActorFaceTowardsOpcode(val actorParam: ByteParam, val objectParam: WordParam) : Instruction {
    override fun toSource(): String {
        return "actor ${actorParam.toSource()} face-towards ${objectParam.toSource()}"
    }

    override val length: Int
        get() = 1 + actorParam.byteCount + objectParam.byteCount
}

class StartScriptInstr(val scriptParam: ByteParam, val scriptArgs: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "TODO start-script ${scriptParam.toSource()} ${scriptArgs.joinToString { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + scriptParam.byteCount + scriptArgs.sumOf { it.byteCount } + scriptArgs.size /* opcode before arg */
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

// 0xd/0x4d/0x8d/0xcd
class WalkActorToActorInstr(val actor1Param: ByteParam, val actor2Param: ByteParam, val dist: Int) : Instruction {
    override fun toSource(): String {
        val within = if (dist != 0xff) " within $dist" else ""
        return "walk-actor ${actor1Param.toSource()} to ${actor2Param.toSource()}$within"
    }

    override val length: Int
        get() = 2 /* opcode, dist */ + actor1Param.byteCount + actor2Param.byteCount
}

// 0xe/0x4e/0x8e/0xce
class PutActorAtObjectInstr(val actorParam: ByteParam, val objectParam: WordParam) : Instruction {
    override fun toSource(): String = "put-actor ${actorParam.toSource()} at-object ${objectParam.toSource()}"
    override val length: Int
        get() = 1 + actorParam.byteCount + objectParam.byteCount
}

class ActorInstr(val actorParam: ByteParam, val subs: List<Sub>) : Instruction {
    sealed interface Sub {
        val byteSize: Int
        val source: String
    }

    // 0x1
    class Costume(val costumeParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + costumeParam.byteCount
        override val source: String
            get() = "costume ${costumeParam.toSource()}"
    }

    // 0x8
    object Default : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "default"
    }

    // 0xc
    class TalkColor(val colorParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + colorParam.byteCount
        override val source: String
            get() = "talk-color ${colorParam.toSource()}"
    }

    // 0xd
    class ActorName(val name: String) : Sub {
        override val byteSize: Int
            get() = 2 /* opcode + \0 */ + name.length
        override val source: String
            get() = "actor-name \"$name\""
    }

    class Invalid(val opcode: Int) : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "!actor-invalid-sub! 0x${opcode.toHexString()}"
    }

    override fun toSource(): String {
        return "TODO actor ${actorParam.toSource()} ${subs.joinToString { it.source }}"
    }

    override val length: Int
        get() = 2 /* opcode + 0xff */ + actorParam.byteCount + subs.sumOf { it.byteSize }
}

class PrintInstr(val who: ByteParam, val subs: List<Sub>) : Instruction {
    sealed interface Sub {
        val byteSize: Int
        val source: String
    }

    // 0x0
    class At(val xParam: WordParam, val yParam: WordParam) : Sub {
        override val byteSize: Int
            get() = 1 + xParam.byteCount + yParam.byteCount
        override val source: String
            get() = "at ${xParam.toSource()}, ${yParam.toSource()}"
    }

    // 0x1
    class Color(val colorParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + colorParam.byteCount
        override val source: String
            get() = "color ${colorParam.toSource()}"
    }

    // 0x4
    object Center : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "center"
    }

    // 0x8
    class SayVoice(val offsetParam: WordParam, val delayParam: WordParam) : Sub {
        override val byteSize: Int
            get() = 1 + offsetParam.byteCount + delayParam.byteCount
        override val source: String
            get() = "say-voice ${offsetParam.toSource()}, ${delayParam.toSource()}"
    }

    class Text(val stringBytes: ByteArray) : Sub {
        override val byteSize: Int
            get() = 2 /* opcode, \0 */ + stringBytes.size
        override val source: String
            get() = "text \"${stringBytes.decodeToString()}\""
    }

    class Invalid(val opcode: Int) : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "!invalid 0x$opcode (0x${opcode.and(0xf)})!"
    }

    override fun toSource(): String {
        return "TODO print ${who.toSource()} ${subs.joinToString(" ") { it.source }}"
    }

    override val length: Int
        get() = 3 /* opcode + who + terminator */ + subs.sumOf { it.byteSize }
}

// 0x18
class JumpInstr(val offset: Int) : Instruction {
    override fun toSource(): String = "jump $offset"
    override val length: Int
        get() = 3
}

// 0x19/0x39/0x59/0x79/0x99/0xb9/0xd9/0xf9
class DoSentenceWithInstr(val verbParam: ByteParam, val object1Param: WordParam, val object2Param: WordParam) : Instruction {
    override fun toSource(): String {
        return "do-sentence ${verbParam.toSource()} ${object1Param.toSource()} with ${object2Param.toSource()}"
    }

    override val length: Int
        get() = 1 + verbParam.byteCount + object1Param.byteCount + object2Param.byteCount
}

// 0x20
object StopMusicOpcode : Opcode("stop-music", 1)

// 0x25, 0x65, 0xa5, 0xe5
class PickUpObjectInstr(val objectParam: WordParam, val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "pick-up-object ${objectParam.toSource()} in-room ${roomParam.toSource()}"

    override val length: Int
        get() = 1 + objectParam.byteCount + roomParam.byteCount
}

fun v5StringVarName(str: Int): String {
    return when (str) {
        1 -> "insert-disk-text"
        2 -> "cannot-find-text"
        4 -> "pause-text"
        5 -> "restart-prompt"
        6 -> "quit-prompt"
        7 -> "save-text"
        8 -> "load-text"
        9 -> "play-text"
        10 -> "cancel-text"
        11 -> "quit-text"
        12 -> "okay-text"
        26 -> "game-mode"
        28 -> "game-version"
        else -> "str$str"
    }
}

// 27 01	*@string = @zeichenkette
class AssignLiteralToStringInstr(val stringParam: ByteParam, val stringBytes: ByteArray) : Instruction {
    override fun toSource() =
        "${if (stringParam is ImmediateByteParam) v5StringVarName(stringParam.byte) else stringParam.toSource()} = \"${stringBytes.decodeToString()}\""

    override val length: Int
        get() = 3 /* opcode, string \0 */ + stringParam.byteCount + stringBytes.size
}

// 27 04	@var = *@str [@idx]
class AssignStringCharAtToVarOpcode(val varSpec: VarSpec, val str: Int, val index: Int)
    : Opcode("assign string char at idx to var", 6) {
    override fun toSource() = "$varSpec = ${v5StringVarName(str)}[$index]"
}

// 27 02	*@str1 = *@str2
class AssignStringToStringOpcode(val destStr: Int, val srcStr: Int) : Opcode("str1 = str2", 4) {
    override fun toSource() = "${v5StringVarName(destStr)} = ${v5StringVarName(srcStr)}"
}

// 27 03	*@str1 [@idx] = @wert
class SetStringCharAtOpcode(val destStr: Int, val index: Int, val char: Int) : Opcode("str[idx] = char", 5) {
    override fun toSource() = "${v5StringVarName(destStr)}[$index] = $char"
}

// 27 05	*@str [@idx]
class PushStringCharAtIdxToVarOpcode(val destStr: Int, val index: Int) : Opcode("push str[idx]", 4) {
    override fun toSource() = "dim ${v5StringVarName(destStr)}[$index]"
}

// 0x28
class JumpIfNullInstr(val varSpec: VarSpec, val jumpOffset: Int) : Instruction {
    override fun toSource(): String = "if-null ${varSpec.toSourceName()} jump $jumpOffset"
    override val length: Int
        get() = 1 + varSpec.byteCount + 2 /* offset */
}

// 0x29/0x69/0xa9/0xe9
class OwnerOfInstr(val objectParam: WordParam, val ownerParam: ByteParam) : Instruction {
    override fun toSource(): String = "owner-of ${objectParam.toSource()} is ${ownerParam.toSource()}"

    override val length: Int
        get() = 1 + objectParam.byteCount + ownerParam.byteCount
}

// 0x2d/0x6d/0xad/0xed
class PutActorInRoomInstr(val actorParam: ByteParam, val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "put-actor ${actorParam.toSource()} in-room ${roomParam.toSource()}"

    override val length: Int
        get() = 1 + actorParam.byteCount + roomParam.byteCount
}

// 0x1a/0x9a
class AssignOpcode(val varSpec: VarSpec, val valueParam: WordParam) : Instruction {
    override fun toSource(): String = "${varSpec.toSourceName()} = ${valueParam.toSource()}"
    override val length: Int
        get() = 1 + varSpec.byteCount + valueParam.byteCount
}

class JumpIfClassOfIsNotInstr(val objectParam: WordParam, val classParams: List<WordParam>, val offset: Int) : Instruction {
    override fun toSource(): String {
        return "if-class-of ${objectParam.toSource()} is-not ${classParams.joinToString(" ") { it.toSource() }} jump $offset"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + objectParam.byteCount + classParams.sumOf { it.byteCount } + classParams.size /* opcode before each class */ + 2 /* jump offset */
}

class RoomScrollInstr(val arg1: WordParam, val arg2: WordParam) : Instruction {
    override fun toSource(): String = "room-scroll $arg1 to $arg2"
    override val length: Int
        get() = 2 /* opcode */ + arg1.byteCount + arg2.byteCount
}

class SetScreenInstr(val arg1: WordParam, val arg2: WordParam) : Instruction {
    override fun toSource(): String = "set-screen ${arg1.toSource()} to ${arg2.toSource()}"
    override val length: Int
        get() = 2 /* opcode */ + arg1.byteCount + arg2.byteCount
}

class FadesInstr(val aParam: WordParam) : Instruction {
    override fun toSource(): String = "fades ${aParam.toSource()}"

    override val length: Int
        get() = 2 + aParam.byteCount
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

// 0x37/0x77/0xb7/0xf7
class StartObjectInstr(val objectParam: WordParam, val verbParam: ByteParam, val args: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "start-object ${objectParam.toSource()} verb ${verbParam.toSource()} ${args.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + objectParam.byteCount + verbParam.byteCount + args.sumOf { it.byteCount } + args.size /* opcode before each arg */
}

// 0x42/0xc2
class ChainsScriptInstr(val scriptParam: ByteParam, val scriptArgs: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "chains-script ${scriptParam.toSource()} ${scriptArgs.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, final 0xff */ + scriptParam.byteCount + scriptArgs.sumOf { it.byteCount } + scriptArgs.size
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

// 0x52/0xd2
class CameraFollowInstr(val actorParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return "camera-follow ${actorParam.toSource()}"
    }

    override val length: Int
        get() = 1 + actorParam.byteCount
}

// 0x54/0xd4
class NewNameOfInstr(val objectParam: WordParam, val name: String) : Instruction {
    override fun toSource(): String {
        return "new-name-of ${objectParam.toSource()} is \"$name\""
    }

    override val length: Int
        get() = 2 /* opcode, name \0 */ + objectParam.byteCount + name.length

}

// 0x5a/0xda
class AddAssignInstr(val resultVar: ResultVar, val valueParam: WordParam) : Instruction {
    override fun toSource(): String {
        return "$resultVar += ${valueParam.toSource()}"
    }

    override val length: Int
        get() = 1 + resultVar.byteCount + valueParam.byteCount
}

// 0x5d/0xdd
class ClassOfInstr(val objectParam: WordParam, val classesParam: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "class-of ${objectParam.toSource()} is ${classesParam.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + objectParam.byteCount + classesParam.sumOf { it.byteCount } + classesParam.size /* opcode before each class */
}

// 0x62/0xe2
class StopScriptInstr(val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return if (scriptParam is ImmediateByteParam && scriptParam.byte == 0) {
            "stop-object-code"
        } else {
            "stop-script ${scriptParam.toSource()}"
        }
    }

    override val length: Int
        get() = 1 + scriptParam.byteCount
}

// 67	@var = width @object
class AssignObjectWidthToVar(val varSpec: VarSpec, val objSpec: ObjSpec) : Opcode("<var> = width <object>", 4) {
}

// 0x68/0xe8
class ScriptRunningInstr(val resultVar: ResultVar, val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} = script-running ${scriptParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + scriptParam.byteCount
}

// 0x71/0xf1
class ActorCostumeInstr(val resultVar: ResultVar, val actorParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} = actor-costume ${actorParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + actorParam.byteCount
}

// 0x72/0xf2
class CurrentRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "current-room ${roomParam.toSource()}"
    override val length: Int
        get() = 1 + roomParam.byteCount
}

object BreakHereInstr : Opcode("break-here", 1)
object EndScriptInstr : Opcode("end-script", 1)

// 0xa8 if (@var)
class JumpIfVarZeroInst(val varSpec: VarSpec, val skipOffset: Int) : Instruction {
    override fun toSource(): String {
        return "if ${nameForVar(varSpec)} == 0 jump $skipOffset"
    }

    override val length: Int
        get() = 3 /* opcode, offset */ + varSpec.byteCount
}

class PseudoRoomInstr(val arg1: Int, val arg2: ByteArray) : Instruction {
    override fun toSource(): String {
        return "pseudo-room $arg1 [ ${arg2.joinToString()} ]"
    }

    override val length: Int
        get() = 3 /* opcode, arg1, arg2 \0 */ + arg2.size /* arg2 bytes */
}

// 0xd8
class SayLineInstr(val subs: List<PrintInstr.Sub>) : Instruction {
    override fun toSource(): String {
        return "say-line ${subs.joinToString(" ") { it.source }}"
    }
    override val length: Int
        get() {
            return if (subs.any { it is PrintInstr.Text }) {
                1 /* opcode */ + subs.sumOf { it.byteSize }
            } else {
                2 /* opcode, 0xff */ + subs.sumOf { it.byteSize }
            }
        }
}

class ExpressionInstr(val resultVar: ResultVar, val subs: List<Sub>) : Instruction {
    sealed interface Sub {
        val byteCount: Int
        fun toSource(): String
    }

    // 0x1
    class Value(val value: WordParam) : Sub {
        override val byteCount: Int
            get() = 1 + value.byteCount

        override fun toSource(): String = value.toSource()

    }

    // 0x2
    object Add : Sub {
        override val byteCount: Int
            get() = 1

        override fun toSource(): String = "add"
    }

    // 0x3
    object Subtract : Sub {
        override val byteCount: Int
            get() = 1

        override fun toSource(): String = "subtract"
    }

    // 0x4
    object Multiply : Sub {
        override val byteCount: Int
            get() = 1

        override fun toSource(): String = "multiply"
    }

    // 0x5
    object Divide : Sub {
        override val byteCount: Int
            get() = 1

        override fun toSource(): String = "divide"
    }

    // 0x6
    class Op(val op: ByteArray) : Sub {
        override val byteCount: Int
            get() = 1

        override fun toSource(): String = "TODO expression op"
    }


    class Invalid(val bytes: ByteArray) : Sub {
        override val byteCount: Int
            get() = bytes.size

        override fun toSource(): String {
            return bytes.joinToString()
        }

    }
    override fun toSource(): String {
        return "TODO expression; ${resultVar.toSource()} = ${subs.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, final 0xff */ + resultVar.byteCount + subs.sumOf { it.byteCount }

}

// 0xae 0x01
class WaitForActorInstr(val actorParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return "wait-for-actor ${actorParam.toSource()}"
    }

    override val length: Int
        get() = 2 + actorParam.byteCount
}

// 0xae 0x02
object WaitForMessageInstr : Instruction {
    override fun toSource(): String = "wait-for-message"

    override val length: Int
        get() = 2
}

// 0xae 0x03
object WaitForCameraInstr : Instruction {
    override fun toSource(): String = "wait-for-camera"

    override val length: Int
        get() = 2
}

// 0xae 0x04
object WaitForSentenceInstr : Instruction {
    override fun toSource(): String = "wait-for-sentence"

    override val length: Int
        get() = 2
}

fun objSpecToString(objSpec: ObjSpec) = "obj$objSpec"


sealed interface VarSpec {
    fun toSourceName(): String
    val byteCount: Int
}

class GlobalVarSpec(val varNum: Int) : VarSpec {
    override fun toSourceName() = nameForGlobalVar(this)
    override val byteCount: Int
        get() = 2
}

class LocalVarSpec(val varNum: Int) : VarSpec {
    override fun toSourceName() = "local$varNum"
    override val byteCount: Int
        get() = 2
}

class BitVarSpec(val varNum: Int, val bitNum: Int) : VarSpec {
    override fun toSourceName() = "bit$varNum:$bitNum"
    override val byteCount: Int
        get() = 2
}

class VarSpec0x2000(val varNum: Int) : VarSpec {
    override fun toSourceName() = "TODOvar0x2000_$varNum"
    override val byteCount: Int
        get() = 2
}

class JumpIfVarNotEqualInstr(val varSpec: VarSpec, val valueParam: WordParam, val skipOffset: Int) : Instruction {
    override fun toSource(): String = "if ${nameForVar(varSpec)} != ${valueParam.toSource()} jump $skipOffset"
    override val length: Int
        get() = 1 + varSpec.byteCount + valueParam.byteCount + 2
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
    is LocalVarSpec -> "local${varSpec.varNum}"
    is VarSpec0x2000 -> "TODOvar0x2000_${varSpec.varNum}"
//    else -> "var?!"
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


fun decodeInstructionsFromScriptBytes(scriptBytes: ByteArray): List<Pair<Int, Instruction>> {
    var offset = 0
    val opcodes = mutableListOf<Pair<Int, Instruction>>()

    try {
        while (offset < scriptBytes.size) {
            val opcode = decompileInstruction(scriptBytes, offset)

            if (opcode != null) {
                opcodes.add(Pair(offset, opcode))
            } else {
                println("@$offset: unbekannter opcode: 0x${Integer.toHexString(scriptBytes[offset].toInt().and(0xff))}")
            }

            offset += opcode?.length ?: 1
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return opcodes
}

sealed interface ByteParam {
    val byteCount: Int
    fun toSource(): String
}

data class ImmediateByteParam(val byte: Int) : ByteParam {
    override val byteCount: Int
        get() = 1

    override fun toSource(): String = byte.toString()
}

data class ByteVarParam(val varSpec: VarSpec) : ByteParam {
    override val byteCount: Int
        get() = varSpec.byteCount

    override fun toSource(): String = varSpec.toSourceName()
}

@Deprecated("use alternative taking parameters")
fun DataInput.readByteParam() : ByteParam { // TODO ByteVarParam
    return ImmediateByteParam(readByte().toInt().and(0xff))
}

sealed interface WordParam {
    val byteCount: Int
    fun toSource(): String
}

data class ImmediateWordParam(val word: Int) : WordParam {
    override val byteCount: Int
        get() = 2

    override fun toSource(): String {
        return word.toString()
    }
}

data class WordVarParam(val varSpec: VarSpec) : WordParam {
    override val byteCount: Int
        get() = varSpec.byteCount

    override fun toSource(): String {
        return varSpec.toSourceName()
    }
}

@Deprecated("use alternative taking parameters")
fun DataInput.readWordParam(): WordParam {  // TODO WordVarParam
    return ImmediateWordParam(readShortLittleEndian().toInt().and(0xffff))
}

fun decompileInstruction(bytes: ByteArray, offset: Int): Instruction? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readUnsignedByte()

    return when (opcode) {
        0 -> EndObject

        0x1, 0x21, 0x41, 0x61, 0x81, 0xa1, 0xc1, 0xe1 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val xParam = readWordParam(data, opcode, 0x40)
            val yParam = readWordParam(data, opcode, 0x20)

            PutActorAtInstr(actorParam, xParam, yParam)
        }

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
            when (val opcode2 = data.readByte().toInt()) {
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
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
        }

        0x7, 0x47, 0x87, 0xc7 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val stateParam = readByteParam(data, opcode, 0x40)

            StateOfObjectIsInstr(objectParam, stateParam)
        }

        8 -> decompileIfNotEqualOpcode(bytes, offset)

        0x9, 0x49, 0x89, 0xc9 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val objectParam = readWordParam(data, opcode, 0x40)

            ActorFaceTowardsOpcode(actorParam, objectParam)
        }

        0xa, 0x2a, 0x4a, 0x6a, 0x8a, 0xaa, 0xca, 0xea -> {
            val scriptParam = readByteParam(data, opcode, 0x80)
            val scriptArgs = readScriptArgs(data)

            StartScriptInstr(scriptParam, scriptArgs)
        }

        0xc -> decompileHeapStuffOpcode(bytes, offset)

        0x0d, 0x4d, 0x8d, 0xcd -> {
            val actor1Param = readByteParam(data, opcode, 0x80)
            val actor2Param = readByteParam(data, opcode, 0x40)
            val distance = data.readUnsignedByte()

            WalkActorToActorInstr(actor1Param, actor2Param, distance)
        }

        0x0e, 0x4e, 0x8e, 0xce -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val objectParam = readWordParam(data, opcode, 0x40)

            PutActorAtObjectInstr(actorParam, objectParam)
        }
        0x13, 0x53, 0x93, 0xd3 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val subs = mutableListOf<ActorInstr.Sub>()

            var opcode2 = data.readUnsignedByte()

            while (opcode2 != 0xff) {
                val sub = when (opcode2.and(0x1f)) {
                    1 -> {
                        val costumeParam = readByteParam(data, opcode2, 0x80)
                        ActorInstr.Costume(costumeParam)
                    }

                    8 -> ActorInstr.Default

                    12 -> {
                        val colorParam = readByteParam(data, opcode2, 0x80)
                        ActorInstr.TalkColor(colorParam)
                    }

                    13 -> {
                        val name = data.readScummStringBytes()
                        ActorInstr.ActorName(name.decodeToString())
                    }

                    else -> ActorInstr.Invalid(opcode2)
                }

                subs.add(sub)

                opcode2 = data.readUnsignedByte()
            }

            ActorInstr(actorParam, subs)
        }

        0x14, 0x94 -> {
            val who = readByteParam(data, opcode, 0x80)

            val subs = decodePrintSubs(data)

            PrintInstr(who, subs)
        }

        0x18 -> JumpInstr(data.readShortLittleEndian().toInt())

        0x19, 0x39, 0x59, 0x79, 0x99, 0xb9, 0xd9, 0xf9 -> {
            val verbParam = readByteParam(data, opcode, 0x80)
            val object1Param = readWordParam(data, opcode, 0x40)
            val object2Param = readWordParam(data, opcode, 0x20)

            DoSentenceWithInstr(verbParam, object1Param, object2Param)
        }

        0x1a, 0x9a -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val value = data.readWordParam()
            AssignOpcode(varSpec, value)
        }

        0x1d, 0x9d -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val classParams = buildList {
                var opcode2 = data.readUnsignedByte()

                while (opcode2 != 0xff) {
                    val classParam = readWordParam(data, opcode2, 0x80)
                    add(classParam)

                    opcode2 = data.readUnsignedByte()
                }
            }

            val jumpOffset = data.readShortLittleEndian().toInt()

            JumpIfClassOfIsNotInstr(objectParam, classParams, jumpOffset)
        }

        0x20 -> StopMusicOpcode

        0x25, 0x65, 0xa5, 0xe5 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val roomParam = readByteParam(data, opcode, 0x40)

            PickUpObjectInstr(objectParam, roomParam)
        }

        0x27 -> decompileStringAssignOpcode(bytes, offset)

        0x28 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val jumpOffset = data.readShortLittleEndian().toInt()

            JumpIfNullInstr(varSpec, jumpOffset)
        }

        0x29, 0x69, 0xa9, 0xe9 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val ownerParam = readByteParam(data, opcode, 0x40)

            OwnerOfInstr(objectParam, ownerParam)
        }

        0x2c -> decompileCursorInstruction(bytes, offset)

        0x2d, 0x6d, 0xad, 0xed -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val roomParam = readByteParam(data, opcode, 0x40)

            PutActorInRoomInstr(actorParam, roomParam)
        }

        0x33, 0x73, 0xb3, 0xf3 -> {
            val opcode2 = data.readUnsignedByte()

            when (opcode2.and(0x1f)) {
                1 -> {
                    val arg1 = readWordParam(data, opcode2, 0x80)
                    val arg2 = readWordParam(data, opcode2, 0x40)
                    RoomScrollInstr(arg1, arg2)
                }

                3 -> {
                    val arg1 = readWordParam(data, opcode2, 0x80)
                    val arg2 = readWordParam(data, opcode2, 0x40)
                    SetScreenInstr(arg1, arg2)
                }

                0xa -> FadesInstr(readWordParam(data, opcode2, 0x80))
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
        }

        0x37, 0x77, 0xb7, 0xf7 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val scriptParam = readByteParam(data, opcode, 0x40)
            val args = readScriptArgs(data)

            StartObjectInstr(objectParam, scriptParam, args)
        }

        0x42, 0xc2 -> {
            val scriptParam = readByteParam(data, opcode, 0x80)
            val scriptArgs = readScriptArgs(data)

            ChainsScriptInstr(scriptParam, scriptArgs)
        }

        0x44 -> decompileIfVarGreaterThanOpcode(bytes, offset)
        0x46 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            IncrementVarOpcode(varSpec)
        }

        0x48, 0xc8 -> decompileIfVarEqualOpcode(bytes, offset)

        0x52, 0xd2 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            CameraFollowInstr(actorParam)
        }

        0x54, 0xd4 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val name = data.readScummStringBytes()

            NewNameOfInstr(objectParam, name.decodeToString())
        }

        0x5a, 0xda -> {
            val resultVar = readResultVar(data)
            val valueParam = readWordParam(data, opcode, 0x80)

            AddAssignInstr(resultVar, valueParam)
        }

        0x5d, 0xdd -> {
            val objectParam = readWordParam(data, opcode, 0x80)

            var opcode2 = data.readUnsignedByte()

            val classesParam = buildList {
                while (opcode2 != 0xff) {
                    add(readWordParam(data, opcode2, 0x80))

                    opcode2 = data.readUnsignedByte()
                }
            }

            ClassOfInstr(objectParam, classesParam)
        }

        0x62, 0xe2 -> {
            val scriptParam = readByteParam(data, opcode, 0x80)
            StopScriptInstr(scriptParam)
        }

        0x67 -> {
            var varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            var objSpec = toObjSpec(data.readByte().toInt())
            AssignObjectWidthToVar(varSpec, objSpec)
        }

        0x68, 0xe8 -> {
            val resultVar = readResultVar(data)
            val scriptParam = readByteParam(data, opcode, 0x80)

            ScriptRunningInstr(resultVar, scriptParam)
        }

        0x71, 0xf1 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            ActorCostumeInstr(resultVar, actorParam)
        }

        0x72, 0xf2 -> CurrentRoomInstr(readByteParam(data, opcode, 0x80))

        0x80 -> BreakHereInstr
        0xa0 -> EndScriptInstr

        0xa8 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val skipOffset = data.readShortLittleEndian().toInt()

            return JumpIfVarZeroInst(varSpec, skipOffset)
        }

        0xac -> {
            val resultVar = readResultVar(data)
            var opcode = data.readUnsignedByte()

            val subs = buildList {
                while (opcode != 0xff) {
                    val sub = when (opcode.and(0x1f)) {
                        1 -> ExpressionInstr.Value(readWordParam(data, opcode, 0x80))
                        2 -> ExpressionInstr.Add
                        3 -> ExpressionInstr.Subtract
                        4 -> ExpressionInstr.Multiply
                        5 -> ExpressionInstr.Divide
                        6 -> ExpressionInstr.Op(byteArrayOf(data.readUnsignedByte().toByte()))  // TODO
                        else -> ExpressionInstr.Invalid(byteArrayOf(opcode.toByte()))
                    }

                    add(sub)

                    opcode = data.readUnsignedByte()
                }
            }

            ExpressionInstr(resultVar, subs)
        }

        0xae -> {
            val opcode2 = data.readUnsignedByte()

            when (opcode2.and(0x1f)) {
                1 -> WaitForActorInstr(readByteParam(data, opcode2, 0x80))
                2 -> WaitForMessageInstr
                3 -> WaitForCameraInstr
                4 -> WaitForSentenceInstr
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
        }

        0xcc -> {
            val arg1 = data.readUnsignedByte()
            val arg2 = readBytesUntilZeroByte(data)

            PseudoRoomInstr(arg1, arg2)
        }

        0xd8 -> SayLineInstr(decodePrintSubs(data))

        else -> InvalidInstruction(opcode.toByte())
    }
}

private fun decodePrintSubs(data: DataInputStream): MutableList<PrintInstr.Sub> {
    val subs = mutableListOf<PrintInstr.Sub>()

    var opcode2 = data.readUnsignedByte()

    while (opcode2 != 0xff) {
        when (opcode2.and(0xf)) {
            0 -> {
                val x = readWordParam(data, opcode2, 0x80)
                val y = readWordParam(data, opcode2, 0x40)
                subs.add(PrintInstr.At(x, y))
            }

            1 -> subs.add(PrintInstr.Color(readByteParam(data, opcode2, 0x80)))

            4 -> subs.add(PrintInstr.Center)

            8 -> {
                val offsetParam = readWordParam(data, opcode2, 0x80)
                val delayParam = readWordParam(data, opcode2, 0x40)
                subs.add(PrintInstr.SayVoice(offsetParam, delayParam))
            }

            15 -> {
                subs.add(PrintInstr.Text(readBytesUntilZeroByte(data)))
                break
            }

            else -> subs.add(PrintInstr.Invalid(opcode2))
        }

        opcode2 = data.readUnsignedByte()
    }
    return subs
}

fun readBytesUntilZeroByte(input: DataInput, copyZeroByte: Boolean = false): ByteArray {
    val bytes = ByteArrayOutputStream()
    var byte = input.readUnsignedByte()

    while (byte != 0) {
        bytes.write(byte)
        byte = input.readUnsignedByte()
    }

    if (copyZeroByte) {
        bytes.write(byte)
    }

    return bytes.toByteArray()
}

fun readScriptArgs(data: DataInputStream): List<WordParam> {
    var opcode = data.readUnsignedByte()

    return buildList {
        while (opcode != 0xff) {
            add(readWordParam(data, opcode, 0x80))

            opcode = data.readUnsignedByte()
        }
    }
}

fun readByteParam(data: DataInput, opcode: Int, parameterMask: Int): ByteParam {
    return if (opcode.and(parameterMask) == 0) {
        ImmediateByteParam(data.readUnsignedByte())
    } else {
        ByteVarParam(toVarSpec(data.readShortLittleEndian().toInt()))
    }
}

fun readWordParam(data: DataInput, opcode: Int, parameterMask: Int): WordParam {
    return if (opcode.and(parameterMask) == 0) {
        ImmediateWordParam(data.readShortLittleEndian().toInt())
    } else {
        WordVarParam(toVarSpec(data.readShortLittleEndian().toInt()))
    }
}

class ResultVar(val varNum: Int) {
    val byteCount: Int get() = 2
    fun toSource(): String = "resultVar$varNum"
}    // TODO indexed etc.

fun readResultVar(data: DataInput): ResultVar {
    var varNum1 = data.readShortLittleEndian().toInt()
    if (varNum1.and(0x2000) != 0) {
        val a = data.readShortLittleEndian().toInt()

        if (a.and(0x2000) != 0) {
// TODO ...
        }
    }
    return ResultVar(varNum1)
}

fun toObjSpec(objSpec: Int) = objSpec

fun DataInput.readScummStringBytes(): ByteArray {    // TODO control code
    val stringBytes = ByteArrayOutputStream()
    var sourceByte = readUnsignedByte()

    while (sourceByte != 0) {
        stringBytes.write(sourceByte)

        if (sourceByte == 0xff) {
            sourceByte = readUnsignedByte()
            stringBytes.write(sourceByte)

            when (sourceByte) {
                1, 2, 3, 8 -> {}
                4 -> {
                    stringBytes.write(readUnsignedByte())
                    stringBytes.write(readUnsignedByte())
                }
                else -> {
                    stringBytes.write(readUnsignedByte())
                    stringBytes.write(readUnsignedByte())
                }
            }
        }

        sourceByte = readUnsignedByte()
    }

    return stringBytes.toByteArray()
}

// 0xc xx
fun decompileHeapStuffOpcode(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    data.readByte() // 0xc
    val opcode2 = data.readUnsignedByte()

    val param = if (opcode2 != 0x11) data.readByte().toInt() else 0  // TODO byteVar

    return when (opcode2) {
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
        else -> InvalidInstruction(byteArrayOf(0xc, opcode2.toByte()))
    }
}

fun decompileStringAssignOpcode(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    data.readByte() // 0x27
    val opcode2 = data.readUnsignedByte()

    return when (opcode2.and(0x1f)) {
        1 -> {
            val str = readByteParam(data, opcode2, 0x80)
            val stringBytes = data.readScummStringBytes()
            AssignLiteralToStringInstr(str, stringBytes)    // TODO convert to string
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

        else -> InvalidInstruction(byteArrayOf(0x27, opcode2.toByte()))
    }
}

fun toVarSpec(varSpec: Int): VarSpec = when {
    varSpec.and(0x8000) != 0 ->
        BitVarSpec(varSpec.and(0x7fff), 0)

    varSpec.and(0x4000) != 0 ->
        LocalVarSpec(varSpec.and(0x3fff))

    varSpec.and(0x2000) != 0 ->
        VarSpec0x2000(varSpec.and(0x1fff))  // TODO need additional bytes for idx

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

// 0x48/0xc8	if (<var> != <word-param>) jump <offs>
fun decompileIfVarEqualOpcode(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readUnsignedByte()
    val varSpec = data.readShortLittleEndian().toInt()
    val value = readWordParam(data, opcode, 0x80)
    val skipOffset = data.readShortLittleEndian().toInt()

    return JumpIfVarNotEqualInstr(toVarSpec(varSpec), value, skipOffset)
}

fun decompileIfNotEqualOpcode(bytes: ByteArray, offset: Int): Opcode? {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readByte()
    val varSpec = data.readShortLittleEndian().toInt()
    val value = data.readShortLittleEndian().toInt()
    val skipOffset = data.readShortLittleEndian().toInt()

    return IfVarNotEqualOpcode(toVarSpec(varSpec), value, skipOffset)
}

fun decompileCursorInstruction(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    data.readByte() // 0x2c

    val opcode2 = data.readUnsignedByte()

    val instruction = when (opcode2.and(0x1f)) {
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

        else -> InvalidInstruction(byteArrayOf(0x2c, opcode2.toByte()))
    }

    return instruction
}
