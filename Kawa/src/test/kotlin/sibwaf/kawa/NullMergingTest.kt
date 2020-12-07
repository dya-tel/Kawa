package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.test.Test

class NullMergingTest : MethodAnalyzerTestBase() {

    @Test fun `Test 'if' possible null merging single-branch`() {
        val method = parseMethod(
            """
            void test(boolean condition) {
                java.util.List<String> list = new java.util.ArrayList<>();
                if (condition) {
                    list = null;
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame as ReachableFrame

        expectThat(frame.getConstraint(variables.getValue("list")))
            .describedAs("list constraint")
            .isNotNull()
            .isA<ReferenceConstraint>()
            .get { nullability }
            .isEqualTo(Nullability.POSSIBLE_NULL)
    }

    @Test fun `Test 'if' possible null merging multi-branch`() {
        val method = parseMethod(
            """
            void test(boolean condition) {
                java.util.List<String> list;
                if (condition) {
                    list = null;
                } else {
                    list = new java.util.ArrayList<>();
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame as ReachableFrame

        expectThat(frame.getConstraint(variables.getValue("list")))
            .describedAs("list constraint")
            .isNotNull()
            .isA<ReferenceConstraint>()
            .get { nullability }
            .isEqualTo(Nullability.POSSIBLE_NULL)
    }

    @Test fun `Test 'if (x == null) { x = not-null }' merging`() {
        val method = parseMethod(
            """
            void test(boolean condition) {
                java.util.List<String> list = new java.util.ArrayList<>();
                if (condition) {
                    list = null;
                }

                if (list == null) {
                    list = new java.util.ArrayList<>();
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame as ReachableFrame//.previous!!

        expectThat(frame.getConstraint(variables.getValue("list")))
            .describedAs("list constraint")
            .isNotNull()
            .isA<ReferenceConstraint>()
            .get { nullability }
            .isEqualTo(Nullability.NEVER_NULL)
    }
}