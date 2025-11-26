package deskumm

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream

// 0x0c01
class LoadScriptInstr(val scriptParam: ByteParam) : Instruction, ScriptReferencing {
    override fun toSource(): String = "load-script ${scriptParam.toSource()}"
    override val length: Int
        get() = 2 + scriptParam.byteCount
    override val referencedScripts: Set<ScriptReference>
        get() = scriptReferences(scriptParam)
}

// 0x0c02
class LoadSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 2 + soundParam.byteCount
}

// 0x0c03/0x8c03
class LoadCostumeInstr(val costumeParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-costume ${costumeParam.toSource()}"
    override val length: Int
        get() = 2 + costumeParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.write(applyParamBits(0x0c, costumeParam))
        out.write(0x03)
        costumeParam.emitBytes(out)
    }
}

// 0x0c04
class LoadRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-room ${roomParam.toSource()}"

    override val length: Int
        get() = 2 + roomParam.byteCount

    fun emitBytes(out: DataOutput) {
        if (roomParam.isImmediate) {
            out.write(byteArrayOf(0x0c, 0x04))
            roomParam.emitBytes(out)
        } else {
            out.write(byteArrayOf(0x8c.toByte(), 0x04))
            roomParam.emitBytes(out)
        }
    }
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
class LockScriptInstr(val scriptParam: ByteParam) : Instruction, ScriptReferencing {
    override fun toSource(): String = "lock-script ${scriptParam.toSource()}"
    override val length: Int
        get() = 2 + scriptParam.byteCount
    override val referencedScripts: Set<ScriptReference>
        get() = scriptReferences(scriptParam)
}

// 0x0c0a
class LockSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 2 + soundParam.byteCount
}

// 0x0c0b/0x8c0b
class LockCostumeInstr(val costumeParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-costume ${costumeParam.toSource()}"
    override val length: Int
        get() = 2 + costumeParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.write(applyParamBits(0x0c, costumeParam))
        out.write(0x0b)
        costumeParam.emitBytes(out)
    }
}

// 0x0c0c
class LockRoomInstr(val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "lock-room ${roomParam.toSource()}"
    override val length: Int
        get() = 2 + roomParam.byteCount

    fun emitBytes(out: DataOutput) {
        if (roomParam.isImmediate) {
            out.write(byteArrayOf(0x0c, 0x0c))
            roomParam.emitBytes(out)
        } else {
            out.write(byteArrayOf(0x8c.toByte(), 0x0c))
            roomParam.emitBytes(out)
        }
    }
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

// 0x0c12/0x8c12
class LoadCharsetInstr(val charsetParam: ByteParam) : Instruction {
    override fun toSource(): String = "load-charset ${charsetParam.toSource()}"
    override val length: Int
        get() = 2 + charsetParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.write(0x0c)
        out.write(applyParamBits(0x12, charsetParam))
        charsetParam.emitBytes(out)
    }

    companion object {
        fun emit(dataOut: DataOutput, charset: Int) {
            LoadCharsetInstr(ImmediateByteParam(charset)).emitBytes(dataOut)
        }
    }
}

// 0x0c13
class NukeCharsetInstr(val charsetParam: ByteParam) : Instruction {
    override fun toSource(): String = "nuke-charset ${charsetParam.toSource()}"

    override val length: Int
        get() = 2 + charsetParam.byteCount
}

abstract class Opcode(val name: String, override val length: Int) : Instruction {
    override fun toSource(): String = name
}

object EndObjectInstr : SimpleInstr("end-object", byteArrayOf(0x00))

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
    override fun toSource(): String =
        "draw-object ${objectParam.toSource()} at ${xParam.toSource()}, ${yParam.toSource()}"

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
class StartMusicInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "start-music ${soundParam.toSource()}"

    override val length: Int
        get() = 1 + soundParam.byteCount

    fun emit(out: DataOutput) {
        val opcode = if (soundParam.isImmediate) 0x02 else 0x82
        out.writeByte(opcode)
        soundParam.emitBytes(out)
    }
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

    fun emitBytes(out: DataOutput) {
        val opcode = applyParamBits(0x1, actorParam, xParam, yParam)
        out.writeByte(opcode)
        actorParam.emitBytes(out)
        xParam.emitBytes(out)
        yParam.emitBytes(out)
    }
}

