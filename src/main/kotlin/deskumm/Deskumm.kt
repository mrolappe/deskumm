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
            println(scriptPath)

            val instructions = decodeInstructionsFromScriptBytes(script)

            instructions.forEach {
                val (offset, instruction) = it
                val bytesString = script.sliceArray(offset ..< offset + instruction.length).joinToString(" ") { "%02x".format(it) }
                println("%6d %s\n       %s".format(offset, instruction.toSource(), bytesString))
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

class DrawObjectAtInstr(val objectParam: WordParam, val xParam: WordParam, val yParam: WordParam) : Instruction {
    override fun toSource(): String = "draw-object ${objectParam.toSource()} at ${xParam.toSource()}, ${yParam.toSource()}"
    override val length: Int
        get() = 2 + objectParam.byteCount + xParam.byteCount + yParam.byteCount
}

class DrawObjectImageInstr(val objectParam: WordParam, val imageParam: WordParam) : Instruction {
    override fun toSource(): String = "draw-object ${objectParam.toSource()} image ${imageParam.toSource()}"
    override val length: Int
        get() = 2 + objectParam.byteCount + imageParam.byteCount
}

class DrawObject1fInstr(val objectParam: WordParam) : Instruction {
    override fun toSource(): String = "draw-object ${objectParam.toSource()} nop-1f"
    override val length: Int
        get() = 2 + objectParam.byteCount
}

// 0x02, 0x82
class StartMusicInstr(val soundParam: ByteParam) : Instruction  {
    override fun toSource(): String = "start-music ${soundParam.toSource()}"

    override val length: Int
        get() = 1 + soundParam.byteCount
}

// 0x03, 0x83
class ActorRoomInstr(val resultVar: ResultVar, val actorParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := actor-room ${actorParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + actorParam.byteCount
}

// 0x01, 0x21, 0x41, 0x61, 0x81, 0xa1, 0xc1, 0xe1
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

// 0x06/0x86
class ActorElevationInstr(val resultVar: ResultVar, val actorParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := actor-elevation ${actorParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + actorParam.byteCount
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
    override fun toSource(): String = "start-script ${scriptParam.toSource()} ${scriptArgs.joinToString { it.toSource() }}"

    override val length: Int
        get() = 2 /* opcode, 0xff */ + scriptParam.byteCount + scriptArgs.sumOf { it.byteCount } + scriptArgs.size /* opcode before arg */
}

// 0x0b, 0x4b, 0x8b, 0xcb
class AssignValidVerbInstr(val resultVar: ResultVar, val aParam: WordParam, val bParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := valid-verb ${aParam.toSource()} ${bParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + aParam.byteCount + bParam.byteCount
}

// 0x0c01
class LoadScriptInstr(val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-script ${scriptParam.toSource()}"
    override val length: Int
        get() = 2 + scriptParam.byteCount
}

// 0x0c02
class LoadSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 2 + soundParam.byteCount
}

// 0x0c03
class LoadCostumeInstr(val costumeParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-costume ${costumeParam.toSource()}"
    override val length: Int
        get() = 2 + costumeParam.byteCount
}

// 0x0c04
class LoadRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-room ${roomParam.toSource()}"

    override val length: Int
        get() = 2 + roomParam.byteCount
}

// 0x0c05
class NukeScriptInstr(val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String = "nuke-script ${scriptParam.toSource()}"
    override val length: Int
        get() = 2 + scriptParam.byteCount
}

// 0x0c06
class NukeSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "nuke-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 2 + soundParam.byteCount
}

// 0x0c07
class NukeCostumeInstr(val costumeParam: ByteParam) : Instruction {
    override fun toSource(): String = "nuke-costume ${costumeParam.toSource()}"
    override val length: Int
        get() = 2 + costumeParam.byteCount
}

// 0x0c08
class NukeRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "nuke-room ${roomParam.toSource()}"
    override val length: Int
        get() = 2 + roomParam.byteCount
}

// 0x0c09
class LockScriptInstr(val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-script $scriptParam"
    override val length: Int
        get() = 2 + scriptParam.byteCount
}

// 0x0c0a
class LockSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 2 + soundParam.byteCount
}

// 0x0c0b
class LockCostumeInstr(val costumeParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-costume ${costumeParam.toSource()}"
    override val length: Int
        get() = 2 + costumeParam.byteCount
}

// 0x0c0c
class LockRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-room ${roomParam.toSource()}"
    override val length: Int
        get() = 2 + roomParam.byteCount
}

// 0x0c0d
class UnlockScriptInstr(val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String = "unlock-script ${scriptParam.toSource()}"
    override val length: Int
        get() = 2 + scriptParam.byteCount
}

// 0x0c0e
class UnlockSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "unlock-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 2 + soundParam.byteCount
}

// 0x0c0f
class UnlockCostumeInstr(val costumeParam: ByteParam) : Instruction {
    override fun toSource(): String = "unlock-costume ${costumeParam.toSource()}"
    override val length: Int
        get() = 2 + costumeParam.byteCount
}

// 0x0c10
class UnlockRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "unlock-room ${roomParam.toSource()}"
    override val length: Int
        get() = 2 + roomParam.byteCount
}

// 0x0c11
object ClearHeapInstr : Instruction {
    override fun toSource(): String = "clear-heap"

    override val length: Int
        get() = 2
}

// 0x0c12
class LoadCharsetInstr(val charsetParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-charset ${charsetParam.toSource()}"
    override val length: Int
        get() = 2 + charsetParam.byteCount
}

// 0x0c13
class NukeCharsetInstr(val charsetParam: ByteParam) : Instruction {
    override fun toSource(): String = "nuke-charset ${charsetParam.toSource()}"

