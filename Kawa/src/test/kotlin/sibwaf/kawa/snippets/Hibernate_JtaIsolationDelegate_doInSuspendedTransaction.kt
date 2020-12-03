package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import spoon.reflect.code.CtIf
import spoon.reflect.declaration.CtVariable
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFalse
import kotlin.test.Test

@Suppress("ClassName")
class Hibernate_JtaIsolationDelegate_doInSuspendedTransaction : MethodAnalyzerTestBase() {

    private companion object {
        val TEXT = """
        private <T> T doInSuspendedTransaction(HibernateCallable<T> callable) {
            try {
                // First we suspend any current JTA transaction
                Transaction surroundingTransaction = transactionManager.suspend();
                LOG.debugf( "Surrounding JTA transaction suspended [%s]", surroundingTransaction );

                boolean hadProblems = false;
                try {
                    return callable.call();
                }
                catch (HibernateException e) {
                    hadProblems = true;
                    throw e;
                }
                finally {
                    try {
                        transactionManager.resume( surroundingTransaction );
                        LOG.debugf( "Surrounding JTA transaction resumed [%s]", surroundingTransaction );
                    }
                    catch (Throwable t) {
                        // if the actually work had an error use that, otherwise error based on t
                        if ( !hadProblems ) {
                            //noinspection ThrowFromFinallyBlock
                            throw new HibernateException( "Unable to resume previously suspended transaction", t );
                        }
                    }
                }
            }
            catch (SystemException e) {
                throw new HibernateException( "Unable to suspend current JTA transaction", e );
            }
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test '!hadProblems' is not a constant condition`() {
        val hadProblems = method.getElementsOf<CtVariable<*>>().single { it.simpleName == "hadProblems" }
        val ifStatement = method.getElementsOf<CtIf>().single()

        val flow = runBlocking { analyze(method) }
        val frame = flow.frames.getValue(ifStatement)

        expectThat(frame.getConstraint(hadProblems))
            .describedAs("hadProblems constraint")
            .isA<BooleanConstraint>()
            .and {
                get { isFalse }.isFalse()
                get { isTrue }.isFalse()
            }
    }
}