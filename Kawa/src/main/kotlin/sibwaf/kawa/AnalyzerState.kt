package sibwaf.kawa

import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtCFlowBreak
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import spoon.reflect.reference.CtExecutableReference

data class AnalyzerState(
    val annotation: MethodFlow,
    val frame: DataFrame,
    val localVariables: MutableSet<CtLocalVariable<*>>,
    val returnPoints: MutableSet<CtStatement>,
    val jumpPoints: MutableCollection<Pair<CtCFlowBreak, DataFrame>>,

    private val methodFlowProvider: suspend (CtExecutableReference<*>) -> MethodFlow,
    private val statementFlowProvider: suspend (AnalyzerState, CtStatement) -> DataFrame,
    private val valueProvider: suspend (AnalyzerState, CtExpression<*>) -> Pair<DataFrame, ConstrainedValue>,
    private val conditionValueProvider: suspend (AnalyzerState, CtExpression<*>) -> ConditionCalculatorResult
) {

    suspend fun getMethodFlow(executable: CtExecutableReference<*>): MethodFlow {
        return methodFlowProvider(executable)
    }

    suspend fun getStatementFlow(statement: CtStatement): DataFrame {
        annotation.frames[statement] = frame

        if (frame is UnreachableFrame) {
            return frame
        }

        return statementFlowProvider(this, statement)
    }

    suspend fun getValue(expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        annotation.frames[expression] = frame

        if (frame is UnreachableFrame) {
            return frame to ConstrainedValue.from(expression, ValueSource.NONE) // TODO: invalid value
        }

        return valueProvider(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        annotation.frames[expression] = frame

        if (frame is UnreachableFrame) {
            // TODO: invalid result
            return ConditionCalculatorResult(
                thenFrame = frame,
                elseFrame = frame,
                value = BooleanValue(ValueSource.NONE),
                constraint = BooleanConstraint.createUnknown()
            )
        }

        return conditionValueProvider(this, expression)
    }
}