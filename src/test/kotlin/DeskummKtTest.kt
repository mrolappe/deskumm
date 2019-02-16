import jmj.deskumm.DecompiledInstruction
import jmj.deskumm.decompile
import org.junit.Assert.*
import org.junit.Test

class DeskummKtTest {
    @Test
    fun correctlyDecompilesEndObject() {
        val data = byteArrayOf(0)

        assertThatStringRepresentationIs(decompile(data), "end-object")
    }

    private fun assertThatStringRepresentationIs(instruction: DecompiledInstruction?, expected: String) {
        assertEquals(expected, instruction?.stringRepresentation)
    }
}