package sibwaf.kawa.analysis

import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtWhile

class CtWhileAnalyzer : CtLoopAnalyzer<CtWhile>() {

    override fun supports(statement: CtStatement) = statement is CtWhile

    override suspend fun getPreCondition(state: StatementAnalyzerState, loop: CtWhile) =
            state.getConditionValue(loop.loopingExpression)

    override suspend fun getPostCondition(state: StatementAnalyzerState, loop: CtWhile): ConditionCalculatorResult? =
            null
}
