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
import io.github.pdvrieze.formats.xmlschema.types.VBlockSet
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

abstract class ResolvedSchemaLike {
    abstract val targetNamespace: VAnyURI?

    abstract val blockDefault: VBlockSet
    abstract val finalDefault: Set<VDerivationControl.Type>
    abstract val defaultOpenContent: XSDefaultOpenContent?

    abstract fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType?

    abstract fun maybeType(typeName: QName): ResolvedGlobalType?

    abstract fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup?

    abstract fun maybeGroup(groupName: QName): ResolvedGlobalGroup?

    abstract fun maybeElement(elementName: QName): ResolvedGlobalElement?

    abstract fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute?

    abstract fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint?

    abstract fun maybeNotation(notationName: QName): ResolvedNotation?

    fun simpleType(typeName: QName): ResolvedGlobalSimpleType = when (typeName.namespaceURI) {
        XmlSchemaConstants.XS_NAMESPACE -> maybeSimpleType(typeName)
            ?: BuiltinSchemaXmlschema.maybeSimpleType(typeName)

        targetNamespace?.value -> maybeSimpleType(typeName)
        else -> maybeSimpleType(typeName)
    } ?: throw NoSuchElementException("No simple type with name $typeName found")

    fun type(typeName: QName): ResolvedGlobalType = when (typeName.namespaceURI) {
        targetNamespace?.value -> when (typeName.namespaceURI) {
            XmlSchemaConstants.XS_NAMESPACE -> maybeType(typeName) ?: BuiltinSchemaXmlschema.maybeType(typeName)
            else -> maybeType(typeName)
        }

        XmlSchemaConstants.XS_NAMESPACE -> BuiltinSchemaXmlschema.maybeType(typeName)
        else -> maybeType(typeName)
    } ?: throw NoSuchElementException("No type with name $typeName found")

    fun attributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup {
        return maybeAttributeGroup(attributeGroupName)
            ?: throw NoSuchElementException("No attribute group with name $attributeGroupName found")
    }

    fun modelGroup(groupName: QName): ResolvedGlobalGroup {
        return maybeGroup(groupName)
            ?: throw NoSuchElementException("No group with name $groupName found")
    }

    fun element(elementName: QName): ResolvedGlobalElement {
        return maybeElement(elementName)
            ?: throw NoSuchElementException("No element with name $elementName found")
    }

    fun attribute(attributeName: QName): ResolvedGlobalAttribute {
        return maybeAttribute(attributeName)
            ?: throw NoSuchElementException("No attribute with name $attributeName found")
    }

    fun identityConstraint(constraintName: QName): ResolvedIdentityConstraint {
        return maybeIdentityConstraint(constraintName)
            ?: throw NoSuchElementException("No identity constraint with name $constraintName exists")
    }

    fun notation(notationName: QName): ResolvedNotation {
        return maybeNotation(notationName)
            ?: throw NoSuchElementException("No notation with name $notationName exists")
    }

    abstract fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement>
}
