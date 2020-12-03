package sibwaf.kawa

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test

class RecursionSingleWorkerTest : MethodAnalyzerTestBase() {

    override val coroutineCount = 1

    @Test fun `Test direct recursion`() {
        val type = parse(
            """
            class A {
                private int x1() {
                    return x1(); 
                }
            }
            """.trimIndent()
        )

        runBlocking {
            withTimeout(100) {
                analyze(type)
            }
        }
    }

    @Test fun `Test indirect recursion`() {
        val type = parse(
            """
            class A {
                private int x1() {
                    return x2(); 
                }
                private int x2() {
                    return x3();
                }
                private int x3() {
                    return x1();
                }
            }
            """.trimIndent()
        )

        runBlocking {
            withTimeout(100) {
                analyze(type)
            }
        }
    }
}

class RecursionMultiWorkerTest : MethodAnalyzerTestBase() {

    override val coroutineCount = 2

    @Test fun `Test direct recursion`() {
        val type = parse(
            """
            class A {
                private int x1() {
                    return x1(); 
                }
            }
            """.trimIndent()
        )

        runBlocking {
            withTimeout(100) {
                analyze(type)
            }
        }
    }

    @Test fun `Test indirect recursion`() {
        val type = parse(
            """
            class A {
                private int x1() {
                    return x2(); 
                }
                private int x2() {
                    return x3();
                }
                private int x3() {
                    return x1();
                }
            }
            """.trimIndent()
        )

        runBlocking {
            withTimeout(100) {
                analyze(type)
            }
        }
    }
}