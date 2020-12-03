package sibwaf.kawa.calculation.conditions

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.CtLiteralCalculator
import sibwaf.kawa.calculation.CtVariableReadCalculator
import sibwaf.kawa.calculation.ValueCalculatorTestBase
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtLocalVariable
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.test.Test

class EqualityConditionCalculatorTest : ValueCalculatorTestBase() {

    @Test fun `Test inferred null constraint with null check`() {
        val method = parseMethod(
            """
            void test(Object x) {
                boolean isNull = x == null;
            }
            """.trimIndent()
        )

        val x = method.extractVariables().getValue("x")
        val expression = method.getElementsOf<CtLocalVariable<*>>().single().defaultExpression

        val calculator = TestValueCalculator(
            calculators = listOf(
                CtVariableReadCalculator(),
                EqualityConditionCalculator(),
                CtLiteralCalculator()
            )
        )

        val frame = MutableDataFrame(null).apply {
            setValue(x, ReferenceValue(ValueSource.NONE))
            setConstraint(x, ReferenceConstraint.createUnknown())
        }

        val state = createState(calculator).copy(frame = frame)
        val result = runBlocking { calculator.calculateCondition(state, expression) }

        expect {
            that(result.thenFrame.getConstraint(x))
                .describedAs("x constraint in then-frame")
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.ALWAYS_NULL)

            that(result.elseFrame.getConstraint(x))
                .describedAs("x constraint in else-frame")
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.NEVER_NULL)
        }
    }

    @Test fun `Test inferred null constraint with not-null check`() {
        val method = parseMethod(
            """
            void test(Object x) {
                boolean isNull = x != null;
            }
            """.trimIndent()
        )

        val x = method.extractVariables().getValue("x")
        val expression = method.getElementsOf<CtLocalVariable<*>>().single().defaultExpression

        val calculator = TestValueCalculator(
            calculators = listOf(
                CtVariableReadCalculator(),
                EqualityConditionCalculator(),
                CtLiteralCalculator()
            )
        )

        val frame = MutableDataFrame(null).apply {
            setValue(x, ReferenceValue(ValueSource.NONE))
            setConstraint(x, ReferenceConstraint.createUnknown())
        }

        val state = createState(calculator).copy(frame = frame)
        val result = runBlocking { calculator.calculateCondition(state, expression) }

        expect {
            that(result.thenFrame.getConstraint(x))
                .describedAs("x constraint in then-frame")
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.NEVER_NULL)

            that(result.elseFrame.getConstraint(x))
                .describedAs("x constraint in else-frame")
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.ALWAYS_NULL)
        }
    }
}