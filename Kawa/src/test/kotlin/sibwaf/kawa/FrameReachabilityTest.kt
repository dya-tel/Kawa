package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import spoon.reflect.code.CtIf
import strikt.api.expect
import strikt.assertions.isA
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
        val thenBranchFrame = flow.frames.getValue(ifStatement.thenBlock)
        val elseBranchFrame = flow.frames.getValue(ifStatement.elseBlock!!)

        expect {
            that(thenBranchFrame)
                .describedAs("then-branch frame")
                .isA<UnreachableFrame>()

            that(elseBranchFrame)
                .describedAs("else-branch frame")
                .not().isA<UnreachableFrame>()
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
        val thenBranchFrame = flow.frames.getValue(ifStatement.thenBlock)
        val elseBranchFrame = flow.frames.getValue(ifStatement.elseBlock!!)

        expect {
            that(thenBranchFrame)
                .describedAs("then-branch frame")
                .not().isA<UnreachableFrame>()

            that(elseBranchFrame)
                .describedAs("else-branch frame")
                .isA<UnreachableFrame>()
        }
    }
}