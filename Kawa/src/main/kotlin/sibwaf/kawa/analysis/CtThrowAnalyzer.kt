package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtThrow

class CtThrowAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtThrow

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        if (state.frame.isReachable) {
            state.returnPoints += statement
        }

        return MutableDataFrame(state.frame).apply { isReachable = false }
    }
}