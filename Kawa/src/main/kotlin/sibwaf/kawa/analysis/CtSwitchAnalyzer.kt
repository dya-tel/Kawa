package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.AnalyzerState
import spoon.reflect.code.CtBreak
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtSwitch

class CtSwitchAnalyzer : StatementAnalyzer {

    private val blockAnalyzer = CtBlockAnalyzer()

    override fun supports(statement: CtStatement) = statement is CtSwitch<*>

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtSwitch<*>

        val localState = state.copy(jumpPoints = ArrayList())

        // TODO: statement.selector frame
        // TODO: mark cases unreachable by selector.value
        // FIXME: fallthrough

        var resultFrame: DataFrame = MutableDataFrame(state.frame)

        for (case in statement.cases) {
            // FIXME: holy shit this is a dirty hack, refactor it
            val frame = blockAnalyzer.analyze(localState, case)

            if (case.caseExpressions.isEmpty()) {
                // We found a 'default' case which should replace 'state.frame' as a fallback
                resultFrame = frame
            }
        }

        // TODO: check if 'default' case is needed at all
        // FIXME: cases without 'break's are ignored

        for (jump in localState.jumpPoints) {
            if (jump.first is CtBreak) {
                resultFrame = DataFrame.merge(state.frame, resultFrame, jump.second.compact(state.frame))
            } else {
                state.jumpPoints += jump
            }
        }

        return resultFrame
    }
}