    override val length: Int
        get() = 2 + charsetParam.byteCount
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

// 0x10/0x90
class GetOwnerOfInstr(val resultVar: ResultVar, val objectParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := owner-of ${objectParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + objectParam.byteCount
}

// 0x11/0x51/0x91/0xd1
class DoAnimationInstr(val actorParam: ByteParam, val animationParam: ByteParam) : Instruction {
    override fun toSource(): String = "do-animation ${actorParam.toSource()} ${animationParam.toSource()}"

    override val length: Int
        get() = 1 + actorParam.byteCount + animationParam.byteCount
}

// 0x12/0x92
class CameraPanToInstr(val xParam: WordParam) : Instruction {
    override fun toSource(): String = "camera-pan-to ${xParam.toSource()}"
    override val length: Int
        get() = 1 + xParam.byteCount
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

    // 0x04
    class WalkAnimation(val animationParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + animationParam.byteCount
        override val source: String
            get() = "walk-animation ${animationParam.toSource()}"
    }

    // 0x05
    class TalkAnimation(val fromParam: ByteParam, val toParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + fromParam.byteCount + toParam.byteCount
        override val source: String
            get() = "talk-animation ${fromParam.toSource()} ${toParam.toSource()}"
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
    class Name(val name: ScummStringBytesV5) : Sub {
        override val byteSize: Int
            get() = 1 /* opcode */ + name.byteCount
        override val source: String
            get() = "actor-name \"${name.toSource()}\""
    }

    // 0x14
    object IgnoreBoxes : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "ignore-boxes"
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
        get() = 2 /* opcode + final 0xff */ + actorParam.byteCount + subs.sumOf { it.byteSize }
}

class PrintInstr(val who: ByteParam, val subs: List<Sub>) : Instruction {
    override fun toSource(): String {
        return "TODO print ${who.toSource()} ${subs.joinToString(" ") { it.source }}"
    }

    override val length: Int
        get() {
            val additional = if (subs.any { it is Text }) 0 else 1  /* 0xff only if there is no text command */
            return 2 /* opcode + who */ + additional + subs.sumOf { it.byteSize }
        }

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

    // 0x04
    object Center : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "center"
    }

    // 0x07
    object Overhead : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "overhead"
    }

    // 0x08
    class SayVoice(val offsetParam: WordParam, val delayParam: WordParam) : Sub {
        override val byteSize: Int
            get() = 1 + offsetParam.byteCount + delayParam.byteCount
        override val source: String
            get() = "say-voice ${offsetParam.toSource()}, ${delayParam.toSource()}"
    }

    class Text(val stringBytes: ScummStringBytesV5) : Sub {
        fun stringBytesToString(stringBytes: ByteArray): String {
            DataInputStream(ByteArrayInputStream(stringBytes)).use { data ->
                // TODO fix
                return buildString {
                    var byte = data.readUnsignedByte()

                    while (byte != 0) {
                        if (byte == 0xff) {
                            val code = data.readUnsignedByte()

                            when (code) {
                                4 -> {
                                    val thing = readResultVar(data)
                                    append("%{${thing.toSource()}}")
                                }
                                else -> append("?0x${code.toHexString()}?")
                            }
                        } else {
                            append(byte.toChar())
                        }

                        byte = data.readUnsignedByte()
                    }
                }
            }
        }

        override val byteSize: Int
            get() = 1 /* opcode 0xf */ + stringBytes.byteCount
        override val source: String
            get() = "text \"${stringBytes.toSource()}\""  // TODO embedded codes
    }

    class Invalid(val opcode: Int) : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "!invalid 0x$opcode (0x${opcode.and(0xf)})!"
    }
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

object StopSentenceScriptInstr : Instruction {
    override fun toSource(): String = "stop-sentence-script"
    override val length: Int
        get() = 2
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
class AssignLiteralToStringInstr(val stringParam: ByteParam, val stringBytes: ScummStringBytesV5) : Instruction {
    override fun toSource(): String {
        val stringSourceName = if (stringParam is ImmediateByteParam) {
            v5StringVarName(stringParam.byte)
        } else {
            stringParam.toSource()
        }

        return "$stringSourceName := \"${stringBytes.toSource()}\""
    }

    override val length: Int
        get() = 2 /* opcode 0x2701 */ + stringParam.byteCount + stringBytes.byteCount
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
class JumpIfVarNotZeroInstr(val varSpec: VarSpec, val jumpOffset: Int) : Instruction {
    override fun toSource(): String = "if ${varSpec.toSourceName()} != 0 jump $jumpOffset"
    override val length: Int
        get() = 1 + varSpec.byteCount + 2 /* offset */
}

// 0x29/0x69/0xa9/0xe9
class OwnerOfIsInstr(val objectParam: WordParam, val ownerParam: ByteParam) : Instruction {
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

// 0x2e
class SleepForJiffiesInstr(val jiffies: Int) : Instruction {
    override fun toSource(): String = "sleep-for $jiffies jiffies"
    override val length: Int
        get() = 4 /* opcode + jiffies */
}

// 0x30/0xb0 0x01
class SetBoxToInstr(val aParam: ByteParam, val bParam: ByteParam) : Instruction {
    override fun toSource(): String = "set-box ${aParam.toSource()} to ${bParam.toSource()}"

    override val length: Int
        get() = 1 + aParam.byteCount + bParam.byteCount
}

// 0x30/0xb0 0x02
class SetBoxScaleInstr(val boxParam: ByteParam, val scaleParam: ByteParam) : Instruction {
    override fun toSource(): String = "set-box ${boxParam.toSource()} scale ${scaleParam.toSource()}"

    override val length: Int
        get() = 1 + boxParam.byteCount + scaleParam.byteCount
}

// 0x30/0xb0 0x03
class SetBoxSlotInstr(val boxParam: ByteParam, val slotParam: ByteParam) : Instruction {
    override fun toSource(): String = "set-box ${boxParam.toSource()} slot ${slotParam.toSource()}"

    override val length: Int
        get() = 1 + boxParam.byteCount + slotParam.byteCount
}

// 0x30/0xb0 0x04
object SetBoxPathInstr : Instruction {
    override fun toSource(): String = "set-box-path"
    override val length: Int
        get() = 1
}

// 0x1a/0x9a
class AssignInstr(val resultVar: ResultVar, val valueParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := ${valueParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + valueParam.byteCount
}

// 0x1c/0x9c
class StartSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "start-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 1 + soundParam.byteCount
}

class JumpIfClassOfIsNotInstr(val objectParam: WordParam, val classParams: List<WordParam>, val offset: Int) : Instruction {
    override fun toSource(): String {
        return "if-class-of ${objectParam.toSource()} is-not ${classParams.joinToString(" ") { it.toSource() }} jump $offset"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + objectParam.byteCount + classParams.sumOf { it.byteCount } + classParams.size /* opcode before each class */ + 2 /* jump offset */
}

// 0x32/0xb2
class CameraAtInstr(val atParam: WordParam) : Instruction {
    override fun toSource(): String = "camera-at ${atParam.toSource()}"
    override val length: Int
        get() = 1 + atParam.byteCount
}

// 0x1e/0x3e/0x5e/0x7e/0x9e/0xbe/0xde/0xfe
class WalkActorToXYInstr(val actorParam: ByteParam, val xParam: WordParam, val yParam: WordParam) : Instruction {
    override fun toSource(): String = "walk-actor ${actorParam.toSource()} to ${xParam.toSource()}, ${yParam.toSource()}"
    override val length: Int
        get() = 1 + actorParam.byteCount + xParam.byteCount + yParam.byteCount
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

// 0x04
class RoomPaletteInstr(val rParam: WordParam, val gParam: WordParam, val bParam: WordParam, val slotParam: ByteParam) : Instruction {
    override fun toSource(): String = "palette ${rParam.toSource()}, ${gParam.toSource()}, ${bParam.toSource()} in-slot ${slotParam.toSource()}"

