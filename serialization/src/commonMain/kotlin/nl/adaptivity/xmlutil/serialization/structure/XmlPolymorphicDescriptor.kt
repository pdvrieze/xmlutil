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
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.descriptors.getPolymorphicDescriptors
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XML.XmlCodecConfig
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.ActualNameInfo
import nl.adaptivity.xmlutil.serialization.impl.maybeSerialName
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
public class XmlPolymorphicDescriptor : XmlValueDescriptor {

    internal constructor(
        codecConfig: XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        defaultPreserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent) {
        this.defaultPreserveSpace = defaultPreserveSpace
        this.codecConfig = codecConfig

        val policy = codecConfig.config.policy
        outputKind = policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute = false)

        val polyAttrName = policy.polymorphicDiscriminatorName(serializerParent, tagParent)
        polymorphicMode = when {
            policy.isTransparentPolymorphic(serializerParent, tagParent) ->
                PolymorphicMode.TRANSPARENT

            polyAttrName == null -> PolymorphicMode.TAG
            else -> PolymorphicMode.ATTR(polyAttrName)
        }

        val xmlPolyChildren = tagParent.useAnnPolyChildren
        val localPolyInfo = HashMap<String, XmlDescriptor>()
        val localQNameToSerialName = HashMap<QName, String>()
        val wrapperUseName = when (polymorphicMode) {
            PolymorphicMode.TRANSPARENT -> null
            PolymorphicMode.TAG -> from(
                codecConfig,
                ParentInfo(codecConfig.config, this, 1), canBeAttribute = false
            ).tagName.let { XmlSerializationPolicy.DeclaredNameInfo(it) }

            is PolymorphicMode.ATTR -> XmlSerializationPolicy.DeclaredNameInfo(tagName)
        }

        when {
            // If the [XmlPolyChildren] annotation is present, use that
            xmlPolyChildren != null -> {
                val baseName = ActualNameInfo(
                    tagParent.descriptor?.serialDescriptor?.serialName ?: "",
                    tagParent.descriptor?.tagName ?: QName("", "")
                )
                val currentPkg = baseName.serialName.substringBeforeLast('.', "")

                val baseClass = serialDescriptor.capturedKClass ?: Any::class

                for (polyChild in xmlPolyChildren.value) {
                    val childInfo =
                        polyTagName(codecConfig, currentPkg, baseName, polyChild, baseClass)

                    val childSerializerParent = PolymorphicParentInfo(
                        parentDescriptor = this,
                        elementTypeDescriptor = childInfo.elementTypeDescriptor,
                        elementUseNameInfo = childInfo.useNameInfo,
                        elementUseOutputKind = OutputKind.Element,
                    )

                    val xmlDescriptor = from(codecConfig, childSerializerParent, tagParent, canBeAttribute = false)
                    localPolyInfo[childInfo.describedName] = xmlDescriptor
                    val qName = policy.typeQName(xmlDescriptor).normalize()
                    localQNameToSerialName[qName] = childInfo.describedName
                }
            }

            serialDescriptor.kind == PolymorphicKind.SEALED -> {
                // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                val d = serialDescriptor.getElementDescriptor(1)
                for (i in 0 until d.elementsCount) {
                    val childTypeDescriptor =
                        codecConfig.config.lookupTypeDesc(tagParent.namespace, d.getElementDescriptor(i))
                    val childDesc = d.getElementDescriptor(i)
                    val childSerializerParent = PolymorphicParentInfo(
                        parentDescriptor = this,
                        elementTypeDescriptor = childTypeDescriptor,
                        elementUseNameInfo = wrapperUseName ?: XmlSerializationPolicy.DeclaredNameInfo("value"),
                        elementUseOutputKind = outputKind,
                    )

                    val xmlDescriptor = from(codecConfig, childSerializerParent, tagParent, canBeAttribute = false)
                    var cd = xmlDescriptor
                    while (cd is XmlInlineDescriptor) {
                        cd = cd.getElementDescriptor(0)
                    }
                    val effectiveSerialName = if (cd.outputKind.isTextOrMixed) "kotlin.String" else childDesc.serialName

                    localPolyInfo[effectiveSerialName] = xmlDescriptor
                    val qName = policy.typeQName(xmlDescriptor).normalize()
                    localQNameToSerialName[qName] = childDesc.serialName

                }
            }

            else -> {

                val childDescriptors = codecConfig.serializersModule.getPolymorphicDescriptors(serialDescriptor)

                for (childDesc in childDescriptors) {
                    val childTypeDescriptor = codecConfig.config.lookupTypeDesc(tagParent.namespace, childDesc)

                    val childNameInfo = wrapperUseName ?: childTypeDescriptor.typeNameInfo

                    val childSerializerParent: SafeParentInfo = PolymorphicParentInfo(
                        parentDescriptor = this,
                        elementTypeDescriptor = childTypeDescriptor,
                        elementUseNameInfo = childNameInfo,
                        elementUseOutputKind = outputKind,
                    )

                    val xmlDescriptor = from(codecConfig, childSerializerParent, tagParent, canBeAttribute = false)

                    var cd = xmlDescriptor
                    while (cd is XmlInlineDescriptor) {
                        cd = cd.getElementDescriptor(0)
                    }
                    val effectiveSerialName = if (cd.outputKind.isTextOrMixed) "kotlin.String" else childDesc.serialName

                    localPolyInfo[effectiveSerialName] = xmlDescriptor

                    val qName = policy.typeQName(xmlDescriptor).normalize()
                    localQNameToSerialName[qName] = childDesc.serialName
                }
            }
        }

