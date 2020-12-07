package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.parseMethod
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFalse
import kotlin.test.Test

@Suppress("ClassName")
class Hibernate_BoundedConcurrentHashMap_containsValue : MethodAnalyzerTestBase() {
    private companion object {
        val TEXT = """
        public boolean containsValue(Object value, /* other variables */ int mcsum, int[] mc, int[] segments) {
            // Code omitted
            boolean cleanSweep = true;
            if ( mcsum != 0 ) {
                for ( int i = 0; i < segments.length; ++i ) {
                    if ( mc[i] != segments[i] ) {
                        cleanSweep = false;
                        break;
                    }
                }
            }
            // Code omitted
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test 'cleanSweep' is not a constant value`() {
        val flow = runBlocking { analyze(method) }
        val cleanSweep = method.extractVariables().getValue("cleanSweep")
        val frame = flow.endFrame as ReachableFrame

        expectThat(frame.getConstraint(cleanSweep))
            .describedAs("cleanSweep")
            .isA<BooleanConstraint>()
            .and {
                get { isFalse }.isFalse()
                get { isTrue }.isFalse()
            }
    }
}