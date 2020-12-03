package sibwaf.kawa.analysis

import sibwaf.kawa.calculation.CtUnaryOperatorIncDecCalculator

class CtUnaryOperatorAnalyzer : ExpressionStatementAnalyzer() {
    override val calculator = CtUnaryOperatorIncDecCalculator() // TODO
}