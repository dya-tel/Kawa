package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.constraints.BooleanConstraint
import strikt.api.expectThat
import kotlin.test.Test

class BooleanExpressionTest : MethodAnalyzerTestBase() {

    private companion object {
        val VARIABLE_REGEX = Regex("""@(\w+)""")
    }

    private fun evaluate(expression: String, prepare: String = ""): BooleanConstraint {
        val parameters = VARIABLE_REGEX.findAll(expression)
                .map { it.groupValues[1] }
                .distinct()
                .joinToString { "boolean $it" }

        val cleanExpression = expression.replace(VARIABLE_REGEX, "$1")

        val method = parseMethod(
                """
                void test($parameters) {
                    $prepare;
                    boolean result = $cleanExpression;
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variable = method.extractVariables().getValue("result")
        return flow.endFrame.getConstraint(variable) as BooleanConstraint
    }

    private fun checkExact(expression: String, expected: Boolean, prepare: String = "") {
        expectThat(evaluate(expression, prepare))
                .describedAs("result")
                .assertThat("is $expected") {
                    if (expected) {
                        it.isTrue
                    } else {
                        it.isFalse
                    }
                }
    }

    @Test fun `x && !x == false`() = checkExact("@x && !@x", false)
    @Test fun `!!x && !x == false`() = checkExact("!!@x && !@x", false)
    @Test fun `x && y && !x == false`() = checkExact("@x && @y && !@x", false)
    @Test fun `(x && y) && !(x && y) == false`() = checkExact("(@x && @y) && !(@x && @y)", false)
    @Test fun `y = !x, x && y == false`() {
        checkExact(
                prepare = "boolean y = !x",
                expression = "@x && y",
                expected = false
        )
    }

    @Test fun `x || !x == true`() = checkExact("@x || !@x", true)
    @Test fun `!!x || !x == true`() = checkExact("!!@x || !@x", true)
    @Test fun `x || y || !x == true`() = checkExact("@x || @y || !@x", true)
    @Test fun `(x && y) || !(x && y) == true`() = checkExact("(@x && @y) || !(@x && @y)", true)
    @Test fun `y = !x, x || y == true`() {
        checkExact(
                prepare = "boolean y = !x",
                expression = "@x || y",
                expected = true
        )
    }

    @Test fun `x || (!x && true) == true`() = checkExact("@x || (!@x && true)", true)
    @Test fun `(!x && true) || x == true`() = checkExact("(!@x && true) || @x", true)
}