package sibwaf.kawa.emulation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.reference.CtExecutableReference

interface MethodEmulator {
    suspend fun emulate(
        state: AnalyzerState,
        method: CtExecutableReference<*>,
        arguments: List<ConstrainedValue>
    ): Pair<DataFrame, ConstrainedValue?>
}