// 0x04
class IfVarLessOrEqualOpcode(val varSpec: VarSpec, val value: Int, val skipOffset: Int) :
    Opcode("if (@var <= @wert)", 7) {
    override fun toSource(): String {
        return "if ${nameForVar(varSpec)} <= $value (else jump $skipOffset)"
    }
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
    override fun toSource(): String =
        "start-script ${scriptParam.toSource()} ${scriptArgs.joinToString { it.toSource() }}"

    override val length: Int
        get() = 2 /* opcode, 0xff */ + scriptParam.byteCount + scriptArgs.sumOf { it.byteCount } + scriptArgs.size /* opcode before arg */
}

// 0x0b, 0x4b, 0x8b, 0xcb
class AssignValidVerbInstr(val resultVar: ResultVar, val aParam: WordParam, val bParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := valid-verb ${aParam.toSource()} ${bParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + aParam.byteCount + bParam.byteCount
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

// 0x0f/0x8f
class AssignGetStateOf(val resultVar: ResultVar, val objectParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := get-state-of ${objectParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + objectParam.byteCount
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

    fun emitBytes(out: DataOutput) {
        val opcode = applyParamBits(0x11, actorParam, animationParam)
        out.write(byteArrayOf(opcode.toByte()))
        actorParam.emitBytes(out)
        animationParam.emitBytes(out)
    }
}

// 0x12/0x92
class CameraPanToInstr(val xParam: WordParam) : Instruction {
    override fun toSource(): String = "camera-pan-to ${xParam.toSource()}"
    override val length: Int
        get() = 1 + xParam.byteCount
}

typealias ScriptId = Int

@JvmInline
value class ScriptReference(val id: ScriptId) {
    override fun toString(): String = id.toString()
}

interface ScriptReferencing {
    val referencedScripts: Set<ScriptReference>
}

fun applyParamBits(
    opcode: Int,
    param1: Instruction.Param,
    param2: Instruction.Param? = null,
    param3: Instruction.Param? = null
): Int {
    val param1Bit = if (param1.isVariable) 0x80 else 0
    val param2Bit = param2?.let { if (it.isVariable) 0x40 else 0 } ?: 0
    val param3Bit = param3?.let { if (it.isVariable) 0x20 else 0 } ?: 0
    return (opcode or param1Bit or param2Bit or param3Bit).and(0xff)
}

sealed interface Instruction {
    fun toSource(): String
    val length: Int

    interface Param {
        val byteCount: Int
        val isVariable: Boolean
        fun toSource(): String
    }
}

class ActorId(val id: Int) {
    fun toSourceName(): String = "actor$id"
}

class ObjId(val id: Int) {
    fun toSourceName(): String = "obj$id"
}

private fun scriptReferences(vararg params: ByteParam): Set<ScriptReference> {
    return params
        .filterIsInstance<ImmediateByteParam>()
        .map { ScriptReference(it.byte) }
        .toSet()
}

// 0x13/0x53/0x93/0xd3
class ActorInstr(val actorParam: ByteParam, val subs: List<Sub>) : Instruction {
    override fun toSource(): String = "TODO actor ${actorParam.toSource()} ${subs.joinToString { it.source }}"

    override val length: Int
        get() = 2 /* opcode + final 0xff */ + actorParam.byteCount + subs.sumOf { it.byteSize }

    fun emitBytes(out: DataOutput) {
        out.writeByte(applyParamBits(0x13, actorParam))
        actorParam.emitBytes(out)
        subs.forEach { it.emitBytes(out) }
        out.writeByte(0xff)
    }

    sealed interface Sub {
        val byteSize: Int
        val source: String
        fun emitBytes(out: DataOutput)
    }

    // 0x1
    class Costume(val costumeParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + costumeParam.byteCount
        override val source: String
            get() = "costume ${costumeParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x1, costumeParam))
        }
    }

    // 0x2
    class StepDist(val xParam: ByteParam, val yParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + xParam.byteCount + yParam.byteCount
        override val source: String
            get() = "step-dist ${xParam.toSource()}, ${yParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x2, xParam, yParam))
        }
    }

    // 0x04
    class WalkAnimation(val animationParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + animationParam.byteCount
        override val source: String
            get() = "walk-animation ${animationParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x4, animationParam))
        }
    }

    // 0x05
    class TalkAnimation(val fromParam: ByteParam, val toParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + fromParam.byteCount + toParam.byteCount
        override val source: String
            get() = "talk-animation ${fromParam.toSource()} ${toParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x5, fromParam, toParam))
        }
    }

    // 0x8
    object Default : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "default"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(0x8)
        }
    }

    // 0x9
    class Elevation(val elevationParam: WordParam) : Sub {
        override val byteSize: Int
            get() = 1 + elevationParam.byteCount
        override val source: String
            get() = "elevation ${elevationParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x9, elevationParam))
        }
    }

    // 0xc
    class TalkColor(val colorParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + colorParam.byteCount
        override val source: String
            get() = "talk-color ${colorParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0xc, colorParam))
        }
    }

    // 0xd
    class Name(val name: ScummStringBytesV5) : Sub {
        override val byteSize: Int
            get() = 1 /* opcode */ + name.byteCount
        override val source: String
            get() = "actor-name \"${name.toSource()}\""

        override fun emitBytes(out: DataOutput) {
            out.writeByte(0xd)
            out.write(name.bytes)
        }
    }

    // 0x12
    object NeverZClip : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "never-zclip"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(0x12)
        }
    }

    // 0x13
    class AlwaysZClip(val someParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + someParam.byteCount
        override val source: String
            get() = "always-zclip force=${someParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x13, someParam))
        }
    }

    // 0x14
    object IgnoreBoxes : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "ignore-boxes"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(0x14)
        }
    }

    // 0x15
    object FollowBoxes : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "follow-boxes"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(0x15)
        }
    }

    class Invalid(val opcode: Int) : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "!actor-invalid-sub! 0x${opcode.toHexString()}"

        override fun emitBytes(out: DataOutput) {
            out.write(opcode)
        }
    }


}

