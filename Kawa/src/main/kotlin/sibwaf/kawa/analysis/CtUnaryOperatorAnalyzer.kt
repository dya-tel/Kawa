package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtUnaryOperator

class CtUnaryOperatorAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtUnaryOperator<*>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtUnaryOperator<*>
        return state.getValue(statement).first
    }
}