package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtReturn
import spoon.reflect.code.CtStatement

class CtReturnAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtReturn<*>

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtReturn<*>
        state.jumpPoints += statement to state.frame

        val frame = if (statement.returnedExpression != null) {
            state.getValue(statement.returnedExpression).first
        } else {
            state.frame
        }

        return UnreachableFrame.after(frame)
    }
}