        polyInfo = localPolyInfo
        typeQNameToSerialName = localQNameToSerialName
    }

    private constructor(
        original: XmlPolymorphicDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { original._tagName },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { original._decoderProperties },
        isCData: Boolean = original.isCData,
        default: String? = original.default,
        defaultPreserveSpace: TypePreserveSpace = original.defaultPreserveSpace,
        codecConfig: XmlCodecConfig = original.codecConfig,
        outputKind: OutputKind = original.outputKind,
        polymorphicMode: PolymorphicMode = original.polymorphicMode,
        polyInfo: Map<String, XmlDescriptor> = original.polyInfo,
        typeQNameToSerialName: Map<QName, String> = original.typeQNameToSerialName,
    ) : super(
        original,
        serializerParent,
        tagParent,
        overriddenSerializer,
        typeDescriptor,
        namespaceDecls,
        tagNameProvider,
        decoderPropertiesProvider,
        isCData,
        default,
    ) {
        this.defaultPreserveSpace = defaultPreserveSpace
        this.codecConfig = codecConfig
        this.outputKind = outputKind
        this.polymorphicMode = polymorphicMode
        this.polyInfo = polyInfo
        this.typeQNameToSerialName = typeQNameToSerialName
    }

    override fun copy(nameProvider: XmlDescriptor.() -> Lazy<QName>): XmlPolymorphicDescriptor {
        return XmlPolymorphicDescriptor(this, tagNameProvider = nameProvider)
    }


    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace

    private val codecConfig: XmlCodecConfig

    override val isIdAttr: Boolean
        get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind

    public val polymorphicMode: PolymorphicMode
    public val isTransparent: Boolean get() = polymorphicMode == PolymorphicMode.TRANSPARENT
    public val polyInfo: Map<String, XmlDescriptor>
    public val typeQNameToSerialName: Map<QName, String>

    @OptIn(ExperimentalSerializationApi::class)
    public val parentSerialName: String? get() =
        tagParent.descriptor?.serialDescriptor?.serialName ?: serialDescriptor.capturedKClass?.maybeSerialName

    private val children by lazy(LazyThreadSafetyMode.PUBLICATION) {
        List(elementsCount) { index ->
            val canBeAttribute = index == 0
            val overrideOutputKind = if (canBeAttribute) OutputKind.Attribute else OutputKind.Element
            val parent = ParentInfo(codecConfig.config, this, index, useOutputKind = overrideOutputKind)

            from(codecConfig, parent, canBeAttribute = canBeAttribute)
        }
    }


    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    public fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
        polyInfo[typeName]?.let { return it }
        throw XmlSerialException("Missing polymorphic information for $typeName")
    }

    public fun getPolymorphicDescriptor(descriptor: SerialDescriptor): XmlDescriptor {
        polyInfo[descriptor.serialName]?.let { return it }

        val typeDescriptor = codecConfig.config.lookupTypeDesc(tagParent.namespace, descriptor)
        val overriddenParentInfo = DetachedParent(tagParent.namespace, typeDescriptor, useNameInfo)
        return from(codecConfig, overriddenParentInfo, tagParent, false)
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
            when {
                isTransparent -> {
                    append(" <~(")
                    for (polyVal in polyInfo.values) {
                        polyVal.toString(this, indent + 4, seen).appendLine(',')
                    }
                }

                else -> {
                    append(" (")
                    append(" <poly> [")
                    for (polyVal in polyInfo.values) {
                        polyVal.toString(this, indent + 4, seen).appendLine(',')
                    }
                    append(']')
                }
            }
        }
    }

    internal fun resolvePolymorphicTypeNameCandidates(name: QName, serializersModule: SerializersModule): List<String> {
        val baseClass = serialDescriptor.capturedKClass
        val directMatches = polyInfo.entries.mapNotNull { (typeName, xmlDesc) ->
            if (xmlDesc.tagName.isEquivalent(name)) return listOf(typeName)
            if (baseClass == null) {
                null
            } else {
                when (serializersModule.getPolymorphic(baseClass, typeName)) {
                    is XmlSerializer -> typeName
                    else -> null
                }
            }
        }
        if (directMatches.isNotEmpty() || baseClass == null) return directMatches
        val defaultMatch = serializersModule.getPolymorphic(baseClass, name.localPart)
        return listOfNotNull(defaultMatch?.descriptor?.serialName)
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlPolymorphicDescriptor

        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (outputKind != other.outputKind) return false
        if (polymorphicMode != other.polymorphicMode) return false
        if (polyInfo != other.polyInfo) return false

        return true
    }

    @Suppress("DuplicatedCode")
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + outputKind.hashCode()
        result = 31 * result + polymorphicMode.hashCode()
        result = 31 * result + polyInfo.hashCode()
        return result
    }

}

