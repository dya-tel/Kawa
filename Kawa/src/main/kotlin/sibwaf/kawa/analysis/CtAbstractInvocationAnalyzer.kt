package sibwaf.kawa.analysis

import sibwaf.kawa.calculation.CtConstructorCallCalculator
import sibwaf.kawa.calculation.CtInvocationCalculator
import sibwaf.kawa.calculation.DelegatingValueCalculator

class CtAbstractInvocationAnalyzer : ExpressionStatementAnalyzer() {
    override val calculator = DelegatingValueCalculator(
            listOf(
                    CtConstructorCallCalculator(),
                    CtInvocationCalculator()
            )
    )
}