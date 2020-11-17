package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import spoon.reflect.code.CtIf
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.test.Test

@Suppress("ClassName")
class Hibernate_AbstractGraph_findOrCreateAttributeNode : MethodAnalyzerTestBase() {
    private companion object {
        val TEXT = """
        private java.util.Map<Object, Object> attrNodeMap = new java.util.HashMap<>();
        public Object findOrCreateAttributeNode(Object attribute) {
            Object attrNode = null;
            if ( attrNodeMap == null ) {
                attrNodeMap = new java.util.HashMap<>();
            }
            else {
                attrNode = attrNodeMap.get( attribute );
            }

            if ( attrNode == null ) {
                attrNode = new Object();
                attrNodeMap.put( attribute, attrNode );
            }

            return attrNode;
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test possible null after map initialization`() {
        val attrNode = method.extractVariables().getValue("attrNode")
        val ifStatement = method.getElementsOf<CtIf>()[1]

        val flow = runBlocking { analyze(method) }
        val frame = flow.frames.getValue(ifStatement)

        expectThat(frame.getConstraint(attrNode))
                .describedAs("attrNode constraint")
                .isA<ReferenceConstraint>()
                .get { nullability }
                .isEqualTo(Nullability.POSSIBLE_NULL)
    }
}