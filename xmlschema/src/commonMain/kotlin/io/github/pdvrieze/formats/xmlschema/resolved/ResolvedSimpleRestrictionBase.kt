/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyAtomicType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.util.CompactFragment

abstract class ResolvedSimpleRestrictionBase(
    rawPart: XSSimpleRestriction?,
) : ResolvedSimpleType.Derivation() {

    abstract override val model: IModel

    override val baseType: ResolvedType get() = model.baseType

    open val otherContents: List<CompactFragment> get() = model.otherContents

    init {
        if (rawPart != null) {
            if (rawPart.base == null) {
                requireNotNull(rawPart.simpleType)
            } else {
                require(rawPart.simpleType == null)
            }
            for (c in rawPart.otherContents) {
                val r = c.getXmlReader()
                while (r.hasNext()) {
                    when (r.nextTag()) {
                        EventType.START_ELEMENT -> {
                            val n = r.name
                            require(n.namespaceURI != XSD_NS_URI) { "Simple types can not contain XMLSchema namespace elements as free content" }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    override fun checkDerivation(checkHelper: CheckHelper) {
        checkAnnotated(checkHelper.version)
        checkHelper.checkType(baseType)
    }

    interface IModel : ResolvedAnnotated.IModel {
        val baseType: ResolvedType
        val facets: FacetList
        val otherContents: List<CompactFragment>
    }

    class Model : ResolvedAnnotated.Model, IModel {
        override val baseType: ResolvedType
        override val facets: FacetList

        override val otherContents: List<CompactFragment>

        internal constructor(
            baseType: ResolvedType = AnySimpleType,
            facets: FacetList = FacetList.EMPTY,
            otherContents: List<CompactFragment> = emptyList(),
            id: VID? = null,
            annotations: List<ResolvedAnnotation> = emptyList(),
            otherAttrs: Map<QName, String> = emptyMap(),
        ) : super(annotations, id, otherAttrs) {
            this.baseType = baseType
            this.otherContents = otherContents
            this.facets = facets
            // does not need restriction type, this is only for builtins
        }

        constructor(
            rawPart: XSSimpleRestriction,
            schema: ResolvedSchemaLike,
            baseType: ResolvedSimpleType,
            annotations: List<ResolvedAnnotation> = listOfNotNull(rawPart.annotation.models())
        ) : super(rawPart, annotations) {
            this.baseType = baseType
            this.otherContents = rawPart.otherContents
            this.facets = FacetList.safe(rawPart.facets, schema, baseType)

            if (schema.version == SchemaVersion.V1_1) {
                require(baseType != AnySimpleType && baseType != AnyAtomicType) {
                    "2.4.2.1) Restrictions may only inherit from non-special types"
                }
            }

        }

        constructor(
            rawPart: XSSimpleRestriction,
            schema: ResolvedSchemaLike,
            annotations: List<ResolvedAnnotation> = listOfNotNull(rawPart.annotation.models()),
            context: ResolvedSimpleType,
        ) : this(
            rawPart,
            schema,
            rawPart.base?.let {
                require(rawPart.simpleType == null) { "Restriction can only specify base or simpleType, not both" }
                schema.simpleType(it)
            } ?: ResolvedLocalSimpleType(
                requireNotNull(rawPart.simpleType) { "Restrictions must have a base" },
                schema,
                context
            ),
            annotations
        )

    }

}
