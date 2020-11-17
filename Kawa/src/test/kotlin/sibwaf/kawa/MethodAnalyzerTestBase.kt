package sibwaf.kawa

import spoon.Launcher
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtMethod

open class MethodAnalyzerTestBase {
    open val coroutineCount = Runtime.getRuntime().availableProcessors() * 2

    init {
        Launcher.parseClass("class InitClass {}")
    }

    suspend fun analyze(types: Iterable<CtClass<*>>) = MethodFlowAnalyzer.analyze(types, coroutineCount)

    suspend fun analyze(clazz: CtClass<*>) = analyze(listOf(clazz))

    suspend fun analyze(method: CtMethod<*>): MethodFlow {
        val flow = analyze(method.declaringType as CtClass<*>)
        return flow.getValue(method)
    }

    suspend fun analyzeMethod(text: String) = analyze(parseMethod(text))
}