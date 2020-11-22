package sibwaf.kawa

import org.slf4j.LoggerFactory
import sibwaf.kawa.calculation.CtConditionalCalculator
import sibwaf.kawa.calculation.CtConstructorCallCalculator
import sibwaf.kawa.calculation.CtExecutableReferenceExpressionCalculator
import sibwaf.kawa.calculation.CtInvocationCalculator
import sibwaf.kawa.calculation.CtLambdaCalculator
import sibwaf.kawa.calculation.CtLiteralCalculator
import sibwaf.kawa.calculation.CtNewArrayCalculator
import sibwaf.kawa.calculation.CtVariableReadCalculator
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.calculation.conditions.BooleanAndCalculator
import sibwaf.kawa.calculation.conditions.BooleanOrCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.calculation.conditions.EqualityConditionCalculator
import sibwaf.kawa.calculation.conditions.InstanceOfCalculator
import sibwaf.kawa.calculation.conditions.InvertedConditionCalculator
import sibwaf.kawa.calculation.conditions.LiteralConditionCalculator
import sibwaf.kawa.calculation.conditions.VariableReadConditionCalculator
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.reference.CtExecutableReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object ValueCalculator {

    private val log = LoggerFactory.getLogger(ValueCalculator::class.java)
    private val failedExpressionTypes = Collections.newSetFromMap<Class<*>>(ConcurrentHashMap())

    private val calculators = listOf(
            BooleanAndCalculator(),
            BooleanOrCalculator(),
            EqualityConditionCalculator(),
            LiteralConditionCalculator(),
            InvertedConditionCalculator(),
            InstanceOfCalculator(),
            CtVariableReadCalculator(),
            CtConstructorCallCalculator(),
            CtInvocationCalculator(),
            CtLiteralCalculator(),
            CtConditionalCalculator(),
            CtNewArrayCalculator(),
            CtLambdaCalculator(),
            CtExecutableReferenceExpressionCalculator()
    )

    private val conditionCalculators = listOf(
            VariableReadConditionCalculator()
    ) + calculators.filterIsInstance<ConditionCalculator>()

    private suspend fun calculateValue(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        state.annotation.frames[expression] = state.frame

        for (calculator in calculators) {
            if (calculator.supports(expression)) {
                return calculator.calculate(state, expression)
            }
        }

        if (failedExpressionTypes.add(expression.javaClass)) {
            log.warn("Failed to find a calculator for {}", expression.javaClass)
        }

        return MutableDataFrame(state.frame) to ConstrainedValue.from(expression, ValueSource.NONE)
    }

    private suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        state.annotation.frames[expression] = state.frame

        for (calculator in conditionCalculators) {
            if (calculator.supports(expression)) {
                return calculator.calculateCondition(state, expression)
            }
        }

        if (failedExpressionTypes.add(expression.javaClass)) {
            log.warn("Failed to find a condition calculator for {}", expression.javaClass)
        }

        val nextFrame = MutableDataFrame(state.frame)
        return ConditionCalculatorResult(
                thenFrame = nextFrame,
                elseFrame = nextFrame,
                value = BooleanValue(ValueSource.NONE),
                constraint = BooleanConstraint()
        )
    }

    suspend fun calculateValue(
            annotation: MethodFlow,
            frame: DataFrame,
            expression: CtExpression<*>,
            flowProvider: suspend (CtExecutableReference<*>) -> MethodFlow
    ): ConstrainedValue {
        // TODO: return frame

        val state = ValueCalculatorState(
                annotation = annotation,
                frame = frame,
                methodFlowProvider = flowProvider,
                valueProvider = { state, expr -> calculateValue(state, expr) },
                conditionValueProvider = { state, expr -> calculateCondition(state, expr) }
        )

        return calculateValue(state, expression).second
    }

    suspend fun calculateCondition(
            annotation: MethodFlow,
            frame: DataFrame,
            expression: CtExpression<*>,
            flowProvider: suspend (CtExecutableReference<*>) -> MethodFlow
    ): ConditionCalculatorResult {
        val state = ValueCalculatorState(
                annotation = annotation,
                frame = frame,
                methodFlowProvider = flowProvider,
                valueProvider = { state, expr -> calculateValue(state, expr) },
                conditionValueProvider = { state, expr -> calculateCondition(state, expr) }
        )

        return calculateCondition(state, expression)
    }
}