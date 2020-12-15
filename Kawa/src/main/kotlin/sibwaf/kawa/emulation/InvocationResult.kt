package sibwaf.kawa.emulation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue

sealed class InvocationResult

object FailedInvocation : InvocationResult()

data class SuccessfulInvocation(
    val frame: DataFrame,
    val value: ConstrainedValue?
) : InvocationResult()