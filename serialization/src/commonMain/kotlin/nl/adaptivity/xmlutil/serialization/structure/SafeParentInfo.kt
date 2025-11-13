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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.serialization.*

/**
 * Interface that provides parent info that does provide actual access to the child. As such it is safe to
 * be used to determine properties of the child.
 */
@MpJvmDefaultWithCompatibility
public interface SafeParentInfo {
    /** Is the parent type an inline class. */
    public val parentIsInline: Boolean

    /** The index of this element in the parent. */
    public val index: Int

    /** The descriptor of the parent (if available - not for the root). */
    public val descriptor: SafeXmlDescriptor?

    /** The descriptor of the type of this element (independent of use). */
    public val elementTypeDescriptor: XmlTypeDescriptor

    /** The information on use site requirements */
    public val elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo

    /** Annotations on the property, not type */
    public val elementUseAnnotations: Collection<Annotation>

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlSerialName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnXmlSerialName: XmlSerialName? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlElement] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsElement: Boolean? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlValue] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsValue: Boolean? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlPolyChildren] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnPolyChildren: XmlPolyChildren? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlIgnoreWhitespace] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIgnoreWhitespace: Boolean? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlChildrenName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnChildrenName: XmlChildrenName? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlKeyName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnKeyName: XmlKeyName? get() = null

    /** Value of the [XmlKeyName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnMapEntryName: XmlMapEntryName? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlCData] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnCData: Boolean? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlId] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsId: Boolean get() = false

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlOtherAttributes] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsOtherAttributes: Boolean get() = false

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlDefault] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnDefault: String? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlBefore] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnBefore: Array<out String>? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlAfter] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnAfter: Array<out String>? get() = null

    /** Value of the [nl.adaptivity.xmlutil.serialization.XmlNamespaceDeclSpecs] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnNsDecls: List<Namespace>? get() = null

    /** The raw serial descriptor of the element*/
    public val elementSerialDescriptor: SerialDescriptor
        get() = elementTypeDescriptor.serialDescriptor

    /** Overidden serializer of the element*/
    public val overriddenSerializer: KSerializer<*>?

    /** Type requirements derived from the use site */
    public val elementUseOutputKind: OutputKind?

    /** The namespace this element has */
    public val namespace: Namespace

    public fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>? = this.overriddenSerializer
    ): SafeParentInfo

    public fun maybeOverrideSerializer(config: XmlConfig, overriddenSerializer: KSerializer<*>?): SafeParentInfo =
        when (overriddenSerializer) {
            null -> this
            else -> copy(config = config, overriddenSerializer = overriddenSerializer)
        }
}
