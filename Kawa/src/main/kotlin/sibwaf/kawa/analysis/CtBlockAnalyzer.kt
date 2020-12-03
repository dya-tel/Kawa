package sibwaf.kawa.analysis

import sibwaf.kawa.BlockFlow
import sibwaf.kawa.DataFrame
import sibwaf.kawa.IdentityHashSet
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtStatementList

class CtBlockAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtBlock<*>

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtStatementList

        val startFrame: DataFrame = MutableDataFrame(state.frame)
        var localState = state.copy(
                frame = startFrame,
                localVariables = IdentityHashSet()
        )

        for (nestedStatement in statement.statements) {
            val nextFrame = localState.getStatementFlow(nestedStatement)
            if (nextFrame == localState.frame) {
                continue
            }

            localState.frame.next = nextFrame
            localState = localState.copy(frame = nextFrame)
        }

        val endFrame = localState.frame

        state.annotation.blocks[statement] = BlockFlow().also {
            it.startFrame = startFrame
            it.endFrame = endFrame
        }

        return if (endFrame is UnreachableFrame) {
            val cleanedFrame = endFrame.previous
                    .compact(state.frame)
                    .copy(retiredVariables = localState.localVariables)

            UnreachableFrame.after(cleanedFrame)
        } else {
            endFrame.compact(state.frame).copy(retiredVariables = localState.localVariables)
        }
    }
}