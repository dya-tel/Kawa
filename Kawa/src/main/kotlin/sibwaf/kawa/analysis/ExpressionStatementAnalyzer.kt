package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.calculation.ValueCalculator
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtStatement

abstract class ExpressionStatementAnalyzer : StatementAnalyzer {

    protected abstract val calculator: ValueCalculator

    override fun supports(statement: CtStatement) = statement is CtExpression<*> && calculator.supports(statement)

    final override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtExpression<*>
        return calculator.calculate(state, statement).first
    }
}