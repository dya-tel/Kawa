package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtUnaryOperator
import spoon.reflect.code.CtVariableAccess
import spoon.reflect.code.UnaryOperatorKind
import spoon.reflect.reference.CtFieldReference

class CtUnaryOperatorIncDecCalculator : ValueCalculator {

    private val supportedKinds = listOf(
        UnaryOperatorKind.POSTINC, UnaryOperatorKind.POSTDEC,
        UnaryOperatorKind.PREINC, UnaryOperatorKind.PREDEC
    )

    override fun supports(expression: CtExpression<*>) = expression is CtUnaryOperator<*> && expression.kind in supportedKinds

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtUnaryOperator<*>

        val (frame, operandValue) = state.getValue(expression.operand)
        // TODO: exit on UnreachableFrame with invalid value

        val operatorValue = ConstrainedValue.from(expression) // TODO

        val variable = (expression.operand as? CtVariableAccess<*>)
            ?.variable
            ?.takeUnless { it is CtFieldReference<*> }
            ?.declaration

        val resultFrame = if (variable != null && frame is ReachableFrame) {
            MutableDataFrame(frame).apply {
                setValue(variable, operatorValue.value)
                setConstraint(operatorValue.value, operatorValue.constraint)
            }
        } else {
            frame
        }

        val result = when (expression.kind) {
            UnaryOperatorKind.POSTINC, UnaryOperatorKind.POSTDEC -> operandValue
            UnaryOperatorKind.PREINC, UnaryOperatorKind.PREDEC -> operatorValue
            else -> throw IllegalArgumentException("This calculator doesn't support ${expression.kind} operators")
        }

        return resultFrame to result
    }
}