package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtWhile

class CtWhileAnalyzer : CtLoopAnalyzer<CtWhile>() {

    override fun supports(statement: CtStatement) = statement is CtWhile

    override suspend fun getPreCondition(state: AnalyzerState, loop: CtWhile) =
        state.getConditionValue(loop.loopingExpression)

    override suspend fun getPostCondition(state: AnalyzerState, loop: CtWhile): ConditionCalculatorResult? =
        null
}
