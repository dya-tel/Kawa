package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtContinue
import spoon.reflect.code.CtStatement

class CtContinueAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtContinue

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtContinue
        state.jumpPoints += statement to state.frame
        return UnreachableFrame.after(state.frame)
    }
}