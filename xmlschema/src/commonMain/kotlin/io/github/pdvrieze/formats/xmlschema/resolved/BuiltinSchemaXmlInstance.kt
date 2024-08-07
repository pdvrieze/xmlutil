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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyURIType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.BooleanType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.QNameType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants

/**
 * Built-in schema attribute declarations (3.2.7)
 */
object BuiltinSchemaXmlInstance : ResolvedSchemaLike() {
    override val version: SchemaVersion get() = SchemaVersion.V1_1
    override val defaultAttributes: Nothing? get() = null

    override val attributeFormDefault: VFormChoice get() = VFormChoice.UNQUALIFIED
    override val elementFormDefault: VFormChoice get() = VFormChoice.QUALIFIED

    private val delegate: ResolvedSchema

    init {
        val rawSchema = XSSchema(
            targetNamespace = XMLConstants.XSI_NS_URI.toAnyUri(),
            attributeFormDefault = VFormChoice.QUALIFIED,
            attributes = listOf(
                XSGlobalAttribute(
                    name = VNCName("type"),
                    type = QNameType.mdlQName
                ),
                XSGlobalAttribute(
                    name = VNCName("nil"),
                    type = BooleanType.mdlQName
                ),
                XSGlobalAttribute(
                    name = VNCName("schemaLocation"),
                    simpleType = XSLocalSimpleType(
                        XSSimpleList(
                            itemTypeName = AnyURIType.mdlQName
                        )
                    )
                ),
                XSGlobalAttribute(name = VNCName("noNamespaceSchemaLocation"), type = AnyURIType.mdlQName),
            ),
        )

        delegate = ResolvedSchema(rawSchema, ResolvedSchema.DummyResolver, version, true)
    }

    internal val resolver: ResolvedSchema.SchemaElementResolver = object : ResolvedSchema.SchemaElementResolver {
        override fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType? {
            return maybeSimpleType(QName(XMLConstants.XSI_NS_URI, typeName))
        }

        override fun maybeType(typeName: String): ResolvedGlobalType? {
            return maybeType(QName(XMLConstants.XSI_NS_URI, typeName))
        }

        override fun maybeAttributeGroup(attributeGroupName: String): ResolvedGlobalAttributeGroup? {
            return maybeAttributeGroup(QName(XMLConstants.XSI_NS_URI, attributeGroupName))
        }

        override fun maybeGroup(groupName: String): ResolvedGlobalGroup? {
            return maybeGroup(QName(XMLConstants.XSI_NS_URI, groupName))
        }

        override fun maybeElement(elementName: String): ResolvedGlobalElement? {
            return maybeElement(QName(XMLConstants.XSI_NS_URI, elementName))
        }

        override fun maybeAttribute(attributeName: String): ResolvedGlobalAttribute? {
            return maybeAttribute(QName(XMLConstants.XSI_NS_URI, attributeName))
        }

        override fun maybeIdentityConstraint(constraintName: String): ResolvedIdentityConstraint? {
            return maybeIdentityConstraint(QName(XMLConstants.XSI_NS_URI, constraintName))
        }

        override fun maybeNotation(notationName: String): ResolvedNotation? {
            return maybeNotation(QName(XMLConstants.XSI_NS_URI, notationName))
        }
    }


    override val targetNamespace: VAnyURI get() = XMLConstants.XSI_NS_URI.toAnyUri()

    override val blockDefault: Set<VDerivationControl.T_BlockSetValues> get() = delegate.blockDefault

    override val finalDefault: Set<VDerivationControl.Type> get() = delegate.finalDefault

    override val defaultOpenContent: XSDefaultOpenContent? get() = delegate.defaultOpenContent

    override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? = delegate.maybeSimpleType(typeName)

    override fun maybeType(typeName: QName): ResolvedGlobalType? = delegate.maybeType(typeName)

    override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup? =
        delegate.maybeAttributeGroup(attributeGroupName)

    override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? = delegate.maybeGroup(groupName)

    override fun maybeElement(elementName: QName): ResolvedGlobalElement? = delegate.maybeElement(elementName)

    override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? =
        delegate.maybeAttribute(attributeName)

    override fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint? =
        delegate.maybeIdentityConstraint(constraintName)

    override fun maybeNotation(notationName: QName): ResolvedNotation? = delegate.maybeNotation(notationName)

    override fun getElements(): Set<ResolvedGlobalElement> {
        return delegate.getElements()
    }

    override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> =
        delegate.substitutionGroupMembers(headName)

}
