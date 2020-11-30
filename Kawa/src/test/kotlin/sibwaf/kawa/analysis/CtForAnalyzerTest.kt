package sibwaf.kawa.analysis

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.extractVariables
import sibwaf.kawa.parseStatement
import spoon.reflect.code.CtFor
import strikt.api.expectThat
import strikt.assertions.isNull
import kotlin.test.Test

class CtForAnalyzerTest : StatementAnalyzerTestBase() {

    @Test fun `Test local variables from initializer are cleaned up`() {
        val statement = parseStatement<CtFor>("for (int i = 0; ; ) {}")
        val variable = statement.extractVariables().getValue("i")

        val analyzer = TestCtStatementAnalyzer(
                listOf(
                        CtForAnalyzer(),
                        CtLocalVariableAnalyzer(),
                        CtBlockAnalyzer()
                )
        )

        val frame = runBlocking { analyzeStatement(analyzer, statement) }

        expectThat(frame.getValue(variable)).isNull()
    }
}