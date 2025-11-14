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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML

public class XmlAttributeMapDescriptor : XmlValueDescriptor {

    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        defaultPreserveSpace: TypePreserveSpace,
    ) : super(codecConfig, serializerParent, tagParent) {
        this.defaultPreserveSpace = defaultPreserveSpace
        _keyDescriptor = lazy(LazyThreadSafetyMode.PUBLICATION) {
            from(
                codecConfig,
                ParentInfo(codecConfig.config, this, 0, useOutputKind = OutputKind.Text),
                tagParent,
                true,
            )
        }
        _valueDescriptor = lazy(LazyThreadSafetyMode.PUBLICATION) {
            from(
                codecConfig,
                ParentInfo(codecConfig.config, this, 1, useOutputKind = OutputKind.Text),
                tagParent,
                true
            )
        }
    }

    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val isIdAttr: Boolean get() = false

    override val outputKind: OutputKind get() = OutputKind.Attribute

    private val _keyDescriptor: Lazy<XmlDescriptor>

    /**
     * The descriptor for the key type of the map
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val keyDescriptor: XmlDescriptor get() = _keyDescriptor.value

    private val _valueDescriptor: Lazy<XmlDescriptor>
    /**
     * The descriptor for the value type of the map
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val valueDescriptor: XmlDescriptor get() = _valueDescriptor.value

    override val elementsCount: Int get() = 2

    override fun getElementDescriptor(index: Int): XmlDescriptor = when (index % 2) {
        0 -> keyDescriptor
        else -> valueDescriptor
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
                .appendLine(" (")
            appendIndent(indent)
            when {
                _keyDescriptor.isInitialized() -> keyDescriptor.toString(this, indent + 4, seen)
                else -> append("<pending key descriptor>")
            }.appendLine(",")
            appendIndent(indent)

            when {
                _valueDescriptor.isInitialized() -> valueDescriptor.toString(this, indent + 4, seen)
                else -> append("<pending value descriptor>")
            }.append(')')
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlAttributeMapDescriptor

        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (keyDescriptor != other.keyDescriptor) return false
        if (valueDescriptor != other.valueDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + keyDescriptor.hashCode()
        result = 31 * result + valueDescriptor.hashCode()
        return result
    }

}
