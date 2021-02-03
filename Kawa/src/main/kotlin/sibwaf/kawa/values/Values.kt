package sibwaf.kawa.values

import sibwaf.kawa.constraints.Constraint
import spoon.SpoonException
import spoon.reflect.code.CtExpression
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtParameter
import spoon.reflect.declaration.CtTypedElement
import spoon.reflect.factory.TypeFactory
import spoon.reflect.reference.CtTypeReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// TODO: split into multiple files

open class Value constructor(val source: CtElement?) {

    companion object {
        private val typeFactory = TypeFactory()

        private val valueFactoryCache: ConcurrentMap<String, (CtElement?) -> Value> = ConcurrentHashMap()

        fun withoutSource(typeProvider: CtTypedElement<*>): Value {
            return withoutSource(typeProvider.type)
        }

        fun withoutSource(type: CtTypeReference<*>?): Value {
            return from(type, null)
        }

        fun from(source: CtParameter<*>): Value {
            return from(source.type, source)
        }

        fun from(source: CtExpression<*>): Value {
            return from(source.type, source)
        }

        private fun from(type: CtTypeReference<*>?, source: CtElement?): Value {
            if (type == null) {
                return Value(source)
            }

            if (type.isPrimitive) {
                if (type.simpleName == "boolean") {
                    return BooleanValue(source)
                }

                return Value(source)
            }

            val valueFactory = valueFactoryCache.computeIfAbsent(type.qualifiedName) { _ ->
                try {
                    if (type.isSubtypeOf(typeFactory.COLLECTION) || type.isSubtypeOf(typeFactory.MAP)) {
                        return@computeIfAbsent { CollectionValue(it) }
                    }
                } catch (ignored: SpoonException) {
                    // Spoon does Spoon things and sometimes dies on 'isSubtypeOf' invocations,
                    // but we can be sure that it's at least a ReferenceValue
                }

                return@computeIfAbsent { ReferenceValue(it) }
            }

            return valueFactory(source)
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

open class BooleanValue(source: CtElement?) : Value(source) {
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

open class ReferenceValue(source: CtElement?) : Value(source) {
    override fun copy(): ReferenceValue = ReferenceValue(source)
}

//object NullValue : ReferenceValue(ValueSource.NONE) {
//    override fun copy(): NullValue = NullValue
//}

class CollectionValue(source: CtElement?) : Value(source) {
    override fun copy(): CollectionValue = CollectionValue(source)
}

class CompositeValue(values: Iterable<Value>) : Value(null) {
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
        fun withoutSource(typeProvider: CtTypedElement<*>): ConstrainedValue {
            return from(Value.withoutSource(typeProvider))
        }

        fun withoutSource(type: CtTypeReference<*>?): ConstrainedValue {
            return from(Value.withoutSource(type))
        }

        fun from(source: CtParameter<*>): ConstrainedValue {
            return from(Value.from(source))
        }

        fun from(source: CtExpression<*>): ConstrainedValue {
            return from(Value.from(source))
        }

        fun from(value: Value): ConstrainedValue {
            return ConstrainedValue(value, Constraint.from(value))
        }
    }

    operator fun component1() = value
    operator fun component2() = constraint
}
