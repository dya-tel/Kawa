package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import spoon.reflect.code.CtIf
import strikt.api.expect
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import kotlin.test.Test

class FrameReachabilityTest : MethodAnalyzerTestBase() {

    @Test fun `Test branch reachability with always false condition`() {
        val method = parseMethod(
                """
                void test() {
                    if (false) {
                        Object x = null;
                    } else {
                        Object y = null;
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val ifStatement = method.getElementsOf<CtIf>().single()
        val thenBranchFrame = flow.blocks.getValue(ifStatement.thenBlock).startFrame
        val elseBranchFrame = flow.blocks.getValue(ifStatement.elseBlock!!).startFrame

        expect {
            that(thenBranchFrame)
                    .describedAs("then-branch frame")
                    .get { isReachable }
                    .isFalse()

            that(elseBranchFrame)
                    .describedAs("else-branch frame")
                    .get { isReachable }
                    .isTrue()
        }
    }

    @Test fun `Test branch reachability with always true condition`() {
        val method = parseMethod(
                """
                void test() {
                    if (true) {
                    } else {
                    }
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val ifStatement = method.getElementsOf<CtIf>().single()
        val thenBranchFrame = flow.blocks.getValue(ifStatement.thenBlock).startFrame
        val elseBranchFrame = flow.blocks.getValue(ifStatement.elseBlock!!).startFrame

        expect {
            that(thenBranchFrame)
                    .describedAs("then-branch frame")
                    .get { isReachable }
                    .isTrue()

            that(elseBranchFrame)
                    .describedAs("else-branch frame")
                    .get { isReachable }
                    .isFalse()
        }
    }
}