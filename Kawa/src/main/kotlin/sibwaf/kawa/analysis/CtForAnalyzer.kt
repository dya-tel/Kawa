package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import spoon.reflect.code.CtFor
import spoon.reflect.code.CtStatement

class CtForAnalyzer : CtLoopAnalyzer<CtFor>() {

    override fun supports(statement: CtStatement) = statement is CtFor

    override suspend fun getPreCondition(state: StatementAnalyzerState, loop: CtFor) =
            loop.expression?.let { state.getConditionValue(it) }

    override suspend fun getPostCondition(state: StatementAnalyzerState, loop: CtFor): ConditionCalculatorResult? =
            null

    override suspend fun getBodyFlow(state: StatementAnalyzerState, loop: CtFor): DataFrame {
        var resultFrame = super.getBodyFlow(state, loop)
        for (update in loop.forUpdate) {
            resultFrame = state.copy(frame = resultFrame).getStatementFlow(update)
        }
        return resultFrame
    }

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtFor

        var initializerState = state//.copy(localVariables = IdentityHashSet()) // FIXME
        for (initializer in statement.forInit) {
            initializerState = initializerState.copy(frame = initializerState.getStatementFlow(initializer))
        }

        return super.analyze(initializerState, statement)
    }
}