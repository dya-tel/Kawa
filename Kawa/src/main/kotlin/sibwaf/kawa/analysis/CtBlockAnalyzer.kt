package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.BlockFlow
import sibwaf.kawa.DataFrame
import sibwaf.kawa.IdentityHashSet
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtStatementList

class CtBlockAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtBlock<*>

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtStatementList

        val startFrame: ReachableFrame = MutableDataFrame(state.frame)
        val localState = state.copy(localVariables = IdentityHashSet())

        var lastFrame: DataFrame = startFrame
        for (nestedStatement in statement.statements) {
            if (lastFrame !is ReachableFrame) {
                break
            }

            val nextFrame = localState.copy(frame = lastFrame).getStatementFlow(nestedStatement)
            if (nextFrame == lastFrame) {
                continue
            }

            lastFrame.next = nextFrame
            lastFrame = nextFrame
        }

        if (statement is CtBlock<*>) {
            state.trace.trace(statement, startFrame, lastFrame)
        }

        lastFrame = lastFrame.compact(state.frame)

        return if (lastFrame is UnreachableFrame) {
            val cleanedFrame = lastFrame.previous.copy(retiredVariables = localState.localVariables)
            UnreachableFrame.after(cleanedFrame)
        } else {
            (lastFrame as ReachableFrame).copy(retiredVariables = localState.localVariables)
        }
    }
}