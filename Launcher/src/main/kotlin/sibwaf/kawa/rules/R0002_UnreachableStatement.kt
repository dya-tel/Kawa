package sibwaf.kawa.rules

import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtComment

class R0002_UnreachableStatement : Rule() {

    override fun <R : Any?> visitCtBlock(block: CtBlock<R>) {
        val flow = getFlow(block) ?: return

        for (statement in block) {
            if (statement is CtComment) {
                continue
            }

            val frame = getFrame(flow, statement) ?: continue
            if (frame is UnreachableFrame) {
                warn("Unreachable statement", statement)
                break
            }
        }
    }
}