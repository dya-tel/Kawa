package sibwaf.kawa

import org.slf4j.LoggerFactory
import sibwaf.kawa.analysis.StatementAnalyzer
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
import sibwaf.kawa.emulation.BasicMethodEmulator
import sibwaf.kawa.emulation.BlackHoleMethodTrace
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtExecutable
import spoon.support.reflect.declaration.CtMethodImpl
import java.util.Collections

private object FailingStatementAnalyzer : StatementAnalyzer {
    override fun supports(statement: CtStatement) = false

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        throw IllegalStateException("Standalone statement analysis in unsupported")
    }
}

private object FallbackCalculator : ConditionCalculator {

    private val log = LoggerFactory.getLogger(FallbackCalculator::class.java)
    private val failedExpressionTypes = ConcurrentHashSet<Class<*>>()

    override fun supports(expression: CtExpression<*>) = true

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        if (failedExpressionTypes.add(expression.javaClass)) {
            log.warn("Failed to find a calculator for {}", expression.javaClass)
        }

        return MutableDataFrame(state.frame) to ConstrainedValue.from(expression)
    }

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        if (failedExpressionTypes.add(expression.javaClass)) {
            log.warn("Failed to find a condition calculator for {}", expression.javaClass)
        }

        return ConditionCalculatorResult(
            thenFrame = MutableDataFrame(state.frame),
            elseFrame = MutableDataFrame(state.frame),
            value = BooleanValue(expression),
            constraint = BooleanConstraint.createUnknown()
        )
    }
}

object ValueCalculator : sibwaf.kawa.calculation.ValueCalculator, ConditionCalculator {

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

    override fun supports(expression: CtExpression<*>) = true

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>) =
        calculator.calculate(state, expression)

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>) =
        conditionCalculator.calculateCondition(state, expression)

    private val placeholderExecutable = CtMethodImpl<Unit>()

    private fun createState(
        frame: ReachableFrame,
        cache: Map<CtExecutable<*>, MethodFlow>
    ): AnalyzerState {
        val emulator = BasicMethodEmulator(Collections.unmodifiableMap(cache).withDefault2(EmptyFlow))
        return AnalyzerState(
            annotation = EmptyFlow,
            trace = BlackHoleMethodTrace,
            frame = frame,
            localVariables = Collections.emptySet(),
            jumpPoints = Collections.emptySet(),
            callChain = RightChain(null, placeholderExecutable),
            methodEmulator = emulator,
            statementFlowProvider = FailingStatementAnalyzer,
            valueProvider = this,
            conditionValueProvider = this
        )
    }

    suspend fun calculateValue(
        frame: ReachableFrame,
        expression: CtExpression<*>,
        cache: Map<CtExecutable<*>, MethodFlow>
    ): Pair<DataFrame, ConstrainedValue> {
        val state = createState(frame, cache)
        return calculate(state, expression)
    }

    suspend fun calculateCondition(
        frame: ReachableFrame,
        expression: CtExpression<*>,
        cache: Map<CtExecutable<*>, MethodFlow>
    ): ConditionCalculatorResult {
        val state = createState(frame, cache)
        return calculateCondition(state, expression)
    }
}