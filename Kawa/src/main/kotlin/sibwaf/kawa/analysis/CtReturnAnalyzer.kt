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

        val result = statement.returnedExpression?.let { state.getValue(it) }
        if (result != null) {
            val constraint = result.second.constraint
            val existingConstraint = state.annotation.returnConstraint
            state.annotation.returnConstraint = existingConstraint?.merge(constraint) ?: constraint
        }

        return UnreachableFrame.after(result?.first ?: state.frame)
    }
}