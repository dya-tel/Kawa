package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtContinue
import spoon.reflect.code.CtLoop
import spoon.reflect.code.CtStatement
import java.util.LinkedList

abstract class CtLoopAnalyzer<T : CtLoop> : StatementAnalyzer {

    protected abstract suspend fun getPreCondition(state: AnalyzerState, loop: T): ConditionCalculatorResult?
    protected abstract suspend fun getPostCondition(state: AnalyzerState, loop: T): ConditionCalculatorResult?

    protected open suspend fun getBodyFlow(state: AnalyzerState, loop: T): DataFrame {
        return state.getStatementFlow(loop.body)
    }

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        @Suppress("unchecked_cast")
        statement as T

        val localState = state.copy(jumpPoints = ArrayList())

        val startFrames = LinkedList<DataFrame>().also { it += MutableDataFrame(localState.frame) }
        val exitFrames = LinkedList<DataFrame>()

        for (iteration in 0 until 2) {
            val iterationState = localState.copy(frame = DataFrame.merge(localState.frame, startFrames))

            var bodyFrame: DataFrame

            // When any condition is a known 'true' value,
            // the 'elseFrame' will be unreachable and won't affect the result

            val preCondition = getPreCondition(iterationState, statement)
            if (preCondition != null) {
                bodyFrame = getBodyFlow(iterationState.copy(frame = preCondition.thenFrame), statement)
                exitFrames += preCondition.elseFrame.compact(state.frame)
            } else {
                bodyFrame = getBodyFlow(iterationState, statement)
            }

            val postCondition = getPostCondition(iterationState.copy(frame = bodyFrame), statement)
            if (postCondition != null) {
                bodyFrame = postCondition.thenFrame
                exitFrames += postCondition.elseFrame.compact(state.frame)
            }

            startFrames += bodyFrame.compact(localState.frame)

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