private fun polyTagName(
    codecConfig: XmlCodecConfig,
    currentPkg: String,
    parentName: ActualNameInfo,
    polyChildSpecification: String,
    baseClass: KClass<*>
): PolyBaseInfo {
    val serializersModule = codecConfig.serializersModule
    val config = codecConfig.config

    val parentTag = parentName.annotatedName

    val eqPos = polyChildSpecification.indexOf('=')
    val pkgPos: Int
    val typeNameBase: String
    val prefix: String
    val localPart: String

    if (eqPos < 0) {
        typeNameBase = polyChildSpecification
        pkgPos = polyChildSpecification.lastIndexOf('.')
        prefix = parentTag.prefix
        localPart = if (pkgPos < 0) polyChildSpecification else polyChildSpecification.substring(pkgPos + 1)
    } else {
        typeNameBase = polyChildSpecification.substring(0, eqPos).trim()
        pkgPos = polyChildSpecification.lastIndexOf('.', eqPos - 1)
        val prefPos = polyChildSpecification.indexOf(':', eqPos + 1)

        if (prefPos < 0) {
            prefix = parentTag.prefix
            localPart = polyChildSpecification.substring(eqPos + 1).trim()
        } else {
            prefix = polyChildSpecification.substring(eqPos + 1, prefPos).trim()
            localPart = polyChildSpecification.substring(prefPos + 1).trim()
        }
    }

    val typename = when {
        pkgPos != 0 || currentPkg.isEmpty() -> typeNameBase

        else -> "$currentPkg.${typeNameBase.substring(1)}"
    }

    val parentNamespace: XmlEvent.NamespaceImpl = XmlEvent.NamespaceImpl(prefix, parentTag.namespaceURI)

    @OptIn(ExperimentalSerializationApi::class)
    val descriptor = serializersModule.getPolymorphic(baseClass, typename)?.descriptor
        ?: throw XmlException("Missing descriptor for $typename in the serial context")


    val elementTypeDescriptor = config.formatCache.lookupTypeOrStore(parentNamespace, descriptor) {
        XmlTypeDescriptor(config, descriptor, parentNamespace)
    }

    val name: QName = when {
        eqPos < 0 -> {
            descriptor.declRequestedName(codecConfig, parentNamespace, elementTypeDescriptor.typeAnnXmlSerialName)
        }

        else -> QName(parentTag.namespaceURI, localPart, prefix)
    }
    return PolyBaseInfo(name, elementTypeDescriptor)
}
