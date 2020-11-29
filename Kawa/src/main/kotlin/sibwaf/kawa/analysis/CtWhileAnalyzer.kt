package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtContinue
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtWhile
import java.util.LinkedList

class CtWhileAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtWhile

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtWhile

        // FIXME: leaving frames/values behind here is probably very-very bad, recalc after second run?

        val localState = state.copy(jumpPoints = ArrayList())

        val startFrames = LinkedList<DataFrame>().also { it += MutableDataFrame(localState.frame) }
        val exitFrames = LinkedList<DataFrame>()

        for (iteration in 0 until 2) {
            val entryFrame = DataFrame.merge(localState.frame, startFrames)
            val iterationState = localState.copy(frame = entryFrame)

            val (thenFrame, elseFrame, _, _) = iterationState.getConditionValue(statement.loopingExpression)

            val bodyFrame = localState.copy(frame = thenFrame).getStatementFlow(statement.body)
            startFrames += bodyFrame.compact(localState.frame)

            // If the condition is always true, this frame will be unreachable
            // and won't affect anything
            exitFrames += elseFrame.compact(localState.frame)

            for (jump in localState.jumpPoints) {
                when (jump.first) {
                    is CtContinue -> startFrames += jump.second.compact(state.frame)
                    is CtBreak -> exitFrames += jump.second.compact(state.frame)
                    else -> state.jumpPoints += jump
                }
            }
            localState.jumpPoints.clear()
        }

        return DataFrame.merge(state.frame, exitFrames)
    }
}