// 0x14/0x94
class PrintInstr(val whoParam: ByteParam, val subs: List<Sub>) : Instruction {
    override fun toSource(): String {
        val instr = when {
            whoParam is ImmediateByteParam -> {
                when (whoParam.byte) {
                    0xfc -> "print-system"
                    0xfd -> "print-debug"
                    0xfe -> "print-text"
                    0xff -> "print-line"
                    else -> "print ${whoParam.toSource()}"
                }
            }

            else -> whoParam.toSource()
        }
        return "$instr ${subs.joinToString(" ") { it.source }}"
    }

    override val length: Int
        get() {
            val additional = if (subs.any { it is Text }) 0 else 1  /* 0xff only if there is no text command */
            return 2 /* opcode + who */ + additional + subs.sumOf { it.byteSize }
        }

    fun emitBytes(out: DataOutput) {
        out.writeByte(applyParamBits(0x14, whoParam))
        whoParam.emitBytes(out)

        subs.forEach { it.emitBytes(out) }

        if (subs.last() !is Text) {
            out.writeByte(0xff)
        }
    }

    sealed interface Sub {
        val byteSize: Int
        val source: String
        fun emitBytes(out: DataOutput)
    }

    // 0x0
    class At(val xParam: WordParam, val yParam: WordParam) : Sub {
        override val byteSize: Int
            get() = 1 + xParam.byteCount + yParam.byteCount
        override val source: String
            get() = "at ${xParam.toSource()}, ${yParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x0, xParam, yParam))
            xParam.emitBytes(out)
            yParam.emitBytes(out)
        }
    }

    // 0x1
    class Color(val colorParam: ByteParam) : Sub {
        override val byteSize: Int
            get() = 1 + colorParam.byteCount
        override val source: String
            get() = "color ${colorParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x1, colorParam))
            colorParam.emitBytes(out)
        }
    }

    // 0x04
    object Center : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "center"

        override fun emitBytes(out: DataOutput) = out.writeByte(0x04)
    }

    // 0x07
    object Overhead : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "overhead"

        override fun emitBytes(out: DataOutput) = out.writeByte(0x07)
    }

    // 0x08
    class SayVoice(val offsetParam: WordParam, val delayParam: WordParam) : Sub {
        override val byteSize: Int
            get() = 1 + offsetParam.byteCount + delayParam.byteCount
        override val source: String
            get() = "say-voice ${offsetParam.toSource()}, ${delayParam.toSource()}"

        override fun emitBytes(out: DataOutput) {
            out.writeByte(applyParamBits(0x08, offsetParam, delayParam))
            offsetParam.emitBytes(out)
            delayParam.emitBytes(out)
        }
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

        override fun emitBytes(out: DataOutput) {
            out.writeByte(0xf)
            out.write(stringBytes.bytes)
        }
    }

    class Invalid(val opcode: Int) : Sub {
        override val byteSize: Int
            get() = 1
        override val source: String
            get() = "!invalid 0x$opcode (0x${opcode.and(0xf)})!"

        override fun emitBytes(out: DataOutput): Unit = throw IllegalStateException("Not gonna happen")
    }
}

