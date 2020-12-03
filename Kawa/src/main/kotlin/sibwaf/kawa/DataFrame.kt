package sibwaf.kawa

import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.declaration.CtVariable
import java.util.Collections

sealed class DataFrame(previous: DataFrame?) {

    companion object {
        internal fun merge(previous: DataFrame, vararg frames: DataFrame): DataFrame {
            return merge(previous, frames.asIterable())
        }

        internal fun merge(previous: DataFrame, frames: Iterable<DataFrame>): DataFrame {
            val reachableFrames = frames.asSequence()
                .filter { it !is UnreachableFrame }
                .onEach { require(it.previous == previous) { "Frames must be compacted before merging" } }
                .toList()

            if (reachableFrames.isEmpty()) {
                return UnreachableFrame.after(previous)
            }

            // TODO: (optimization) if there is a single reachable frame it should be returned as it is

            val result = MutableDataFrame(previous)
            // TODO: next?

            val affectedValues = reachableFrames.asSequence()
                .flatMap { it.constraintDiff.keys }
                .distinct()

            // TODO: volatile constraints

            for (value in affectedValues) {
                val mergedConstraint = reachableFrames.mapNotNull { it.getConstraint(value) }.reduce(Constraint::merge)
                result.setConstraint(value, mergedConstraint)
            }

            val affectedVariables = reachableFrames.asSequence()
                .flatMap { it.valueDiff.keys }
                .distinct()

            for (variable in affectedVariables) {
                val values = reachableFrames.mapNotNull { frame -> frame.getValue(variable) }

                // There will be at least one value because the variable exists in at least one frame
                if (values.size == 1) {
                    result.setValue(variable, values.single())
                    // Constraint is already present in the resulting frame, no need to copy it
                    continue
                }

                // TODO: compound value
                val mergedValue = Value.from(variable.type, ValueSource.NONE)
                result.setValue(variable, mergedValue)

                val mergedConstraint = reachableFrames.mapNotNull { it.getConstraint(variable) }.reduce(Constraint::merge)
                result.setConstraint(mergedValue, mergedConstraint)
            }

            result.volatileConstraintDiff = Collections.emptyMap()
            return result
        }
    }

    open val previous: DataFrame? = previous

    open var next: DataFrame? = null
        internal set

    protected var valueDiff: MutableMap<CtVariable<*>, Value> = Collections.emptyMap()
    protected var constraintDiff: MutableMap<Value, Constraint> = Collections.emptyMap()
    protected var volatileConstraintDiff: MutableMap<Value, Constraint> = Collections.emptyMap()

    init {
        if (previous is UnreachableFrame) {
            throw IllegalArgumentException("No frames can be linked to an unreachable one")
        }
    }

    fun getValue(variable: CtVariable<*>): Value? {
        return valueDiff.getOrElse(variable) { previous?.getValue(variable) }
    }

    open fun getValue(condition: Condition, variable: CtVariable<*>): Value? {
        return getValue(variable)
    }

    // TODO: private fun getConstraintWithType(): Pair<ConstraintType, Constraint>?

    fun getConstraint(value: Value): Constraint? {
        return volatileConstraintDiff[value]
            ?: constraintDiff[value]
            ?: previous?.getConstraint(value)
    }

    fun getConstraint(variable: CtVariable<*>): Constraint? {
        val value = getValue(variable) ?: return null
        return getConstraint(value)
    }

    open fun copy(retiredVariables: Collection<CtVariable<*>> = emptySet(), keepVolatileConstraints: Boolean = true): DataFrame {
        val frame = MutableDataFrame(previous)
        frame.next = next

        for (diff in valueDiff) {
            if (diff.key !in retiredVariables) {
                frame.setValue(diff.key, diff.value)
            }
        }

        for (diff in constraintDiff) {
            // TODO: (optimization) do not copy constraints if corresponding values can't be obtained
            frame.setConstraint(diff.key, diff.value)
        }

        if (keepVolatileConstraints) {
            for (diff in volatileConstraintDiff) {
                frame.setVolatileConstraint(diff.key, diff.value)
            }
        }

        return frame
    }

