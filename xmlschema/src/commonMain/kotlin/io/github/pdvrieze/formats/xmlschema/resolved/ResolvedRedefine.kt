/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDefaultOpenContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSRedefine
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.types.T_Redefine
import nl.adaptivity.xmlutil.QName

class ResolvedRedefine(
    override val rawPart: XSRedefine,
    override val schema: ResolvedSchemaLike,
    resolver: ResolvedSchema.Resolver
) : ResolvedSchemaLike(), ResolvedPart, T_Redefine {

    val nestedSchema: XSSchema = resolver.readSchema(rawPart.schemaLocation)

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
        get() = schema.targetNamespace

    override val defaultOpenContent: XSDefaultOpenContent?
        get() = nestedSchema.defaultOpenContent

    init {
        val collatedSchema = CollatedSchema(nestedSchema, resolver.delegate(rawPart.schemaLocation), this)

        collatedSchema.applyRedefines(rawPart, targetNamespace, schema)

        elements = DelegateList(collatedSchema.elements.values.toList()) { (schema, it) ->
            ResolvedGlobalElement(it, schema)
        }
        attributes = DelegateList(collatedSchema.attributes.values.toList()) { (schema, it) ->
            ResolvedGlobalAttribute(it, schema)
        }
        simpleTypes = DelegateList(collatedSchema.simpleTypes.values.toList()) { (schema, it) ->
            ResolvedGlobalSimpleType(it, schema)
        }
        complexTypes = DelegateList(collatedSchema.complexTypes.values.toList()) { (schema, it) ->
            ResolvedGlobalComplexType(it, schema)
        }
        groups = DelegateList(collatedSchema.groups.values.toList()) { (schema, it) ->
            ResolvedToplevelGroup(it, schema)
        }
        attributeGroups = DelegateList(collatedSchema.attributeGroups.values.toList()) { (schema, it) ->
            ResolvedToplevelAttributeGroup(it, schema)
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

private fun VNCName.toQName(schema: XSSchema): QName {
    return toQname(schema.targetNamespace)
}
