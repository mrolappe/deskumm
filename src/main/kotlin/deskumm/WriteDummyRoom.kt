package deskumm

import java.io.DataOutputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    writeDummyRoom(Paths.get("data/ROOM/dummy.ROOM"))
}

fun writeDummyRoom(path: Path) {
    DataOutputStream(FileOutputStream(path.toFile())).use { out ->
        val contentLength = 1177
        writeBlockHeader(out, BlockId4("ROOM"), contentLength)

        // CYCL
        // TRNS
        // EPAL
        // BOXD
        // BOXM
        // CLUT
        // SCAL
        // RMIM
        // OBIM
        // OBCD
        // EXCD
        // ENCD
        // NLSC
        // LSCR
    }
}
