package sibwaf.kawa.rules

import sibwaf.kawa.constraints.BooleanConstraint
import spoon.reflect.code.CtConditional
import spoon.reflect.code.CtDo
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtFor
import spoon.reflect.code.CtIf
import spoon.reflect.code.CtLiteral
import spoon.reflect.code.CtWhile

class R0001_ConstantCondition : Rule() {

    private fun checkExpression(expression: CtExpression<*>) {
        if (expression is CtLiteral<*>) {
            return
        }

        val flow = getFlow(expression) ?: return

        val (_, constraint) = getValue(flow, expression) ?: return
        if (constraint !is BooleanConstraint) {
            return
        }

        if (constraint.isFalse) {
            warn("Condition is always false: '$expression'", expression)
        } else if (constraint.isTrue) {
            warn("Condition is always true: '$expression'", expression)
        }
    }

    override fun visitCtIf(ifElement: CtIf) {
        checkExpression(ifElement.condition)
    }

    override fun <T : Any?> visitCtConditional(conditional: CtConditional<T>) {
        checkExpression(conditional.condition)
    }

    override fun visitCtWhile(whileLoop: CtWhile) {
        checkExpression(whileLoop.loopingExpression)
    }

    override fun visitCtDo(doLoop: CtDo) {
        checkExpression(doLoop.loopingExpression)
    }

    override fun visitCtFor(forLoop: CtFor) {
        forLoop.expression?.let { checkExpression(it) }
    }
}