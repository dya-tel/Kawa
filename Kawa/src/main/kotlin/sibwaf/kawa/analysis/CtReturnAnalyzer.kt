package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtReturn
import spoon.reflect.code.CtStatement

class CtReturnAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtReturn<*>

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtReturn<*>

        val frame = if (statement.returnedExpression != null) {
            state.getValue(statement.returnedExpression).first
        } else {
            state.frame
        }

        return if (frame is ReachableFrame) {
            state.jumpPoints += statement to frame
            UnreachableFrame.after(frame)
        } else {
            state.jumpPoints += statement to (frame as UnreachableFrame).previous
            frame
        }
    }
}