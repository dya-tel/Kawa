package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement

class CtLocalVariableAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtLocalVariable<*>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtLocalVariable<*>

        state.localVariables += statement
        val expression = statement.defaultExpression
        return if (expression != null) {
            val (value, constraint) = state.getValue(expression)
            MutableDataFrame(state.frame).apply {
                setValue(statement, value)
                setConstraint(value, constraint)
            }
        } else {
            state.frame
        }
    }
}