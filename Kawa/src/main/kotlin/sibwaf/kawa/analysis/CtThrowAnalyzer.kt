package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtThrow

class CtThrowAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtThrow

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtThrow

        val (frame, _) = state.getValue(statement.thrownExpression)

        return if (frame is ReachableFrame) {
            state.jumpPoints += statement to frame
            UnreachableFrame.after(frame)
        } else {
            state.jumpPoints += statement to (frame as UnreachableFrame).previous
            frame
        }
    }
}