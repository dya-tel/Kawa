package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.AnalyzerState
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement

class CtLocalVariableAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtLocalVariable<*>

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtLocalVariable<*>

        state.localVariables += statement
        val expression = statement.defaultExpression
        return if (expression != null) {
            val (frame, result) = state.getValue(expression)
            MutableDataFrame(frame).apply {
                setValue(statement, result.value)
                setConstraint(result.value, result.constraint)
            }
        } else {
            state.frame
        }
    }
}