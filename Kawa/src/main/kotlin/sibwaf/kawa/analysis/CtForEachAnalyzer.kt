package sibwaf.kawa.analysis

import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtForEach
import spoon.reflect.code.CtStatement

class CtForEachAnalyzer : CtLoopAnalyzer<CtForEach>() {

    override fun supports(statement: CtStatement) = statement is CtForEach

    override suspend fun getPreCondition(state: StatementAnalyzerState, loop: CtForEach) =
            ConditionCalculatorResult(
                    thenFrame = state.frame,
                    elseFrame = state.frame,
                    value = BooleanValue(ValueSource.NONE),
                    constraint = BooleanConstraint.createUnknown()
            )

    override suspend fun getPostCondition(state: StatementAnalyzerState, loop: CtForEach): ConditionCalculatorResult? =
            null
}