    /**
     * Creates a new frame which has [bound] frame as it's previous and [next] as it's next.
     * Resulting frame reachability is determined by this frame.
     *
     * TODO: write about values and frames
     *
     * @param bound boundary for compacting (exclusive)
     * @return the compacted frame
     */
    open fun compact(bound: DataFrame): DataFrame {
        if (previous == bound) {
            return this
        }

        if (bound == this) {
            throw IllegalArgumentException("Frame can't be compacted with itself as a bound")
        }

        var chain = RightChain(null, this)
        var currentFrame = this
        while (currentFrame.previous != bound) {
            currentFrame = currentFrame.previous
                ?: throw IllegalStateException("Specified frame is not linked to this one")

            chain = RightChain(chain, currentFrame)
        }

        val result = MutableDataFrame(bound)
        result.next = next

        while (true) {
            for ((variable, value) in chain.current.valueDiff) {
                result.setValue(variable, value)
            }
            for ((value, constraint) in chain.current.constraintDiff) {
                result.setConstraint(value, constraint)
            }
            for ((value, constraint) in chain.current.volatileConstraintDiff) {
                result.setVolatileConstraint(value, constraint)
            }

            chain = chain.previous ?: break
        }

        return result
    }

    /**
     * Returns a full copy of this frame with all **changed** values and constraints
     * set to new empty non-typed instances.
     *
     * Volatile constraints will not be copied to the resulting frame.
     *
     * @return copy of this frame with erased values and constraints
     */
    open fun eraseValues(): DataFrame {
        val result = MutableDataFrame(previous)
        result.next = next

        for ((variable, value) in valueDiff) {
            val erasedValue = value.copy()
            result.setValue(variable, erasedValue)//Value(value.source))
            result.setConstraint(variable, Constraint.from(erasedValue))
        }

        for ((value, _) in constraintDiff) {
            result.setConstraint(value, Constraint.from(value))
        }

        return result
    }
}

internal class MutableDataFrame(previous: DataFrame?) : DataFrame(previous) {

    fun setValue(variable: CtVariable<*>, value: Value) {
        if (value === getValue(variable)) {
            return
        }

        if (valueDiff.isEmpty()) {
            valueDiff = HashMap()
        }

        valueDiff[variable] = value
    }

    fun setConstraint(value: Value, constraint: Constraint) {
//        if (constraint == getConstraint(value))

        if (constraintDiff.isEmpty()) {
            constraintDiff = HashMap()
        }

        volatileConstraintDiff.remove(value) // FIXME: 'tis is a fucking time bomb, come on
        constraintDiff[value] = constraint
    }

    fun setConstraint(variable: CtVariable<*>, constraint: Constraint) {
        val value = getValue(variable) ?: return // TODO
        setConstraint(value, constraint)
    }

    fun setVolatileConstraint(value: Value, constraint: Constraint) {
        setConstraint(value, constraint)
//        if (volatileConstraintDiff.isEmpty()) {
//            volatileConstraintDiff = HashMap()
//        }
//
//        volatileConstraintDiff[value] = constraint
    }

    fun setVolatileConstraint(variable: CtVariable<*>, constraint: Constraint) {
        val value = getValue(variable) ?: return // TODO
        setVolatileConstraint(value, constraint)
    }
}

class UnreachableFrame private constructor(previous: DataFrame) : DataFrame(previous) {

    companion object {
        fun after(previous: DataFrame): UnreachableFrame {
            return if (previous is UnreachableFrame) {
                previous
            } else {
                UnreachableFrame(previous)
            }
        }
    }

    override val previous: DataFrame
        get() = super.previous!!

    override var next: DataFrame?
        get() = super.next
        set(_) = throw UnsupportedOperationException("Unreachable frame can't be modified")

    override fun copy(retiredVariables: Collection<CtVariable<*>>, keepVolatileConstraints: Boolean): DataFrame =
        throw UnsupportedOperationException("Unreachable frame can't be copied")

    override fun compact(bound: DataFrame): DataFrame {
        if (previous == bound) {
            return this
        }

        if (bound == this) {
            throw IllegalArgumentException("Frame can't be compacted with itself as a bound")
        }

        val compactedTail = previous.compact(bound)
        return UnreachableFrame(compactedTail)
    }

    override fun eraseValues(): DataFrame =
        throw UnsupportedOperationException("Unreachable frame can't be copied with erased values")
}