package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.constraints.BooleanConstraint
import strikt.api.expectThat
import kotlin.test.Test

class EqualityComparisonTest : MethodAnalyzerTestBase() {

    private companion object {
        val VARIABLE_REGEX = Regex("""@(\w+)""")
    }

    private fun evaluate(expression: String, prepare: String = "", type: String = "Object"): BooleanConstraint {
        val parameters = VARIABLE_REGEX.findAll(expression)
                .map { it.groupValues[1] }
                .distinct()
                .joinToString { "$type $it" }

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

    private fun checkExact(expression: String, expected: Boolean, prepare: String = "", type: String = "Object") {
        expectThat(evaluate(expression, prepare, type))
                .describedAs("result")
                .assertThat("is $expected") {
                    if (expected) {
                        it.isTrue
                    } else {
                        it.isFalse
                    }
                }
    }

    @Test fun `(null == null) == true`() = checkExact("null == null", true)
    @Test fun `(null != null) == false`() = checkExact("null != null", false)

    @Test fun `(new Object() == null) == false`() = checkExact("new Object() == null", false)
    @Test fun `(new Object() != null) == true`() = checkExact("new Object() != null", true)

    @Test fun `(a && b) == (b && a)`() = checkExact("(@a && @b) == (@b && @a)", true, type = "boolean")

    @Test fun `Same values are equal`() {
        checkExact(
                prepare = "Object y = x;",
                expression = "@x == y",
                expected = true
        )
    }
}