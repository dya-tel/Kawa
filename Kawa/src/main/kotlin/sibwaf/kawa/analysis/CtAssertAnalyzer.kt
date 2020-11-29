package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import spoon.reflect.code.CtAssert
import spoon.reflect.code.CtStatement

class CtAssertAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtAssert<*>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtAssert<*>

        // TODO: statement.expression can change state, should also be handled
        return state.getConditionValue(statement.assertExpression).thenFrame
    }
}