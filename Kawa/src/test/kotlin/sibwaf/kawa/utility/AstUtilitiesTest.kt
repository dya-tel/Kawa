package sibwaf.kawa.utility

import sibwaf.kawa.parseExpression
import spoon.reflect.code.BinaryOperatorKind
import strikt.api.expectThat
import strikt.assertions.containsExactly
import kotlin.test.Test

class AstUtilitiesTest {

    @Test fun `Test expression flattening`() {
        val expression = parseExpression(
                "a && (b || c) && (d || e) && f",
                ('a'..'f').map { "boolean $it" }
        )

        val parts = flattenExpression(expression, BinaryOperatorKind.AND)
                .map { it.toString() }

        expectThat(parts) {
            containsExactly(
                    "a",
                    "(b || c)",
                    "(d || e)",
                    "f"
            )
        }
    }
}