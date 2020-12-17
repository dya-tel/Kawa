package sibwaf.kawa

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sibwaf.kawa.analysis.CtAbstractInvocationAnalyzer
import sibwaf.kawa.analysis.CtAssertAnalyzer
import sibwaf.kawa.analysis.CtAssignmentAnalyzer
import sibwaf.kawa.analysis.CtBlockAnalyzer
import sibwaf.kawa.analysis.CtBreakAnalyzer
import sibwaf.kawa.analysis.CtContinueAnalyzer
import sibwaf.kawa.analysis.CtDoAnalyzer
import sibwaf.kawa.analysis.CtForAnalyzer
import sibwaf.kawa.analysis.CtForEachAnalyzer
import sibwaf.kawa.analysis.CtIfAnalyzer
import sibwaf.kawa.analysis.CtLocalVariableAnalyzer
import sibwaf.kawa.analysis.CtReturnAnalyzer
import sibwaf.kawa.analysis.CtSwitchAnalyzer
import sibwaf.kawa.analysis.CtSynchronizedAnalyzer
import sibwaf.kawa.analysis.CtThrowAnalyzer
import sibwaf.kawa.analysis.CtTryAnalyzer
import sibwaf.kawa.analysis.CtUnaryOperatorAnalyzer
import sibwaf.kawa.analysis.CtWhileAnalyzer
import sibwaf.kawa.analysis.DelegatingStatementAnalyzer
import sibwaf.kawa.analysis.StatementAnalyzer
import sibwaf.kawa.emulation.BasicMethodEmulator
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.declaration.CtType
import spoon.reflect.declaration.CtTypeMember
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.system.measureNanoTime

private object FallbackAnalyzer : StatementAnalyzer {

    private val log = LoggerFactory.getLogger(FallbackAnalyzer::class.java)
    private val failedStatementTypes = ConcurrentHashSet<Class<*>>()

    override fun supports(statement: CtStatement) = true

    override suspend fun analyze(state: AnalyzerState, statement: CtStatement): DataFrame {
        if (failedStatementTypes.add(statement::class.java)) {
            log.warn("Failed to find an analyzer for {}", statement::class.java)
        }

        return state.frame
    }
}

class MethodFlowAnalyzer private constructor() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MethodFlowAnalyzer::class.java)

        suspend fun analyze(types: Iterable<CtType<*>>, baseCoroutineCount: Int): Map<CtExecutable<*>, MethodFlow> {
            return Executors.newFixedThreadPool(baseCoroutineCount).asCoroutineDispatcher().use { dispatcher ->
                coroutineScope {
//                val x = types.toList()
//                var total = 0

                    val x = types.flatMap { it.methods }.toList()
                    val total = x.size

                    // FIXME: no parallelism at all with producer
                    //  Maybe raw channel should be used?
                    /*val channel = produce<CtExecutable<*>>(Dispatchers.Unconfined, capacity = baseCoroutineCount * 128) {
                    for (type in x) {
                        for (method in type.methods) {
                            total++
                            send(method)
                        }
                    }
                }*/

                    val startTime = System.currentTimeMillis()

                    val analyzer = MethodFlowAnalyzer()
                    val processors = ArrayList<Job>(baseCoroutineCount)

                    val methodsPerProcessor = ceil(x.size.toDouble() / baseCoroutineCount).toInt()
                    for ((index, chunk) in x.chunked(methodsPerProcessor).withIndex()) {
                        processors += launch(dispatcher + CoroutineName("Processor #$index")) {
                            for (method in chunk) {
                                analyzer.analyze(method)
                            }
                        }
                    }

                    /*repeat(baseCoroutineCount) { index ->
                    processors += launch(dispatcher + CoroutineName("Processor #$index")) {
                        while (isActive) {
                            val method = channel.receiveOrNull() ?: break
                            coroutineScope {
                                analyzer.getFlowFor(method, null)
                            }
                        }
                    }
                }*/

                    val debugPrinter = launch {
                        while (log.isDebugEnabled && isActive) {
                            val time = (System.currentTimeMillis() - startTime) / 1000
                            log.debug("${analyzer.cache.size}/$total analyzed, $time seconds)")
                            delay(1000)
                        }
                    }

                    processors.joinAll()
                    debugPrinter.cancelAndJoin()

                    if (log.isDebugEnabled) {
                        val topMethods = analyzer.benchmark.asSequence()
                            .sortedByDescending { it.second }
                            .take(16)

                        for ((method, time) in topMethods) {
                            method as CtTypeMember
                            val name = "${method.declaringType.qualifiedName}.${method.simpleName}"
                            log.debug("$name: %.2f ms".format(time.toDouble() / 1000 / 1000))
                        }
                    }

                    IdentityHashMap(analyzer.cache)
                }
            }
        }
    }

    // TODO: concurrent identity
    private val cache = ConcurrentHashMap<CtExecutable<*>, MethodFlow>()

    private val benchmark = ConcurrentLinkedQueue<Pair<CtExecutable<*>, Long>>()

    private val analyzer = DelegatingStatementAnalyzer(
        listOf(
            CtLocalVariableAnalyzer(),
            CtAssignmentAnalyzer(),
            // TODO: all flow breaks should probably be merged into one StatementAnalyzer
            CtReturnAnalyzer(),
            CtThrowAnalyzer(),
            CtContinueAnalyzer(),
            CtBreakAnalyzer(),
            CtAbstractInvocationAnalyzer(),
            CtUnaryOperatorAnalyzer(),
            CtIfAnalyzer(),
            CtSwitchAnalyzer(),
            CtForAnalyzer(),
            CtForEachAnalyzer(),
            CtWhileAnalyzer(),
            CtDoAnalyzer(),
            CtTryAnalyzer(),
            CtSynchronizedAnalyzer(),
            CtBlockAnalyzer(),
            CtAssertAnalyzer(),
            FallbackAnalyzer
        )
    )

    private val emulator = BasicMethodEmulator(cache)
    private val interproceduralEmulator = emulator

    private val rootState = AnalyzerState(
        annotation = EmptyFlow,
        frame = MutableDataFrame(null),
        localVariables = Collections.emptySet(),
        jumpPoints = Collections.emptyList(),
        methodEmulator = interproceduralEmulator::emulate,
        statementFlowProvider = analyzer::analyze,
        valueProvider = ValueCalculator::calculateValue,
        conditionValueProvider = ValueCalculator::calculateCondition
    )

    private suspend fun analyze(method: CtExecutable<*>) {
        val time = measureNanoTime {
            val state = rootState.copy(callChain = RightChain(null, method))
            emulator.emulate(state, method.reference, emptyList())
        }

        if (log.isDebugEnabled) {
            method as CtTypeMember
            benchmark += method to time
        }
    }
}