// 0x15/0x55/0x95/0xd5
class AssignFindActorInstr(val resultVar: ResultVar, val xParam: WordParam, val yParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := find-actor ${xParam.toSource()}, ${yParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + xParam.byteCount + yParam.byteCount
}

// 0x16/0x96
class AssignRandomInstr(val resultVar: ResultVar, val maxParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := random ${maxParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + maxParam.byteCount
}

// 0x18
class JumpInstr(val offset: Int) : Instruction {
    override fun toSource(): String = "jump $offset"
    override val length: Int
        get() = 3
}

// 0x19/0x39/0x59/0x79/0x99/0xb9/0xd9/0xf9
class DoSentenceWithInstr(val verbParam: ByteParam, val object1Param: WordParam, val object2Param: WordParam) :
    Instruction {
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
object StopMusicInstr : SimpleInstr("stop-music", byteArrayOf(0x20))

// 0x23, 0xa3
class AssignResultOfActorYInstr(val resultVar: ResultVar, val objectParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := actor-y ${objectParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + objectParam.byteCount
}

// 0x24, 0x64, 0xa4, 0xe4
class ComeOutInRoomInstr(val objectParam: WordParam, val roomParam: ByteParam, val x: Int, val y: Int) : Instruction {
    override fun toSource(): String =
        "come-out ${objectParam.toSource()} in-room ${roomParam.toSource()} walk-to $x, $y"

    override val length: Int
        get() = 1 + objectParam.byteCount + roomParam.byteCount + 4 /* x and y */

    fun emitBytes(out: DataOutput) {
        out.writeByte(applyParamBits(0x24, objectParam))
        objectParam.emitBytes(out)
        roomParam.emitBytes(out)
        out.writeInt(x)
        out.writeInt(y)
    }
}

// 0x25, 0x65, 0xa5, 0xe5
class PickUpObjectInstr(val objectParam: WordParam, val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "pick-up-object ${objectParam.toSource()} in-room ${roomParam.toSource()}"

    override val length: Int
        get() = 1 + objectParam.byteCount + roomParam.byteCount
}

// 0x26/0xa6
class AssignVarRangeB(val resultVar: ResultVar, val values: List<Int>) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()}... := ${values.joinToString()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + values.size
}

class AssignVarRangeW(val resultVar: ResultVar, val values: List<Int>) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()}... := ${values.joinToString()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + values.size * 2
}

fun v5StringVarName(param: Instruction.Param): String {
    return "str[${param.toSource()}]"
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

// 0x2701
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

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x27)
        out.writeByte(applyParamBits(0x01, stringParam))
        stringParam.emitBytes(out)
        stringBytes.emitBytes(out)
    }
}

//0x2702
class AssignStringToStringOpcode(val destStringParam: ByteParam, val srcStringParam: ByteParam) : Instruction {
    override fun toSource() = "${v5StringVarName(destStringParam)} = ${v5StringVarName(srcStringParam)}"
    override val length: Int
        get() = 2 + destStringParam.byteCount + srcStringParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x27)
        out.writeByte(applyParamBits(0x02, destStringParam, srcStringParam))
        destStringParam.emitBytes(out)
        srcStringParam.emitBytes(out)
    }
}

// 0x2703
class SetStringCharAtInstr(val destStringParam: ByteParam, val indexParam: ByteParam, val charParam: ByteParam) :
    Instruction {
    override fun toSource() = "${v5StringVarName(destStringParam)}[${indexParam.toSource()}] = ${charParam.toSource()}"
    override val length: Int
        get() = 2 + destStringParam.byteCount + indexParam.byteCount + charParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x27)
        out.writeByte(applyParamBits(0x03, destStringParam, indexParam, charParam))
        destStringParam.emitBytes(out)
        indexParam.emitBytes(out)
        charParam.emitBytes(out)
    }
}

// 0x2704
class AssignGetStringCharAtInstr(val varSpec: VarSpec, val stringParam: ByteParam, val indexParam: ByteParam) :
    Instruction {
    override fun toSource() = "${varSpec.toSourceName()} = ${v5StringVarName(stringParam)}[${indexParam.toSource()}]"
    override val length: Int
        get() = 2 + varSpec.byteCount + stringParam.byteCount + indexParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.write(0x27)
        out.write(applyParamBits(0x4, stringParam, indexParam))
        varSpec.emitBytes(out)
        stringParam.emitBytes(out)
        indexParam.emitBytes(out)
    }
}

