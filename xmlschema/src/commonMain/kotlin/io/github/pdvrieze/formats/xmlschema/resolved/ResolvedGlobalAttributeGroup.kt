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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttributeGroup
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalAttributeGroup(
    rawPart: XSAttributeGroup,
    schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedAnnotated, NamedPart, VAttributeScope.Member {

    override val model: Model by lazy { Model(this, rawPart, schema) }

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    val attributes: List<IResolvedAttributeUse> get() = model.attributes

    val attributeGroups: List<ResolvedAttributeGroupRef> get() = model.attributeGroups

    val anyAttribute: XSAnyAttribute? = rawPart.anyAttribute

    internal constructor(rawPart: SchemaAssociatedElement<XSAttributeGroup>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    init {
        if (schema is CollatedSchema.RedefineWrapper) {
            val baseAttrs = schema.nestedAttributeGroup(mdlQName).getAttributeUses().mapTo(HashSet()) { it.mdlAttributeDeclaration.mdlQName }
            val attrUses = getAttributeUses().mapTo(HashSet()) { it.mdlAttributeDeclaration.mdlQName }
            require(baseAttrs.all { it in attrUses } || attrUses.all { it in baseAttrs}) {
                "Redefining attribute groups must be super or subsets of their bases.\n !(${attrUses.joinToString()}).containsAll(${baseAttrs.joinToString()})"
            }
        }
    }

    fun getAttributeUses(): Collection<IResolvedAttributeUse> {
        val uses = mutableMapOf<QName, IResolvedAttributeUse>()
        val seenGroups = mutableSetOf(this)
        val groups = ArrayDeque<ResolvedGlobalAttributeGroup>()
        groups.add(this)
        while (groups.isNotEmpty()) {
            val group = groups.removeFirst()
            for (a in group.attributes) {
                uses.put(a.mdlAttributeDeclaration.mdlQName, a)
            }
            for (g in group.attributeGroups) {
                val resolvedGroup = g.resolvedGroup
                if (seenGroups.add(resolvedGroup)) {
                    groups.add(resolvedGroup)
                }
            }
        }
        return uses.values
    }

    fun checkAttributeGroup(checkHelper: CheckHelper) {
        for (a in attributes) { a.checkUse(checkHelper) }
        for (ag in attributeGroups) { ag.checkRef(checkHelper) }
    }

    class Model(parent: ResolvedGlobalAttributeGroup, rawPart: XSAttributeGroup, schema: ResolvedSchemaLike): ResolvedAnnotated.Model(rawPart) {

        val attributes: List<IResolvedAttributeUse> = rawPart.attributes.map {
            ResolvedLocalAttribute(parent, it, schema)
        }

        val attributeGroups: List<ResolvedAttributeGroupRef> = rawPart.attributeGroups.map {
            ResolvedAttributeGroupRef(it, schema)
        }
    }
}
