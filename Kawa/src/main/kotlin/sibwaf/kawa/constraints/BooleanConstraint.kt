package sibwaf.kawa.constraints

val FALSE_CONSTRAINT = BooleanConstraint().apply { isFalse = true }
val TRUE_CONSTRAINT = BooleanConstraint().apply { isTrue = true }

//val FALSE_CONSTRAINED_VALUE = ConstrainedValue(FalseValue, FALSE_CONSTRAINT)
//val TRUE_CONSTRAINED_VALUE = ConstrainedValue(TrueValue, TRUE_CONSTRAINT)

open class BooleanConstraint : Constraint(), InvertibleConstraint<BooleanConstraint> {
    open var isTrue: Boolean = false
        internal set

    open var isFalse: Boolean = false
        internal set

    override fun copy(): Constraint {
        return BooleanConstraint().also {
            it.isTrue = isTrue
            it.isFalse = isFalse
        }
    }

    override fun createInstanceForMerging(other: Constraint): Constraint {
        return if (other is BooleanConstraint) {
            BooleanConstraint()
        } else {
            super.createInstanceForMerging(other)
        }
    }

    override fun merge(result: Constraint, other: Constraint) {
        super.merge(result, other)
        if (result is BooleanConstraint && other is BooleanConstraint) {
            result.isFalse = isFalse && other.isFalse
            result.isTrue = isTrue && other.isTrue
        }
    }

    override fun invert(): BooleanConstraint = InvertedBooleanConstraint(this)

    open fun and(other: BooleanConstraint): BooleanConstraint {
        if (this !is InvertedBooleanConstraint && other is InvertedBooleanConstraint) {
            return other.and(this)
        }

        return when {
            other == this -> this
            isFalse || other.isFalse -> FALSE_CONSTRAINT
            isTrue && other.isTrue -> TRUE_CONSTRAINT
            isTrue -> other
            other.isTrue -> this
            else -> BooleanConstraint()
        }
    }

    open fun or(other: BooleanConstraint): BooleanConstraint {
        if (this !is InvertedBooleanConstraint && other is InvertedBooleanConstraint) {
            return other.or(this)
        }

        return when {
            other == this -> this
            isTrue || other.isTrue -> TRUE_CONSTRAINT
            isFalse && other.isFalse -> FALSE_CONSTRAINT
            isFalse -> other
            other.isFalse -> this
            else -> BooleanConstraint()
        }
    }

    open fun isEqual(other: BooleanConstraint): BooleanConstraint {
        if (this !is InvertedBooleanConstraint && other is InvertedBooleanConstraint) {
            return other.isEqual(this)
        }

        return when {
            other == this -> this
            isTrue && other.isTrue -> TRUE_CONSTRAINT
            isFalse && other.isFalse -> TRUE_CONSTRAINT
            else -> BooleanConstraint()
        }
    }

    open fun isNotEqual(other: BooleanConstraint): BooleanConstraint {
        return isEqual(other).invert()
    }

    override fun toString(): String {
        val text = when {
            isTrue -> "true"
            isFalse -> "false"
            else -> "unknown"
        }
        return "boolean: $text"
    }
}

class InvertedBooleanConstraint(private val original: BooleanConstraint) : BooleanConstraint() {

    override var isTrue: Boolean
        get() = original.isFalse
        set(_) = throw IllegalStateException("Can't change value")

    override var isFalse: Boolean
        get() = original.isTrue
        set(_) = throw IllegalStateException("Can't change value")

    override fun copy(): Constraint {
        return InvertedBooleanConstraint(original.copy() as BooleanConstraint)
    }

    override fun invert(): BooleanConstraint = original

    override fun and(other: BooleanConstraint): BooleanConstraint {
        return if (other == original) {
            FALSE_CONSTRAINT
        } else {
            super.and(other)
        }
    }

    override fun or(other: BooleanConstraint): BooleanConstraint {
        return if (other == original) {
            TRUE_CONSTRAINT
        } else {
            super.or(other)
        }
    }

    override fun isEqual(other: BooleanConstraint): BooleanConstraint {
        return if (other == original) {
            FALSE_CONSTRAINT
        } else {
            super.isEqual(other)
        }
    }
}