package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtTry

class CtTryAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtTry

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtTry

        val localState = state.copy(jumpPoints = ArrayList())

        val bodyFrame = localState.getStatementFlow(statement.body)

        // TODO: .lastReachableParent
        // TODO: do not erase values until exception can happen
        val lastReachableBodyFrame = if (bodyFrame is UnreachableFrame) {
            bodyFrame.previous
        } else {
            bodyFrame
        }.eraseValues()

        // TODO: some throws can be caught by our catchers
        val catcherState = localState.copy(frame = lastReachableBodyFrame)
        val catcherFrames = statement.catchers
                .map { catcherState.getStatementFlow(it.body).compact(localState.frame) }

        state.jumpPoints.addAll(localState.jumpPoints)

        val finalizer = statement.finalizer
        return if (finalizer == null) {
            DataFrame.merge(state.frame, catcherFrames + bodyFrame)
        } else {
            val jumpFrames = localState.returnPoints
                    .asSequence()
                    .filter { it.hasParent(statement) }
                    .map { localState.annotation.frames.getValue(it) }
                    .plus(localState.jumpPoints.map { it.second })
                    .map { it.compact(state.frame) }

            // FIXME: next frame reachability should be determined only by framesToMerge

            val finalizerFrame = DataFrame.merge(
                    state.frame,
                    (jumpFrames + catcherFrames + lastReachableBodyFrame).toList()
            )

            val resultFrame = state.copy(frame = finalizerFrame).getStatementFlow(finalizer)
            if (bodyFrame is UnreachableFrame && catcherFrames.all { it is UnreachableFrame }) {
                UnreachableFrame.after(resultFrame)
            } else {
                resultFrame
            }
        }
    }
}