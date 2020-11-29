package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtContinue
import spoon.reflect.code.CtFor
import spoon.reflect.code.CtStatement
import java.util.LinkedList

class CtForAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtFor

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtFor

        // FIXME: leaving frames/values behind here is probably very-very bad, recalc after second run?

        var initializerState = state
        for (initializer in statement.forInit) {
            initializerState = initializerState.copy(frame = initializerState.getStatementFlow(initializer))
        }

        val localState = initializerState.copy(jumpPoints = ArrayList())

        val startFrames = LinkedList<DataFrame>().also { it += MutableDataFrame(localState.frame) }
        val exitFrames = LinkedList<DataFrame>()

        for (iteration in 0 until 2) {
            val iterationState = localState.copy(frame = DataFrame.merge(localState.frame, startFrames))

            val bodyFrame: DataFrame
            if (statement.expression != null) {
                val (thenFrame, elseFrame, _, _) = iterationState.getConditionValue(statement.expression)

                bodyFrame = iterationState.copy(frame = thenFrame).getStatementFlow(statement.body)

                // If the condition is always true, this frame will be unreachable
                // and won't affect anything
                exitFrames += elseFrame.compact(state.frame)
            } else {
                bodyFrame = iterationState.getStatementFlow(statement.body)
            }

            var updateFrame = bodyFrame
            for (update in statement.forUpdate) {
                updateFrame = iterationState.copy(frame = updateFrame).getStatementFlow(update)
            }

            startFrames += updateFrame.compact(localState.frame)

            for (jump in localState.jumpPoints) {
                when (jump.first) {
                    is CtContinue -> startFrames += jump.second.compact(localState.frame)
                    is CtBreak -> exitFrames += jump.second.compact(state.frame)
                    else -> state.jumpPoints += jump
                }
            }
            localState.jumpPoints.clear()
        }

        return DataFrame.merge(state.frame, exitFrames)
    }
}