    override val length: Int
        get() = 3 /* opcode */ + rParam.byteCount + gParam.byteCount + bParam.byteCount + slotParam.byteCount
}

// 0x08
class RoomIntensityInstr(val scaleParam: ByteParam, val fromParam: ByteParam, val toParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return "room-intensity scale ${scaleParam.toSource()} from ${fromParam.toSource()} to ${toParam.toSource()}"
    }

    override val length: Int
        get() = 2 + scaleParam.byteCount + fromParam.byteCount + toParam.byteCount
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

class CursorImageInstr(val cursorParam: ByteParam, val imageParam: ByteParam) : Instruction {
    override fun toSource(): String = "cursor ${cursorParam.toSource()} image ${imageParam.toSource()}"

    override val length: Int
        get() = 2 + cursorParam.byteCount + imageParam.byteCount
}

class CursorHotspotInstr (val cursorParam: ByteParam, val hotspotXParam: ByteParam, val hotspotYParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return "cursor ${cursorParam.toSource()} hotspot ${hotspotXParam.toSource()}, ${hotspotYParam.toSource()}"
    }

    override val length: Int
        get() = 2 + cursorParam.byteCount + hotspotXParam.byteCount + hotspotYParam.byteCount
}

class CursorInstr(val cursorParam: ByteParam) : Instruction {
    override fun toSource(): String = "cursor ${cursorParam.toSource()}"

    override val length: Int
        get() = 2 + cursorParam.byteCount
}

class CharsetInstr(val charsetParam: ByteParam) : Instruction {
    override fun toSource(): String = "charset ${charsetParam.toSource()}"
    override val length: Int
        get() = 2 + charsetParam.byteCount
}

class CharsetEInstr(val args: List<WordParam>) : Instruction  {
    override fun toSource(): String = "charset-e ${args.joinToString(" ") { it.toSource() }}"
    override val length: Int
        get() = 3 /* opcode, final 0xff */ + args.sumOf { it.byteCount } + args.size

}

// 0x34/0x74/0xb4/0xf4
class AssignProximityInstr(val resultVar: ResultVar, val object1Param: WordParam, val object2Param: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := proximity ${object1Param.toSource()}, ${object2Param.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + object1Param.byteCount + object2Param.byteCount
}

// 0x36/0x76/0xb6/0xf6
class WalkActorToObjectInstr(val actorParam: ByteParam, val objectParam: WordParam) : Instruction {
    override fun toSource(): String {
        return "walk ${actorParam.toSource()} to-object ${objectParam.toSource()}"
    }

    override val length: Int
        get() = 1 + actorParam.byteCount + objectParam.byteCount
}

// 0x37/0x77/0xb7/0xf7
class StartObjectInstr(val objectParam: WordParam, val verbParam: ByteParam, val args: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "start-object ${objectParam.toSource()} verb ${verbParam.toSource()} ${args.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + objectParam.byteCount + verbParam.byteCount + args.sumOf { it.byteCount } + args.size /* opcode before each arg */
}

// 0x38/0xb8
class JumpIfVarLessInstr(val varSpec: VarSpec, val valueParam: WordParam, val offset: Int) : Instruction {
    override fun toSource(): String {
        return "if ${varSpec.toSourceName()} < ${valueParam.toSource()} jump $offset"
    }

    override val length: Int
        get() = 1 + varSpec.byteCount + valueParam.byteCount + 2
}

// 0x3a/0xba
class SubtractInstr(val resultVar: ResultVar, val valueParam: WordParam) : Instruction {
    override fun toSource(): String {
        return "${resultVar.toSource()} -:= ${valueParam.toSource()}"
    }

    override val length: Int
        get() = 1 + resultVar.byteCount + valueParam.byteCount
}

// 0x3d, 0x7d, 0xbd, 0xfd
class AssignFindInventoryInstr(val resultVar: ResultVar, val xParam: ByteParam, val yParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := find-inventory ${xParam.toSource()}, ${yParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + xParam.byteCount + yParam.byteCount
}

// 0x3f, 0x7f, 0xbf, 0xff
class DrawBoxInstr(val xParam: WordParam, val yParam: WordParam, val x2Param: WordParam, val y2Param: WordParam, val colorParam: ByteParam) : Instruction {
    override fun toSource(): String = "draw-box ${xParam.toSource()}, ${yParam.toSource()} to ${x2Param.toSource()}, ${y2Param.toSource()} color ${colorParam.toSource()}"

    override val length: Int
        get() = 2 + xParam.byteCount + yParam.byteCount + x2Param.byteCount + y2Param.byteCount + colorParam.byteCount

    fun emit(out: DataOutput) {
        val opcode1 = 0x3f.or(paramBits(xParam, 0x80)).or(paramBits(yParam, 0x40))
        out.writeByte(opcode1)
        xParam.emitBytes(out)
        yParam.emitBytes(out)

        val opcode2 = 0x0.or(paramBits(x2Param, 0x80).or(paramBits(y2Param, 0x40).or(paramBits(colorParam, 0x20))))
        out.writeByte(opcode2)
        x2Param.emitBytes(out)
        y2Param.emitBytes(out)
        colorParam.emitBytes(out)
    }

    fun emitBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out -> emit(out) }
        return baos.toByteArray()
    }

    private fun paramBits(param: WordParam, bits: Int): Int = if (!param.isImmediate) bits else 0
    private fun paramBits(param: ByteParam, bits: Int): Int = if (!param.isImmediate) bits else 0
}

// 0x40
class CutSceneInstr(val scriptArgs: List<WordParam>) : Instruction {
    override fun toSource(): String = "cut-scene ${scriptArgs.joinToString(" ") { it.toSource() }}"
    override val length: Int
        get() = 2 /* opcode, final 0xff */ + scriptArgs.sumOf { it.byteCount } + scriptArgs.size /* opcode before each arg */
}

// 0x42/0xc2
class ChainsScriptInstr(val scriptParam: ByteParam, val scriptArgs: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "chains-script ${scriptParam.toSource()} ${scriptArgs.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, final 0xff */ + scriptParam.byteCount + scriptArgs.sumOf { it.byteCount } + scriptArgs.size
}

// 0x43/0xc3
class AssignActorXInstr(val resultVar: ResultVar, val objectParam: WordParam) : Instruction {
    override fun toSource(): String {
        return "${resultVar.toSource()} := actor-x ${objectParam.toSource()}"
    }