// 0x2705
class NewStringInstr(val stringParam: ByteParam, val sizeParam: ByteParam) : Instruction {
    override fun toSource() = "new ${v5StringVarName(stringParam)}[${sizeParam.toSource()}]"
    override val length: Int
        get() = 2 + stringParam.byteCount + sizeParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x27)
        out.writeByte(applyParamBits(0x5, stringParam, sizeParam))
        stringParam.emitBytes(out)
        sizeParam.emitBytes(out)
    }
}

// 0x02
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

// 0x2b
class SleepForVarJiffiesInstr(val jiffiesVar: VarSpec) : Instruction {
    override fun toSource(): String = "sleep-for ${jiffiesVar.toSourceName()} jiffies"
    override val length: Int
        get() = 1 + jiffiesVar.byteCount
}

// 0x2d/0x6d/0xad/0xed
class PutActorInRoomInstr(val actorParam: ByteParam, val roomParam: ByteParam) : Instruction {
    override fun toSource(): String = "put-actor ${actorParam.toSource()} in-room ${roomParam.toSource()}"

    override val length: Int
        get() = 1 + actorParam.byteCount + roomParam.byteCount

    fun emitBytes(out: DataOutput) {
        val opcode = when (Pair(actorParam.isVariable, roomParam.isVariable)) {
            Pair(false, false) -> 0x2d
            Pair(false, true) -> 0x6d
            Pair(true, false) -> 0xad
            Pair(true, true) -> 0xed
            else -> error("!?#!")
        }

        out.writeByte(opcode)
        actorParam.emitBytes(out)
        roomParam.emitBytes(out)
    }
}

// 0x2e
class SleepForJiffiesInstr(val jiffies: Int) : Instruction {
    override fun toSource(): String = "sleep-for $jiffies jiffies"
    override val length: Int
        get() = 4 /* opcode + jiffies */

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x2e)
        out.writeByte(jiffies.and(0xff))
        out.writeByte(jiffies.shr(8).and(0xff))
        out.writeByte(jiffies.shr(16).and(0xff))
    }
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
class AssignValueToVarInstr(val resultVar: ResultVar, val valueParam: WordParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := ${valueParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + valueParam.byteCount

    fun emitBytes(dataOut: DataOutput) {
        dataOut.writeByte(applyParamBits(0x1a, valueParam))
        resultVar.emitBytes(dataOut)
        valueParam.emitBytes(dataOut)
    }

    companion object {
        fun emit(dataOut: DataOutput, resultVar: ResultVar, value: Int) {
            AssignValueToVarInstr(resultVar, ImmediateWordParam(value)).emitBytes(dataOut)
        }
    }
}

// 0x1c/0x9c
class StartSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "start-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 1 + soundParam.byteCount
}

class JumpIfClassOfIsNotInstr(val objectParam: WordParam, val classParams: List<WordParam>, val offset: Int) :
    Instruction {
    override fun toSource(): String {
        return "if-class-of ${objectParam.toSource()} is-not ${classParams.joinToString(" ") { it.toSource() }} jump $offset"
    }

    override val length: Int
        get() = 2 /* opcode, 0xff */ + objectParam.byteCount + classParams.sumOf { it.byteCount } + classParams.size /* opcode before each class */ + 2 /* jump offset */
}

// 0x31/0xb1
class AssignResultOfInventorySizeInstr(val resultVar: ResultVar, val ownerParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := inventory-size ${ownerParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + ownerParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.writeByte(applyParamBits(0x31, ownerParam))
        resultVar.emitBytes(out)
        ownerParam.emitBytes(out)
    }
}

// 0x32/0xb2
class CameraAtInstr(val atParam: WordParam) : Instruction {
    override fun toSource(): String = "camera-at ${atParam.toSource()}"
    override val length: Int
        get() = 1 + atParam.byteCount
}

// 0x1e/0x3e/0x5e/0x7e/0x9e/0xbe/0xde/0xfe
class WalkActorToXYInstr(val actorParam: ByteParam, val xParam: WordParam, val yParam: WordParam) : Instruction {
    override fun toSource(): String =
        "walk-actor ${actorParam.toSource()} to ${xParam.toSource()}, ${yParam.toSource()}"

    override val length: Int
        get() = 1 + actorParam.byteCount + xParam.byteCount + yParam.byteCount
}

