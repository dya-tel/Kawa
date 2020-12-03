package sibwaf.kawa.utility

import spoon.reflect.code.BinaryOperatorKind
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression

// TODO: optimize to deque
// TODO: tailrec if possible

fun flattenExpression(expression: CtExpression<*>, operatorKind: BinaryOperatorKind): List<CtExpression<*>> {
    if (expression is CtBinaryOperator<*> && expression.kind == operatorKind) {
        return flattenExpression(expression)
    }

    return listOf(expression)
}

fun flattenExpression(expression: CtBinaryOperator<*>) =
    flattenExpression(expression.leftHandOperand, expression.kind) + flattenExpression(expression.rightHandOperand, expression.kind)