    override val length: Int
        get() = 1 + resultVar.byteCount + objectParam.byteCount
}

// 0x44/0xc4
class JumpIfVarLessEqualInstr(val varSpec: VarSpec, val valueParam: WordParam, val jumpOffset: Int) : Instruction {
    override fun toSource() = "if ${varSpec.toSourceName()} <= ${valueParam.toSource()} jump $jumpOffset"
    override val length: Int
        get() = 1 + varSpec.byteCount + valueParam.byteCount + 2
}

// 46 ++@var
class IncrementVarOpcode(val varSpec: VarSpec) : Opcode("++var", 3) {
    override fun toSource() = "++${nameForVar(varSpec)}"
}

typealias ObjSpec = Int

// 0x4c
class SoundKludgeInstr(val params: List<WordParam>) : Instruction {
    override fun toSource(): String {
        return "sound-kludge ${params.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcd, final 0xff */ + params.sumOf { it.byteCount } + params.size /* opcode before each param */
}

// 0x52/0xd2
class CameraFollowInstr(val actorParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return "camera-follow ${actorParam.toSource()}"
    }

    override val length: Int
        get() = 1 + actorParam.byteCount
}

// 0x54/0xd4
class NewNameOfInstr(val objectParam: WordParam, val name: ScummStringBytesV5) : Instruction {
    override fun toSource(): String {
        return "new-name-of ${objectParam.toSource()} is \"${name.toSource()}\""
    }

    override val length: Int
        get() = 1 /* opcode */ + objectParam.byteCount + name.byteCount

}

// 0x58 <byte != 0x00>
object BeginOverrideInstr : Instruction {
    override fun toSource(): String {
        return "begin-override"
    }

    override val length: Int
        get() = 2
}

// 0x58 0x00
object EndOverrideInstr : Instruction {
    override fun toSource(): String {
        return "end-override"
    }

    override val length: Int
        get() = 2
}

// 0x5a/0xda
class AddAssignInstr(val resultVar: ResultVar, val valueParam: WordParam) : Instruction {
    override fun toSource(): String {
        return "${resultVar.toSource()} += ${valueParam.toSource()}"
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

// 0x63/0xe3
class ActorFacingInstr(val resultVar: ResultVar, val actorParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} = actor-facing ${actorParam.toSource()}"

    override val length: Int
        get() =  1 + resultVar.byteCount + actorParam.byteCount
}

// 0x67	@var = width @object
class AssignObjectWidthToVar(val varSpec: VarSpec, val objSpec: ObjSpec) : Opcode("<var> = width <object>", 4) {
}

// 0x68/0xe8
class ScriptRunningInstr(val resultVar: ResultVar, val scriptParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} = script-running ${scriptParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + scriptParam.byteCount
}

// 0x6c/0xec
class AssignActorWidthInstr(val resultVar: ResultVar, val actorParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := actor-width ${actorParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + actorParam.byteCount
}

// 0x6e/0xee
class StopObjectInstr(val objectParam: WordParam) : Instruction {
    override fun toSource(): String = "stop-object ${objectParam.toSource()}"
    override val length: Int
        get() = 1 + objectParam.byteCount
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

    fun emit(out: DataOutput) {
        out.writeByte(0x72)
        roomParam.emitBytes(out)
    }

    fun emitBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out -> emit(out) }
        return baos.toByteArray()
    }
}

// 0x78/0xf8
class JumpIfVarLessOrEqualInstr(val variable: ResultVar, val valueParam: WordParam, val offset: Int) : Instruction {
    override fun toSource(): String = "if ${variable.toSource()} <= ${valueParam.toSource()} jump $offset"

