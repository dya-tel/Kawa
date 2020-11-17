package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.elseBlock
import sibwaf.kawa.thenBlock
import spoon.reflect.code.CtIf
import spoon.reflect.code.CtStatement

class CtIfAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtIf

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtIf

        val (thenStartFrame, elseStartFrame, constraint) = state.getConditionValue(statement.condition)
        val conditionConstraint = constraint.constraint as? BooleanConstraint ?: BooleanConstraint()

        val thenBranch = state.copy(frame = thenStartFrame).getStatementFlow(statement.thenBlock)
                .compact(state.frame)

        val elseBranch = (statement.elseBlock?.let { state.copy(frame = elseStartFrame).getStatementFlow(it) } ?: elseStartFrame)
                .compact(state.frame)

        return if (conditionConstraint.isFalse != conditionConstraint.isTrue) {
            if (conditionConstraint.isTrue) {
                thenBranch
            } else {
                elseBranch
            }
        } else {
            DataFrame.merge(state.frame, thenBranch, elseBranch)
        }
    }
}