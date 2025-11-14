/*
 * Copyright (c) 2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy

public class XmlRootDescriptor : XmlDescriptor {

    private val _element: Lazy<XmlDescriptor>

    // TODO get rid of coded, put policy in its place
    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        descriptor: SerialDescriptor,
        tagName: XmlSerializationPolicy.DeclaredNameInfo
    ) : super(codecConfig, DetachedParent(codecConfig, descriptor, tagName, true)) {
        _element = lazy(LazyThreadSafetyMode.PUBLICATION) {
            from(codecConfig, tagParent, canBeAttribute = false)
        }
    }

    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        descriptor: SerialDescriptor,
    ) : this(codecConfig, descriptor, XmlSerializationPolicy.DeclaredNameInfo(descriptor))

    private constructor(
        original: XmlRootDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo = original.useNameInfo,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { lazyOf(original.tagName) },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { original._decoderProperties },
        element: Lazy<XmlDescriptor> = original._element
    ) : super(
        original,
        serializerParent,
        tagParent,
        overriddenSerializer,
        useNameInfo,
        typeDescriptor,
        namespaceDecls,
        tagNameProvider,
        decoderPropertiesProvider,
    ) {
        _element = element
    }


    private val element: XmlDescriptor get() = _element.value

    override val isIdAttr: Boolean get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true // effectively a root descriptor is inline

    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace
        get() = element.defaultPreserveSpace

    override val tagName: QName
        get() {
            val useNameInfo = useNameInfo
            return useNameInfo.annotatedName ?: element.tagName
        }

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IndexOutOfBoundsException("There is exactly one child to a root tag")

        return element
    }

    override val elementsCount: Int get() = 1

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append("<root>(")
            getElementDescriptor(0).appendTo(builder, indent + 4, seen)
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlRootDescriptor

        return element == other.element
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + element.serialDescriptor.hashCode()
        return result
    }

}
