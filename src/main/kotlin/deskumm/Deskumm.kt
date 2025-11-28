package deskumm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import java.io.*


typealias ObjSpec = Int


sealed interface VarSpec {
    fun toSourceName(): String
    val byteCount: Int
    fun emitBytes(out: DataOutput)
    val indexingMode: VarIndexingMode
}

abstract class VarSpecBase(override val indexingMode: VarIndexingMode = NotIndexed) : VarSpec

class GlobalVarSpec(val varNum: Int, indexingMode: VarIndexingMode = NotIndexed) : VarSpecBase(indexingMode) {
    override fun toSourceName(): String {
        val globalVarName = nameForGlobalVar(this)

        return when (indexingMode) {
            NotIndexed -> globalVarName
            is ValueIndexed -> "$globalVarName[${indexingMode.value}]"
            is VariableIndexed -> "$globalVarName[${indexingMode.varSpec.toSourceName()}]"
        }
    }

    override val byteCount: Int
        get() = 2 + indexingMode.byteCount

    override fun emitBytes(out: DataOutput) {
        out.writeShortLittleEndian(varNum.toShort())
        TODO()  // indexing
//        out.writeByte(varNum.shr(8))
//        out.writeByte(varNum)
    }
}

class LocalVarSpec(val varNum: Int, indexingMode: VarIndexingMode) : VarSpecBase(indexingMode) {
    override fun toSourceName() = when (indexingMode) {
        NotIndexed -> "local$varNum"
        is ValueIndexed -> "local$varNum[${indexingMode.value}]"
        is VariableIndexed -> "local$varNum[${indexingMode.varSpec.toSourceName()}]"
    }

    override val byteCount: Int
        get() = 2 + indexingMode.byteCount

    override fun emitBytes(out: DataOutput) {
        val value = varNum.and(0x3fff).or(0x4000).toShort()
        println("LVS, emit; value varnum $varNum -> 0x${value.toHexString()}")
        out.writeShortLittleEndian(value)
        TODO()  // indexing
//        out.writeByte(varNum.shr(8))
//        out.writeByte(varNum)
    }
}

class BitVarSpec(val varNum: Int, val bitNum: Int, indexingMode: VarIndexingMode) : VarSpecBase(indexingMode) {
    override fun toSourceName() = when (indexingMode) {
        NotIndexed -> "bit$varNum:$bitNum"
        is ValueIndexed -> "bit$varNum:$bitNum[${indexingMode.value}]"
        is VariableIndexed -> "bit$varNum:$bitNum[${indexingMode.varSpec.toSourceName()}]"
    }

    override val byteCount: Int
        get() = 2 + indexingMode.byteCount

    override fun emitBytes(out: DataOutput) {
        // TODO set bits for bit var
        TODO()  // indexing
        out.writeByte(varNum.shr(8))
        out.writeByte(varNum)
    }
}

//class VarSpecIdxVar(val baseVarNum: Int, val indexVar: Int) : VarSpec {
//    override fun toSourceName() = "TODO var$baseVarNum[var$indexVar]"
//    override val byteCount: Int
//        get() = 2
//
//    override fun emitBytes(out: DataOutput) {
//        out.writeShortLittleEndian(baseVarNum.toShort() or 0x2000)
//        out.writeShortLittleEndian(indexVar.toShort() or 0x2000)
//    }
//}
//
//class VarSpecIdxImm(val baseVarNum: Int, val index: Int) : VarSpec {
//    override fun toSourceName() = "TODO var$baseVarNum[$index]"
//    override val byteCount: Int
//        get() = 2
//
//    override fun emitBytes(out: DataOutput) {
//        out.writeShortLittleEndian(baseVarNum.toShort() or 0x2000)
//        out.writeShortLittleEndian(index.toShort() and 0xfff)
//    }
//}

