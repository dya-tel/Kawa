package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.BooleanConstraint
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtWhile

class CtWhileAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtWhile

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtWhile

        // TODO: if condition is always true even on the second run, it could be an infinite loop
        // FIXME: leaving frames/values behind here is probably very-very bad, recalc after second run?

        val (firstThenFrame, firstElseFrame, firstValue) = state.getConditionValue(statement.loopingExpression)
        val firstConditionConstraint = firstValue.constraint as? BooleanConstraint ?: BooleanConstraint()
        if (firstConditionConstraint.isFalse) {
            return DataFrame.merge(
                    state.frame,
                    firstThenFrame,
                    firstElseFrame
            )
        }

        var firstRunFrame = state.copy(frame = firstThenFrame).getStatementFlow(statement.body)
        if (!firstConditionConstraint.isTrue) {
            firstRunFrame = DataFrame.merge(
                    state.frame,
                    MutableDataFrame(state.frame),
                    firstRunFrame.compact(state.frame)
            )
        }

        val (secondThenFrame, secondElseFrame, secondValue) = state.copy(frame = firstRunFrame).getConditionValue(statement.loopingExpression)
        val secondConditionConstraint = secondValue.constraint as? BooleanConstraint ?: BooleanConstraint()
        if (secondConditionConstraint.isFalse) {
            return DataFrame.merge(
                    state.frame,
                    secondThenFrame.compact(state.frame),
                    secondElseFrame.compact(state.frame)
            )
        }

        val secondRunFrame = state.copy(frame = secondThenFrame).getStatementFlow(statement.body)

        return DataFrame.merge(
                state.frame,
                firstRunFrame.compact(state.frame),
                secondRunFrame.compact(state.frame)
        )
    }
}
