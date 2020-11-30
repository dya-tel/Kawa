package sibwaf.kawa.analysis

import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtDo
import spoon.reflect.code.CtStatement

class CtDoAnalyzer : CtLoopAnalyzer<CtDo>() {

    override fun supports(statement: CtStatement) = statement is CtDo

    override suspend fun getPreCondition(state: StatementAnalyzerState, loop: CtDo): ConditionCalculatorResult? = null

    override suspend fun getPostCondition(state: StatementAnalyzerState, loop: CtDo) =
            state.getConditionValue(loop.loopingExpression)
}