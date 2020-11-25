package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtStatement

class CtBreakAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtBreak

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtBreak
        state.jumpPoints += statement to state.frame
        return MutableDataFrame(state.frame).apply { isReachable = false }
    }
}