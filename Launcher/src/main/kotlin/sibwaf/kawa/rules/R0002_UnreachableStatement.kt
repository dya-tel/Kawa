package sibwaf.kawa.rules

import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtComment

class R0002_UnreachableStatement : Rule() {

    override fun <R : Any?> visitCtBlock(block: CtBlock<R>) {
        val flow = getFlow(block) ?: return

        for (statement in block) {
            if (statement is CtComment) {
                continue
            }

            if (getFrame(flow, statement) == null) {
                warn("Unreachable statement", statement)
                break
            }
        }
    }
}