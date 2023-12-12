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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSEnumeration
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants

object BuiltinSchemaXml : ResolvedSchemaLike() {
    override val version: SchemaVersion get() = SchemaVersion.V1_1

    override val defaultAttributes: Nothing? get() = null

    private val delegate: ResolvedSchema

    override val attributeFormDefault: VFormChoice get() = VFormChoice.UNQUALIFIED
    override val elementFormDefault: VFormChoice get() = VFormChoice.QUALIFIED

    init {
        val rawSchema = XSSchema(
            targetNamespace = XMLConstants.XML_NS_URI.toAnyUri(),
            attributeFormDefault = VFormChoice.QUALIFIED,
            attributes = listOf(
                XSGlobalAttribute(
                    name = VNCName("lang"),
                    simpleType = XSLocalSimpleType(
                        simpleDerivation = XSSimpleUnion(
                            memberTypes = listOf(QName(XMLConstants.XSD_NS_URI, "language")),
                            simpleTypes = listOf(
                                XSLocalSimpleType(
                                    simpleDerivation = XSSimpleRestriction(
                                        base = QName(XMLConstants.XSD_NS_URI, "string"),
                                        facets = listOf(XSEnumeration(VString("")))
                                    )
                                )
                            )
                        )
                    ),
                ),
                XSGlobalAttribute(
                    name = VNCName("space"), default = VString("preserve"),
                    simpleType = XSLocalSimpleType(
                        simpleDerivation = XSSimpleRestriction(
                            base = QName(XMLConstants.XSD_NS_URI, "NCName"),
                            facets = listOf(
                                XSEnumeration(VString("default")),
                                XSEnumeration(VString("preserve")),
                            ),
                        )
                    )
                ),
                XSGlobalAttribute(name = VNCName("base"), type = QName(XMLConstants.XSD_NS_URI, "anyURI")),
                XSGlobalAttribute(name = VNCName("id"), type = QName(XMLConstants.XSD_NS_URI, "ID")),
            ),
            attributeGroups = listOf(
                XSAttributeGroup(
                    VNCName("specialAttrs"), attributes = listOf(
                        XSLocalAttribute(ref = QName(XMLConstants.XML_NS_URI, "base")),
                        XSLocalAttribute(ref = QName(XMLConstants.XML_NS_URI, "lang")),
                        XSLocalAttribute(ref = QName(XMLConstants.XML_NS_URI, "space")),
                        XSLocalAttribute(ref = QName(XMLConstants.XML_NS_URI, "id")),
                    )
                )
            ),
        )

        delegate = ResolvedSchema(rawSchema, ResolvedSchema.DummyResolver)
    }

    internal val resolver: ResolvedSchema.SchemaElementResolver = object : ResolvedSchema.SchemaElementResolver {
        override fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType? {
            return maybeSimpleType(QName(XMLConstants.XML_NS_URI, typeName))
        }

        override fun maybeType(typeName: String): ResolvedGlobalType? {
            return maybeType(QName(XMLConstants.XML_NS_URI, typeName))
        }

        override fun maybeAttributeGroup(attributeGroupName: String): ResolvedGlobalAttributeGroup? {
            return maybeAttributeGroup(QName(XMLConstants.XML_NS_URI, attributeGroupName))
        }

        override fun maybeGroup(groupName: String): ResolvedGlobalGroup? {
            return maybeGroup(QName(XMLConstants.XML_NS_URI, groupName))
        }

        override fun maybeElement(elementName: String): ResolvedGlobalElement? {
            return maybeElement(QName(XMLConstants.XML_NS_URI, elementName))
        }

        override fun maybeAttribute(attributeName: String): ResolvedGlobalAttribute? {
            return maybeAttribute(QName(XMLConstants.XML_NS_URI, attributeName))
        }

        override fun maybeIdentityConstraint(constraintName: String): ResolvedIdentityConstraint? {
            return maybeIdentityConstraint(QName(XMLConstants.XML_NS_URI, constraintName))
        }

        override fun maybeNotation(notationName: String): ResolvedNotation? {
            return maybeNotation(QName(XMLConstants.XML_NS_URI, notationName))
        }
    }


    override val targetNamespace: VAnyURI get() = delegate.targetNamespace

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

    override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> =
        delegate.substitutionGroupMembers(headName)

    override fun getElements(): Set<ResolvedGlobalElement> {
        return delegate.getElements()
    }
}
