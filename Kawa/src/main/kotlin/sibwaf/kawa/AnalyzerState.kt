package sibwaf.kawa

import sibwaf.kawa.analysis.StatementAnalyzer
import sibwaf.kawa.calculation.ValueCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.emulation.FailedInvocation
import sibwaf.kawa.emulation.InvocationResult
import sibwaf.kawa.emulation.MethodEmulator
import sibwaf.kawa.emulation.MutableMethodTrace
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtCFlowBreak
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtConstructor
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.declaration.CtTypeMember
import spoon.reflect.declaration.CtVariable
import spoon.reflect.reference.CtExecutableReference

// TODO: remove 'annotation'
data class AnalyzerState(
    private val annotation: MethodFlow,

    val trace: MutableMethodTrace,

    val frame: ReachableFrame,
    val localVariables: MutableSet<CtVariable<*>>,
    val jumpPoints: MutableCollection<Pair<CtCFlowBreak, ReachableFrame>>,
    val callChain: RightChain<CtExecutable<*>>? = null, // TODO: probably should be non-null

    private val methodEmulator: MethodEmulator,
    private val statementFlowProvider: StatementAnalyzer,
    private val valueProvider: ValueCalculator,
    private val conditionValueProvider: ConditionCalculator,

    val cache: ModelInfoCache = ModelInfoCache()
) {

    suspend fun getInvocationFlow(executable: CtExecutableReference<*>, arguments: List<ConstrainedValue>): InvocationResult {
        // TODO: manual annotations go here

        val declaration = cache.getDeclaration(executable) ?: return FailedInvocation

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
        return methodEmulator.emulate(this.copy(callChain = chain), executable, arguments)
    }

    suspend fun getStatementFlow(statement: CtStatement): DataFrame {
        trace.trace(statement, frame)
        return statementFlowProvider.analyze(this, statement)
    }

    suspend fun getValue(expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        trace.trace(expression, frame)
        return valueProvider.calculate(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        trace.trace(expression, frame)
        return conditionValueProvider.calculateCondition(this, expression)
    }
}