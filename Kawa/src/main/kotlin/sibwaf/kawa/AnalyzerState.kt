package sibwaf.kawa

import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.emulation.FailedInvocation
import sibwaf.kawa.emulation.InvocationResult
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtCFlowBreak
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtConstructor
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.declaration.CtTypeMember
import spoon.reflect.declaration.CtVariable
import spoon.reflect.reference.CtExecutableReference

// TODO: move frame tracing to a separate collection to get rid of side effects and allow no-trace runs
// TODO: remove 'annotation'
data class AnalyzerState(
    val annotation: MethodFlow,
    val frame: ReachableFrame,
    val localVariables: MutableSet<CtVariable<*>>,
    val jumpPoints: MutableCollection<Pair<CtCFlowBreak, ReachableFrame>>,
    val callChain: RightChain<CtExecutable<*>>? = null, // TODO: probably should be non-null

    private val methodEmulator: suspend (AnalyzerState, CtExecutableReference<*>, List<ConstrainedValue>) -> InvocationResult,
    private val statementFlowProvider: suspend (AnalyzerState, CtStatement) -> DataFrame,
    private val valueProvider: suspend (AnalyzerState, CtExpression<*>) -> Pair<DataFrame, ConstrainedValue>,
    private val conditionValueProvider: suspend (AnalyzerState, CtExpression<*>) -> ConditionCalculatorResult
) {

    suspend fun getInvocationFlow(executable: CtExecutableReference<*>, arguments: List<ConstrainedValue>): InvocationResult {
        // TODO: manual annotations go here

        val declaration = executable.executableDeclaration ?: return FailedInvocation

        if (callChain?.contains(declaration) == true) {
            // TODO: handle recursive calls?
            return FailedInvocation
        }

        if (callChain != null && declaration !is CtConstructor<*>) {
            declaration as CtTypeMember
            if (!declaration.isPrivate && !declaration.isFinal && !declaration.isStatic) {
                if (!declaration.declaringType.isFinal) {
                    return FailedInvocation
                }
            }
        }

        val chain = RightChain(callChain, declaration)
        return methodEmulator(this.copy(callChain = chain), executable, arguments)
    }

    suspend fun getStatementFlow(statement: CtStatement): DataFrame {
        annotation.frames[statement] = frame
        return statementFlowProvider(this, statement)
    }

    suspend fun getValue(expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        annotation.frames[expression] = frame
        return valueProvider(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        annotation.frames[expression] = frame
        return conditionValueProvider(this, expression)
    }
}