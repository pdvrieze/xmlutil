/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.serialization.impl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlOrderConstraint

internal class ShadowPolicy(basePolicy: XmlSerializationPolicy, internal val cache: FormatCache): XmlSerializationPolicy {
    internal val basePolicy: XmlSerializationPolicy = when (basePolicy) {
        is ShadowPolicy -> basePolicy.basePolicy
        else -> basePolicy
    }

    override fun ignoredSerialInfo(message: String) {
        return basePolicy.ignoredSerialInfo(message)
    }

    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: XmlSerializationPolicy.DeclaredNameInfo
    ): QName {
        return basePolicy.effectiveName(serializerParent, tagParent, outputKind, useName)
    }

    override fun isListEluded(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        return basePolicy.isListEluded(serializerParent, tagParent)
    }

    override fun isTransparentPolymorphic(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        return basePolicy.isTransparentPolymorphic(serializerParent, tagParent)
    }

    override fun polymorphicDiscriminatorName(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): QName? {
        return basePolicy.polymorphicDiscriminatorName(serializerParent, tagParent)
    }

    override fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName {
        return basePolicy.serialNameToQName(serialName, parentNamespace)
    }

    override fun effectiveOutputKind(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): OutputKind {
        return basePolicy.effectiveOutputKind(serializerParent, tagParent)
    }

    override fun handleUnknownContent(
        input: XmlReader,
        inputKind: InputKind,
        name: QName?,
        candidates: Collection<Any>
    ) {
        return basePolicy.handleUnknownContent(input, inputKind, name, candidates)
    }

    override fun shouldEncodeElementDefault(elementDescriptor: XmlDescriptor?): Boolean {
        return basePolicy.shouldEncodeElementDefault(elementDescriptor)
    }

    override val defaultPrimitiveOutputKind: OutputKind
        get() = basePolicy.defaultPrimitiveOutputKind
    override val defaultObjectOutputKind: OutputKind
        get() = basePolicy.defaultObjectOutputKind
    override val isStrictNames: Boolean
        get() = basePolicy.isStrictNames
    override val isStrictAttributeNames: Boolean
        get() = basePolicy.isStrictAttributeNames
    override val isStrictBoolean: Boolean
        get() = basePolicy.isStrictBoolean
    override val isStrictOtherAttributes: Boolean
        get() = basePolicy.isStrictOtherAttributes

    @ExperimentalXmlUtilApi
    override val verifyElementOrder: Boolean
        get() = basePolicy.verifyElementOrder

    @ExperimentalXmlUtilApi
    override fun defaultOutputKind(serialKind: SerialKind): OutputKind {
        return basePolicy.defaultOutputKind(serialKind)
    }

    override fun invalidOutputKind(message: String) {
        basePolicy.invalidOutputKind(message)
    }

    override fun serialTypeNameToQName(
        typeNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName {
        return basePolicy.serialTypeNameToQName(typeNameInfo, parentNamespace)
    }

    override fun serialUseNameToQName(
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName {
        return basePolicy.serialUseNameToQName(useNameInfo, parentNamespace)
    }

    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
    ): OutputKind {
        return basePolicy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)
    }

    override fun overrideSerializerOrNull(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): KSerializer<*>? {
        return basePolicy.overrideSerializerOrNull(serializerParent, tagParent)
    }

    @ExperimentalXmlUtilApi
    override fun handleUnknownContentRecovering(
        input: XmlReader,
        inputKind: InputKind,
        descriptor: XmlDescriptor,
        name: QName?,
        candidates: Collection<Any>
    ): List<XML.ParsedData<*>> {
        return basePolicy.handleUnknownContentRecovering(input, inputKind, descriptor, name, candidates)
    }

    override fun onElementRepeated(parentDescriptor: XmlDescriptor, childIndex: Int) {
        basePolicy.onElementRepeated(parentDescriptor, childIndex)
    }

    override fun handleAttributeOrderConflict(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind
    ): OutputKind {
        return basePolicy.handleAttributeOrderConflict(serializerParent, tagParent, outputKind)
    }

    override fun initialChildReorderMap(parentDescriptor: SerialDescriptor): Collection<XmlOrderConstraint>? {
        return basePolicy.initialChildReorderMap(parentDescriptor)
    }

    override fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
    ): Collection<XmlOrderConstraint> {
        return basePolicy.updateReorderMap(original, children)
    }

    override fun enumEncoding(enumDescriptor: SerialDescriptor, index: Int): String {
        return basePolicy.enumEncoding(enumDescriptor, index)
    }

    @ExperimentalXmlUtilApi
    override fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        return basePolicy.preserveSpace(serializerParent, tagParent)
    }

    override fun mapKeyName(serializerParent: SafeParentInfo): XmlSerializationPolicy.DeclaredNameInfo {
        return basePolicy.mapKeyName(serializerParent)
    }

    override fun mapValueName(
        serializerParent: SafeParentInfo,
        isListEluded: Boolean
    ): XmlSerializationPolicy.DeclaredNameInfo {
        return basePolicy.mapValueName(serializerParent, isListEluded)
    }

    override fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName {
        return basePolicy.mapEntryName(serializerParent, isListEluded)
    }

    override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
        return basePolicy.isMapValueCollapsed(mapParent, valueDescriptor)
    }

    @ExperimentalXmlUtilApi
    override fun elementNamespaceDecls(serializerParent: SafeParentInfo): List<Namespace> {
        return basePolicy.elementNamespaceDecls(serializerParent)
    }

    @ExperimentalXmlUtilApi
    override fun attributeListDelimiters(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Array<String> {
        return basePolicy.attributeListDelimiters(serializerParent, tagParent)
    }

    @ExperimentalXmlUtilApi
    override fun textListDelimiters(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Array<String> {
        return basePolicy.textListDelimiters(serializerParent, tagParent)
    }
}
