package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.constraints.BooleanConstraint
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import kotlin.test.Test

class InterproceduralTest : MethodAnalyzerTestBase() {

    @Test fun `Test interprocedural analysis skips non-private methods`() {
        val type = parse(
            """
            class A {
                boolean getTrue() {
                    return true; 
                }
                boolean test() {
                    return getTrue();
                }
            }
            """.trimIndent()
        )

        val method = type.getMethodsByName("test").single()
        val flow = runBlocking { analyze(type) }.getValue(method)

        expectThat(flow.returnConstraint)
            .describedAs("returned constraint")
            .isNotNull()
            .isA<BooleanConstraint>()
            .and {
                get { isTrue }.isFalse()
                get { isFalse }.isFalse()
            }
    }

    @Test fun `Test interprocedural analysis doesn't skip private methods`() {
        val type = parse(
            """
            class A {
                private boolean getTrue() {
                    return true; 
                }
                boolean test() {
                    return getTrue();
                }
            }
            """.trimIndent()
        )

        val method = type.getMethodsByName("test").single()
        val flow = runBlocking { analyze(type) }.getValue(method)

        expectThat(flow.returnConstraint)
            .describedAs("returned constraint")
            .isNotNull()
            .isA<BooleanConstraint>()
            .and {
                get { isTrue }.isTrue()
                get { isFalse }.isFalse()
            }
    }

}