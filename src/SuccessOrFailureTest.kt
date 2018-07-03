import org.junit.*
import kotlin.test.*

class SuccessOrFailureTest {
    @Test
    fun testRunCatchingSuccess() {
        val ok = runCatching { "OK" }
        checkSuccess(ok, "OK", true)
    }

    @Test
    fun testRunCatchingFailure() {
        val fail = runCatching { error("F") }
        checkFailure(fail, "F", true)
    }

    private fun <T> checkSuccess(ok: SuccessOrFailure<T>, v: T, topLevel: Boolean = false) {
        assertTrue(ok.isSuccess)
        assertFalse(ok.isFailure)
        assertEquals(v, ok.getOrThrow())
        assertEquals(v, ok.getOrNull())
        assertEquals(v, ok.getOrElse { "DEF" })
        assertEquals(null, ok.exceptionOrNull())
        if (topLevel) {
            checkSuccess(ok.map { 42 }, 42)
            checkSuccess(ok.mapCatching { 42 }, 42)
            checkFailure(ok.mapCatching { error("FAIL") }, "FAIL")
            checkSuccess(ok.recover { 42 }, "OK")
            checkSuccess(ok.recoverCatching { 42 }, "OK")
            checkSuccess(ok.recoverCatching { error("FAIL") }, "OK")
        }
    }

    private fun <T> checkFailure(fail: SuccessOrFailure<T>, msg: String, topLevel: Boolean = false) {
        assertFalse(fail.isSuccess)
        assertTrue(fail.isFailure)
        assertFails { fail.getOrThrow() }
        assertEquals(null, fail.getOrNull())
        assertEquals("DEF", fail.getOrElse { "DEF" })
        assertEquals(msg, fail.exceptionOrNull()!!.message)
        if (topLevel) {
            checkFailure(fail.map { 42 }, msg)
            checkFailure(fail.mapCatching { 42 }, msg)
            checkFailure(fail.mapCatching { error("FAIL") }, msg)
            checkSuccess(fail.recover { 42 }, 42)
            checkSuccess(fail.recoverCatching { 42 }, 42)
            checkFailure(fail.recoverCatching { error("FAIL") }, "FAIL")
        }
    }

}