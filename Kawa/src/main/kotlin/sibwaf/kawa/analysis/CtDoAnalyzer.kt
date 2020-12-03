package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtDo
import spoon.reflect.code.CtStatement

class CtDoAnalyzer : CtLoopAnalyzer<CtDo>() {

    override fun supports(statement: CtStatement) = statement is CtDo

    override suspend fun getPreCondition(state: AnalyzerState, loop: CtDo): ConditionCalculatorResult? = null

    override suspend fun getPostCondition(state: AnalyzerState, loop: CtDo) =
            state.getConditionValue(loop.loopingExpression)
}