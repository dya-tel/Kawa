package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import sibwaf.kawa.constraints.BooleanConstraint
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.test.Test

class MainTest : MethodAnalyzerTestBase() {

    @Test fun `Test unknown method`() {
        val type = parse(
                """
                class A {
                    boolean x1() {
                        return x2();
                    }
                }
                """.trimIndent()
        )

        val result = runBlocking {
            analyze(type)
        }

        expectThat(result) {
            get { size }.isEqualTo(1)
            get { entries.single() }.and {
                get { key.simpleName }.isEqualTo("x1")
                get { value } and {
                    get { returnConstraint } and {
                        isNotNull()
                        isA<BooleanConstraint>()
                    }
                }
            }
        }
    }

    @Test fun `Test every method gets analyzed`() {
        val type = parse(
                """
                class A {
                    boolean x1() {
                        return true;
                    }
                    boolean x2() {
                        return false;
                    }
                }
                """.trimIndent()
        )

        val result = runBlocking {
            withTimeout(100) {
                analyze(type)
            }
        }

        expectThat(result) {
            get { size }.isEqualTo(2)
            get { keys.map { it.simpleName } }
                    .describedAs("analyzed methods")
                    .containsExactlyInAnyOrder("x1", "x2")
        }
    }
}