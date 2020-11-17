package sibwaf.kawa.constraints

import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.CollectionValue
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.Value
import kotlin.math.max
import kotlin.math.min

interface InvertibleConstraint<T : Constraint> {
    fun invert(): T
}

open class Constraint {

    companion object {
        fun from(value: Value): Constraint {
            return when (value) {
                is CollectionValue -> CollectionConstraint()
                is ReferenceValue -> ReferenceConstraint()
                is BooleanValue -> BooleanConstraint()
                else -> Constraint()
            }
        }
    }

    open fun copy(): Constraint {
        return Constraint()
    }

    fun merge(other: Constraint): Constraint {
        val result = createInstanceForMerging(other)
        merge(result, other)
        return result
    }

    protected open fun createInstanceForMerging(other: Constraint): Constraint {
        return Constraint()
    }

    protected open fun merge(result: Constraint, other: Constraint) {
    }
}

class CollectionConstraint : ReferenceConstraint() {
    var minSize = 0L
        internal set
    var maxSize = Long.MAX_VALUE
        internal set

    val isEmpty
        get() = minSize == 0L && maxSize == 0L

    val isNotEmpty
        get() = minSize > 0 && maxSize > 0

    override fun copy(): Constraint {
        return CollectionConstraint().also {
            it.minSize = minSize
            it.maxSize = maxSize
        }
    }

    override fun createInstanceForMerging(other: Constraint): Constraint {
        return if (other is CollectionConstraint) {
            CollectionConstraint()
        } else {
            super.createInstanceForMerging(other)
        }
    }

    override fun merge(result: Constraint, other: Constraint) {
        super.merge(result, other)
        if (result is CollectionConstraint && other is CollectionConstraint) {
            result.minSize = min(minSize, other.minSize)
            result.maxSize = max(maxSize, other.maxSize)
        }
    }
}