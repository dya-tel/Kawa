package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import spoon.reflect.code.CtReturn
import strikt.api.expectThat
import strikt.assertions.isA
import kotlin.test.Test

class Hibernate_H2IdleConnectionCounter_count : MethodAnalyzerTestBase() {

    private companion object {
        val TEXT = """
        @Override
        public int count(Connection connection) {
            try ( Statement statement = connection.createStatement() ) {
                try ( ResultSet resultSet = statement.executeQuery(
                        "select count(*) " +
                                "from information_schema.sessions " +
                                "where statement is null" ) ) {
                    while ( resultSet.next() ) {
                        return resultSet.getInt( 1 );
                    }
                    return 0;
                }
            }
            catch ( SQLException e ) {
                throw new IllegalStateException( e );
            }
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test 'while ( unknown ) { return }' doesn't make next frame unreachable`() {
        val returnStatement = method.getElementsOf<CtReturn<*>>().last()

        val flow = runBlocking { analyze(method) }
        val frame = flow.frames.getValue(returnStatement)

        expectThat(frame)
                .not().isA<UnreachableFrame>()
    }
}