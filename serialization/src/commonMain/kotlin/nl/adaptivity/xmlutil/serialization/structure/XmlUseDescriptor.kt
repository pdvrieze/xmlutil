/*
 * Copyright (c) 2020.
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

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.PolymorphicKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlCodecBase
import nl.adaptivity.xmlutil.serialization.firstOrNull
import nl.adaptivity.xmlutil.serialization.impl.capturedKClass

interface XmlUseDescriptor {
    val serialName: String
    val qName: QName?
    val tagName: QName
    val typeDescriptor: XmlDescriptor
}


internal class XmlPolymorphicUseDescriptorImpl(
    private val parent: ParentInfo,
    override val serialName: String,
    override val qName: QName?,
    private val codec: XmlCodecBase,
    private val descriptor: SerialDescriptor,
    private val declParent: ParentInfo,
    private val outputKind: OutputKind
                                              ): XmlUseDescriptor {

    override val tagName: QName
        get() = qName ?: typeDescriptor.tagName
    override val typeDescriptor: XmlDescriptor by lazy {

        XmlDescriptor.fromCommon(
            parent,
            descriptor,
            codec,
            XmlSerializationPolicy.NameInfo(serialName, qName),
            declParent,
            outputKind,
            emptyList(),
            null
                                )
    }

}

class XmlUseDescriptorImpl internal constructor(
    val parent: ParentInfo,
    val index: Int,
    private val codec: XmlCodecBase,
    private val overrideOutputKind: OutputKind? = null,
    val declParent: ParentInfo,
    val useAnnotations: Collection<Annotation>
                                               ): XmlUseDescriptor {
    private val serialDescriptor get() = parent.getElementSerialDescriptor(index)

    override val serialName = run {
        val parent = parent.descriptor
        when {
            parent is XmlListDescriptor && parent.anonymous -> parent.tagName.localPart // TODO use declUseParent
            else                                            -> parent.serialDescriptor.getElementName(index)
        }
    }

    val serialKind get() = serialDescriptor.kind
    override val qName: QName? = when (parent.descriptor.serialKind) {
        StructureKind.LIST -> with(parent.descriptor as XmlListDescriptor){
            when {
                childrenName == null && anonymous -> useAnnotations.firstOrNull<XmlSerialName>()?.toQName()

                else                              -> childrenName
            }
        }
        is PolymorphicKind ->  null
        else               -> parent.getElementAnnotations().firstOrNull<XmlSerialName>()?.toQName()
    }
    val elementsCount get()= serialDescriptor.elementsCount

    fun getElementDescriptor(index:Int) = typeDescriptor.getElementDescriptor(index)

    override val tagName: QName
        get() = typeDescriptor.tagName

    override val typeDescriptor: XmlDescriptor by lazy {
        val baseClass = serialDescriptor.capturedKClass(codec.context)

        val declParent: ParentInfo = run {
            val parent2 = parent.descriptor
            when (parent2) {
                is XmlListDescriptor        -> parent2.declParent
                is XmlPolymorphicDescriptor -> parent2.declParent
                else                        -> parent
            }
        }

        val useAnnotations2 = declParent.getElementAnnotations()

        val useName = XmlSerializationPolicy.NameInfo(serialName, qName)

        XmlDescriptor.fromCommon(
            parent,
            serialDescriptor,
            codec,
            useName,
            declParent,
            overrideOutputKind,
            useAnnotations,
            baseClass
                                )
    }
}

