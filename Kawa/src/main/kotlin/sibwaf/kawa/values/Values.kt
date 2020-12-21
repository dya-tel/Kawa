package sibwaf.kawa.values

import sibwaf.kawa.constraints.Constraint
import spoon.SpoonException
import spoon.reflect.declaration.CtTypedElement
import spoon.reflect.factory.TypeFactory
import spoon.reflect.reference.CtTypeReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

enum class ValueSource {
    PARAMETER, LOCAL_VARIABLE, NONE
}

// TODO: replace ValueSource with CtExpression
// TODO: split into multiple files

open class Value constructor(val source: ValueSource) {

    companion object {
        private val typeFactory = TypeFactory()

        private val valueFactoryCache: ConcurrentMap<String, () -> Value> = ConcurrentHashMap()

        fun from(element: CtTypedElement<*>, source: ValueSource): Value {
            return from(element.type, source)
        }

        fun from(type: CtTypeReference<*>?, source: ValueSource): Value {
            if (type == null) {
                return Value(source)
            }

            if (type.isPrimitive) {
                if (type.simpleName == "boolean") {
                    return BooleanValue(source)
                }

                return Value(source)
            }

            val valueFactory = valueFactoryCache.computeIfAbsent(type.qualifiedName) {
                try {
                    if (type.isSubtypeOf(typeFactory.COLLECTION) || type.isSubtypeOf(typeFactory.MAP)) {
                        return@computeIfAbsent { CollectionValue(source) }
                    }
                } catch (ignored: SpoonException) {
                    // Spoon does Spoon things and sometimes dies on 'isSubtypeOf' invocations,
                    // but we can be sure that it's at least a ReferenceValue
                }

                return@computeIfAbsent { ReferenceValue(source) }
            }

            return valueFactory()
        }
    }

    open fun copy(): Value = Value(source)

    open infix fun isSameAs(other: Value): Boolean {
        return when (other) {
            is CompositeValue -> other.isSameAs(this)
            else -> other === this
        }
    }
}

open class BooleanValue(source: ValueSource) : Value(source) {
    override fun copy(): BooleanValue = BooleanValue(source)
    internal open fun invert(): BooleanValue = InvertedBooleanValue(this)
}

internal class InvertedBooleanValue(private val original: BooleanValue) : BooleanValue(original.source) {
    override fun copy(): InvertedBooleanValue = InvertedBooleanValue(original.copy())
    override fun invert(): BooleanValue = original

    override fun isSameAs(other: Value): Boolean {
        return when (other) {
            is CompositeValue -> other.values.any { it is InvertedBooleanValue && it.original isSameAs original }
            is InvertedBooleanValue -> other.original isSameAs original
            else -> other === this
        }
    }
}

//object FalseValue : BooleanValue(ValueSource.NONE) {
//    override fun copy(): BooleanValue = FalseValue
//    override fun invert(): BooleanValue = TrueValue
//}
//
//object TrueValue : BooleanValue(ValueSource.NONE) {
//    override fun copy(): BooleanValue = TrueValue
//    override fun invert(): BooleanValue = FalseValue
//}

open class ReferenceValue(source: ValueSource) : Value(source) {
    override fun copy(): ReferenceValue = ReferenceValue(source)
}

//object NullValue : ReferenceValue(ValueSource.NONE) {
//    override fun copy(): NullValue = NullValue
//}

class CollectionValue(source: ValueSource) : Value(source) {
    override fun copy(): CollectionValue = CollectionValue(source)
}

class CompositeValue(values: Iterable<Value>) : Value(ValueSource.NONE) {
    val values = values.toSet()

    override fun isSameAs(other: Value): Boolean {
        return when {
            other === this -> return true
            else -> other in values
        }
    }
}

class ConstrainedValue(val value: Value, val constraint: Constraint) {
    companion object {
        fun from(element: CtTypedElement<*>, source: ValueSource): ConstrainedValue {
            return from(element.type, source)
        }

        fun from(type: CtTypeReference<*>?, source: ValueSource): ConstrainedValue {
            val value = Value.from(type, source)
            val constraint = Constraint.from(value)
            return ConstrainedValue(value, constraint)
        }
    }

    operator fun component1() = value
    operator fun component2() = constraint
}
