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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDefaultOpenContent
import io.github.pdvrieze.formats.xmlschema.model.qName
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.types.T_FullDerivationSet
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.namespaceURI

abstract sealed class ResolvedSchemaLike {
    abstract val targetNamespace: VAnyURI?

    abstract val elements: List<ResolvedGlobalElement>

    abstract val attributes: List<ResolvedGlobalAttribute>

    abstract val simpleTypes: List<ResolvedGlobalSimpleType>

    abstract val complexTypes: List<ResolvedGlobalComplexType>

    abstract val groups: List<ResolvedToplevelGroup>

    abstract val attributeGroups: List<ResolvedToplevelAttributeGroup>
    abstract val blockDefault: T_BlockSet
    abstract val finalDefault: T_FullDerivationSet
    abstract val defaultOpenContent: XSDefaultOpenContent?

    open fun simpleType(typeName: QName): ResolvedGlobalSimpleType {
        return if (typeName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE) {
            BuiltinXmlSchema.simpleType(typeName)
        } else {
            simpleTypes.firstOrNull { it.qName == typeName }
                ?: throw NoSuchElementException("No simple type with name $typeName found")
        }
    }

    open fun type(typeName: QName): ResolvedGlobalType {
        return if (typeName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE) {
            BuiltinXmlSchema.simpleType(typeName)
        } else {
            simpleTypes.firstOrNull { it.qName == typeName }
                ?: complexTypes.firstOrNull { it.qName == typeName }
                ?: throw NoSuchElementException("No type with name $typeName found")
        }
    }

    fun attributeGroup(attributeGroupName: QName): ResolvedToplevelAttributeGroup {
        return attributeGroups.firstOrNull { it.qName.isEquivalent(attributeGroupName) }
            ?: throw NoSuchElementException("No attribute group with name $attributeGroupName found")
    }

    fun modelGroup(groupName: QName): ResolvedToplevelGroup {
        return groups.firstOrNull { it.qName.isEquivalent(groupName) }
            ?: throw NoSuchElementException("No group with name $groupName found")
    }

    fun element(elementName: QName): ResolvedGlobalElement {
        return elements.firstOrNull { it.qName.isEquivalent(elementName) }
            ?: throw NoSuchElementException("No element with name $elementName found")
    }

    fun attribute(attributeName: QName): ResolvedGlobalAttribute {
        return attributes.firstOrNull { it.qName.isEquivalent(attributeName) }
            ?: throw NoSuchElementException("No attribute with name $attributeName found")
    }

    open fun check() {
        for (s in elements) {
            s.check()
        }
        for (a in attributes) {
            a.check()
        }
        for (t in simpleTypes) {
            t.check()
        }
        for (t in complexTypes) {
            t.check()
        }
        for (g in groups) {
            g.check()
        }
        for (ag in attributeGroups) {
            ag.check()
        }
    }

    fun identityConstraint(constraintName: QName): ResolvedIdentityConstraint {
        return elements.asSequence().firstNotNullOfOrNull {
            it.identityConstraints.asSequence().filterIsInstance<ResolvedNamedIdentityConstraint>().firstOrNull { it.qName.isEquivalent(constraintName) }
        } ?: throw NoSuchElementException("No identity constraint with name $constraintName exists")
    }
}
