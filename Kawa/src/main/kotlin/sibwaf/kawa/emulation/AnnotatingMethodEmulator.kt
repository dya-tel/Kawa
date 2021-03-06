package sibwaf.kawa.emulation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.EmptyFlow
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.MethodPurity
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import spoon.reflect.code.CtReturn
import spoon.reflect.code.CtThrow
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.reference.CtExecutableReference
import java.util.Collections
import java.util.LinkedList

class AnnotatingMethodEmulator(private val cache: MutableMap<CtExecutable<*>, MethodFlow>) : MethodEmulator {

    private suspend fun createAnnotation(state: AnalyzerState, method: CtExecutableReference<*>): MethodFlow {
        val declaration = state.cache.getDeclaration(method) ?: return EmptyFlow
        val body = declaration.body ?: return EmptyFlow

        val trace = MutableMethodTraceImpl()
        val annotation = MethodFlow(trace)

        val startFrame = MutableDataFrame(null)
        for (parameter in declaration.parameters) {
            val value = Value.from(parameter)
            startFrame.setValue(parameter, value)

            val constraint = Constraint.from(value)
            startFrame.setConstraint(value, constraint)
        }

        val localState = state.copy(
            annotation = annotation,
            trace = trace,
            frame = startFrame,
            localVariables = Collections.emptySet(),
            jumpPoints = ArrayList()
        )

        localState.getStatementFlow(body)

        val blockFlow = annotation.blocks.getValue(body)
        annotation.startFrame = blockFlow.startFrame
        annotation.endFrame = blockFlow.endFrame

        annotation.neverReturns = annotation.endFrame is UnreachableFrame && localState.jumpPoints.all { it.first is CtThrow }

        if (annotation.purity == null) {
            annotation.purity = MethodPurity.PURE
        }

        if (declaration.type.qualifiedName != "void") {
            val returnConstraint = if (annotation.neverReturns) {
                null
            } else {
                val returnedConstraints = LinkedList<Constraint>()
                for (jump in localState.jumpPoints) {
                    val statement = jump.first
                    if (statement !is CtReturn<*> || statement.returnedExpression == null) {
                        continue
                    }

                    returnedConstraints += annotation.expressions.getValue(statement.returnedExpression).constraint
                }

                returnedConstraints.reduceOrNull(Constraint::merge)
            }

            annotation.returnConstraint = returnConstraint ?: Constraint.from(Value.withoutSource(declaration))
        }

        return annotation
    }

    override suspend fun emulate(
        state: AnalyzerState,
        method: CtExecutableReference<*>,
        arguments: List<ConstrainedValue>
    ): InvocationResult {
        val declaration = state.cache.getDeclaration(method) ?: return FailedInvocation

        val annotation = cache.getOrElse(declaration) {
            createAnnotation(state, method).also {
                cache.putIfAbsent(declaration, it)
            }
        }

        if (annotation.neverReturns) {
            // TODO: invalid value
            return SuccessfulInvocation(
                frame = UnreachableFrame.after(state.frame),
                value = ConstrainedValue.withoutSource(method.type)
            )
        }

        val returnConstraint = annotation.returnConstraint
        val result = if (returnConstraint != null) {
            val value = Value.withoutSource(method.type)
            val constraint = returnConstraint.copy()
            ConstrainedValue(value, constraint)
        } else {
            null
        }

        // TODO: invocation side-effects

        return SuccessfulInvocation(MutableDataFrame(state.frame), result)
    }
}