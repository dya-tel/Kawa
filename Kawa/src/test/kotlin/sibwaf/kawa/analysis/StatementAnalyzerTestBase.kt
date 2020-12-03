package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.EmptyFlow
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.calculation.DelegatingValueCalculator
import sibwaf.kawa.calculation.conditions.DelegatingConditionCalculator
import spoon.reflect.code.CtStatement
import java.util.Collections

abstract class StatementAnalyzerTestBase {

    protected open class StatementAnalyzerWrapper(private val analyzer: StatementAnalyzer) : StatementAnalyzer {
        override fun supports(statement: CtStatement) = analyzer.supports(statement)

        override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
            return analyzer.analyze(state, statement)
        }
    }

    protected suspend fun analyzeStatement(
            statementAnalyzer: StatementAnalyzer,
            statement: CtStatement,
            customizeState: AnalyzerState.() -> AnalyzerState = { this }
    ): DataFrame {
        val state = AnalyzerState(
                annotation = EmptyFlow,
                frame = MutableDataFrame(null),
                localVariables = Collections.emptySet(),
                returnPoints = Collections.emptySet(),
                jumpPoints = Collections.emptyList(),
                methodFlowProvider = { EmptyFlow },
                statementFlowProvider = statementAnalyzer::analyze,
                valueProvider = DelegatingValueCalculator(emptyList())::calculate,
                conditionValueProvider = DelegatingConditionCalculator(emptyList())::calculateCondition
        )

        return state.customizeState().getStatementFlow(statement)
    }
}