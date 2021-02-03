package sibwaf.kawa.utility

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.analysis.StatementAnalyzer
import sibwaf.kawa.calculation.ValueCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.emulation.MethodEmulator
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtStatement
import spoon.reflect.reference.CtExecutableReference

object FailingMethodEmulator : MethodEmulator {
    override suspend fun emulate(state: AnalyzerState, method: CtExecutableReference<*>, arguments: List<ConstrainedValue>) =
        throw IllegalStateException("Method emulation is unsupported")
}

object FailingStatementAnalyzer : StatementAnalyzer {
    override fun supports(statement: CtStatement) = false
    override suspend fun analyze(state: AnalyzerState, statement: CtStatement) =
        throw IllegalStateException("Statement analysis is unsupported")
}

object IdentityStatementAnalyzer : StatementAnalyzer {
    override fun supports(statement: CtStatement) = true
    override suspend fun analyze(state: AnalyzerState, statement: CtStatement) = state.frame
}

object FailingValueCalculator : ValueCalculator {
    override fun supports(expression: CtExpression<*>) = false
    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>) =
        throw IllegalStateException("Value calculation is unsupported")
}

object IdentityValueCalculator : ValueCalculator {
    override fun supports(expression: CtExpression<*>) = true
    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        return state.frame to ConstrainedValue.from(expression)
    }
}

object FailingConditionCalculator : ConditionCalculator {
    override fun supports(expression: CtExpression<*>) = false
    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>) =
        throw IllegalStateException("Condition calculation is unsupported")
}

object IdentityConditionCalculator : ConditionCalculator {
    override fun supports(expression: CtExpression<*>) = true
    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        return ConditionCalculatorResult(
            state.frame,
            state.frame,
            BooleanValue(expression),
            BooleanConstraint.createUnknown()
        )
    }
}