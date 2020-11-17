package sibwaf.kawa.rules

import spoon.reflect.code.CtBlock

class R0002_UnreachableStatement : Rule() {

    override fun <R : Any?> visitCtBlock(block: CtBlock<R>) {
        val flow = getFlow(block) ?: return

        for (statement in block) {
            val frame = flow.frames[statement] ?: continue
            if (!frame.isReachable) {
                warn("Unreachable statement", statement)
                break
            }
        }
    }
}