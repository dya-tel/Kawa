package sibwaf.kawa.constraints

enum class Nullability {
    UNKNOWN, POSSIBLE_NULL, ALWAYS_NULL, NEVER_NULL
}

open class ReferenceConstraint : Constraint() {

    open var nullability = Nullability.UNKNOWN
        internal set

    override fun copy(): Constraint {
        return ReferenceConstraint().also {
            it.nullability = nullability
        }
    }

    override fun createInstanceForMerging(other: Constraint): Constraint {
        return if (other is ReferenceConstraint) {
            ReferenceConstraint()
        } else {
            super.createInstanceForMerging(other)
        }
    }

    override fun merge(result: Constraint, other: Constraint) {
        super.merge(result, other)
        if (result is ReferenceConstraint && other is ReferenceConstraint) {
            result.nullability = when {
                nullability == Nullability.NEVER_NULL && other.nullability == Nullability.NEVER_NULL -> Nullability.NEVER_NULL
                nullability == Nullability.ALWAYS_NULL && other.nullability == Nullability.ALWAYS_NULL -> Nullability.ALWAYS_NULL
                nullability == Nullability.ALWAYS_NULL || other.nullability == Nullability.ALWAYS_NULL -> Nullability.POSSIBLE_NULL
                nullability == Nullability.POSSIBLE_NULL || other.nullability == Nullability.POSSIBLE_NULL -> Nullability.POSSIBLE_NULL
                else -> Nullability.UNKNOWN
            }
        }
    }

    open fun isEqual(other: ReferenceConstraint): BooleanConstraint {
        return when {
            other == this -> TRUE_CONSTRAINT
            nullability == Nullability.ALWAYS_NULL && other.nullability == Nullability.ALWAYS_NULL -> TRUE_CONSTRAINT
            nullability == Nullability.NEVER_NULL && other.nullability == Nullability.ALWAYS_NULL -> FALSE_CONSTRAINT
            nullability == Nullability.ALWAYS_NULL && other.nullability == Nullability.NEVER_NULL -> FALSE_CONSTRAINT
            else -> BooleanConstraint()
        }
    }

    open fun isNotEqual(other: ReferenceConstraint): BooleanConstraint {
        return isEqual(other).invert()
    }

    override fun toString(): String {
        return "reference: $nullability"
    }
}

/*
class TypeCheckedReferenceConstraint(
        private val original: ReferenceConstraint,
        val type: CtTypeReference<*>
) : ReferenceConstraint() {
fun invert():

}*/
