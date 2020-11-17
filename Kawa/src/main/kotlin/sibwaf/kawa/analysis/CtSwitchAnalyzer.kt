package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtSwitch

class CtSwitchAnalyzer : StatementAnalyzer {

    private val blockAnalyzer = CtBlockAnalyzer()

    override fun supports(statement: CtStatement) = statement is CtSwitch<*>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtSwitch<*>

        // TODO: statement.selector frame
        // TODO: mark cases unreachable by selector.value

        // FIXME: fall through?
        val frames = statement.cases.map {
            // TODO: holy shit this is a dirty hack, refactor it
            blockAnalyzer.analyze(state, it).compact(state.frame)
        }

        val resultFrame = DataFrame.merge(state.frame, frames)

        // TODO: check if 'default' case is needed at all
        return if (statement.cases.any { it.caseExpressions.isEmpty() }) {
            resultFrame
        } else {
            val nonMatchedFrame = MutableDataFrame(state.frame)
            DataFrame.merge(state.frame, resultFrame, nonMatchedFrame)
        }
    }
}