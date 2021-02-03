package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.Value
import spoon.reflect.code.CtForEach
import spoon.reflect.code.CtStatement

class CtForEachAnalyzer : CtLoopAnalyzer<CtForEach>() {

    override fun supports(statement: CtStatement) = statement is CtForEach

    override suspend fun getPreCondition(state: AnalyzerState, loop: CtForEach) =
        ConditionCalculatorResult(
            thenFrame = state.frame,
            elseFrame = state.frame,
            value = BooleanValue(null),
            constraint = BooleanConstraint.createUnknown()
        )

    override suspend fun getPostCondition(state: AnalyzerState, loop: CtForEach): ConditionCalculatorResult? =
        null

    override suspend fun getBodyFlow(state: AnalyzerState, loop: CtForEach): DataFrame {
        val frame = MutableDataFrame(state.frame).apply {
            val variable = loop.variable
            val value = Value.withoutSource(loop.variable)

            setValue(variable, value)
            setConstraint(value, ReferenceConstraint.createUnknown())
        }

        return super.getBodyFlow(state.copy(frame = frame), loop)
    }

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtForEach

        val (frame, _) = state.getValue(statement.expression)
        if (frame !is ReachableFrame) {
            return frame
        }

        val resultFrame = super.analyze(state.copy(frame = frame), statement).compact(state.frame)
        return if (resultFrame is UnreachableFrame) {
            val cleanedFrame = resultFrame.previous
                .copy(retiredVariables = listOf(statement.variable))

            UnreachableFrame.after(cleanedFrame)
        } else {
            (resultFrame as ReachableFrame).copy(retiredVariables = listOf(statement.variable))
        }
    }
}