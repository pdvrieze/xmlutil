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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDefaultOpenContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSInclude
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.types.T_Include
import nl.adaptivity.xmlutil.QName

class ResolvedInclude(
    override val rawPart: XSInclude,
    override val schema: ResolvedSchemaLike,
    resolver: ResolvedSchema.Resolver
) : ResolvedSchemaLike(), ResolvedPart, T_Include {

    val nestedSchema: XSSchema = resolver.readSchema(rawPart.schemaLocation)

    override val defaultOpenContent: XSDefaultOpenContent?
        get() = schema.defaultOpenContent

    override val schemaLocation: VAnyURI
        get() = rawPart.schemaLocation

    override val blockDefault: T_BlockSet
        get() = schema.blockDefault // TODO maybe correct, maybe not

    override val finalDefault: Set<TypeModel.Derivation>
        get() = schema.finalDefault // TODO maybe correct, maybe not

    override val elements: List<ResolvedGlobalElement>

    override val attributes: List<ResolvedGlobalAttribute>

    override val simpleTypes: List<ResolvedGlobalSimpleType>

    override val complexTypes: List<ResolvedGlobalComplexType>

    override val groups: List<ResolvedToplevelGroup>

    override val attributeGroups: List<ResolvedToplevelAttributeGroup>

    override val targetNamespace: VAnyURI?
        get() = nestedSchema.targetNamespace

    init {
        require(nestedSchema.targetNamespace != schema.targetNamespace)
        val collatedSchema = CollatedSchema(nestedSchema, resolver.delegate(rawPart.schemaLocation), schema)

        elements = DelegateList(collatedSchema.elements.values.toList()) { (k, v) ->
            ResolvedGlobalElement(v, k)
        }
        attributes = DelegateList(collatedSchema.attributes.values.toList()) { (k, v) ->
            ResolvedGlobalAttribute(v, k)
        }
        simpleTypes = DelegateList(collatedSchema.simpleTypes.values.toList()) { (k, v) ->
            ResolvedGlobalSimpleType(v, k)
        }
        complexTypes = DelegateList(collatedSchema.complexTypes.values.toList()) { (k, v) ->
            ResolvedGlobalComplexType(v, k)
        }
        groups = DelegateList(collatedSchema.groups.values.toList()) { (k, v) ->
            ResolvedToplevelGroup(v, k)
        }
        attributeGroups = DelegateList(collatedSchema.attributeGroups.values.toList()) { (k, v) ->
            ResolvedToplevelAttributeGroup(v, k)
        }

    }

    override val id: VID?
        get() = rawPart.id

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedSchemaLike>.check()
    }
}
