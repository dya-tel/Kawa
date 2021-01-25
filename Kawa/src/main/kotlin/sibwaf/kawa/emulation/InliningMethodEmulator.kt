package sibwaf.kawa.emulation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.IdentityHashSet
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtReturn
import spoon.reflect.declaration.CtVariable
import spoon.reflect.reference.CtExecutableReference
import java.util.LinkedList

class InliningMethodEmulator : MethodEmulator {

    override suspend fun emulate(
        state: AnalyzerState,
        method: CtExecutableReference<*>,
        arguments: List<ConstrainedValue>
    ): InvocationResult {
        if (method.declaringType.isShadow) {
            return FailedInvocation
        }

        val declaration = state.cache.getDeclaration(method) ?: return FailedInvocation
        val body = declaration.body ?: return FailedInvocation

        val parameters = declaration.parameters
        if (parameters.size > 0) {
            val requiredCount = if (parameters.last().isVarArgs) {
                parameters.size - 1
            } else {
                parameters.size
            }

            if (arguments.size < requiredCount) {
                throw IllegalStateException("Not enough arguments for '$declaration': expected ${requiredCount}, got ${arguments.size}")
            }
        }

        // TODO: (optimization) compact the start frame as much as possible
        val frame = MutableDataFrame(state.frame)
        for ((parameter, argument) in parameters.zip(arguments)) {
            if (parameter.isVarArgs) {
                // TODO: collect all arguments
                frame.setValue(parameter, ReferenceValue(ValueSource.NONE))
                frame.setConstraint(parameter, ReferenceConstraint.createNonNull()) // TODO: might be a wrong assumption
            } else {
                frame.setValue(parameter, argument.value)
                frame.setConstraint(argument.value, argument.constraint)
            }
        }

        // empty vararg
        if (parameters.size > 0 && arguments.size < parameters.size) {
            val vararg = parameters.last()
            frame.setValue(vararg, ReferenceValue(ValueSource.NONE))
            frame.setConstraint(vararg, ReferenceConstraint.createNonNull())
        }

        val localState = state.copy(
            frame = frame,
            trace = MutableMethodTraceImpl(),
            localVariables = IdentityHashSet<CtVariable<*>>().apply { addAll(parameters) },
            jumpPoints = ArrayList()
        )

        val bodyFrame = localState.getStatementFlow(body)

        // TODO: transfer throws to parent jumpPoints?

        val returnFrames = localState.jumpPoints.filter { it.first is CtReturn<*> }

        var resultFrame = DataFrame.merge(
            state.frame,
            (returnFrames.map { it.second } + bodyFrame).map { it.compact(state.frame) } // TODO: sequences
        )

        resultFrame = if (resultFrame is UnreachableFrame) {
            val cleanedFrame = resultFrame.previous.copy(retiredVariables = localState.localVariables)
            UnreachableFrame.after(cleanedFrame)
        } else {
            (resultFrame as ReachableFrame).copy(retiredVariables = localState.localVariables)
        }

        val resultValue = if (declaration.type.qualifiedName == "void") {
            null
        } else {
            val values = LinkedList<Value>()
            val constraints = LinkedList<Constraint>()
            for ((statement, _) in returnFrames) {
                statement as CtReturn<*>

                if (statement.returnedExpression == null) {
                    continue
                }

                val value = localState.trace.expressions.getValue(statement.returnedExpression)
                values += value.value
                constraints += value.constraint
            }

            val value = if (values.size == 1) {
                values.single()
            } else {
                // TODO: composite value
                Value.from(method.type, ValueSource.NONE)
            }

            val constraint = if (constraints.isEmpty()) {
                Constraint.from(value)
            } else {
                constraints.reduce(Constraint::merge)
            }

            ConstrainedValue(value, constraint)
        }

        return SuccessfulInvocation(resultFrame, resultValue)
    }
}