package sibwaf.kawa.rules

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.EmptyFlow
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.ValueCalculator
import sibwaf.kawa.constraints.BooleanConstraint
import spoon.reflect.code.CtIf

class R0001_ConstantCondition : Rule() {

    override fun visitCtIf(ifElement: CtIf) {
        val condition = ifElement.condition

        val methodFlow = getFlow(condition) ?: return
//        val statement = getStatement(condition) ?: return
        val frame = getFrame(methodFlow, ifElement) ?: return

        val result = runBlocking {
            ValueCalculator.calculateValue(MethodFlow(), frame, condition, { flow[it] ?: EmptyFlow })
        }.constraint as? BooleanConstraint ?: return

        if (result.isFalse) {
            warn("Condition is always false: '$condition'", condition)
        } else if (result.isTrue) {
            warn("Condition is always true: '$condition'", condition)
        }
    }
}