package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtTry
import java.util.LinkedList

class CtTryAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtTry

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtTry

        // TODO: do not erase values until exception can happen
        val bodyFrame = state.getStatementFlow(statement.body).eraseValues()
        val catcherState = state.copy(frame = bodyFrame.copy().apply { isReachable = true })

        val framesToMerge = LinkedList<DataFrame>()
        framesToMerge += bodyFrame
        statement.catchers.mapTo(framesToMerge) { catcherState.getStatementFlow(it.body) }

        val compactedFrames = framesToMerge.map { it.compact(state.frame) }

        val finalizer = statement.finalizer
        return if (finalizer == null) {
            DataFrame.merge(state.frame, compactedFrames)
        } else {
            // FIXME: no-return methods make it actually unreachable

            for (frame in compactedFrames) {
                frame.isReachable = true
            }

            val finalizerFrame = DataFrame.merge(state.frame, compactedFrames)
            state.copy(frame = finalizerFrame).getStatementFlow(finalizer)
        }
    }
}