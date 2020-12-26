package sibwaf.kawa.rules

import sibwaf.kawa.constraints.BooleanConstraint
import spoon.reflect.code.CtIf

class R0001_ConstantCondition : Rule() {

    override fun visitCtIf(ifElement: CtIf) {
        val condition = ifElement.condition

        val flow = getFlow(condition) ?: return

        val (_, constraint) = getValue(flow, condition) ?: return
        if (constraint !is BooleanConstraint) {
            return
        }

        if (constraint.isFalse) {
            warn("Condition is always false: '$condition'", condition)
        } else if (constraint.isTrue) {
            warn("Condition is always true: '$condition'", condition)
        }
    }
}