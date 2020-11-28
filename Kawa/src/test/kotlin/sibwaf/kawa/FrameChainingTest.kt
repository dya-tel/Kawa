package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isSameInstanceAs
import kotlin.test.Test

class FrameChainingTest : MethodAnalyzerTestBase() {

    @Test fun `Test flow start and end with empty body`() {
        val method = parseMethod(
                """
                void test() {
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        expectThat(flow.startFrame)
                .isSameInstanceAs(flow.endFrame)
    }

    @Test fun `Test simple backward chaining`() {
        val method = parseMethod(
                """
                void test() {
                    int a = 1;
                    int b = 2;
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()

        val aFrame = flow.frames.getValue(variables.getValue("a") as CtStatement)
        val bFrame = flow.frames.getValue(variables.getValue("b") as CtStatement)

        expect {
            that(bFrame).describedAs("b frame")
                    .get { previous }
                    .isSameInstanceAs(aFrame)
            that(flow.endFrame).describedAs("end frame")
                    .get { previous }
                    .isSameInstanceAs(bFrame)
        }
    }

    @Test fun `Test backward chaining in blocks`() {
        val method = parseMethod(
                """
                void test() {
                    int a = 1;
                    {
                        int b = 2;
                        int c = 3;
                    }
                    int d = 4;
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val variables = method.extractVariables()
        val aFrame = flow.frames.getValue(variables.getValue("a") as CtStatement)
        val bFrame = flow.frames.getValue(variables.getValue("b") as CtStatement)
        val cFrame = flow.frames.getValue(variables.getValue("c") as CtStatement)
        val dFrame = flow.frames.getValue(variables.getValue("d") as CtStatement)

        val block = method.body.directChildren
                .mapNotNull { it as? CtBlock<*> }
                .single()
        val blockFrame = flow.blocks.getValue(block).endFrame // FIXME

        expect {
            that(bFrame).describedAs("b frame") and {
                get { previous }
                        .isNotNull()
                        .isNotSameInstanceAs(aFrame)
            }
            that(cFrame).describedAs("c frame") and {
                get { previous }.isSameInstanceAs(bFrame)
            }
            that(dFrame).describedAs("d frame") and {
                get { previous }
                        .isNotNull()
                        .isNotSameInstanceAs(blockFrame)
            }
        }
    }

    @Test fun `Test simple forward chaining`() {
        val method = parseMethod(
                """
                void test() {
                    int a = 1;
                    int b = 2;
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val variables = method.extractVariables()

        val aFrame = flow.frames.getValue(variables.getValue("a") as CtStatement)
        val bFrame = flow.frames.getValue(variables.getValue("b") as CtStatement)

        expect {
            that(aFrame).describedAs("a frame")
                    .get { next }
                    .isSameInstanceAs(bFrame)

            that(bFrame).describedAs("b frame")
                    .get { next }
                    .isSameInstanceAs(flow.endFrame)
        }
    }

    @Test fun `Test forward chaining in blocks`() {
        val method = parseMethod(
                """
                void test() {
                    int a = 1;
                    {
                        int b = 2;
                        int c = 3;
                    }
                    int d = 4;
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }

        val variables = method.extractVariables()
        val aFrame = flow.frames.getValue(variables.getValue("a") as CtStatement)
        val bFrame = flow.frames.getValue(variables.getValue("b") as CtStatement)
        val cFrame = flow.frames.getValue(variables.getValue("c") as CtStatement)
        val dFrame = flow.frames.getValue(variables.getValue("d") as CtStatement)

        expect {
            that(aFrame).describedAs("a frame") and {
                get { next }
                        .isNotNull()
                        .isNotSameInstanceAs(bFrame)
            }
            that(bFrame).describedAs("b frame") and {
                get { next }.isSameInstanceAs(cFrame)
            }
            that(cFrame).describedAs("c frame") and {
                get { next }
                        .isNotNull()
                        .isNotSameInstanceAs(dFrame)
            }
        }
    }

    @Test fun `Test unreachable frame chaining`() {
        val method = parseMethod(
                """
                void test() {
                    int x = 0;
                    return;
                    int y = 4;
                }
                """.trimIndent()
        )

        val flow = runBlocking { analyze(method) }
        val unreachableStatement = method.getElementsOf<CtLocalVariable<*>>().single { it.simpleName == "y" }
        val unreachableFrame = flow.frames[unreachableStatement]

        expectThat(unreachableFrame)
                .describedAs("unreachable frame")
                .isA<UnreachableFrame>()
    }
}