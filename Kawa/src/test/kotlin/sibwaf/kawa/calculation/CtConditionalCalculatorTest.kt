package sibwaf.kawa.calculation

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.conditions.EqualityConditionCalculator
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtConditional
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.test.Test

class CtConditionalCalculatorTest : ValueCalculatorTestBase() {

    @Test fun `Test null check inferred constraints in branches`() {
        val method = parseMethod(
                """
                void test(Object x) {
                    Object y = x == null ? x : x; 
                }
                """.trimIndent()
        )

        val x = method.extractVariables().getValue("x")

        val conditional = method.getElementsOf<CtLocalVariable<*>>().single().defaultExpression

        lateinit var thenFrame: DataFrame
        lateinit var elseFrame: DataFrame
        val calculator = object : TestValueCalculator(
                calculators = listOf(
                        EqualityConditionCalculator(),
                        CtVariableReadCalculator(),
                        CtConditionalCalculator(),
                        CtLiteralCalculator()
                )
        ) {
            override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
                val parent = expression.parent
                if (parent is CtConditional<*>) {
                    when {
                        expression === parent.thenExpression -> thenFrame = state.frame
                        expression === parent.elseExpression -> elseFrame = state.frame
                    }
                }

                return super.calculate(state, expression)
            }
        }

        val frame = MutableDataFrame(null).apply {
            setValue(x, ReferenceValue(ValueSource.NONE))
            setConstraint(x, ReferenceConstraint())
        }

        val state = createState(calculator).copy(frame = frame)
        runBlocking { calculator.calculate(state, conditional) }

        expect {
            that(thenFrame)
                    .describedAs("then frame")
                    .get { getConstraint(x) }
                    .describedAs("inferred x constraint")
                    .isA<ReferenceConstraint>()
                    .get { nullability }
                    .isEqualTo(Nullability.ALWAYS_NULL)

            that(elseFrame)
                    .describedAs("else frame")
                    .get { getConstraint(x) }
                    .describedAs("inferred x constraint")
                    .isA<ReferenceConstraint>()
                    .get { nullability }
                    .isEqualTo(Nullability.NEVER_NULL)
        }
    }
}