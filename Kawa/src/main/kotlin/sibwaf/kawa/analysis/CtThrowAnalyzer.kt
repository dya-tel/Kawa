package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtThrow

class CtThrowAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtThrow

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtThrow

        state.returnPoints += statement

        val (frame, _) = state.getValue(statement.thrownExpression)
        return UnreachableFrame.after(frame)
    }
}