package sibwaf.kawa

import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.emulation.BlackHoleMethodTrace
import sibwaf.kawa.emulation.MethodTrace
import sibwaf.kawa.values.Value
import spoon.reflect.code.CtStatement
import java.util.IdentityHashMap

enum class MethodPurity {
    PURE, DIRTIES_THIS, DIRTIES_GLOBAL_STATE
}

open class BlockFlow {
    lateinit var startFrame: ReachableFrame
        internal set
    lateinit var endFrame: DataFrame // TODO: 'ReachableFrame?' should be better
        internal set
}

open class MethodFlow(trace: MethodTrace) : BlockFlow(), MethodTrace by trace {
    var purity: MethodPurity? = null

    val parameters = ArrayList<Value>()

    val statements = IdentityHashMap<CtStatement, Int>()

    var neverReturns = false

    var returnConstraint: Constraint? = null
}

// FIXME: holy shit it is mutable
object EmptyFlow : MethodFlow(BlackHoleMethodTrace)