    override val length: Int
        get() = 1 + variable.byteCount + valueParam.byteCount + 2
}

// 0x7a/0xfa
class VerbInstr(val verbParam: ByteParam, val subs: List<Sub>) : Instruction {
    override fun toSource(): String {
        return "verb ${verbParam.toSource()} ${subs.joinToString { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, final 0xff */ + verbParam.byteCount + subs.sumOf { it.byteCount }

    sealed interface Sub {
        val byteCount: Int
        fun toSource(): String
    }

    // 0x01
    class Image(val imageParam: WordParam) : Sub {
        override val byteCount: Int = 1 + imageParam.byteCount
        override fun toSource(): String = "image ${imageParam.toSource()}"
    }

    // 0x02
    class Name(val nameBytes: ScummStringBytesV5) : Sub {
        override val byteCount: Int
            get() = 1 + nameBytes.byteCount

        override fun toSource(): String = "name \"${nameBytes.toSource()}\""
    }

   // 0x03
    class Color(val colorParam: ByteParam) : Sub {
        override val byteCount: Int
            get() = 1 + colorParam.byteCount

        override fun toSource(): String = "color ${colorParam.toSource()}"
    }

    // 0x04
    class HiColor(val colorParam: ByteParam) : Sub {
        override val byteCount: Int
            get() = 1 + colorParam.byteCount

        override fun toSource(): String = "hi-color ${colorParam.toSource()}"
    }

    // 0x05
    class At(val leftParam: WordParam, val topParam: WordParam) : Sub {
        override val byteCount: Int
            get() = 1 + leftParam.byteCount + topParam.byteCount

        override fun toSource(): String {
            return "at ${leftParam.toSource()}, ${topParam.toSource()}"
        }
    }

    // 0x06
    object On : Sub {
        override val byteCount: Int = 1
        override fun toSource(): String = "on"
    }

    // 0x07
    object Off : Sub {
        override val byteCount: Int = 1
        override fun toSource(): String = "off"
    }

    // 0x08
    object Delete : Sub {
        override val byteCount: Int = 1
        override fun toSource(): String = "delete"
    }

    // 0x09
    object New : Sub {
        override val byteCount: Int = 1
        override fun toSource(): String = "new"
    }

    // 0x10
    class DimColor(val colorParam: ByteParam) : Sub {
        override val byteCount: Int = 1 + colorParam.byteCount
        override fun toSource(): String = "dim-color ${colorParam.toSource()}"
    }

    // 0x11
    object Dim : Sub {
        override val byteCount: Int = 1
        override fun toSource(): String = "dim"
    }

    // 0x12
    class Key(val keyParam: ByteParam) : Sub {
        override val byteCount: Int = 1 + keyParam.byteCount
        override fun toSource(): String = "key ${keyParam.toSource()}"
    }

    // 0x13
    object Center : Sub {
        override val byteCount: Int = 1
        override fun toSource(): String = "center"
    }

    // 0x14
    class NameString(val stringParam: WordParam) : Sub {
        override val byteCount: Int = 1 + stringParam.byteCount
        override fun toSource(): String = "name-string ${stringParam.toSource()}"
    }

    // 0x16
    class ImageInRoom(val imageParam: WordParam, val roomParam: ByteParam) : Sub {
        override val byteCount: Int = 1 + imageParam.byteCount + roomParam.byteCount
        override fun toSource(): String = "image ${imageParam.toSource()} in-room ${roomParam.toSource()}"
    }

    // 0x17
    class BakColor(val colorParam: ByteParam) : Sub {
        override val byteCount: Int = 1 + colorParam.byteCount
        override fun toSource(): String = "bakcolor ${colorParam.toSource()}"
    }

    class Invalid(val bytes: ByteArray) : Sub {
        override val byteCount: Int
            get() = bytes.size

        override fun toSource(): String {
            return "!invalid ${bytes.toHexString()}!"
        }
    }
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

// 0xab 0x01
class SaveVerbsInstr(val fromIdParam: ByteParam, val toIdParam: ByteParam, val saveIdParam: ByteParam) : Instruction {
    override fun toSource(): String = "save-verbs from-id ${fromIdParam.toSource()}, to-id ${toIdParam.toSource()}, save-id ${saveIdParam.toSource()}"

    override val length: Int
        get() = 2 + fromIdParam.byteCount + toIdParam.byteCount + saveIdParam.byteCount
}

// 0xab 0x02
class RestoreVerbsInstr(val fromIdParam: ByteParam, val toIdParam: ByteParam, val saveIdParam: ByteParam) : Instruction {
    override fun toSource(): String = "restore-verbs from-id ${fromIdParam.toSource()}, to-id ${toIdParam.toSource()}, save-id ${saveIdParam.toSource()}"

    override val length: Int
        get() = 2 + fromIdParam.byteCount + toIdParam.byteCount + saveIdParam.byteCount
}

// 0xab 0x03
class DeleteVerbsInstr(val fromIdParam: ByteParam, val toIdParam: ByteParam, val saveIdParam: ByteParam) : Instruction {
    override fun toSource(): String = "delete-verbs from-id ${fromIdParam.toSource()}, to-id ${toIdParam.toSource()}, save-id ${saveIdParam.toSource()}"

    override val length: Int
        get() = 2 + fromIdParam.byteCount + toIdParam.byteCount + saveIdParam.byteCount
}

// 0xc0
object EndCutSceneInstr : Instruction {
    override fun toSource(): String = "end-cut-scene"

    override val length: Int
        get() = 1
}

// 0xc6
class DecrementVar(val resultVar: ResultVar) : Instruction {
    override fun toSource(): String = "--${nameForVar(resultVar.varSpec)}"
    override val length: Int
        get() = 1 + resultVar.byteCount
}

// 0xcc
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
    override fun toSource(): String {
        return "TODO expression; ${resultVar.toSource()} = ${subs.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, final 0xff */ + resultVar.byteCount + subs.sumOf { it.byteCount }

    sealed interface Sub {
        val byteCount: Int
        fun toSource(): String
    }

    // 0x1
    class Value(val valueParam: WordParam) : Sub {
        override val byteCount: Int
            get() = 1 + valueParam.byteCount

        override fun toSource(): String = valueParam.toSource()

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

    fun emitTo(out: DataOutput) {
        out.writeByte(0xae)
        out.writeByte(0x02)
    }
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
    fun emitBytes(out: DataOutput)
}

class GlobalVarSpec(val varNum: Int) : VarSpec {
    override fun toSourceName() = nameForGlobalVar(this)
    override val byteCount: Int
        get() = 2

    override fun emitBytes(out: DataOutput) {
        out.writeByte(varNum.shr(8))
        out.writeByte(varNum)
    }
}

class LocalVarSpec(val varNum: Int) : VarSpec {
    override fun toSourceName() = "local$varNum"
    override val byteCount: Int
        get() = 2

    override fun emitBytes(out: DataOutput) {
        // TODO set bits for local var
        out.writeByte(varNum.shr(8))
        out.writeByte(varNum)
    }
}

class BitVarSpec(val varNum: Int, val bitNum: Int) : VarSpec {
    override fun toSourceName() = "bit$varNum:$bitNum"
    override val byteCount: Int
        get() = 2

    override fun emitBytes(out: DataOutput) {
        // TODO set bits for bit var
        out.writeByte(varNum.shr(8))
        out.writeByte(varNum)
    }
}

class VarSpec0x2000(val varNum: Int) : VarSpec {
    override fun toSourceName() = "TODOvar0x2000_$varNum"
    override val byteCount: Int
        get() = 2

    override fun emitBytes(out: DataOutput) {
        // TODO set bits for var spec
        out.writeByte(varNum.shr(8))
        out.writeByte(varNum)
    }
}

class JumpIfVarNotEqualInstr(val varSpec: VarSpec, val valueParam: WordParam, val offset: Int) : Instruction {
    override fun toSource(): String = "if ${nameForVar(varSpec)} != ${valueParam.toSource()} jump $offset"
    override val length: Int
        get() = 1 + varSpec.byteCount + valueParam.byteCount + 2
}

// 0x08/0x88
class JumpIfVarEqualInstr(val varSpec: VarSpec, val valueParam: WordParam, val offset: Int) : Instruction {
    override fun toSource(): String = "if ${nameForVar(varSpec)} == ${valueParam.toSource()} jump $offset"
    override val length: Int
        get() = 1 + varSpec.byteCount + valueParam.byteCount + 2
}

val globalVarNames = mapOf(
        1 to "main-actor",
        6 to "machine-speed",
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
    val isImmediate: Boolean
    fun emitBytes(out: DataOutput)
}

data class ImmediateByteParam(val byte: Int) : ByteParam {
    override val byteCount: Int
        get() = 1

    override fun toSource(): String = byte.toString()
    override val isImmediate: Boolean
        get() = true

    override fun emitBytes(out: DataOutput) {
        out.writeByte(byte)
    }
}

data class ByteVarParam(val varSpec: VarSpec) : ByteParam {
    override val byteCount: Int
        get() = varSpec.byteCount

    override fun toSource(): String = varSpec.toSourceName()
    override val isImmediate: Boolean
        get() = false

    override fun emitBytes(out: DataOutput) {
        varSpec.emitBytes(out)
    }
}

@Deprecated("use alternative taking parameters")
fun DataInput.readByteParam() : ByteParam { // TODO ByteVarParam
    return ImmediateByteParam(readByte().toInt().and(0xff))
}

sealed interface WordParam {
    val byteCount: Int
    fun toSource(): String
    val isImmediate: Boolean
    fun emitBytes(out: DataOutput)
}

data class ImmediateWordParam(val word: Int) : WordParam {
    override val byteCount: Int
        get() = 2

    override fun toSource(): String {
        return word.toString()
    }

    override val isImmediate: Boolean
        get() = true

    override fun emitBytes(out: DataOutput) {
        out.writeByte(word)
        out.writeByte(word.shr(8))
    }
}

data class WordVarParam(val varSpec: VarSpec) : WordParam {
    override val byteCount: Int
        get() = varSpec.byteCount

    override fun toSource(): String {
        return varSpec.toSourceName()
    }

    override val isImmediate: Boolean
        get() = false

    override fun emitBytes(out: DataOutput) {
        // TODO non-global vars
        varSpec.emitBytes(out)
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

        0x2, 0x82 -> StartMusicInstr(readByteParam(data, opcode, 0x80))

        0x3, 0x83 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            ActorRoomInstr(resultVar, actorParam)
        }

        0x4, 0x84 -> {
            val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

            val opcode = data.readByte()
            val varSpec = data.readShortLittleEndian().toInt()
            val value = data.readShortLittleEndian().toInt()
            val skipOffset = data.readShortLittleEndian().toInt()

            IfVarLessOrEqualOpcode(toVarSpec(varSpec), value, skipOffset)
        }

        0x05, 0x85 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val opcode2 = data.readUnsignedByte()

            when (opcode2.and(0x1f)) {
                1 -> {
                    val xParam = readWordParam(data, opcode2, 0x80)
                    val yParam = readWordParam(data, opcode2, 0x40)
                    DrawObjectAtInstr(objectParam, xParam, yParam)
                }
                2 -> {
                    val stateParam = readWordParam(data, opcode2, 0x80)
                    DrawObjectImageInstr(objectParam, stateParam)
                }

                0x1f -> DrawObject1fInstr(objectParam)

                else -> {
                    val numBytes = 2 + objectParam.byteCount
                    InvalidInstruction(bytes.sliceArray(offset until offset + numBytes))
                }
            }
        }

        0x06, 0x86 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            ActorElevationInstr(resultVar, actorParam)
        }

        0x07, 0x47, 0x87, 0xc7 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val stateParam = readByteParam(data, opcode, 0x40)

            StateOfObjectIsInstr(objectParam, stateParam)
        }

