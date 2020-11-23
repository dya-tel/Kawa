package sibwaf.kawa.constraints

import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.CollectionValue
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.Value

interface InvertibleConstraint<T : Constraint> {
    fun invert(): T
}

interface Constraint {
    companion object {
        fun from(value: Value): Constraint {
            return when (value) {
                is CollectionValue -> CollectionConstraint.createUnknown()
                is ReferenceValue -> ReferenceConstraint.createUnknown()
                is BooleanValue -> BooleanConstraint.createUnknown()
                else -> ConstraintImpl()
            }
        }

        fun createUnknown(): Constraint = ConstraintImpl()
    }

    fun copy(): Constraint
    fun merge(other: Constraint): Constraint

    fun isEqual(other: Constraint): BooleanConstraint
    fun isNotEqual(other: Constraint): BooleanConstraint // FIXME: could be very-very broken, but isn't used
}


internal open class ConstraintImpl : Constraint {

    override fun copy(): Constraint {
        return ConstraintImpl()
    }

    final override fun merge(other: Constraint): Constraint {
        val result = createInstanceForMerging(other)
        merge(result, other)
        return result
    }

    protected open fun createInstanceForMerging(other: Constraint): Constraint {
        return ConstraintImpl()
    }

    protected open fun merge(result: Constraint, other: Constraint) {
    }

    override fun isEqual(other: Constraint): BooleanConstraint {
        return if (this == other) {
            BooleanConstraint.createTrue()
        } else {
            BooleanConstraint.createUnknown()
        }
    }

    override fun isNotEqual(other: Constraint): BooleanConstraint = isEqual(other).invert()
}
