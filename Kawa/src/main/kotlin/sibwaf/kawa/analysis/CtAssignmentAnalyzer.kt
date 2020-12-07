package sibwaf.kawa.analysis

import sibwaf.kawa.calculation.CtAssignmentCalculator

class CtAssignmentAnalyzer : ExpressionStatementAnalyzer() {
    override val calculator = CtAssignmentCalculator()
}