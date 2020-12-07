package sibwaf.kawa.analysis

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.calculation.CtLiteralCalculator
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

        val statementAnalyzer = DelegatingStatementAnalyzer(listOf(CtLocalVariableAnalyzer(), CtBlockAnalyzer()))
        val wrappedStatementAnalyzer = object : StatementAnalyzerWrapper(statementAnalyzer) {
            override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
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
            analyzeStatement(wrappedStatementAnalyzer, outerBlock) {
                val calculator = CtLiteralCalculator()
                copy(valueProvider = { state, expression -> calculator.calculate(state, expression) })
            }
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

        val statementAnalyzer = DelegatingStatementAnalyzer(listOf(CtLocalVariableAnalyzer(), CtBlockAnalyzer()))
        val wrappedStatementAnalyzer = object : StatementAnalyzerWrapper(statementAnalyzer) {
            override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
                if (statement is CtLocalVariable<*>) {
                    frames[statement] = state.frame
                }
                return super.analyze(state, statement)
            }
        }

        runBlocking {
            analyzeStatement(wrappedStatementAnalyzer, method.body) {
                val calculator = CtLiteralCalculator()
                copy(valueProvider = { state, expression -> calculator.calculate(state, expression) })
            }
        }

        expect {
            that(frames[xVariable] as ReachableFrame)
                .describedAs("first frame")
                .and {
                    get { next }.isNotNull().isSameInstanceAs(frames[yVariable])
                }

            that(frames[yVariable] as ReachableFrame)
                .describedAs("middle frame")
                .and {
                    get { previous }.isNotNull().isSameInstanceAs(frames[xVariable])
                    get { next }.isNotNull().isSameInstanceAs(frames[zVariable])
                }

            that(frames[zVariable] as ReachableFrame)
                .describedAs("last frame")
                .and {
                    get { previous }.isNotNull().isSameInstanceAs(frames[yVariable])
                }
        }
    }

}