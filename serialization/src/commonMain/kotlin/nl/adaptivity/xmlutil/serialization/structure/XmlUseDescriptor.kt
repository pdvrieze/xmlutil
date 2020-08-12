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
    private val parent: XmlDescriptor,
    override val serialName: String,
    override val qName: QName?,
    private val codec: XmlCodecBase,
    private val descriptor: SerialDescriptor,
    private val declParent: XmlDescriptor,
    private val outputKind: OutputKind
                                              ): XmlUseDescriptor {

    override val tagName: QName
        get() = qName ?: typeDescriptor.tagName
    override val typeDescriptor: XmlDescriptor by lazy {

        XmlDescriptor.fromCommon(descriptor, codec, XmlSerializationPolicy.NameInfo(serialName, qName), declParent, outputKind, emptyList(), null)
    }

}

class XmlUseDescriptorImpl internal constructor(
    val parent: XmlDescriptor,
    val index: Int,
    private val codec: XmlCodecBase,
    private val overrideOutputKind: OutputKind? = null,
    val declParent: XmlDescriptor,
    val useAnnotations: Collection<Annotation>
                                               ): XmlUseDescriptor {
    private val serialDescriptor get() = parent.serialDescriptor.getElementDescriptor(index)

    override val serialName = when {
        parent is XmlListDescriptor && parent.anonymous -> parent.tagName.localPart // TODO use declUseParent
        else -> parent.serialDescriptor.getElementName(index)
    }

    val serialKind get() = serialDescriptor.kind
    override val qName: QName? = when (parent.serialKind) {
        StructureKind.LIST -> with(parent as XmlListDescriptor){
            when {
                childrenName == null && anonymous -> useAnnotations.firstOrNull<XmlSerialName>()?.toQName()

                else                              -> childrenName
            }
        }
        is PolymorphicKind ->  null
        else               -> parent.serialDescriptor.getElementAnnotations(index).firstOrNull<XmlSerialName>()?.toQName()
    }
    val elementsCount get()= serialDescriptor.elementsCount

    fun getElementDescriptor(index:Int) = typeDescriptor.getElementDescriptor(index)

    override val tagName: QName
        get() = typeDescriptor.tagName

    override val typeDescriptor: XmlDescriptor by lazy {
        val baseClass = serialDescriptor.capturedKClass(codec.context)

        val declParent = when(parent) {
            is XmlListDescriptor -> parent.declParent
            is XmlPolymorphicDescriptor -> parent.declParent
            else -> parent
        }

        val useAnnotations2 = declParent.serialDescriptor.getElementAnnotations(index)

        val useName = XmlSerializationPolicy.NameInfo(serialName, qName)

        XmlDescriptor.fromCommon(
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

