package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtBodyHolder
import spoon.reflect.code.CtStatement

class CtBodyHolderAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtBodyHolder

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtBodyHolder

        val blockFrame = (statement.body as CtBlock<*>?)?.let { state.getStatementFlow(it) }
        return blockFrame ?: state.frame
    }
}