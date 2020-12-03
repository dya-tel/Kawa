package sibwaf.kawa

import org.slf4j.LoggerFactory
import sibwaf.kawa.calculation.CtAssignmentCalculator
import sibwaf.kawa.calculation.CtConditionalCalculator
import sibwaf.kawa.calculation.CtConstructorCallCalculator
import sibwaf.kawa.calculation.CtExecutableReferenceExpressionCalculator
import sibwaf.kawa.calculation.CtInvocationCalculator
import sibwaf.kawa.calculation.CtLambdaCalculator
import sibwaf.kawa.calculation.CtLiteralCalculator
import sibwaf.kawa.calculation.CtNewArrayCalculator
import sibwaf.kawa.calculation.CtUnaryOperatorIncDecCalculator
import sibwaf.kawa.calculation.CtVariableReadCalculator
import sibwaf.kawa.calculation.DelegatingValueCalculator
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.calculation.conditions.BooleanAndCalculator
import sibwaf.kawa.calculation.conditions.BooleanOrCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.calculation.conditions.DelegatingConditionCalculator
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

private object FallbackCalculator : ConditionCalculator {

    private val log = LoggerFactory.getLogger(FallbackCalculator::class.java)
    private val failedExpressionTypes = ConcurrentHashSet<Class<*>>()

    override fun supports(expression: CtExpression<*>) = true

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        if (failedExpressionTypes.add(expression.javaClass)) {
            log.warn("Failed to find a calculator for {}", expression.javaClass)
        }

        return MutableDataFrame(state.frame) to ConstrainedValue.from(expression, ValueSource.NONE)
    }

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        if (failedExpressionTypes.add(expression.javaClass)) {
            log.warn("Failed to find a condition calculator for {}", expression.javaClass)
        }

        return ConditionCalculatorResult(
                thenFrame = MutableDataFrame(state.frame),
                elseFrame = MutableDataFrame(state.frame),
                value = BooleanValue(ValueSource.NONE),
                constraint = BooleanConstraint.createUnknown()
        )
    }
}

object ValueCalculator {

    private val calculators = listOf(
            BooleanAndCalculator(),
            BooleanOrCalculator(),
            EqualityConditionCalculator(),
            LiteralConditionCalculator(),
            InvertedConditionCalculator(),
            InstanceOfCalculator(),
            CtUnaryOperatorIncDecCalculator(),
            CtVariableReadCalculator(),
            CtConstructorCallCalculator(),
            CtInvocationCalculator(),
            CtLiteralCalculator(),
            CtConditionalCalculator(),
            CtAssignmentCalculator(),
            CtNewArrayCalculator(),
            CtLambdaCalculator(),
            CtExecutableReferenceExpressionCalculator(),
            FallbackCalculator
    )

    private val conditionCalculators = listOf(
            VariableReadConditionCalculator()
    ) + calculators.filterIsInstance<ConditionCalculator>()

    private val calculator = DelegatingValueCalculator(calculators)
    private val conditionCalculator = DelegatingConditionCalculator(conditionCalculators)

    suspend fun calculateValue(state: ValueCalculatorState, expression: CtExpression<*>) =
        calculator.calculate(state, expression)

    suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>) =
        conditionCalculator.calculateCondition(state, expression)

    suspend fun calculateValue(
            annotation: MethodFlow,
            frame: DataFrame,
            expression: CtExpression<*>,
            flowProvider: suspend (CtExecutableReference<*>) -> MethodFlow
    ): Pair<DataFrame, ConstrainedValue> {
        val state = ValueCalculatorState(
                annotation = annotation,
                frame = frame,
                methodFlowProvider = flowProvider,
                valueProvider = { state, expr -> calculateValue(state, expr) },
                conditionValueProvider = { state, expr -> calculateCondition(state, expr) }
        )

        return calculateValue(state, expression)
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