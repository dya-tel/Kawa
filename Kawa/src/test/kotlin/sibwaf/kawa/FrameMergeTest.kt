package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isSameInstanceAs
import kotlin.test.Test

class FrameMergeTest : MethodAnalyzerTestBase() {

    @Test fun `Test 'if (true)' merging`() {
        val method = parseMethod(
                """
                void test(int a) {
                    int x = 0;
                    if (true) {
                        x = a;
                    } else {
                        x = 1;
                    }
                    int y = a;
                    if (true) {
                    } else {
                        y = 0;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expect {
            that(frame.getValue(variables.getValue("x")))
                    .describedAs("x value")
                    .isSameInstanceAs(frame.getValue(variables.getValue("a")))

            that(frame.getValue(variables.getValue("y")))
                    .describedAs("y value")
                    .isSameInstanceAs(frame.getValue(variables.getValue("a")))
        }
    }

    @Test fun `Test 'if (false)' merging`() {
        val method = parseMethod(
                """
                void test(int a) {
                    int x = 0;
                    if (false) {
                        x = 1;
                    } else {
                        x = a;
                    }
                    int y = a;
                    if (false) {
                        y = 1;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expect {
            that(frame.getValue(variables.getValue("x")))
                    .describedAs("x value")
                    .isSameInstanceAs(frame.getValue(variables.getValue("a")))

            that(frame.getValue(variables.getValue("y")))
                    .describedAs("y value")
                    .isSameInstanceAs(frame.getValue(variables.getValue("a")))
        }
    }

    @Test fun `Test 'if' merging with single branch`() {
        val method = parseMethod(
                """
                void test(boolean condition, int a, int b) {
                    int x = a;
                    if (condition) {
                        x = b;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expectThat(frame.getValue(variables.getValue("x")))
                .describedAs("x value")
                .isNotNull()
                .isNotSameInstanceAs(frame.getValue(variables.getValue("a")))
                .isNotSameInstanceAs(frame.getValue(variables.getValue("b")))
    }

    @Test fun `Test 'if' variable value merging`() {
        val method = parseMethod(
                """
                void test(boolean condition, int a, int b, int c) {
                    int x = a;
                    if (condition) {
                        x = b;
                    } else {
                        x = c;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expectThat(frame.getValue(variables.getValue("x")))
                .describedAs("x value")
                .isNotNull()
                .isNotSameInstanceAs(frame.getValue(variables.getValue("a")))
                .isNotSameInstanceAs(frame.getValue(variables.getValue("b")))
                .isNotSameInstanceAs(frame.getValue(variables.getValue("c")))
    }

    @Test fun `Test flow-breaking branch doesn't change values`() {
        val method = parseMethod(
                """
                void test(boolean condition, int a) {
                    int x = a;
                    if (condition) {
                        x = 1;
                        return;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expectThat(frame.getValue(variables.getValue("x")))
                .describedAs("x value")
                .isSameInstanceAs(frame.getValue(variables.getValue("a")))
    }

    @Test fun `Test flow-breaking in both branches ends flow`() {
        val method = parseMethod(
                """
                void test(boolean condition, int a) {
                    if (condition) {
                        return;
                    } else {
                        return;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        expectThat(flow.endFrame)
                .describedAs("end frame")
                .get { isReachable }
                .isFalse()
    }

    @Test fun `Test 'if' merging for non-sequential value diff`() {
        val method = parseMethod(
                """
                void test(boolean condition, int a) {
                    int x = 1;
                    int y = 2;
                    if (condition) {
                        x = a;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expectThat(frame.getValue(variables.getValue("x")))
                .describedAs("x value")
                .isNotNull()
                .isNotSameInstanceAs(frame.getValue(variables.getValue("a")))
    }

    @Test fun `Test 'if' merging doesn't overwrite unrelated values`() {
        val method = parseMethod(
                """
                void test(boolean condition, int a, int b) {
                    int x = 1;
                    int y = a;
                    if (condition) {
                        x = a;
                    } else {
                        x = b;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()
        val frame = flow.endFrame

        expectThat(frame.getValue(variables.getValue("y")))
                .describedAs("y value")
                .isNotNull()
                .isSameInstanceAs(frame.getValue(variables.getValue("a")))
    }

//    @Test fun `Test `
}