        0x08, 0x88 -> decompileJumpIfEqualInstr(bytes, offset)

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

        0x0b, 0x4b, 0x8b, 0xcb -> {
            val resultVar = readResultVar(data)
            val aParam = readWordParam(data, opcode, 0x80)
            val bParam = readWordParam(data, opcode, 0x40)

            AssignValidVerbInstr(resultVar, aParam, bParam)
        }

        0x0c, 0x8c -> decompileHeapStuffOpcode(bytes, offset)

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

        0x10, 0x90 -> {
            val resultVar = readResultVar(data)
            val objectParam = readWordParam(data, opcode, 0x80)

            GetOwnerOfInstr(resultVar, objectParam)
        }

        0x11, 0x51, 0x91, 0xd1 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val animationParam = readByteParam(data, opcode, 0x40)

            DoAnimationInstr(actorParam, animationParam)
        }

        0x12, 0x92 -> CameraPanToInstr(readWordParam(data, opcode, 0x80))

        0x13, 0x53, 0x93, 0xd3 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val subs = mutableListOf<ActorInstr.Sub>()

            var opcode2 = data.readUnsignedByte()

            while (opcode2 != 0xff) {
                val sub = when (opcode2.and(0x1f)) {
                    1 -> ActorInstr.Costume(readByteParam(data, opcode2, 0x80))
/*
                    2 -> {
                        val xParam = readByteParam(data, opcode2, 0x80)
                        val yParam = readByteParam(data, opcode2, 0x40)
                        ActorInstr.StepDist(xParam, yParam)
                    }
*/
//                    3 -> ActorInstr.Sound(readByteParam(data, opcode2, 0x80))
                    4 -> ActorInstr.WalkAnimation(readByteParam(data, opcode2, 0x80))
                    5 -> ActorInstr.TalkAnimation(readByteParam(data, opcode2, 0x80), readByteParam(data, opcode2, 0x40))
//                    6 -> ActorInstr.StandAnimation(readByteParam(data, opcode2, 0x80))
//                    7 -> ActorInstr.Animation(readByteParam(data, opcode2, 0x80), readByteParam(data, opcode2, 0x40), readByteParam(data, opcode2, 0x20))
                    8 -> ActorInstr.Default
//                    9 -> ActorInstr.Elevation(readWordParam(data, opcode2, 0x80))
//                    10 -> ActorInstr.AnimationDefault
/*
                    11 -> {
                        val paletteParam = readByteParam(data, opcode2, 0x80)
                        val slotParam = readByteParam(data, opcode2, 0x40)
                        ActorInstr.Palette(paletteParam, slotParam)
                    }
*/
                    12 -> ActorInstr.TalkColor(readByteParam(data, opcode2, 0x80))
                    13 -> ActorInstr.Name(data.readScummStringBytes())
//                    14 -> ActorInstr.InitAnimation(readByteParam(data, opcode2, 0x80))
//                    16 -> ActorInstr.Width(readByteParam(data, opcode2, 0x80))
//                    17 -> ActorInstr.Scale(readByteParam(data, opcode2, 0x80), readByteParam(data, opcode2, 0x40))
//                    18 -> ActorInstr.NeverZClip
//                    19 -> ActorInstr.AlwaysZClip(readByteParam(data, opcode2, 0x80))
                    20 -> ActorInstr.IgnoreBoxes
//                    21 -> ActorInstr.FollowBoxes
//                    22 -> ActorInstr.AnimationSpeed(readByteParam(data, opcode2, 0x80))
//                    23 -> ActorInstr.Shadow(readByteParam(data, opcode2, 0x80))
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

            if (verbParam is ImmediateByteParam && verbParam.byte == 0xfe) {
                StopSentenceScriptInstr
            } else {
                val object1Param = readWordParam(data, opcode, 0x40)
                val object2Param = readWordParam(data, opcode, 0x20)

                DoSentenceWithInstr(verbParam, object1Param, object2Param)
            }
        }

        0x1a, 0x9a -> {
            val resultVar = readResultVar(data)
            val valueParam = readWordParam(data, opcode, 0x80)
            AssignInstr(resultVar, valueParam)
        }

        0x1c, 0x09c -> StartSoundInstr(readByteParam(data, opcode, 0x80))

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

        0x1e, 0x3e, 0x5e, 0x7e, 0x9e, 0xbe, 0xde, 0xfe -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val xParam = readWordParam(data, opcode, 0x40)
            val yParam = readWordParam(data, opcode, 0x20)

            WalkActorToXYInstr(actorParam, xParam, yParam)
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

            JumpIfVarNotZeroInstr(varSpec, jumpOffset)
        }

        0x29, 0x69, 0xa9, 0xe9 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val ownerParam = readByteParam(data, opcode, 0x40)

            OwnerOfIsInstr(objectParam, ownerParam)
        }

        0x2c -> decompileCursorInstruction(bytes, offset)

        0x2d, 0x6d, 0xad, 0xed -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val roomParam = readByteParam(data, opcode, 0x40)

            PutActorInRoomInstr(actorParam, roomParam)
        }

        0x2e -> {
            val jiffies = data.readUnsignedByte()
                .or(data.readUnsignedByte().shl(8))
                .or(data.readUnsignedByte().shl(16))

            SleepForJiffiesInstr(jiffies)
        }

