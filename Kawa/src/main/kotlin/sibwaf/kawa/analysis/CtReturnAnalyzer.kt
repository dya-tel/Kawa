package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtReturn
import spoon.reflect.code.CtStatement

class CtReturnAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtReturn<*>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtReturn<*>

        if (state.frame.isReachable) {
            state.returnPoints += statement
        }

        val constraint = statement.returnedExpression
                ?.let { state.getValue(it) }
                ?.constraint

        if (constraint != null) {
            val existingConstraint = state.annotation.returnConstraint
            state.annotation.returnConstraint = existingConstraint?.merge(constraint) ?: constraint
        }

        return MutableDataFrame(state.frame).apply { isReachable = false }
    }
}