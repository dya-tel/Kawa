package sibwaf.kawa

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
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
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtReturn
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtThrow
import spoon.reflect.declaration.CtConstructor
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.declaration.CtType
import spoon.reflect.declaration.CtTypeMember
import spoon.reflect.reference.CtExecutableReference
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.ceil

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
                                analyzer.getFlowFor(method, null)
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
                            val cache = analyzer.cache
                            val (completed, remaining) = cache.values.partition { it.isCompleted }

                            val time = (System.currentTimeMillis() - startTime) / 1000

                            log.debug("${completed.size} analyzed out of ${completed.size + remaining.size} ($total sent, $time seconds)")

                            delay(1000)
                        }
                    }

                    processors.joinAll()
                    debugPrinter.cancelAndJoin()

                    IdentityHashMap<CtExecutable<*>, MethodFlow>().apply {
                        for ((executable, flow) in analyzer.cache) {
                            put(executable, flow.await())
                        }
                    }
                }
            }
        }
    }

    // TODO: concurrent identity
    private val cache = ConcurrentHashMap<CtExecutable<*>, Deferred<MethodFlow>>()

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

    private suspend fun getFlowFor(
        method: CtExecutableReference<*>,
        callChain: RightChain<CtExecutable<*>>?
    ): MethodFlow {
        val declaration = method.executableDeclaration ?: return EmptyFlow
        return getFlowFor(declaration, callChain)
    }

    private suspend fun getFlowFor(
        method: CtExecutable<*>,
        callChain: RightChain<CtExecutable<*>>?
    ): MethodFlow {
        // TODO: if method.simpleName in manualAnnotations

        if (method.body == null) {
            return EmptyFlow
        }

        if (callChain?.contains(method) == true) {
            // TODO: handle recursive calls?
            return EmptyFlow
        }

        if (callChain != null && method is CtTypeMember && method !is CtConstructor<*>) {
            if (!method.isPrivate && !method.isFinal && !method.isStatic) {
                if (!method.declaringType.isFinal) {
                    return EmptyFlow
                }
            }
        }

        return coroutineScope {
            val coroutine = async(CoroutineName("Method processor: ${method.simpleName}"), start = CoroutineStart.LAZY) {
                analyze(method, RightChain(callChain, method))
            }

            var task = cache.putIfAbsent(method, coroutine)
            if (task == null) {
                task = coroutine // We are the first ones to create a task for this method
                task.start()
            } else {
                coroutine.cancel() // This method is already being processed, no need for our coroutine
            }

            return@coroutineScope task.await()
        }
    }

    private suspend fun analyze(
        method: CtExecutable<*>,
        callChain: RightChain<CtExecutable<*>>?
    ): MethodFlow {
        val annotation = MethodFlow()

        val startFrame = MutableDataFrame(null)
        for (parameter in method.parameters) {
            val value = Value.from(parameter, ValueSource.PARAMETER)
            startFrame.setValue(parameter, value)

            val constraint = Constraint.from(value)
            startFrame.setConstraint(value, constraint)
        }

        val bodyBlock: CtBlock<*> = method.body

        val flowProvider: suspend (CtExecutableReference<*>) -> MethodFlow = { getFlowFor(it, callChain) }
        val analyzerState = AnalyzerState(
            annotation = annotation,
            frame = startFrame,
            localVariables = Collections.emptySet(),
            jumpPoints = ArrayList(),
            methodFlowProvider = flowProvider,
            statementFlowProvider = analyzer::analyze,
            valueProvider = ValueCalculator::calculateValue,
            conditionValueProvider = ValueCalculator::calculateCondition
        )

        analyzer.analyze(analyzerState, bodyBlock)

        val blockFlow = annotation.blocks.getValue(bodyBlock)
        annotation.startFrame = blockFlow.startFrame
        annotation.endFrame = blockFlow.endFrame

        annotation.neverReturns = annotation.endFrame is UnreachableFrame && analyzerState.jumpPoints.all { it.first is CtThrow }

        if (annotation.purity == null) {
            annotation.purity = MethodPurity.PURE
        }

        if (method.type.qualifiedName != "void") {
            fun invalidConstraint() = Constraint.from(Value.from(method, ValueSource.NONE))

            annotation.returnConstraint = if (annotation.neverReturns) {
                invalidConstraint()
            } else {
                val returnedConstraints = LinkedList<Constraint>()
                for ((statement, frame) in analyzerState.jumpPoints) {
                    if (statement !is CtReturn<*> || statement.returnedExpression == null) {
                        continue
                    }

                    // TODO: non-optimal, should use value cache in the future
                    val (_, value) = analyzerState.copy(frame = frame).getValue(statement.returnedExpression)
                    returnedConstraints += value.constraint
                }

                if (returnedConstraints.isEmpty()) {
                    invalidConstraint()
                } else {
                    returnedConstraints.reduce(Constraint::merge)
                }
            }
        }

        return annotation
    }
}