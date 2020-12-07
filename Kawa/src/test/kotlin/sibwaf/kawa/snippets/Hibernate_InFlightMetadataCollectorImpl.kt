package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.parseMethod
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.test.Test

@Suppress("ClassName")
class Hibernate_InFlightMetadataCollectorImpl : MethodAnalyzerTestBase() {

    private companion object {
        val TEXT = """
        @Override
        public String getLogicalColumnName(Table table, Identifier physicalName) throws MappingException {
            final String physicalNameString = physicalName.render( getDatabase().getJdbcEnvironment().getDialect() );
            Identifier logicalName = null;

            Table currentTable = table;
            while ( currentTable != null ) {
                final TableColumnNameBinding binding = columnNameBindingByTableMap.get( currentTable );

                if ( binding != null ) {
                    logicalName = binding.physicalToLogical.get( physicalNameString );
                    if ( logicalName != null ) {
                        break;
                    }
                }

                if ( DenormalizedTable.class.isInstance( currentTable ) ) {
                    currentTable = ( (DenormalizedTable) currentTable ).getIncludedTable();
                }
                else {
                    currentTable = null;
                }
            }

            if ( logicalName == null ) {
                throw new MappingException(
                        "Unable to find column with physical name " + physicalNameString + " in table " + table.getName()
                );
            }
            return logicalName.render();
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test 'currentTable' is a possible null after loop`() {
        val currentTable = method.extractVariables().getValue("currentTable")

        val flow = runBlocking { analyze(method) }
        val frame = (flow.endFrame as UnreachableFrame).previous

        expectThat(frame.getConstraint(currentTable))
            .isA<ReferenceConstraint>()
            .get { nullability }
            .isEqualTo(Nullability.POSSIBLE_NULL)
    }
}