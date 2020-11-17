package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtLoop
import spoon.reflect.code.CtStatement

class CtLoopAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtLoop

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtLoop

        val body = statement.body as CtBlock<*>

        val blockFrame = state.getStatementFlow(body).compact(state.frame)
        val secondIterationFrame = DataFrame.merge(
                state.frame,
                MutableDataFrame(state.frame),
                blockFrame
        )

        val secondIterationState = state.copy(frame = secondIterationFrame)
        return secondIterationState.getStatementFlow(body)//.eraseValues()
    }
}

/*
abstract class CtLoopAnalyzer : StatementAnalyzer {

    protected open suspend fun beforeLoop(state: StatementAnalyzerState, loop: CtLoop): Pair<DataFrame, Boolean> =
            state.frame to true

    protected open suspend fun afterLoop(state: StatementAnalyzerState, loop: CtLoop): Pair<DataFrame, Boolean> =
            state.frame to true

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtLoop

        val body = statement.body as CtBlock<*>

        var (loopFrame, exitFrame) = beforeLoop(state, statement)

        val blockFrame = state.copy(frame = loopFrame).getStatementFlow(body).compact(state.frame)
        val secondIterationFrame = DataFrame.merge(
                state.frame,
                MutableDataFrame(state.frame),
                blockFrame
        )


        val secondIterationState = state.copy(frame = secondIterationFrame)
        return secondIterationState.getStatementFlow(body).eraseValues()
    }
}*/