        0x30, 0xb0 -> {
            val opcode2 = data.readUnsignedByte()

            return when (opcode2.and(0x1f)) {
                1 -> {
                    val aParam = readByteParam(data, opcode2, 0x80)
                    val bParam = readByteParam(data, opcode2, 0x40)

                    SetBoxToInstr(aParam, bParam)
                }

                2 -> {
                    val boxParam = readByteParam(data, opcode2, 0x80)
                    val scaleParam = readByteParam(data, opcode2, 0x40)

                    SetBoxScaleInstr(boxParam, scaleParam)
                }

                3 -> {
                    val boxParam = readByteParam(data, opcode2, 0x80)
                    val slotParam = readByteParam(data, opcode2, 0x40)

                    SetBoxSlotInstr(boxParam, slotParam)
                }

                4 -> SetBoxPathInstr
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
        }

        0x32, 0xb2 -> CameraAtInstr(readWordParam(data, opcode, 0x80))

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

                4 -> {
                    val rParam = readWordParam(data, opcode2, 0x80)
                    val gParam = readWordParam(data, opcode2, 0x40)
                    val bParam = readWordParam(data, opcode2, 0x20)

                    val opcode3 = data.readUnsignedByte()
                    val slotParam = readByteParam(data, opcode3, 0x80)

                    RoomPaletteInstr(rParam, gParam, bParam, slotParam)
                }

                8 -> {
                    val scaleParam = readByteParam(data, opcode2, 0x80)
                    val fromParam = readByteParam(data, opcode2, 0x40)
                    val toParam = readByteParam(data, opcode2, 0x20)

                    RoomIntensityInstr(scaleParam, fromParam, toParam)
                }

                0xa -> FadesInstr(readWordParam(data, opcode2, 0x80))
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
        }

        0x34, 0x74, 0xb4, 0xf4 -> {
            val resultVar = readResultVar(data)
            val object1Param = readWordParam(data, opcode, 0x80)
            val object2Param = readWordParam(data, opcode, 0x40)

            AssignProximityInstr(resultVar, object1Param, object2Param)
        }

        0x36, 0x76, 0xb6, 0xf6 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            val objectParam = readWordParam(data, opcode, 0x40)

            WalkActorToObjectInstr(actorParam, objectParam)
        }

        0x37, 0x77, 0xb7, 0xf7 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val scriptParam = readByteParam(data, opcode, 0x40)
            val args = readScriptArgs(data)

            StartObjectInstr(objectParam, scriptParam, args)
        }

        0x38, 0xb8 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val valueParam = readWordParam(data, opcode, 0x80)
            val offset = data.readShortLittleEndian().toInt()

