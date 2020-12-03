package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import org.junit.Test
import spoon.reflect.code.CtLocalVariable
import strikt.api.expectThat
import strikt.assertions.isNotSameInstanceAs

class ValueErasureTest : MethodAnalyzerTestBase() {

    @Test fun `Test value erasure`() {
        val method = parseMethod(
            """
            void test() {
                Object x = null;
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variable = method.extractVariables().values.single() as CtLocalVariable<*>

        val assignmentFrame = flow.frames.getValue(variable)
        val nextFrame = assignmentFrame.next!!

        expectThat(assignmentFrame.eraseValues())
            .describedAs("erased frame")
            .and {
                get { getValue(variable) }
                    .isNotSameInstanceAs(nextFrame.getValue(variable))

                get { getConstraint(variable) }
                    .isNotSameInstanceAs(nextFrame.getConstraint(variable))
            }
    }
}