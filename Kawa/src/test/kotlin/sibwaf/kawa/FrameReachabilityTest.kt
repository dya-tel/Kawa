package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import spoon.reflect.code.CtIf
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isNull
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
        val thenBranchFrame = flow.frames[ifStatement.thenBlock]
        val elseBranchFrame = flow.frames[ifStatement.elseBlock!!]

        expect {
            that(thenBranchFrame)
                .describedAs("then-branch frame")
                .isNull()

            that(elseBranchFrame)
                .describedAs("else-branch frame")
                .isA<ReachableFrame>()
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
        val thenBranchFrame = flow.frames[ifStatement.thenBlock]
        val elseBranchFrame = flow.frames[ifStatement.elseBlock!!]

        expect {
            that(thenBranchFrame)
                .describedAs("then-branch frame")
                .isA<ReachableFrame>()

            that(elseBranchFrame)
                .describedAs("else-branch frame")
                .isNull()
        }
    }
}