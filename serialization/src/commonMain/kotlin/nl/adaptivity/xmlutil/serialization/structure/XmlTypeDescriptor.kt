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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.impl.multiplatform.maybeAnnotations
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.toNamespace

public class XmlTypeDescriptor internal constructor(
    config: XmlConfig,
    public val serialDescriptor: SerialDescriptor,
    parentNamespace: Namespace?
) {
    /** Value of the [XmlNamespaceDeclSpec] annotation */
    @ExperimentalXmlUtilApi
    public var typeAnnNsDecls: List<Namespace>? = null
        private set

    /** Value of the [XmlNamespaceDeclSpec] annotation */
    @ExperimentalXmlUtilApi
    public var typeAnnXmlSerialName: XmlSerialName? = null
        private set

    /** Value of the [XmlCData] annotation */
    @ExperimentalXmlUtilApi
    public var typeAnnCData: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public var typeAnnIsXmlValue: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public var typeAnnIsId: Boolean = false
        private set

    @ExperimentalXmlUtilApi
    public var typeAnnIsElement: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public var typeAnnChildrenName: XmlChildrenName? = null
        private set

    @ExperimentalXmlUtilApi
    public var typeAnnPolyChildren: XmlPolyChildren? = null
        private set

    init {
        @OptIn(ExperimentalSerializationApi::class)
        for (a in serialDescriptor.annotations) {
            when (a) {
                is XmlNamespaceDeclSpec -> typeAnnNsDecls = a.namespaces
                is XmlSerialName -> typeAnnXmlSerialName = a
                is XmlCData -> typeAnnCData = a.value
                is XmlValue -> typeAnnIsXmlValue = a.value
                is XmlId -> typeAnnIsId = true
                is XmlElement -> typeAnnIsElement = a.value
                is XmlChildrenName -> typeAnnChildrenName = a
                is XmlPolyChildren -> typeAnnPolyChildren = a
            }
        }
        if (typeAnnXmlSerialName==null) {
            @OptIn(ExperimentalSerializationApi::class)
            typeAnnXmlSerialName = serialDescriptor.capturedKClass?.maybeAnnotations?.firstOrNull<XmlSerialName>()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    public val typeNameInfo: DeclaredNameInfo = serialDescriptor.getNameInfo(
        config,
        parentNamespace,
        typeAnnXmlSerialName
    )

    @OptIn(ExperimentalSerializationApi::class)
    public val serialName: String
        get() = serialDescriptor.serialName

    public val typeQname: QName? get() = typeNameInfo.annotatedName

    @OptIn(ExperimentalSerializationApi::class)
    public val elementsCount: Int
        get() = serialDescriptor.elementsCount

    internal val initialChildReorderInfo: Collection<XmlOrderConstraint>? by lazy {
        config.policy.initialChildReorderMap(serialDescriptor)
    }


    public operator fun get(index: Int): XmlTypeDescriptor {
        return children[index]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XmlTypeDescriptor

        return when {
            typeNameInfo != other.typeNameInfo -> false
            serialDescriptor != other.serialDescriptor -> false
            (0 until serialDescriptor.elementsCount).any {
                serialDescriptor.getElementName(it) != other.serialDescriptor.getElementName(
                    it
                )
            } -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        var result = serialDescriptor.hashCode()
        result = 31 * result + typeNameInfo.hashCode()
        return result
    }

    private val children by lazy {
        @OptIn(ExperimentalSerializationApi::class)
        Array(serialDescriptor.elementsCount) { idx ->
            val desc = serialDescriptor.getElementDescriptor(idx).getXmlOverride()
            val ns = typeQname?.toNamespace() ?: parentNamespace
            config.formatCache.lookupType(ns, desc) {
                XmlTypeDescriptor(config, desc, ns)
            }
        }
    }

    override fun toString(): String {
        return "TypeDescriptor($typeQname, ${serialDescriptor.kind})"
    }
}
