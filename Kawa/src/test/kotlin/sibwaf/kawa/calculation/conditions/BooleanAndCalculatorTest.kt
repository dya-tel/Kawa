package sibwaf.kawa.calculation.conditions

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
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
import strikt.assertions.isOneOf
import kotlin.test.Test

class BooleanAndCalculatorTest : ValueCalculatorTestBase() {

    @Test fun `Test inferred constraints with multiple null checks`() {
        val method = parseMethod(
            """
            void test(Object x, Object y) {
                boolean a = x == null && y == null;
            }
            """.trimIndent()
        )

        val variables = method.extractVariables()
        val x = variables.getValue("x")
        val y = variables.getValue("y")
        val expression = method.getElementsOf<CtLocalVariable<*>>().single().defaultExpression

        val calculator = TestValueCalculator(
            calculators = listOf(
                BooleanAndCalculator(),
                CtVariableReadCalculator(),
                EqualityConditionCalculator(),
                CtLiteralCalculator()
            )
        )

        val frame = MutableDataFrame(null).apply {
            setValue(x, ReferenceValue(ValueSource.NONE))
            setConstraint(x, ReferenceConstraint.createUnknown())

            setValue(y, ReferenceValue(ValueSource.NONE))
            setConstraint(y, ReferenceConstraint.createUnknown())
        }

        val state = createState(calculator).copy(frame = frame)
        val result = runBlocking { calculator.calculateCondition(state, expression) }

        expect {
            that(result.thenFrame as ReachableFrame).describedAs("then frame").and {
                get { getConstraint(x) }
                    .describedAs("inferred x constraint")
                    .isA<ReferenceConstraint>()
                    .get { nullability }
                    .isEqualTo(Nullability.ALWAYS_NULL)

                get { getConstraint(y) }
                    .describedAs("inferred y constraint")
                    .isA<ReferenceConstraint>()
                    .get { nullability }
                    .isEqualTo(Nullability.ALWAYS_NULL)
            }

            // TODO: proper nullability

            that(result.elseFrame as ReachableFrame).describedAs("else frame").and {
                get { getConstraint(x) }
                    .describedAs("old x constraint")
                    .isA<ReferenceConstraint>()
                    .get { nullability }
//                        .isEqualTo((frame.getConstraint(x) as ReferenceConstraint).nullability)
                    .isOneOf(Nullability.UNKNOWN, Nullability.POSSIBLE_NULL)

                get { getConstraint(y) }
                    .describedAs("old y constraint")
                    .isA<ReferenceConstraint>()
                    .get { nullability }
//                        .isEqualTo((frame.getConstraint(y) as ReferenceConstraint).nullability)
                    .isOneOf(Nullability.UNKNOWN, Nullability.POSSIBLE_NULL)
            }
        }
    }

}