package sibwaf.kawa.analysis

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.elseBlock
import sibwaf.kawa.thenBlock
import spoon.reflect.code.CtIf
import spoon.reflect.code.CtStatement

class CtIfAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtIf

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        statement as CtIf

        var (thenBranch, elseBranch, _, _) = state.getConditionValue(statement.condition)

        if (thenBranch is ReachableFrame) {
            thenBranch = state.copy(frame = thenBranch).getStatementFlow(statement.thenBlock)
        }

        val elseBlock = statement.elseBlock
        if (elseBranch is ReachableFrame && elseBlock != null) {
            elseBranch = state.copy(frame = elseBranch).getStatementFlow(elseBlock)
        }

        return DataFrame.merge(
            state.frame,
            thenBranch.compact(state.frame),
            elseBranch.compact(state.frame)
        )
    }
}