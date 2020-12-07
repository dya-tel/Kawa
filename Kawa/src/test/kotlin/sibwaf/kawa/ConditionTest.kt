package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import spoon.reflect.code.CtIf
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.test.Test

class ConditionTest : MethodAnalyzerTestBase() {

    @Test fun `Test single null-checked value inside 'if' branches`() {
        val method = parseMethod(
            """
            void test(Object x) {
                if (x == null) {
                } else {
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val parameter = method.extractVariables().values.single()
        val ifStatement = method.getElementsOf<CtIf>().single()
        val thenBranchFrame = flow.blocks.getValue(ifStatement.thenBlock).startFrame
        val elseBranchFrame = flow.blocks.getValue(ifStatement.elseBlock!!).startFrame

        expect {
            that(thenBranchFrame.getConstraint(parameter) as ReferenceConstraint)
                .describedAs("parameter constraint in then-branch")
                .get { nullability }
                .isEqualTo(Nullability.ALWAYS_NULL)

            that(elseBranchFrame.getConstraint(parameter) as ReferenceConstraint)
                .describedAs("parameter constraint in else-branch")
                .get { nullability }
                .isEqualTo(Nullability.NEVER_NULL)
        }
    }

    @Test fun `Test single non-null-checked value inside 'if' branches`() {
        val method = parseMethod(
            """
            void test(Object x) {
                if (x != null) {
                } else {
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val parameter = method.extractVariables().values.single()
        val ifStatement = method.getElementsOf<CtIf>().single()
        val thenBranchFrame = flow.blocks.getValue(ifStatement.thenBlock).startFrame
        val elseBranchFrame = flow.blocks.getValue(ifStatement.elseBlock)!!.startFrame

        expect {
            that(thenBranchFrame.getConstraint(parameter) as ReferenceConstraint)
                .describedAs("parameter constraint in then-branch")
                .get { nullability }
                .isEqualTo(Nullability.NEVER_NULL)

            that(elseBranchFrame.getConstraint(parameter) as ReferenceConstraint)
                .describedAs("parameter constraint in else-branch")
                .get { nullability }
                .isEqualTo(Nullability.ALWAYS_NULL)
        }
    }

    @Test fun `Test null-check with optional flow break doesn't affect next frame`() {
        val method = parseMethod(
            """
            void test(boolean condition, Object x) {
                if (x == null || condition) {
                    if (condition) {
                        return;
                    }
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val parameter = method.extractVariables().getValue("x")
        val frame = flow.endFrame as ReachableFrame

        expect {
            that(frame.getConstraint(parameter))
                .describedAs("parameter constraint")
                .isNotNull()
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.UNKNOWN)
        }
    }

    @Test fun `Test null-check with flow break affects next frames`() {
        val method = parseMethod(
            """
            void test(Object x) {
                if (x == null) {
                    return;
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val parameter = method.extractVariables().values.single()
        val frame = flow.endFrame as ReachableFrame

        expect {
            that(frame.getConstraint(parameter))
                .describedAs("parameter constraint")
                .isNotNull()
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.NEVER_NULL)
        }
    }

    @Test fun `Test 'if' merging doesn't change nullability information`() {
        val method = parseMethod(
            """
            void test(Object x) {
                if (x == null) {
                } else {
                }
            }
            """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val parameter = method.extractVariables().values.single()
        val frame = flow.endFrame as ReachableFrame

        expectThat(frame.getConstraint(parameter))
            .describedAs("parameter constraint")
            .isNotNull()
            .isA<ReferenceConstraint>()
            .get { nullability }
            .isEqualTo(Nullability.UNKNOWN)
    }
}