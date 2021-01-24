package sibwaf.kawa

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.reflections.Reflections
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sibwaf.kawa.rules.Rule
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.visitor.CtScanner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class Analyzer(private val modelLoader: ModelLoader, private val baseCoroutineCount: Int) {

    private companion object {
        val PACKAGE_NAME: String = Rule::class.java.`package`.name
        val log: Logger = LoggerFactory.getLogger(Analyzer::class.java)
    }

    fun analyze(): Collection<Warning> {
        val model = modelLoader.model

        val types = model.model.allTypes.toList()

        val flow: Map<CtExecutable<*>, MethodFlow>
        val flowTime = measureTimeMillis {
            flow = runBlocking {
                MethodFlowAnalyzer.analyze(types, baseCoroutineCount)
            }
        }

        log.info("Completed flow analysis in %.2f seconds".format(flowTime.toDouble() / 1000))

        val ruleTypes = Reflections(PACKAGE_NAME).getSubTypesOf(Rule::class.java)

        Executors.newFixedThreadPool(baseCoroutineCount).asCoroutineDispatcher().use { dispatcher ->
            val benchmark = ConcurrentHashMap<String, AtomicLong>()

            val allWarnings = runBlocking {
                val processors = types.splitIntoParts(baseCoroutineCount).map { part ->
                    val rules = ruleTypes
                        .map { it.newInstance() }
                        .onEach { it.flow = flow }

                    val scanner = object : CtScanner() {
                        override fun scan(element: CtElement?) {
                            if (element != null) {
                                for (rule in rules) {
                                    val time = measureNanoTime {
                                        element.accept(rule)
                                    }

                                    benchmark.computeIfAbsent(rule.name) { AtomicLong(0) }.addAndGet(time)
                                }
                            }
                            super.scan(element)
                        }
                    }

                    async(dispatcher) {
                        scanner.scan(part)
                        rules.flatMap { it.warnings }
                    }
                }

                processors.awaitAll().flatten()
            }

            for (rule in benchmark.keys.sorted()) {
                val millis = (benchmark.getValue(rule).toDouble() / 1000 / 1000).toInt()
                log.info("$rule: $millis ms")
            }

            return allWarnings
        }
    }
}