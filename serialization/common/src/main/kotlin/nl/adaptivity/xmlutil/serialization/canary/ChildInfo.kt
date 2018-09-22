/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.KSerialClassKind
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.*
import nl.adaptivity.xmlutil.serialization.compat.PrimitiveKind
import nl.adaptivity.xmlutil.serialization.compat.SerialKind
import nl.adaptivity.xmlutil.serialization.compat.StructureKind
import kotlin.reflect.KClass

data class ChildInfo(val name: String,
                     val useAnnotations: List<Annotation> = emptyList(),
                     var classAnnotations: List<Annotation>,
                     override var kind: KSerialClassKind? = null,
                     var childCount: Int = 0,
                     override var type: ChildType = ChildType.UNKNOWN,
                     override var isNullable: Boolean = false) : BaseInfo {

    override fun toString(): String {
        return "ChildInfo(name='$name', useAnnotations=$useAnnotations, classAnnotations=$classAnnotations, kind=$kind, childCount=$childCount, type=$type, isNullable=$isNullable)"
    }

    inline fun <reified T> findAnnotation():T? {
        for (e in useAnnotations) {
            if (e is T) return e
        }
        for (e in classAnnotations) {
            if (e is T) return e
        }
        return null
    }

    fun <T:Annotation> findAnnotation(klass: KClass<T>): T? {
        for (e in useAnnotations) {
            @Suppress("UNCHECKED_CAST")
            if (klass.isInstance(e)) return e as T
        }
        for (e in classAnnotations) {
            @Suppress("UNCHECKED_CAST")
            if (klass.isInstance(e)) return e as T
        }
        return null
    }
}


enum class ChildType(private val serializer: KSerializer<*>?, val serialKind: SerialKind) {
    DOUBLE(DoubleSerializer, PrimitiveKind.DOUBLE),
    INT(IntSerializer, PrimitiveKind.INT),
    FLOAT(FloatSerializer, PrimitiveKind.FLOAT),
    STRING(StringSerializer, PrimitiveKind.STRING),
    UNKNOWN(null, StructureKind.CLASS),
    BOOLEAN(BooleanSerializer, PrimitiveKind.BOOLEAN),
    BYTE(BooleanSerializer, PrimitiveKind.BYTE),
    UNIT(UnitSerializer, PrimitiveKind.UNIT),
    CHAR(CharSerializer, PrimitiveKind.CHAR),
    ENUM(null, PrimitiveKind.ENUM),
    LONG(LongSerializer, PrimitiveKind.LONG),
    NONSERIALIZABLE(null, StructureKind.CLASS),
    SHORT(ShortSerializer, PrimitiveKind.SHORT),
    @Deprecated("Don't use this, it is unclear")
    ELEMENT(null, StructureKind.CLASS);

    val isPrimitive
        get() = when(this) {
            ChildType.UNKNOWN,
            ChildType.UNIT,
            ChildType.ELEMENT,
            ChildType.NONSERIALIZABLE -> false
            else            -> true
        }

    val primitiveSerializer: KSerializer<*> get() = serializer!!

}