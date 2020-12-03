package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.AnalyzerState
import spoon.reflect.code.CtStatement

class DelegatingStatementAnalyzer(private val analyzers: List<StatementAnalyzer>) : StatementAnalyzer {

    override fun supports(statement: CtStatement): Boolean {
        return analyzers.isNotEmpty() && analyzers.any { it.supports(statement) }
    }

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        for (analyzer in analyzers) {
            if (analyzer.supports(statement)) {
                return analyzer.analyze(state, statement)
            }
        }

        throw IllegalArgumentException("Unsupported statement: $statement")
    }
}