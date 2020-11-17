package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtSynchronized

class CtSynchronizedAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtSynchronized

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtSynchronized
        return state.getStatementFlow(statement.block)
    }
}