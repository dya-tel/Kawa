package sibwaf.kawa.emulation

import sibwaf.kawa.BlockFlow
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtExpression
import spoon.reflect.declaration.CtElement
import java.util.IdentityHashMap

interface MethodTrace {
    val frames: Map<CtElement, ReachableFrame>
    val blocks: Map<CtBlock<*>, BlockFlow>
    val expressions: Map<CtExpression<*>, ConstrainedValue>
}

interface MutableMethodTrace : MethodTrace {
    fun trace(element: CtElement, frame: ReachableFrame)
    fun trace(block: CtBlock<*>, start: ReachableFrame, end: DataFrame)
    fun trace(expression: CtExpression<*>, value: ConstrainedValue)
}

class MutableMethodTraceImpl : MutableMethodTrace {

    override val frames = IdentityHashMap<CtElement, ReachableFrame>()
    override val blocks = IdentityHashMap<CtBlock<*>, BlockFlow>()
    override val expressions = IdentityHashMap<CtExpression<*>, ConstrainedValue>()

    override fun trace(element: CtElement, frame: ReachableFrame) {
        frames[element] = frame
    }

    override fun trace(block: CtBlock<*>, start: ReachableFrame, end: DataFrame) {
        blocks[block] = BlockFlow().apply {
            startFrame = start
            endFrame = end
        }
    }

    override fun trace(expression: CtExpression<*>, value: ConstrainedValue) {
        expressions[expression] = value
    }
}

object BlackHoleMethodTrace : MutableMethodTrace {

    override val frames: Map<CtElement, ReachableFrame> = emptyMap()
    override val blocks: Map<CtBlock<*>, BlockFlow> = emptyMap()
    override val expressions: Map<CtExpression<*>, ConstrainedValue> = emptyMap()

    override fun trace(element: CtElement, frame: ReachableFrame) = Unit
    override fun trace(block: CtBlock<*>, start: ReachableFrame, end: DataFrame) = Unit
    override fun trace(expression: CtExpression<*>, value: ConstrainedValue) = Unit
}