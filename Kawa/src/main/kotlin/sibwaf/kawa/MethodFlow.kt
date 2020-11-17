package sibwaf.kawa

import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.Value
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtStatementList
import spoon.reflect.declaration.CtElement
import java.util.IdentityHashMap

enum class MethodPurity {
    PURE, DIRTIES_THIS, DIRTIES_GLOBAL_STATE
}

open class BlockFlow {
    lateinit var startFrame: DataFrame
        internal set
    lateinit var endFrame: DataFrame
        internal set
}

open class MethodFlow : BlockFlow() {
    var purity: MethodPurity? = null

    val parameters = ArrayList<Value>()

    val frames = IdentityHashMap<CtElement, DataFrame>()
    val blocks = IdentityHashMap<CtStatementList, BlockFlow>()
    val statements = IdentityHashMap<CtStatement, Int>()

    var neverReturns = false

    var returnConstraint: Constraint? = null
}

// FIXME: holy shit it is mutable
object EmptyFlow : MethodFlow()