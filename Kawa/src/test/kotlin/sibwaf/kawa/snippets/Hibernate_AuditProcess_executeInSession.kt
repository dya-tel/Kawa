package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtWhile
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.test.Test

@Suppress("ClassName")
class Hibernate_AuditProcess_executeInSession : MethodAnalyzerTestBase() {

    private companion object {
        val TEXT = """
        private void executeInSession(Session session, java.util.Queue<AuditWorkUnit> undoQueue) {
            // Making sure the revision data is persisted.
            final Object currentRevisionData = getCurrentRevisionData( session, true );

            AuditWorkUnit vwu;

            // First undoing any performed work units
            while ( (vwu = undoQueue.poll()) != null ) {
                vwu.undo( session );
            }

            while ( (vwu = workUnits.poll()) != null ) {
                vwu.perform( session, revisionData );
                entityChangeNotifier.entityChanged( session, currentRevisionData, vwu );
            }
        }
        """.trimIndent()
    }

    val method by lazy { parseMethod(TEXT) }

    @Test fun `Test 'vwu' is non-null after assignment with check`() {
        val loopStarts = method.getElementsOf<CtWhile>()
                .map { it.body as CtBlock<*> }
                .map { it.first() }

        val vwu = method.extractVariables().getValue("vwu")

        val flow = runBlocking { analyze(method) }

        expectThat(loopStarts)
                .describedAs("for first statements in loops")
                .all {
                    get { flow.frames.getValue(this).getConstraint(vwu) }
                            .describedAs("vwu constraint")
                            .isA<ReferenceConstraint>()
                            .get { nullability }
                            .isEqualTo(Nullability.NEVER_NULL)
                }
    }
}