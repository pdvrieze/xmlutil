/*
 * Copyright (c) 2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.serialization.structure.*
import kotlin.jvm.JvmStatic

/**
 * Opaque caching class that allows for caching format related data (to speed up reuse). This is
 * intended to be stored on the config, thus reused through multiple serializations.
 * Note that this requires the `serialName` attribute of `SerialDescriptor` instances to be unique.
 */
public class TestFormatCache : FormatCache() {
    private val typeDescCache = HashMap<TypeKey, XmlTypeDescriptor>()
    private val elemDescCache = HashMap<DescKey, XmlDescriptor>()
    private val pendingDescs = HashSet<DescKey>()
    private val seenSerialDescs = HashMap<String, MutableList<ListKey>>()

    override fun copy(): TestFormatCache = TestFormatCache()

    override fun unsafeCache(): TestFormatCache {
        return this
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun lookupType(
        namespace: Namespace?,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return lookupType(TypeKey(namespace?.namespaceURI, serialDesc), serialDesc.kind, defaultValue)
    }

    /**
     * Lookup a type descriptor for this type with the given namespace.
     * @param parentName A key
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun lookupType(
        parentName: QName,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return lookupType(TypeKey(parentName.namespaceURI, serialDesc), serialDesc.kind, defaultValue)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun lookupType(name: TypeKey, kind: SerialKind, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return when (kind) {
            StructureKind.MAP,
            StructureKind.LIST -> defaultValue()

            else -> typeDescCache.getOrPut(name, defaultValue)
        }
    }

    override fun lookupDescriptor(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor {
        val key =
            DescKey(overridenSerializer, serializerParent, tagParent.takeIf { it !== serializerParent }, canBeAttribute)

        @OptIn(ExperimentalSerializationApi::class)
        check(pendingDescs.add(key)) {
            "Recursive lookup of ${serializerParent.elementSerialDescriptor.serialName} with key: $key"
        }

        // This has to be getOrPut rather than `computeIfAbsent` as computeIfAbsent prevents other
        // changes to different types. GetOrPut does not have that property (but is technically slower)
        return elemDescCache.getOrPut(key) {
            val sn = key.childDescriptor.serialKeyName()
            val descList = seenSerialDescs.getOrPut(sn) { ArrayList() }
            descList.add(ListKey(serializerParent.descriptor?.serialDescriptor?.serialName ?: "<root>", key))
            if(descList.size > 1) {
                println("LOOKUP_DESCRIPTOR ($sn): Duplicate cache entries (n=${descList.size}) for serial descriptor:\n  ${descList.joinToString(",\n    ")}")
            }
            defaultValue()
        }.also {
            pendingDescs.remove(key)
        }
    }

    override fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace
    ): XmlCompositeDescriptor {
        return XmlCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }

    /**
     * @property overridenSerializer If the serializer is different, this changes the key
     * @property parentNamespace If the parent has a different namespace, this may change the name
     * @property effectiveUseNameInfo
     */
    internal data class DescKey(
        val overridenSerializer: KSerializer<*>?,
        val parentNamespace: String?,
        val effectiveUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        val useAnnotations: Set<Annotation>,
        val canBeAttribute: Boolean,
        val childDescriptor: SerialDescriptor
    ) {
        constructor(
            overridenSerializer: KSerializer<*>?,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo?,
            canBeAttribute: Boolean
        ) : this(
            overridenSerializer = overridenSerializer,
            parentNamespace = (tagParent ?: serializerParent).namespace.namespaceURI,
            effectiveUseNameInfo = serializerParent.elementUseNameInfo,
            useAnnotations = (serializerParent.elementUseAnnotations as? Set) ?: serializerParent.elementUseAnnotations.toHashSet(),
            canBeAttribute = canBeAttribute,
            childDescriptor = serializerParent.elementSerialDescriptor
        )

        private fun createHashCode(): Int {
            var result = canBeAttribute.hashCode()
            result = 31 * result + (overridenSerializer?.hashCode() ?: 0)
            result = 31 * result + (parentNamespace?.hashCode() ?: 0)
            result = 31 * result + childDescriptor.hashCode()
            result = 31 * result + effectiveUseNameInfo.hashCode()
            result = 31 * result + useAnnotations.hashCode()
            result = 31 * result + hashcode.hashCode()
            return result
        }

        private val hashcode = createHashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as DescKey

            if (canBeAttribute != other.canBeAttribute) return false
            if (overridenSerializer != other.overridenSerializer) return false
            if (parentNamespace != other.parentNamespace) return false
            if (childDescriptor != other.childDescriptor) return false
            if (effectiveUseNameInfo != other.effectiveUseNameInfo) return false
            if (useAnnotations != other.useAnnotations) return false
            if (hashcode != other.hashcode) return false

            for (i in 0 until childDescriptor.elementsCount) {
                if (childDescriptor.getElementName(i) != other.childDescriptor.getElementName(i)) return false
            }

            return true
        }

        @Suppress("DuplicatedCode")
        override fun hashCode(): Int {
            var result = canBeAttribute.hashCode()
            result = 31 * result + (overridenSerializer?.hashCode() ?: 0)
            result = 31 * result + (parentNamespace?.hashCode() ?: 0)
            result = 31 * result + effectiveUseNameInfo.hashCode()
            result = 31 * result + useAnnotations.hashCode()
            result = 31 * result + hashcode.hashCode()
            return result
        }


    }

    @Suppress("EqualsOrHashCode")
    private data class TypeKey(val namespace: String, val descriptor: SerialDescriptor) {
        /**
         * Note that the default hash key implementation is good enough. But equality needs
         * a special case to also check element names in the rare case of duplicate serial names
         * with differently named children (with the same types). Considering this in the hash
         * key puts a burden on code that does not incorrectly duplicate the serial name of a type.
         */
        override fun equals(other: Any?): Boolean {
            return when {
                other !is TypeKey -> false
                namespace != other.namespace -> false
                descriptor != other.descriptor -> false
                (0 until descriptor.elementsCount).any {
                    descriptor.getElementName(it) != other.descriptor.getElementName(
                        it
                    )
                } -> false

                else -> true
            }
        }
    }

    private data class ListKey(val parentName: String, val key: DescKey)

    private companion object {
        @JvmStatic
        private fun TypeKey(namespace: String?, descriptor: SerialDescriptor) =
            TestFormatCache.TypeKey(namespace ?: "", descriptor)
    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.serialKeyName(): String = when(kind) {
    is PrimitiveKind -> "Primitive<$serialName>"
    SerialKind.ENUM -> "Enum<$serialName>"
    SerialKind.CONTEXTUAL -> "Contextual<$serialName>"
    PolymorphicKind.OPEN -> "Poly<$serialName>"
    PolymorphicKind.SEALED -> "Sealed<$serialName>"
    StructureKind.CLASS -> serialName
    StructureKind.OBJECT -> "Object<$serialName>"
    StructureKind.LIST -> "List<${getElementDescriptor(1).serialKeyName()}>"
    StructureKind.MAP -> "Map<${getElementDescriptor(0).serialKeyName()}, ${getElementDescriptor(1).serialKeyName()}>)}"
}
