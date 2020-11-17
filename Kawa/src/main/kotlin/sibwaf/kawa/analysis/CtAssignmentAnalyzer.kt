package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodPurity
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtAssignment
import spoon.reflect.code.CtFieldWrite
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtVariableAccess

class CtAssignmentAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtAssignment<*, *>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtAssignment<*, *>

        val target = statement.assigned
        if (target is CtFieldWrite<*>) {
            state.annotation.purity = MethodPurity.DIRTIES_THIS // TODO
        } else {
            val targetVariable = (target as? CtVariableAccess<*>)
                    ?.variable
                    ?.declaration

            if (targetVariable != null) {
                val (value, constraint) = state.getValue(statement.assignment)
                return MutableDataFrame(state.frame).apply {
                    setValue(targetVariable, value)
                    setConstraint(value, constraint)
                }
            }
        }

        return state.frame
    }
}