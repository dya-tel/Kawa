package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import spoon.reflect.code.CtStatement

interface StatementAnalyzer {
    fun supports(statement: CtStatement): Boolean
    suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame
}