package sibwaf.kawa.rules

import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.BooleanConstraint
import spoon.reflect.code.CtIf

class R0001_ConstantCondition : Rule() {

    override fun visitCtIf(ifElement: CtIf) {
        val condition = ifElement.condition

        val methodFlow = getFlow(condition) ?: return
        val frame = getFrame(methodFlow, ifElement)?.takeUnless { it is UnreachableFrame } ?: return

        val (_, constraint) = getValue(frame, condition)
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