class RoomScrollInstr(val arg1: WordParam, val arg2: WordParam) : Instruction {
    override fun toSource(): String = "room-scroll $arg1 to $arg2"
    override val length: Int
        get() = 2 /* opcode */ + arg1.byteCount + arg2.byteCount
}

// 0x33/0x73/0xb3/0xf3
class SetScreenInstr(val arg1: WordParam, val arg2: WordParam) : Instruction {
    override fun toSource(): String = "set-screen ${arg1.toSource()} to ${arg2.toSource()}"
    override val length: Int
        get() = 2 /* opcode */ + arg1.byteCount + arg2.byteCount

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x33)
        out.writeByte(applyParamBits(0x3, arg1, arg2))
        arg1.emitBytes(out)
        arg2.emitBytes(out)
    }

    companion object {
        fun emit(dataOut: DataOutput, arg1: Int, arg2: Int) {
            SetScreenInstr(ImmediateWordParam(arg1), ImmediateWordParam(arg2)).emitBytes(dataOut)
        }
    }
}

// 0x04
class RoomPaletteInstr(val rParam: WordParam, val gParam: WordParam, val bParam: WordParam, val slotParam: ByteParam) :
    Instruction {
    override fun toSource(): String =
        "palette ${rParam.toSource()}, ${gParam.toSource()}, ${bParam.toSource()} in-slot ${slotParam.toSource()}"

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

object CursorOnInstr : SimpleInstr( "cursor on", byteArrayOf(0x2c, 0x01))
object CursorOffInstr : SimpleInstr( "cursor off", byteArrayOf(0x2c, 0x02))
object UserPutOn : SimpleInstr( "userput on", byteArrayOf(0x2c, 0x03))
object UserPutOff : SimpleInstr( "userput off", byteArrayOf(0x2c, 0x04))
object CursorSoftOn : SimpleInstr( "cursor soft-on", byteArrayOf(0x2c, 0x05))
object CursorSoftOff : SimpleInstr( "cursor soft-off", byteArrayOf(0x2c, 0x06))
object UserPutSoftOn : SimpleInstr( "userput soft-on", byteArrayOf(0x2c, 0x07))
object UserPutSoftOff : SimpleInstr( "userput soft-off", byteArrayOf(0x2c, 0x08))

class CursorImageInstr(val cursorParam: ByteParam, val imageParam: ByteParam) : Instruction {
    override fun toSource(): String = "cursor ${cursorParam.toSource()} image ${imageParam.toSource()}"

    override val length: Int
        get() = 2 + cursorParam.byteCount + imageParam.byteCount
}

class CursorHotspotInstr(val cursorParam: ByteParam, val hotspotXParam: ByteParam, val hotspotYParam: ByteParam) :
    Instruction {
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

// 0x2c0d
class CharsetInstr(val charsetParam: ByteParam) : Instruction {
    override fun toSource(): String = "charset ${charsetParam.toSource()}"
    override val length: Int
        get() = 2 + charsetParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.write(byteArrayOf(0x2c, applyParamBits(0xd, charsetParam).toByte()))
        charsetParam.emitBytes(out)
    }

    companion object {
        fun emit(dataOut: DataOutput, charset: Int) {
            CharsetInstr(ImmediateByteParam(charset)).emitBytes(dataOut)
        }
    }
}

class CharsetEInstr(val args: List<WordParam>) : Instruction {
    override fun toSource(): String = "charset-e ${args.joinToString(" ") { it.toSource() }}"
    override val length: Int
        get() = 3 /* opcode, final 0xff */ + args.sumOf { it.byteCount } + args.size

}

// 0x34/0x74/0xb4/0xf4
class AssignProximityInstr(val resultVar: ResultVar, val object1Param: WordParam, val object2Param: WordParam) :
    Instruction {
    override fun toSource(): String =
        "${resultVar.toSource()} := proximity ${object1Param.toSource()}, ${object2Param.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + object1Param.byteCount + object2Param.byteCount
}

// 0x35/0x75/0xb5/0xf5
class AssignFindObjectInstr(val resultVar: ResultVar, val xParam: ByteParam, val yParam: ByteParam) : Instruction {
    override fun toSource(): String =
        "${resultVar.toSource()} := find-object ${xParam.toSource()}, ${yParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + xParam.byteCount + yParam.byteCount
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

// 0x3c, 0xbc
class StopSoundInstr(val soundParam: ByteParam) : Instruction {
    override fun toSource(): String = "stop-sound ${soundParam.toSource()}"
    override val length: Int
        get() = 1 + soundParam.byteCount
}

// 0x3d, 0x7d, 0xbd, 0xfd
class AssignFindInventoryInstr(val resultVar: ResultVar, val xParam: ByteParam, val yParam: ByteParam) : Instruction {
    override fun toSource(): String =
        "${resultVar.toSource()} := find-inventory ${xParam.toSource()}, ${yParam.toSource()}"

    override val length: Int
        get() = 1 + resultVar.byteCount + xParam.byteCount + yParam.byteCount
}

// 0x3f, 0x7f, 0xbf, 0xff
class DrawBoxInstr(
    val xParam: WordParam,
    val yParam: WordParam,
    val x2Param: WordParam,
    val y2Param: WordParam,
    val colorParam: ByteParam
) : Instruction {
    override fun toSource(): String =
        "draw-box ${xParam.toSource()}, ${yParam.toSource()} to ${x2Param.toSource()}, ${y2Param.toSource()} color ${colorParam.toSource()}"

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

    companion object {
        fun emit(dataOut: DataOutput, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
            DrawBoxInstr(
                ImmediateWordParam(x1),
                ImmediateWordParam(y1),
                ImmediateWordParam(x2),
                ImmediateWordParam(y2),
                ImmediateByteParam(color)
            ).emit(dataOut)
        }
    }
}

// 0x40
class CutSceneInstr(val scriptArgs: List<WordParam>) : Instruction {
    override fun toSource(): String = "cut-scene ${scriptArgs.joinToString(" ") { it.toSource() }}"
    override val length: Int
        get() = 2 /* opcode, final 0xff */ + scriptArgs.sumOf { it.byteCount } + scriptArgs.size /* opcode before each arg */
}

// 0x42/0xc2
class ChainsScriptInstr(val scriptParam: ByteParam, val scriptArgs: List<WordParam>) : Instruction, ScriptReferencing {
    override fun toSource(): String {
        return "chains-script ${scriptParam.toSource()} ${scriptArgs.joinToString(" ") { it.toSource() }}"
    }

    override val length: Int
        get() = 2 /* opcode, final 0xff */ + scriptParam.byteCount + scriptArgs.sumOf { it.byteCount } + scriptArgs.size
    override val referencedScripts: Set<ScriptReference>
        get() = scriptReferences(scriptParam)
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

// 0x56/0xd6
class AssignResultOfActorMovingInstr(val resultVar: ResultVar, val actorParam: ByteParam) : Instruction {
    override fun toSource(): String = "${resultVar.toSource()} := actor-moving ${actorParam.toSource()}"
    override val length: Int
        get() = 1 + resultVar.byteCount + actorParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.write(applyParamBits(0x56, actorParam))
        resultVar.emitBytes(out)
        actorParam.emitBytes(out)
    }
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

// 0x60/0xe0
class FreezeScriptsInstr(val switchParam: ByteParam) : Instruction {
    override fun toSource(): String {
        return if (switchParam is ImmediateByteParam) {
            if (switchParam.byte == 0) { "unfreeze-scripts" } else { "freeze-scripts" }
        } else {
            "freeze-scripts ${switchParam.toSource()}"
        }
    }

    override val length: Int
        get() = 1 + switchParam.byteCount
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
        get() = 1 + resultVar.byteCount + actorParam.byteCount
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

// 0x6b/0xeb
class DebugInstr(val valueParam: WordParam) : Instruction {
    override fun toSource(): String = "debug ${valueParam.toSource()}"

    override val length: Int
        get() = 1 + valueParam.byteCount

    fun emitBytes(out: DataOutput) {
        out.writeByte(applyParamBits(0x6b, valueParam))
        valueParam.emitBytes(out)
    }
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

    fun emitBytes(out: DataOutput) {
        out.writeByte(0x72)
        roomParam.emitBytes(out)
    }

    fun emitBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out -> emitBytes(out) }
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

abstract class SimpleInstr(val name: String, private val bytes: ByteArray) : Instruction {
    override fun toSource(): String = name
    override val length: Int
        get() = bytes.size

    fun emitBytes(out: DataOutput) {
        out.write(bytes)
    }
}

// 0x98 0x01
object RestartInstr : SimpleInstr("restart", byteArrayOf(0x98.toByte(), 0x01))

// 0x98 0x02
object PauseInstr : SimpleInstr("pause", byteArrayOf(0x98.toByte(), 0x02))

// 0x98 0x03
object QuitInstr : SimpleInstr("quit", byteArrayOf(0x98.toByte(), 0x03))

object BreakHereInstr : SimpleInstr("break-here", byteArrayOf(0x80.toByte()))
object EndScriptInstr : SimpleInstr("end-script", byteArrayOf(0xa0.toByte()))

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
    override fun toSource(): String =
        "save-verbs from-id ${fromIdParam.toSource()}, to-id ${toIdParam.toSource()}, save-id ${saveIdParam.toSource()}"

    override val length: Int
        get() = 2 + fromIdParam.byteCount + toIdParam.byteCount + saveIdParam.byteCount
}

// 0xab 0x02
class RestoreVerbsInstr(val fromIdParam: ByteParam, val toIdParam: ByteParam, val saveIdParam: ByteParam) :
    Instruction {
    override fun toSource(): String =
        "restore-verbs from-id ${fromIdParam.toSource()}, to-id ${toIdParam.toSource()}, save-id ${saveIdParam.toSource()}"

    override val length: Int
        get() = 2 + fromIdParam.byteCount + toIdParam.byteCount + saveIdParam.byteCount
}

// 0xab 0x03
class DeleteVerbsInstr(val fromIdParam: ByteParam, val toIdParam: ByteParam, val saveIdParam: ByteParam) : Instruction {
    override fun toSource(): String =
        "delete-verbs from-id ${fromIdParam.toSource()}, to-id ${toIdParam.toSource()}, save-id ${saveIdParam.toSource()}"

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

    fun emitBytes(out: DataOutput) {
        out.writeByte(0xd8)
        subs.forEach { it.emitBytes(out) }

        if (subs.last() !is PrintInstr.Text) {
            out.writeByte(0xff)
        }
    }
}

class ExpressionInstr(val resultVar: ResultVar, val subs: List<Sub>) : Instruction {
    override fun toSource(): String {
        return "${resultVar.toSource()} = ${subs.joinToString(" ") { it.toSource() }}"
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

        override fun toSource(): String = "push ${valueParam.toSource()}"
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
    class Op(val op: Instruction) : Sub {
        override val byteCount: Int
            get() = 1 + op.length

        override fun toSource(): String = "op(${op.toSource()})"
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

fun decodePrintSubs(data: DataInputStream): MutableList<PrintInstr.Sub> {
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

fun readResultVar(data: DataInput): ResultVar {
    var varNum1 = data.readShortLittleEndian().toInt()

    return ResultVar(toVarSpec(varNum1))
}

// raw string bytes, including \0 terminator
data class ScummStringBytesV5(val bytes: ByteArray) {
    init {
        require(bytes.last() == 0.toByte()) { "ScummStringBytesV5 must end with a 0 byte" }
    }

    companion object {
        fun from(string: String) = ScummStringBytesV5(string.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0))
    }

    val byteCount: Int get() = bytes.size
    fun toSource(): String = bytes.decodeToString()

    fun emitBytes(out: DataOutput) {
        out.write(bytes)
    }
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

    val opcode = data.readUnsignedByte()    // 0x0c/0x8c
    val opcode2 = data.readUnsignedByte()
    val resIdParam = if (opcode != 0x11) readByteParam(data, opcode2, 0x80) else ImmediateByteParam(0)

    return when (opcode2.and(0x3f)) {
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
            val stringParam = readByteParam(data, opcode2, 0x80)
            val stringBytes = data.readScummStringBytes()
            AssignLiteralToStringInstr(stringParam, stringBytes)    // TODO convert to string
        }

        2 -> {
            val destStringParam = readByteParam(data, opcode2, 0x80)
            val srcStringParam = readByteParam(data, opcode2, 0x40)
            AssignStringToStringOpcode(destStringParam, srcStringParam)
        }

        3 -> {
            val stringParam = readByteParam(data, opcode2, 0x80)
            val indexParam = readByteParam(data, opcode2, 0x40)
            val charParam = readByteParam(data, opcode2, 0x20)
            SetStringCharAtInstr(stringParam, indexParam, charParam)
        }

        4 -> {
            val destVar = readResultVar(data)
            val stringParam = readByteParam(data, opcode2, 0x80)
            val indexParam = readByteParam(data, opcode2, 0x40)
            AssignGetStringCharAtInstr(destVar.varSpec, stringParam, indexParam)
        }

        5 -> {  // *@str [@idx]
            val stringParam = readByteParam(data, opcode2, 0x80)
            val indexParam = readByteParam(data, opcode2, 0x40)
            NewStringInstr(stringParam, indexParam)
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
        1 -> CursorOnInstr
        2 -> CursorOffInstr
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