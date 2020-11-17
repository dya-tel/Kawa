package sibwaf.kawa.analysis

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.DataFrame
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isNotNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.map
import kotlin.test.Test

class CtBlockAnalyzerTest : StatementAnalyzerTestBase() {

    @Test fun `Test local variable context isolation`() {
        val method = parseMethod(
                """
                void test() {
                    String x = null;
                    {
                        String y = null;
                    }
                }
                """.trimIndent()
        )

        val outerBlock = method.body!!
        val innerBlock = outerBlock.directChildren.filterIsInstance<CtBlock<*>>().single()

        lateinit var outerBlockVariables: Set<CtLocalVariable<*>>
        lateinit var innerBlockVariables: Set<CtLocalVariable<*>>

        val statementAnalyzer = object : TestCtStatementAnalyzer(listOf(CtLocalVariableAnalyzer(), CtBlockAnalyzer())) {
            override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
                val frame = super.analyze(state, statement)

                if (statement is CtLocalVariable<*>) {
                    when (statement.parent) {
                        outerBlock -> outerBlockVariables = state.localVariables.toSet()
                        innerBlock -> innerBlockVariables = state.localVariables.toSet()
                    }
                }

                return frame
            }
        }

        runBlocking {
            analyzeStatement(
                    statementAnalyzer = statementAnalyzer,
                    statement = outerBlock
            )
        }

        expect {
            that(outerBlockVariables)
                    .describedAs("outer block variables")
                    .map { it.simpleName }
                    .containsExactly("x")

            that(innerBlockVariables)
                    .describedAs("inner block variables")
                    .map { it.simpleName }
                    .containsExactly("y")
        }
    }

    @Test fun `Test local variable cleanup after block`() {
        // TODO
    }

    @Test fun `Test inner frame chaining`() {
        val method = parseMethod(
                """
                void test() {
                    String x = null;
                    String y = null;
                    String z = null;
                }
                """.trimIndent()
        )

        val (xVariable, yVariable, zVariable) = method.getElementsOf<CtLocalVariable<*>>()

        val frames = HashMap<CtStatement, DataFrame>()
        val statementAnalyzer = object : TestCtStatementAnalyzer(listOf(CtLocalVariableAnalyzer(), CtBlockAnalyzer())) {
            override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
                if (statement is CtLocalVariable<*>) {
                    frames[statement] = state.frame
                }
                return super.analyze(state, statement)
            }
        }

        runBlocking {
            analyzeStatement(
                    statementAnalyzer = statementAnalyzer,
                    statement = method.body
            )
        }

        expect {
            that(frames[xVariable])
                    .describedAs("first frame")
                    .isNotNull()
                    .and {
                        get { next }.isNotNull().isSameInstanceAs(frames[yVariable])
                    }

            that(frames[yVariable])
                    .describedAs("middle frame")
                    .isNotNull()
                    .and {
                        get { previous }.isNotNull().isSameInstanceAs(frames[xVariable])
                        get { next }.isNotNull().isSameInstanceAs(frames[zVariable])
                    }

            that(frames[zVariable])
                    .describedAs("last frame")
                    .isNotNull()
                    .and {
                        get { previous }.isNotNull().isSameInstanceAs(frames[yVariable])
                    }
        }
    }

}