package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.parse
import kotlin.test.Test

@Suppress("ClassName")
class Hibernate_Query_setParameters : MethodAnalyzerTestBase() {
    private companion object {
        val TEXT = """
        public abstract class Query<R> {
            public abstract Query<R> setParameter(int position, Object val, String type);

        	public Query<R> setParameters(Object[] values, String[] types) {
        		assert values.length == types.length;
        		for ( int i = 0; i < values.length; i++ ) {
        			setParameter( i, values[i], types[i] );
        		}

        		return this;
        	}
        }
        """.trimIndent()
    }

    private val type by lazy { parse(TEXT) }

    @Test fun `Test analysis doesn't crash`() {
        runBlocking { analyze(type) }
    }
}