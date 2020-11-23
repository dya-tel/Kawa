package sibwaf.kawa.constraints

enum class Nullability {
    UNKNOWN, POSSIBLE_NULL, ALWAYS_NULL, NEVER_NULL
}

interface ReferenceConstraint : Constraint {

    companion object {
        private val NULL_CONSTRAINT = ReferenceConstraintImpl(nullability = Nullability.ALWAYS_NULL)

        fun createNull(): ReferenceConstraint = NULL_CONSTRAINT
        fun createNonNull(): ReferenceConstraint = ReferenceConstraintImpl(nullability = Nullability.NEVER_NULL)
        fun createPossibleNull(): ReferenceConstraint = ReferenceConstraintImpl(nullability = Nullability.POSSIBLE_NULL)
        fun createUnknown(): ReferenceConstraint = ReferenceConstraintImpl(nullability = Nullability.UNKNOWN)
    }

    val nullability: Nullability

    override fun isEqual(other: Constraint): BooleanConstraint {
        return when {
            other == this -> BooleanConstraint.createTrue()
            other !is ReferenceConstraint -> BooleanConstraint.createUnknown() // TODO: createFalse()
            nullability == Nullability.ALWAYS_NULL && other.nullability == Nullability.ALWAYS_NULL -> BooleanConstraint.createTrue()
            nullability == Nullability.NEVER_NULL && other.nullability == Nullability.ALWAYS_NULL -> BooleanConstraint.createFalse()
            nullability == Nullability.ALWAYS_NULL && other.nullability == Nullability.NEVER_NULL -> BooleanConstraint.createFalse()
            else -> BooleanConstraint.createUnknown()
        }
    }
}

private class ReferenceConstraintImpl(nullability: Nullability) : ConstraintImpl(), ReferenceConstraint {

    override var nullability: Nullability = nullability
        private set

    override fun copy(): ReferenceConstraint {
        return ReferenceConstraintImpl(nullability)
    }

    override fun createInstanceForMerging(other: Constraint): Constraint {
        return if (other is ReferenceConstraint) {
            ReferenceConstraintImpl(Nullability.UNKNOWN)
        } else {
            super.createInstanceForMerging(other)
        }
    }

    override fun merge(result: Constraint, other: Constraint) {
        super.merge(result, other)
        if (result is ReferenceConstraintImpl && other is ReferenceConstraint) {
            result.nullability = when {
                nullability == Nullability.NEVER_NULL && other.nullability == Nullability.NEVER_NULL -> Nullability.NEVER_NULL
                nullability == Nullability.ALWAYS_NULL && other.nullability == Nullability.ALWAYS_NULL -> Nullability.ALWAYS_NULL
                nullability == Nullability.ALWAYS_NULL || other.nullability == Nullability.ALWAYS_NULL -> Nullability.POSSIBLE_NULL
                nullability == Nullability.POSSIBLE_NULL || other.nullability == Nullability.POSSIBLE_NULL -> Nullability.POSSIBLE_NULL
                else -> Nullability.UNKNOWN
            }
        }
    }

    override fun isEqual(other: Constraint): BooleanConstraint = super<ReferenceConstraint>.isEqual(other)

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
