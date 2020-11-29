package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtLoop
import spoon.reflect.code.CtStatement

class CtLoopAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtLoop

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtLoop

        val localState = state.copy(jumpPoints = ArrayList())

        val body = statement.body as CtBlock<*>

        val blockFrame = localState.getStatementFlow(body).compact(state.frame)
        val secondIterationFrame = DataFrame.merge(
                state.frame,
                MutableDataFrame(state.frame),
                blockFrame
        )

        val secondIterationState = localState.copy(frame = secondIterationFrame)

        var resultFrame = secondIterationState.getStatementFlow(body).compact(state.frame)

        for (jump in localState.jumpPoints) {
            // TODO: check labels
            if (jump.first is CtBreak) {
                resultFrame = DataFrame.merge(state.frame, resultFrame, jump.second.compact(state.frame))
            } else {
                state.jumpPoints.add(jump)
            }
        }

        return resultFrame
    }
}
