package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.EmptyFlow
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtStatement
import java.util.Collections

abstract class StatementAnalyzerTestBase {

    protected open class TestCtStatementAnalyzer(private val analyzers: List<StatementAnalyzer>) : StatementAnalyzer {
        override fun supports(statement: CtStatement) = true

        override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
            for (analyzer in analyzers) {
                if (analyzer.supports(statement)) {
                    return analyzer.analyze(state, statement)
                }
            }

            throw IllegalStateException("No analyzer registered for ${statement.javaClass}")
        }
    }

    protected suspend fun analyzeStatement(
            statementAnalyzer: StatementAnalyzer,
            statement: CtStatement,
            customizeState: StatementAnalyzerState.() -> StatementAnalyzerState = { this }
    ) {
        val state = StatementAnalyzerState(
                annotation = EmptyFlow,
                frame = MutableDataFrame(null),
                localVariables = Collections.emptySet(),
                returnPoints = Collections.emptySet(),
                methodFlowProvider = { EmptyFlow },
                statementFlowProvider = { state, currentStatement -> statementAnalyzer.analyze(state, currentStatement) },
                valueProvider = { _, _ -> ConstrainedValue(Value(ValueSource.NONE), Constraint()) }
        )
        statementAnalyzer.analyze(state.customizeState(), statement)
    }
}