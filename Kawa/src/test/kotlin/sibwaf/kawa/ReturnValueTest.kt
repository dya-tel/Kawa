package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.constraints.BooleanConstraint
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import kotlin.test.Test

class ReturnValueTest : MethodAnalyzerTestBase() {

    @Test fun `Test void method`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    void whatever() {
                        return;
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint).isNull()
    }

    @Test fun `Test unknown boolean value`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    boolean test(boolean x) {
                        return x;
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint)
                .isNotNull()
                .isA<BooleanConstraint>()
                .and {
                    get { isTrue }.isFalse()
                    get { isFalse }.isFalse()
                }
    }

    @Test fun `Test 'false' boolean value`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    boolean test() {
                        return false;
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint)
                .isNotNull()
                .isA<BooleanConstraint>()
                .and {
                    get { isTrue }.isFalse()
                    get { isFalse }.isTrue()
                }
    }

    @Test fun `Test 'true' boolean value`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    boolean test() {
                        return true;
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint)
                .isNotNull()
                .isA<BooleanConstraint>()
                .and {
                    get { isTrue }.isTrue()
                    get { isFalse }.isFalse()
                }
    }

    @Test fun `Test unknown boolean merging`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    boolean test(boolean x) {
                        if (x) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint)
                .isNotNull()
                .isA<BooleanConstraint>()
                .and {
                    get { isTrue }.isFalse()
                    get { isFalse }.isFalse()
                }
    }

    @Test fun `Test 'false' boolean merging`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    boolean test(boolean x) {
                        if (x) {
                            return false;
                        } else {
                            return false;
                        }
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint)
                .isNotNull()
                .isA<BooleanConstraint>()
                .and {
                    get { isTrue }.isFalse()
                    get { isFalse }.isTrue()
                }
    }

    @Test fun `Test 'true' boolean merging`() {
        val flow = runBlocking {
            analyzeMethod(
                    """
                    boolean test(boolean x) {
                        if (x) {
                            return true;
                        } else {
                            return true;
                        }
                    }
                    """.trimIndent()
            )
        }

        expectThat(flow.returnConstraint)
                .isNotNull()
                .isA<BooleanConstraint>()
                .and {
                    get { isTrue }.isTrue()
                    get { isFalse }.isFalse()
                }
    }
}