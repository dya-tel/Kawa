package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.IdentityHashSet
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtFor
import spoon.reflect.code.CtStatement

class CtForAnalyzer : CtLoopAnalyzer<CtFor>() {

    override fun supports(statement: CtStatement) = statement is CtFor

    override suspend fun getPreCondition(state: AnalyzerState, loop: CtFor) =
        loop.expression?.let { state.getConditionValue(it) }

    override suspend fun getPostCondition(state: AnalyzerState, loop: CtFor): ConditionCalculatorResult? =
        null

    override suspend fun getBodyFlow(state: AnalyzerState, loop: CtFor): DataFrame {
        var resultFrame = super.getBodyFlow(state, loop)
        for (update in loop.forUpdate) {
            if (resultFrame !is ReachableFrame) {
                break
            }

            resultFrame = state.copy(frame = resultFrame).getStatementFlow(update)
        }
        return resultFrame
    }

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtFor

        val initializerState = state.copy(localVariables = IdentityHashSet())
        var frame: ReachableFrame = initializerState.frame
        for (initializer in statement.forInit) {
            val initializerFrame = initializerState.copy(frame = frame).getStatementFlow(initializer)

            if (initializerFrame !is ReachableFrame) {
                return frame
            }
            frame = initializerFrame
        }

        val resultFrame = super.analyze(initializerState.copy(frame = frame), statement).compact(state.frame)
        return if (resultFrame is UnreachableFrame) {
            val cleanedFrame = resultFrame.previous
                .copy(retiredVariables = initializerState.localVariables)

            UnreachableFrame.after(cleanedFrame)
        } else {
            (resultFrame as ReachableFrame).copy(retiredVariables = initializerState.localVariables)
        }
    }
}