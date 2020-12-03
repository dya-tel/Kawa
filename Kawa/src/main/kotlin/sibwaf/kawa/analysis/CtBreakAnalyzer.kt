package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtStatement

class CtBreakAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtBreak

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtBreak
        state.jumpPoints += statement to state.frame
        return UnreachableFrame.after(state.frame)
    }
}