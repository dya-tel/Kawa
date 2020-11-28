package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodPurity
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import spoon.reflect.code.CtAbstractInvocation
import spoon.reflect.code.CtStatement

class CtAbstractInvocationAnalyzer : StatementAnalyzer {

    override fun supports(statement: CtStatement) = statement is CtAbstractInvocation<*>

    override suspend fun analyze(state: StatementAnalyzerState, statement: CtStatement): DataFrame {
        statement as CtAbstractInvocation<*>

        val flow = state.getMethodFlow(statement.executable)

        // TODO
        if (flow.purity != MethodPurity.PURE) {
            state.annotation.purity = MethodPurity.DIRTIES_THIS
        }

        if (flow.neverReturns) {
            return UnreachableFrame.after(state.frame)
        }

        return MutableDataFrame(state.frame)
    }
}