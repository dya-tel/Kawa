package sibwaf.kawa.calculation.conditions

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.calculation.ValueCalculatorTestBase
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtLocalVariable
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isTrue
import kotlin.test.Test

class VariableReadConditionCalculatorTest : ValueCalculatorTestBase() {

    @Test fun `Test inferred constraint`() {
        val method = parseMethod(
            """
            void test(boolean x) {
                boolean y = x;
            }
            """.trimIndent()
        )

        val x = method.extractVariables().getValue("x")
        val expression = method.getElementsOf<CtLocalVariable<*>>().single().defaultExpression

        val calculator = VariableReadConditionCalculator()

        val frame = MutableDataFrame(null).apply {
            setValue(x, BooleanValue(ValueSource.NONE))
            setConstraint(x, BooleanConstraint.createUnknown())
        }

        val state = createState(calculator).copy(frame = frame)
        val result = runBlocking { calculator.calculateCondition(state, expression) }

        expect {
            that((result.thenFrame as ReachableFrame).getConstraint(x))
                .describedAs("x constraint in then-frame")
                .isA<BooleanConstraint>()
                .get { isTrue }
                .isTrue()

            that((result.elseFrame as ReachableFrame).getConstraint(x))
                .describedAs("x constraint in else-frame")
                .isA<BooleanConstraint>()
                .get { isFalse }
                .isTrue()
        }
    }
}