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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TypeDerivationControl
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.namespaceURI

abstract sealed class ResolvedSchemaLike {
    abstract val targetNamespace: VAnyURI

    abstract val elements: List<ResolvedToplevelElement>

    abstract val attributes: List<ResolvedToplevelAttribute>

    abstract val simpleTypes: List<ResolvedToplevelSimpleType>

    abstract val complexTypes: List<ResolvedToplevelComplexType>

    abstract val groups: List<ResolvedDirectGroup>

    abstract val attributeGroups: List<ResolvedDirectAttributeGroup>
    abstract val blockDefault: T_BlockSet
    abstract val finalDefault: Set<T_TypeDerivationControl>

    open fun simpleType(typeName: QName): ResolvedToplevelSimpleType {
        return if (typeName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE) {
            BuiltinXmlSchema.simpleType(typeName)
        } else {
            simpleTypes.firstOrNull { it.qName == typeName }
                ?: throw NoSuchElementException("No simple type with name $typeName found")
        }
    }

    open fun type(typeName: QName): ResolvedToplevelType {
        return if (typeName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE) {
            BuiltinXmlSchema.simpleType(typeName)
        } else {
            simpleTypes.firstOrNull { it.qName == typeName }
                ?: complexTypes.firstOrNull { it.qName == typeName }
                ?: throw NoSuchElementException("No type with name $typeName found")
        }
    }

    fun attributeGroup(attributeGroupName: QName): ResolvedDirectAttributeGroup {
        return attributeGroups.firstOrNull { it.qName.isEquivalent(attributeGroupName) }
            ?: throw NoSuchElementException("No attribute group with name $attributeGroupName found")
    }

    fun modelGroup(groupName: QName): ResolvedDirectGroup {
        return groups.firstOrNull { it.qName.isEquivalent(groupName) }
            ?: throw NoSuchElementException("No group with name $groupName found")
    }

    fun element(elementName: QName): ResolvedToplevelElement {
        return elements.firstOrNull { it.qName.isEquivalent(elementName) }
            ?: throw NoSuchElementException("No element with name $elementName found")
    }

    fun attribute(attributeName: QName): ResolvedToplevelAttribute {
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
            it.identityConstraints.firstOrNull { it.qName?.isEquivalent(constraintName) == true }
        } ?: throw NoSuchElementException("No identity constraint with name $constraintName exists")
    }
}
