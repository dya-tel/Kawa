package sibwaf.kawa.constraints

import kotlin.math.max
import kotlin.math.min

interface CollectionConstraint : ReferenceConstraint {

    companion object {
        fun createUnknown(): CollectionConstraint = CollectionConstraintImpl(Nullability.UNKNOWN)
    }

    val minSize: Long
    val maxSize: Long

    val isEmpty: Boolean
        get() = minSize == 0L && maxSize == 0L

    val isNotEmpty: Boolean
        get() = minSize > 0 && maxSize > 0
}

// FIXME: merging with null ReferenceConstraint
private class CollectionConstraintImpl(nullability: Nullability) : ConstraintImpl(), CollectionConstraint {

    override var nullability: Nullability = nullability
        private set

    override var minSize = 0L
        private set
    override var maxSize = Long.MAX_VALUE
        private set

    override fun copy(): Constraint {
        return CollectionConstraintImpl(nullability).also {
            it.minSize = minSize
            it.maxSize = maxSize
        }
    }

    override fun createInstanceForMerging(other: Constraint): Constraint {
        return if (other is CollectionConstraint) {
            CollectionConstraintImpl(Nullability.UNKNOWN)
        } else {
            super.createInstanceForMerging(other)
        }
    }

    override fun merge(result: Constraint, other: Constraint) {
        super.merge(result, other)
        if (result is CollectionConstraintImpl && other is CollectionConstraint) {
            result.minSize = min(minSize, other.minSize)
            result.maxSize = max(maxSize, other.maxSize)
        }
    }

    override fun isEqual(other: Constraint): BooleanConstraint = super<CollectionConstraint>.isEqual(other)
}
