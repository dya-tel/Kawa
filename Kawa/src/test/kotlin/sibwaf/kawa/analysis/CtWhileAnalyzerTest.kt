package sibwaf.kawa.analysis

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.calculation.conditions.LiteralConditionCalculator
import sibwaf.kawa.parseStatement
import spoon.reflect.code.CtWhile
import strikt.api.expectThat
import strikt.assertions.isA
import kotlin.test.Test

class CtWhileAnalyzerTest : StatementAnalyzerTestBase() {

    @Test fun `Test resulting frame is unreachable in while (true) without jumps`() {
        val loop = parseStatement<CtWhile>("while (true) {}")

        val analyzer = DelegatingStatementAnalyzer(listOf(CtWhileAnalyzer(), CtBlockAnalyzer()))

        val resultFrame = runBlocking {
            analyzeStatement(analyzer, loop) {
                copy(conditionValueProvider = LiteralConditionCalculator())
            }
        }

        expectThat(resultFrame).isA<UnreachableFrame>()
    }
}