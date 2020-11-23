package sibwaf.kawa.constraints

interface BooleanConstraint : Constraint, InvertibleConstraint<BooleanConstraint> {

    companion object {
        private val FALSE_CONSTRAINT = BooleanConstraintImpl(isFalse = true)
        private val TRUE_CONSTRAINT = BooleanConstraintImpl(isTrue = true)

        fun createTrue(): BooleanConstraint = TRUE_CONSTRAINT
        fun createFalse(): BooleanConstraint = FALSE_CONSTRAINT
        fun createUnknown(): BooleanConstraint = BooleanConstraintImpl()
        fun create(isTrue: Boolean = false, isFalse: Boolean = false): BooleanConstraint = BooleanConstraintImpl(isTrue, isFalse)
    }

    val isTrue: Boolean
    val isFalse: Boolean

    fun and(other: BooleanConstraint): BooleanConstraint {
        if (this !is InvertedBooleanConstraintImpl && other is InvertedBooleanConstraintImpl) {
            return other.and(this)
        }

        return when {
            other == this -> this
            isFalse || other.isFalse -> createFalse()
            isTrue && other.isTrue -> createTrue()
            isTrue -> other
            other.isTrue -> this
            else -> createUnknown()
        }
    }

    fun or(other: BooleanConstraint): BooleanConstraint {
        if (this !is InvertedBooleanConstraintImpl && other is InvertedBooleanConstraintImpl) {
            return other.or(this)
        }

        return when {
            other == this -> this
            isTrue || other.isTrue -> createTrue()
            isFalse && other.isFalse -> createFalse()
            isFalse -> other
            other.isFalse -> this
            else -> createUnknown()
        }
    }

    override fun isEqual(other: Constraint): BooleanConstraint {
        if (this !is InvertedBooleanConstraintImpl && other is InvertedBooleanConstraintImpl) {
            return other.isEqual(this)
        }

        return when {
            other == this -> this
            other !is BooleanConstraint -> createUnknown() // TODO: createFalse()
            isTrue && other.isTrue -> createTrue()
            isFalse && other.isFalse -> createTrue()
            else -> createUnknown()
        }
    }
}

private class BooleanConstraintImpl(isTrue: Boolean = false, isFalse: Boolean = false) : ConstraintImpl(), BooleanConstraint {

    override var isTrue: Boolean = isTrue
        private set

    override var isFalse: Boolean = isFalse
        private set

    init {
        check(!isTrue || !isFalse) { "Boolean constraint can't be 'true' and 'false' at the same time" }
    }

    override fun copy(): BooleanConstraint = BooleanConstraintImpl(isTrue, isFalse)

    override fun createInstanceForMerging(other: Constraint): Constraint {
        return if (other is BooleanConstraint) {
            BooleanConstraintImpl()
        } else {
            super.createInstanceForMerging(other)
        }
    }

    override fun merge(result: Constraint, other: Constraint) {
        super.merge(result, other)
        if (result is BooleanConstraintImpl && other is BooleanConstraint) {
            result.isFalse = isFalse && other.isFalse
            result.isTrue = isTrue && other.isTrue
        }
    }

    override fun invert(): BooleanConstraint = InvertedBooleanConstraintImpl(this)

    override fun isEqual(other: Constraint): BooleanConstraint = super<BooleanConstraint>.isEqual(other)

    override fun toString(): String {
        val text = when {
            isTrue -> "true"
            isFalse -> "false"
            else -> "unknown"
        }
        return "boolean: $text"
    }
}

// FIXME: merging
private class InvertedBooleanConstraintImpl(private val original: BooleanConstraint) : ConstraintImpl(), BooleanConstraint {

    override val isTrue: Boolean
        get() = original.isFalse

    override val isFalse: Boolean
        get() = original.isTrue

    override fun copy(): BooleanConstraint {
        return InvertedBooleanConstraintImpl(original.copy() as BooleanConstraint)
    }

    override fun invert(): BooleanConstraint = original

    override fun and(other: BooleanConstraint): BooleanConstraint {
        return if (other == original) {
            BooleanConstraint.createFalse()
        } else {
            super.and(other)
        }
    }

    override fun or(other: BooleanConstraint): BooleanConstraint {
        return if (other == original) {
            BooleanConstraint.createTrue()
        } else {
            super.or(other)
        }
    }

    override fun isEqual(other: Constraint): BooleanConstraint {
        return if (other == original) {
            BooleanConstraint.createFalse()
        } else {
            super<BooleanConstraint>.isEqual(other)
        }
    }
}