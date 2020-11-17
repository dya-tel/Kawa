package sibwaf.kawa

@DslMarker
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class AnnotationDsl

fun namespace(name: String, block: NamespaceScope.() -> Unit) {
    val scope = NamespaceScope(name)
    scope.block()
}

@AnnotationDsl
class NamespaceScope(val name: String, val parent: NamespaceScope? = null) {
    fun namespace(name: String, block: NamespaceScope.() -> Unit) {
        val scope = NamespaceScope(name, this)
        scope.block()
    }

    fun type(name: String, block: TypeScope.() -> Unit) {
        val scope = TypeScope(name, this)
        scope.block()
    }
}

@AnnotationDsl
class TypeScope(val name: String, val namespace: NamespaceScope) {
    fun method(name: String, block: MethodScope.() -> Unit) {
        val scope = MethodScope(name, this)
        scope.block()
    }
}

@AnnotationDsl
class MethodScope(val name: String, val type: TypeScope) {
    var isPure = false

    fun variant(block: MethodVariant.() -> Unit) {
        val variant = MethodVariant()
        variant.block()
    }
}

@AnnotationDsl
class MethodVariant {
    var isPure = false
    fun parameter(type: String): ParameterDescription = ParameterDescription(type)
}

class ParameterDescription(val type: String)