val globalVarNames = mapOf(
    0 to "last-result",
    1 to "selected-actor",
    2 to "camera-pos-x",
    3 to "have-msg",
    4 to "room",
    6 to "machine-speed",
    8 to "actor-count",
    27 to "scroll-script",
    39 to "debug-mode",
    42 to "restart-key",
    43 to "pause-key",
    44 to "mouse-x",
    45 to "mouse-y",
    46 to "timer",
    47 to "timer-total",
    48 to "sound-card",
    49 to "video-mode",
    50 to "main-menu-key",
    51 to "running-from",
    52 to "cursor-state",
    53 to "user-put",
    68 to "machine-speed",
    69 to "video-spéed"
)

@Deprecated("use varSpec specific methods instead")
fun nameForVar(varSpec: VarSpec) = when (varSpec) {
    is GlobalVarSpec -> nameForGlobalVar(varSpec)
    is BitVarSpec -> "bit${varSpec.varNum}:${varSpec.bitNum}"
    is LocalVarSpec -> "local${varSpec.varNum}"
    else -> "var?! $varSpec"
}

fun nameForGlobalVar(varSpec: GlobalVarSpec): String =
    globalVarNames.getOrDefault(varSpec.varNum, "global${varSpec.varNum}")

fun loadScript(path: File, xorDecode: Boolean = false): Result<ByteArray, String> {
    var stream: InputStream = FileInputStream(path)

    if (xorDecode) {
        stream = XorInputStream(stream)
    }

    DataInputStream(stream).use {
        val blockIdBytes = ByteArray(4)
        it.read(blockIdBytes)

        if (String(blockIdBytes) != "SCRP") {
            return Err("File does not begin with SCRP block id")
        } else {
            val blockLength = it.readInt()
            val scriptBuffer = ByteArray(blockLength - 8)

            it.read(scriptBuffer)
            return Ok(scriptBuffer)
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

sealed interface ByteParam : Instruction.Param {
    //    val byteCount: Int
    val isImmediate: Boolean
    override val isVariable: Boolean
        get() = !isImmediate

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
fun DataInput.readByteParam(): ByteParam { // TODO ByteVarParam
    return ImmediateByteParam(readByte().toInt().and(0xff))
}

sealed interface WordParam : Instruction.Param {
    //    val byteCount: Int
    val isImmediate: Boolean
    override val isVariable: Boolean
        get() = !isImmediate

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
        0 -> EndObjectInstr

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

            data.readByte()
            val varSpec = readVarSpec(data)
            val value = readWordParam(data, opcode, 0x80)
            val skipOffset = data.readShortLittleEndian().toInt()

            JumpIfVarGreaterInstr(varSpec, value, skipOffset)
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

        0x0f, 0x8f -> {
            val resultVar = readResultVar(data)
            val objectParam = readWordParam(data, opcode, 0x80)

            AssignGetStateOf(resultVar, objectParam)
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
                    2 -> {
                        val xParam = readByteParam(data, opcode2, 0x80)
                        val yParam = readByteParam(data, opcode2, 0x40)
                        ActorInstr.StepDist(xParam, yParam)
                    }
//                    3 -> ActorInstr.Sound(readByteParam(data, opcode2, 0x80))
                    4 -> ActorInstr.WalkAnimation(readByteParam(data, opcode2, 0x80))
                    5 -> ActorInstr.TalkAnimation(
                        readByteParam(data, opcode2, 0x80),
                        readByteParam(data, opcode2, 0x40)
                    )
//                    6 -> ActorInstr.StandAnimation(readByteParam(data, opcode2, 0x80))
//                    7 -> ActorInstr.Animation(readByteParam(data, opcode2, 0x80), readByteParam(data, opcode2, 0x40), readByteParam(data, opcode2, 0x20))
                    8 -> ActorInstr.Default
                    9 -> ActorInstr.Elevation(readWordParam(data, opcode2, 0x80))
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
                    18 -> ActorInstr.NeverZClip
                    19 -> ActorInstr.AlwaysZClip(readByteParam(data, opcode2, 0x80))
                    20 -> ActorInstr.IgnoreBoxes
                    21 -> ActorInstr.FollowBoxes
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

        0x15, 0x55, 0x95, 0xd5 -> {
            val resultVar = readResultVar(data)
            val xParam = readWordParam(data, opcode, 0x80)
            val yParam = readWordParam(data, opcode, 0x40)

            AssignFindActorInstr(resultVar, xParam, yParam)
        }

        0x16, 0x96 -> {
            val resultVar = readResultVar(data)
            val maxParam = readByteParam(data, opcode, 0x80)

            AssignRandomInstr(resultVar, maxParam)
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
            AssignValueToVarInstr(resultVar, valueParam)
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

        0x20 -> StopMusicInstr

        0x23, 0xa3 -> {
            val resultVar = readResultVar(data)
            val objectParam = readWordParam(data, opcode, 0x80)
            AssignResultOfActorYInstr(resultVar, objectParam)
        }

        0x24, 0x64, 0xa4, 0xe4 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val roomParam = readByteParam(data, opcode, 0x40)
            val x = data.readShortLittleEndian().toInt()
            val y = data.readShortLittleEndian().toInt()

            ComeOutInRoomInstr(objectParam, roomParam, x, y)
        }

        0x25, 0x65, 0xa5, 0xe5 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val roomParam = readByteParam(data, opcode, 0x40)

            PickUpObjectInstr(objectParam, roomParam)
        }

        0x26, 0xa6 -> {
            val resultVar = readResultVar(data)
            val count = data.readUnsignedByte()

            if (opcode == 0x26) {
                val values = buildList {
                    repeat(count) { add(data.readUnsignedByte()) }
                }

                AssignVarRangeB(resultVar, values)
            } else {
                val values = buildList {
                    repeat(count) { add(data.readShortLittleEndian().toInt()) }
                }

                AssignVarRangeW(resultVar, values)
            }
        }

        0x27 -> decompileStringAssignOpcode(bytes, offset)

        0x28 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt(), data)
            val jumpOffset = data.readShortLittleEndian().toInt()

            JumpIfVarNotZeroInstr(varSpec, jumpOffset)
        }

        0x29, 0x69, 0xa9, 0xe9 -> {
            val objectParam = readWordParam(data, opcode, 0x80)
            val ownerParam = readByteParam(data, opcode, 0x40)

            OwnerOfIsInstr(objectParam, ownerParam)
        }

        0x2b -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt(), data)
            SleepForVarJiffiesInstr(varSpec)
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

        0x31, 0xb1 -> {
            val resultVar = readResultVar(data)
            val ownerParam = readByteParam(data, opcode, 0x80)

            AssignResultOfInventorySizeInstr(resultVar, ownerParam)
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

        0x35, 0x75, 0xb5, 0xf5 -> {
            val resultVar = readResultVar(data)
            val xParam = readByteParam(data, opcode, 0x80)
            val yParam = readByteParam(data, opcode, 0x40)

            AssignFindObjectInstr(resultVar, xParam, yParam)
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
            val varSpec = readVarSpec(data)
            val valueParam = readWordParam(data, opcode, 0x80)
            val offset = data.readShortLittleEndian().toInt()

            JumpIfVarLessInstr(varSpec, valueParam, offset)
        }

        0x3a, 0xba -> {
            val resultVar = readResultVar(data)
            val valueParam = readWordParam(data, opcode, 0x80)

            SubtractInstr(resultVar, valueParam)
        }

        0x3c, 0xbc -> StopSoundInstr(readByteParam(data, opcode, 0x80))

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
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt(), data)
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

        0x56, 0xd6 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)
            AssignResultOfActorMovingInstr(resultVar, actorParam)
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

        0x60, 0xe0 -> FreezeScriptsInstr(readByteParam(data, opcode, 0x80))

        0x62, 0xe2 -> StopScriptInstr(readByteParam(data, opcode, 0x80))

        0x63, 0xe3 -> {
            val resultVar = readResultVar(data)
            val actorParam = readByteParam(data, opcode, 0x80)

            ActorFacingInstr(resultVar, actorParam)
        }

        0x67 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt(), data)
            val objSpec = toObjSpec(data.readByte().toInt())
            AssignObjectWidthToVar(varSpec, objSpec)
        }

        0x68, 0xe8 -> {
            val resultVar = readResultVar(data)
            val scriptParam = readByteParam(data, opcode, 0x80)

            ScriptRunningInstr(resultVar, scriptParam)
        }

        0x6b, 0xeb -> DebugInstr(readWordParam(data, opcode, 0x80))

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

        0x98 -> {
            return when (val opcode2 = data.readUnsignedByte()) {
                1 -> RestartInstr
                2 -> PauseInstr
                3 -> QuitInstr
                else -> InvalidInstruction(byteArrayOf(opcode.toByte(), opcode2.toByte()))
            }
        }

        0xa0 -> EndScriptInstr

        0xa8 -> {
            val varSpec = toVarSpec(data.readShortLittleEndian().toInt(), data)
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

            val subs = mutableListOf<ExpressionInstr.Sub>()

            while (opcode != 0xff) {
                val sub = when (opcode.and(0x1f)) {
                    1 -> ExpressionInstr.Value(readWordParam(data, opcode, 0x80))
                    2 -> ExpressionInstr.Add
                    3 -> ExpressionInstr.Subtract
                    4 -> ExpressionInstr.Multiply
                    5 -> ExpressionInstr.Divide
                    6 -> {
                        val opOffset =
                            2 /* opcode 0xac 0x6 */ + offset + resultVar.byteCount + subs.sumOf { it.byteCount }
                        val decompiledOp = decompileInstruction(bytes, opOffset)!!
                        repeat(decompiledOp.length) { data.readUnsignedByte() }     // TODO find a more elegant way
                        ExpressionInstr.Op(decompiledOp)
                    }

                    else -> ExpressionInstr.Invalid(byteArrayOf(opcode.toByte()))
                }

                subs.add(sub)

                opcode = data.readUnsignedByte()
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

class ResultVar(val varSpec: VarSpec) {
    val byteCount: Int get() = varSpec.byteCount
    fun toSource(): String = varSpec.toSourceName()
    fun emitBytes(out: DataOutput) = varSpec.emitBytes(out)
}    // TODO indexed etc.

fun toObjSpec(objSpec: Int) = objSpec

fun main(args: Array<String>) = DeskummCommand().main(args)

class DeskummCommand : CliktCommand() {
    val scriptPath by argument(name = "script-path").file(mustExist = true, canBeDir = false)
    val outputFile by option("-o", "--output", help = "Output file").file(mustExist = false, canBeDir = false)

    val listReferencedScripts by option("--list-referenced-scripts", "-l").flag(default = false)

    override fun run() {
        loadScript(scriptPath)
            .map { script ->
                println(scriptPath)

                script to decodeInstructionsFromScriptBytes(script)
            }
            .map { (script, offsetAndInstructions) ->
                if (listReferencedScripts) {
                    val referencedScripts = collectReferencedScripts(offsetAndInstructions.map { it.second })

                    if (!referencedScripts.isEmpty()) {
                        println(
                            "referenced scripts: ${
                                referencedScripts.sortedBy { it.id }.joinToString { it.id.toString() }
                            }"
                        )
                    } else {
                        println("no referenced scripts found")
                    }
                }

                script to offsetAndInstructions
            }
            .map { (script, instructions) ->
                instructions.map { (offset, instruction) ->
                    val bytesString = script.sliceArray(offset..<offset + instruction.length)
                        .joinToString(" ") { "%02x".format(it) }

                    "%6d %s\n       %s".format(offset, instruction.toSource(), bytesString)
                }
            }
            .mapBoth(
                success = { strings ->
                    if (outputFile != null) {
                        PrintStream(outputFile).use { out -> printStrings(strings, out) }
                    } else {
                        printStrings(strings, System.out)
                    }
                },
                failure = { err -> println(err) }
            )
    }

    private fun printStrings(strings: List<String>, out: PrintStream) {
        strings.forEach { out.println(it) }
    }

    private fun collectReferencedScripts(instructions: Collection<Instruction>): Collection<ScriptReference> {
        return instructions
            .filterIsInstance<ScriptReferencing>()
            .flatMap { it.referencedScripts }
            .toSet()
    }
}