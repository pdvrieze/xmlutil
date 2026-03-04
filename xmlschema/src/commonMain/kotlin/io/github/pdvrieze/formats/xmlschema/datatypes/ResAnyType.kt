/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIAssertCommon
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.*
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants

object ResAnyType : ResolvedGlobalComplexType(
    name = XsdQName(XMLConstants.XSD_NS_URI, "anyType"),
    schema = BuiltinSchemaXmlschema,
    modelFactory = { AnyModel },
), ResolvedBuiltinType, AnyType {
    override val mdlQName: XsdQName get() = super<ResolvedBuiltinType>.mdlQName

    override val id: Nothing? get() = null
    override val otherAttrs: Map<SerializableQName, Nothing> get() = emptyMap()
    override val mdlAnnotations: List<ResolvedAnnotation> get() = emptyList()

    override val mdlFinal: Set<VDerivationControl.Complex> get() = emptySet()
    override val isSpecial: Boolean get() = true

    //    override val final: Set<Nothing> get() = emptySet()
    override val model: AnyModel get() = AnyModel

    override val baseType: ResAnyType get() = this

    val urWildcard: ResolvedAny = ResolvedAny(
        VNamespaceConstraint(VNamespaceConstraint.Variety.ANY, emptySet(), VQNameList()),
        VProcessContents.LAX
    )

    override fun validate(representation: VString, version: SchemaVersion) {
//        error("anyType cannot be directly implemented")
    }

    override fun checkType(checkHelper: CheckHelper) {}

    override fun toString(): String = "xsd:anyType"

    object AnyModel : ResolvedAnnotated.EmptyModel(), Model {
        override val mdlBaseTypeDefinition: ResolvedType get() = ResAnyType
        override val mdlAssertions: List<XSIAssertCommon> get() = emptyList()

        override val mdlFinal: Set<VDerivationControl.Complex> get() = emptySet()
        override val mdlContentType: MixedContentType = MixedContentType(
            SyntheticSequence(
                VNonNegativeInteger.Companion.ZERO, VAllNNI.UNBOUNDED,
                listOf(urWildcard)
            ),
            { false },
            schema = schema
        )

        override val mdlDerivationMethod: VDerivationControl.Complex get() = VDerivationControl.RESTRICTION

        override val mdlAttributeWildcard: ResolvedAnyAttribute = ResolvedAnyAttribute(
            VNamespaceConstraint(VNamespaceConstraint.Variety.ANY, emptySet(), VAttrQNameList()),
            VProcessContents.LAX
        )

        override val hasLocalNsInContext: Boolean
            get() = false

        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> get() = emptySet()
        override val mdlAttributeUses: Map<XsdQName, IResolvedAttributeUse> get() = emptyMap()
    }
}
