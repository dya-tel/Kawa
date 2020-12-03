package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import strikt.api.expect
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import kotlin.test.Test

class VariableRetirementTest : MethodAnalyzerTestBase() {

    @Test fun `Test variable retirement after 'if' block`() {
        val method = parseMethod(
            """
            void test(boolean flag) {
                int a = 0;
                if (flag) {
                    int b = 0;
                } else {
                    int c = 0;
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expect {
            that(frame.getValue(variables.getValue("a")))
                .describedAs("visible variable a")
                .isNotNull()
            that(frame.getValue(variables.getValue("b")))
                .describedAs("invisible variable b")
                .isNull()
            that(frame.getValue(variables.getValue("c")))
                .describedAs("invisible variable c")
                .isNull()
        }
    }

    @Test fun `Test variable retirement after block`() {
        val method = parseMethod(
            """
            void test() {
                int a = 0;
                {
                    int b = 0;
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expect {
            that(frame.getValue(variables.getValue("a")))
                .describedAs("visible variable a")
                .isNotNull()
            that(frame.getValue(variables.getValue("b")))
                .describedAs("invisible variable b")
                .isNull()
        }
    }
}