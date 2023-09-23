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
            val selfRefCount = attributeGroups.count { it.resolvedGroup.mdlQName == mdlQName }

            val baseGroup = schema.nestedAttributeGroup(mdlQName)
            val baseAttrs = baseGroup.getAttributeUses().associateByTo(mutableMapOf()) { it.mdlAttributeDeclaration.mdlQName }
            val attrUses = getAttributeUses()/*.mapTo(HashSet()) { it.mdlAttributeDeclaration.mdlQName }*/

            when(selfRefCount) {
                0 -> {
                    for (dAttr in attrUses) {
                        val dName = dAttr.mdlAttributeDeclaration.mdlQName
                        val bAttr = requireNotNull(baseAttrs.remove(dName)) { "Attribute group redefines without self reference may only restrict" }
                        require(dAttr.isValidRestrictionOf(bAttr)) {
                            "3.4.6.3 - ${dAttr} doesn't restrict base attribute validly"
                        }
                    }
                    for(b in baseAttrs.values) {
                        require(! b.mdlRequired) {
                            "Redefinition of attribute group must be a valid restriction, as such required attributes (${b.mdlAttributeDeclaration.mdlQName}) must remain present. "
                        }
                    }
                    if (baseAttrs.values.any { it is ResolvedProhibitedAttribute }) {

                    }
                }

                1 -> /*require(baseAttrs.all { it in attrUses }) {
                    "Redefining attribute groups must be super or subsets of their bases.\n !(${attrUses.joinToString()}).containsAll(${baseAttrs.joinToString()})"
                }*/
                {} // automatically extends

                else -> throw IllegalArgumentException("Multiple self-reference in attribute group redefine: $mdlQName")
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
                require(uses.put(a.mdlQName, a) == null) {
                    "Duplicate attribute name ${a.mdlQName} in attribute group $mdlQName"
                }
                uses.put(a.mdlQName, a)
            }
            val seenGroupNames = mutableSetOf<QName>()
            for (g in group.attributeGroups) {
                require(seenGroupNames.add(g.ref)) {
                    "Duplicate nested attribute group name ${g.ref} in attribute group $mdlQName"
                }
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

    override fun toString(): String {
        return "attributeGroup(name=$mdlQName, attrs=[${attributes.joinToString()}], attrGroups=[${attributeGroups.joinToString { "@${it.ref}" }}]"
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