            JumpIfVarLessInstr(varSpec, valueParam, offset)
        }

        0x3a, 0xba -> {
            val resultVar = readResultVar(data)
            val valueParam = readWordParam(data, opcode, 0x80)

            SubtractInstr(resultVar, valueParam)
        }

        0x3d, 0x7d, 0xbd, 0xfd -> {
            val resultVar = readResultVar(data)
            val xParam = readByteParam(data, opcode, 0x80)
            val yParam = readByteParam(data, opcode, 0x40)

            AssignFindInventoryInstr(resultVar, xParam, yParam)
        }

        0x3f, 0x7f, 0xbf, 0xff -> {
            val xParam = readWordParam(data, opcode, 0x80)
            val yParam = readWordParam(data, opcode, 0x40)

            val opcode2 = data.readUnsignedByte()
            val x2Param = readWordParam(data, opcode2, 0x80)
            val y2Param = readWordParam(data, opcode2, 0x40)
            val colorParam = readByteParam(data, opcode2, 0x20)

            DrawBoxInstr(xParam, yParam, x2Param, y2Param, colorParam)
        }

        0x40 -> CutSceneInstr(readScriptArgs(data))

        0x42, 0xc2 -> {
            val scriptParam = readByteParam(data, opcode, 0x80)
            val scriptArgs = readScriptArgs(data)

            ChainsScriptInstr(scriptParam, scriptArgs)
        }

        0x43, 0xc3 -> {
            val resultVar = readResultVar(data)
            val objectParam = readWordParam(data, opcode, 0x80)

            AssignActorXInstr(resultVar, objectParam)
        }

        0x44, 0xc4 -> decompileIfVarGreaterThanOpcode(bytes, offset)

        0x46 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            IncrementVarOpcode(varSpec)
        }

        0x48, 0xc8 -> decompileIfVarEqualOpcode(bytes, offset)

        0x4c -> SoundKludgeInstr(readScriptArgs(data))

        0x52, 0xd2 -> {
            val actorParam = readByteParam(data, opcode, 0x80)
            CameraFollowInstr(actorParam)
        }

        0x54, 0xd4 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val nameStringBytes = data.readScummStringBytes()

            NewNameOfInstr(objectParam, nameStringBytes)
        }

        0x58 -> if (data.readUnsignedByte() != 0) BeginOverrideInstr else EndOverrideInstr

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

        0x62, 0xe2 -> StopScriptInstr(readByteParam(data, opcode, 0x80))

        0x63, 0xe3 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            ActorFacingInstr(resultVar, actorParam)
        }

        0x67 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val objSpec = toObjSpec(data.readByte().toInt())
            AssignObjectWidthToVar(varSpec, objSpec)
        }

        0x68, 0xe8 -> {
            val resultVar = readResultVar(data)
            val scriptParam = readByteParam(data, opcode, 0x80)

            ScriptRunningInstr(resultVar, scriptParam)
        }

        0x6c, 0xec -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            AssignActorWidthInstr(resultVar, actorParam)
        }

        0x6e, 0xee -> StopObjectInstr(readWordParam(data, opcode, 0x80))

        0x71, 0xf1 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            ActorCostumeInstr(resultVar, actorParam)
        }

        0x72, 0xf2 -> CurrentRoomInstr(readByteParam(data, opcode, 0x80))

        0x78, 0xf8 -> {
            val variable = readResultVar(data)
            val valueParam = readWordParam(data, opcode, 0x80)
            val jumpOffset = data.readShortLittleEndian().toInt()

            JumpIfVarLessOrEqualInstr(variable, valueParam, jumpOffset)
        }

        0x7a, 0xfa -> {
            val verbParam = readByteParam(data, opcode, 0x80)

            var opcode2 = data.readUnsignedByte()

            val subs = buildList {
                while (opcode2 != 0xff) {
                    val sub = when (opcode2.and(0x1f)) {
                        1 -> VerbInstr.Image(readWordParam(data, opcode2, 0x80))
                        2 -> VerbInstr.Name(data.readScummStringBytes())
                        3 -> VerbInstr.Color(readByteParam(data, opcode2, 0x80))
                        4 -> VerbInstr.HiColor(readByteParam(data, opcode2, 0x80))

                        5 -> {
                            val leftParam = readWordParam(data, opcode2, 0x80)
                            val topParam = readWordParam(data, opcode2, 0x40)

                            VerbInstr.At(leftParam, topParam)
                        }

                        6 -> VerbInstr.On
                        7 -> VerbInstr.Off
                        8 -> VerbInstr.Delete
                        9 -> VerbInstr.New
                        16 -> VerbInstr.DimColor(readByteParam(data, opcode2, 0x80))
                        17 -> VerbInstr.Dim
                        18 -> VerbInstr.Key(readByteParam(data, opcode2, 0x80))
                        19 -> VerbInstr.Center
                        20 -> VerbInstr.NameString(readWordParam(data, opcode2, 0x80))

                        22 -> {
                            val imageParam = readWordParam(data, opcode2, 0x80)
                            val roomParam = readByteParam(data, opcode2, 0x40)

                            VerbInstr.ImageInRoom(imageParam, roomParam)
                        }

                        23 -> VerbInstr.BakColor(readByteParam(data, opcode2, 0x80))

                        else -> VerbInstr.Invalid(byteArrayOf(opcode.toByte(), opcode2.toByte()))
                    }

                    add(sub)

                    opcode2 = data.readUnsignedByte()
                }
            }

            VerbInstr(verbParam, subs)
        }

        0x80 -> BreakHereInstr
        0xa0 -> EndScriptInstr

        0xa8 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
            val skipOffset = data.readShortLittleEndian().toInt()

            return JumpIfVarZeroInst(varSpec, skipOffset)
        }

        0xab -> {
            val opcode2 = data.readUnsignedByte()
            val fromIdParam = readByteParam(data, opcode2, 0x80)
            val toIdParam = readByteParam(data, opcode2, 0x40)
            val saveIdParam = readByteParam(data, opcode2, 0x20)

            return when (opcode2) {
                1 -> SaveVerbsInstr(fromIdParam, toIdParam, saveIdParam)
                2 -> RestoreVerbsInstr(fromIdParam, toIdParam, saveIdParam)
                3 -> DeleteVerbsInstr(fromIdParam, toIdParam, saveIdParam)
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
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

        0xc0 -> EndCutSceneInstr
        0xc6 -> DecrementVar(readResultVar(data))

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
            7 -> subs.add(PrintInstr.Overhead)

            8 -> {
                val offsetParam = readWordParam(data, opcode2, 0x80)
                val delayParam = readWordParam(data, opcode2, 0x40)
                subs.add(PrintInstr.SayVoice(offsetParam, delayParam))
            }

            15 -> {
                subs.add(PrintInstr.Text(data.readScummStringBytes()))
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

class ResultVar(val varSpec: VarSpec) {
    val byteCount: Int get() = varSpec.byteCount
    fun toSource(): String = varSpec.toSourceName()
}    // TODO indexed etc.

fun readResultVar(data: DataInput): ResultVar {
    var varNum1 = data.readShortLittleEndian().toInt()

    return ResultVar(toVarSpec(varNum1))
}

fun toObjSpec(objSpec: Int) = objSpec

// raw string bytes, including \0 terminator
data class ScummStringBytesV5(val bytes: ByteArray) {
    val byteCount: Int get() = bytes.size
    fun toSource(): String = bytes.decodeToString()
}

fun DataInput.readScummStringBytes(): ScummStringBytesV5 {    // TODO control code
    val stringBytes = ByteArrayOutputStream()
    var sourceByte = readUnsignedByte()

    while (sourceByte != 0) {
        stringBytes.write(sourceByte)

        if (sourceByte == 0xff) {
            sourceByte = readUnsignedByte()
            stringBytes.write(sourceByte)

            when (sourceByte) {
                1, 2, 3, 8 -> {}
                else -> {
                    stringBytes.write(readUnsignedByte())
                    stringBytes.write(readUnsignedByte())
                }
            }
        }

        sourceByte = readUnsignedByte()
    }

    stringBytes.write(sourceByte)

    return ScummStringBytesV5(stringBytes.toByteArray())
}

// 0x0c/0x8c
fun decompileHeapStuffOpcode(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readUnsignedByte()
    val opcode2 = data.readUnsignedByte()

    val resIdParam = if (opcode2 != 0x11) readByteParam(data, opcode2, 0x80) else ImmediateByteParam(0)

    return when (opcode2) {
        1 -> LoadScriptInstr(resIdParam)
        2 -> LoadSoundInstr(resIdParam)
        3 -> LoadCostumeInstr(resIdParam)
        4 -> LoadRoomInstr(resIdParam)
        5 -> NukeScriptInstr(resIdParam)
        6 -> NukeSoundInstr(resIdParam)
        7 -> NukeCostumeInstr(resIdParam)
        8 -> NukeRoomInstr(resIdParam)
        9 -> LockScriptInstr(resIdParam)
        0xa -> LockSoundInstr(resIdParam)
        0xb -> LockCostumeInstr(resIdParam)
        0xc -> LockRoomInstr(resIdParam)
        0xd -> UnlockScriptInstr(resIdParam)
        0xe -> UnlockSoundInstr(resIdParam)
        0xf -> UnlockCostumeInstr(resIdParam)
        0x10 -> UnlockRoomInstr(resIdParam)
        0x11 -> ClearHeapInstr
        0x12 -> LoadCharsetInstr(resIdParam)
        0x13 -> NukeCharsetInstr(resIdParam)
        else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
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

// 0x44/0xc4	if (@var > @wert)
fun decompileIfVarGreaterThanOpcode(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readUnsignedByte()
    val varSpec = data.readShortLittleEndian().toInt()
    val value = readWordParam(data, opcode, 0x80)
    val jumpOffset = data.readShortLittleEndian().toInt()

    return JumpIfVarLessEqualInstr(toVarSpec(varSpec), value, jumpOffset)
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

fun decompileJumpIfEqualInstr(bytes: ByteArray, offset: Int): Instruction {
    val data = DataInputStream(ByteArrayInputStream(bytes, offset, bytes.size - offset))

    val opcode = data.readUnsignedByte()
    val varSpec = toVarSpec(data.readShortLittleEndian().toInt())
    val valueParam = readWordParam(data, opcode, 0x80)
    val skipOffset = data.readShortLittleEndian().toInt()

    return JumpIfVarEqualInstr(varSpec, valueParam, skipOffset)
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
        10 -> {
            val cursorParam = readByteParam(data, opcode2, 0x80)
            val imageParam = readByteParam(data, opcode2, 0x40)
            CursorImageInstr(cursorParam, imageParam)
        }
        11 -> {
            val cursorParam = readByteParam(data, opcode2, 0x80)
            val hotspotXParam = readByteParam(data, opcode2, 0x40)
            val hotspotYParam = readByteParam(data, opcode2, 0x20)
            CursorHotspotInstr(cursorParam, hotspotXParam, hotspotYParam)
        }
        12 -> CursorInstr(readByteParam(data, opcode2, 0x80))
        13 -> CharsetInstr(readByteParam(data, opcode2, 0x80))
        14 -> CharsetEInstr(readScriptArgs(data))
        else -> InvalidInstruction(byteArrayOf(0x2c, opcode2.toByte()))
    }

    return instruction
}
