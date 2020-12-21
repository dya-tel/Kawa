package sibwaf.kawa.analysis

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.extractVariables
import sibwaf.kawa.parseStatement
import sibwaf.kawa.utility.IdentityValueCalculator
import spoon.reflect.code.CtFor
import strikt.api.expectThat
import strikt.assertions.isNull
import kotlin.test.Test

class CtForAnalyzerTest : StatementAnalyzerTestBase() {

    @Test fun `Test local variables from initializer are cleaned up`() {
        val statement = parseStatement<CtFor>("for (int i = 0; ; ) { break; }")
        val variable = statement.extractVariables().getValue("i")

        val analyzer = DelegatingStatementAnalyzer(
            listOf(
                CtForAnalyzer(),
                CtLocalVariableAnalyzer(),
                CtBreakAnalyzer(),
                CtBlockAnalyzer()
            )
        )

        val frame = runBlocking {
            analyzeStatement(analyzer, statement) {
                copy(valueProvider = IdentityValueCalculator)
            }
        } as ReachableFrame

        expectThat(frame.getValue(variable)).isNull()
    }
}