package sibwaf.kawa

import spoon.reflect.declaration.CtExecutable
import spoon.reflect.reference.CtExecutableReference
import spoon.support.reflect.declaration.CtMethodImpl
import java.util.Collections
import java.util.IdentityHashMap

class ModelInfoCache {

    companion object {
        private val INVALID_EXECUTABLE = CtMethodImpl<Unit>()
    }

    private val executableDeclarations = Collections.synchronizedMap(IdentityHashMap<CtExecutableReference<*>, CtExecutable<*>>())

    fun <T> getDeclaration(executable: CtExecutableReference<T>): CtExecutable<T>? {
        return when (val existing = executableDeclarations[executable]) {
            INVALID_EXECUTABLE -> null
            null -> {
                val declaration = executable.executableDeclaration
                executableDeclarations.putIfAbsent(executable, declaration ?: INVALID_EXECUTABLE)
                declaration
            }
            else -> @Suppress("unchecked_cast") (existing as CtExecutable<T>)
        }
    }
}