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

        // var lastGuaranteedFrame: DataFrame = MutableDataFrame(state.frame)
        // var lastGuaranteedFrameFound = false
        lateinit var lastExitFrame: DataFrame
        var currentState = state

        for (iteration in 0 until 2) {
            val (thenFrame, elseFrame, _, conditionConstraint) = currentState.getConditionValue(statement.loopingExpression)
            if (conditionConstraint.isFalse) {
                return elseFrame.compact(state.frame)
            }

            val bodyFrame = currentState.copy(frame = thenFrame).getStatementFlow(statement.body)

            if (conditionConstraint.isTrue) {
                /*if (!lastGuaranteedFrameFound) {
                    lastGuaranteedFrame = thenFrame
                }*/

                currentState = currentState.copy(frame = bodyFrame)
            } else {
                // lastGuaranteedFrameFound = true

                val nextFrame = DataFrame.merge(
                        currentState.frame,
                        bodyFrame.compact(currentState.frame),
                        elseFrame.compact(currentState.frame)
                )

                currentState = currentState.copy(frame = nextFrame)
            }

            lastExitFrame = elseFrame
        }

        return lastExitFrame.compact(state.frame)
        /*DataFrame.merge(
                state.frame,
                lastGuaranteedFrame.compact(state.frame),
                lastExitFrame.compact(state.frame)
        )*/
    }
}
