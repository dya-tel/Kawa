package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.parseMethod
import kotlin.test.Test

class Snippet1 : MethodAnalyzerTestBase() {
    private companion object {
        private val TEXT = """
        static int test1(String a, String b) {
            a = b;
            {
                if (a == null) {
                    System.out.println(b.substring(1));
                    return -1;
                }
            }
            return a.compareTo(b);
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test analyzer doesn't crash`() {
        runBlocking { analyze(method) }
    }
}