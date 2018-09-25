package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.*
import nl.adaptivity.xmlutil.serialization.compat.PrimitiveKind
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.compat.SerialKind
import nl.adaptivity.xmlutil.serialization.compat.StructureKind

enum class ChildType(private val serializer: KSerializer<*>?, val serialKind: SerialKind) {
    DOUBLE(DoubleSerializer,
           PrimitiveKind.DOUBLE),
    INT(IntSerializer, PrimitiveKind.INT),
    FLOAT(FloatSerializer,
          PrimitiveKind.FLOAT),
    STRING(StringSerializer,
           PrimitiveKind.STRING),
    UNKNOWN(null, StructureKind.CLASS),
    BOOLEAN(BooleanSerializer,
            PrimitiveKind.BOOLEAN),
    BYTE(BooleanSerializer,
         PrimitiveKind.BYTE),
    UNIT(UnitSerializer, PrimitiveKind.UNIT),
    CHAR(CharSerializer, PrimitiveKind.CHAR),
    ENUM(null, PrimitiveKind.ENUM),
    LONG(LongSerializer, PrimitiveKind.LONG),
    NONSERIALIZABLE(null, StructureKind.CLASS),
    SHORT(ShortSerializer,
          PrimitiveKind.SHORT),
    CLASS(null, StructureKind.CLASS);

    val isPrimitive
        get() = when(this) {
            ChildType.UNKNOWN,
            ChildType.UNIT,
            ChildType.CLASS,
            ChildType.NONSERIALIZABLE -> false
            else                      -> true
        }

    val primitiveSerializer: KSerializer<*>
        get() = serializer ?:
                throw UnsupportedOperationException("The type is not a primitive")

    // Create this lazilly as it is unique but only exists for primitives
    val primitiveSerialDescriptor: SerialDescriptor by lazy {
        ExtSerialDescriptor(primitiveSerializer.serialClassDesc, serialKind, emptyArray())
    }
}