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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants.XML_NAMESPACE
import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants.XS_NAMESPACE
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSEnumeration
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.types.T_FormChoice
import nl.adaptivity.xmlutil.QName

object BuiltinSchemaXml : ResolvedSchemaLike() {

    private val delegate: ResolvedSchema

    init {
        val rawSchema = XSSchema(
            targetNamespace = VAnyURI(XML_NAMESPACE),
            attributeFormDefault = T_FormChoice.QUALIFIED,
            attributes = listOf(
                XSGlobalAttribute(
                    name = VNCName("lang"),
                    simpleType = XSLocalSimpleType(
                        simpleDerivation = XSSimpleUnion(
                            memberTypes = listOf(QName(XS_NAMESPACE, "language")),
                            simpleTypes = listOf(
                                XSLocalSimpleType(simpleDerivation = XSSimpleRestriction(
                                    base = QName(XS_NAMESPACE),
                                    facets = listOf(XSEnumeration(VString("")))
                                ))
                            )
                        )
                    ),
                ),
                XSGlobalAttribute(
                    name = VNCName("space"), default = VString("preserve"),
                    simpleType = XSLocalSimpleType(simpleDerivation = XSSimpleRestriction(
                        base = QName(XS_NAMESPACE, "NCName"),
                        facets = listOf(
                            XSEnumeration(VString("default")),
                            XSEnumeration(VString("preserve")),
                        ),
                    ))
                ),
                XSGlobalAttribute(name = VNCName("base"), type = QName(XS_NAMESPACE, "anyURI")),
                XSGlobalAttribute(name = VNCName("id"), type = QName(XS_NAMESPACE, "ID")),
            ),
            attributeGroups = listOf(
                XSAttributeGroup(VNCName("specialAttrs"), attributes= listOf(
                    XSLocalAttribute(ref = QName(XML_NAMESPACE, "base")),
                    XSLocalAttribute(ref = QName(XML_NAMESPACE, "lang")),
                    XSLocalAttribute(ref = QName(XML_NAMESPACE, "space")),
                    XSLocalAttribute(ref = QName(XML_NAMESPACE, "id")),
                ))
            ),
        )

        delegate = ResolvedSchema(rawSchema, ResolvedSchema.DummyResolver)
    }

    internal val resolver: ResolvedSchema.SchemaElementResolver = object : ResolvedSchema.SchemaElementResolver {
        override fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType? {
            return maybeSimpleType(QName(XML_NAMESPACE, typeName))
        }

        override fun maybeType(typeName: String): ResolvedGlobalType? {
            return maybeType(QName(XML_NAMESPACE, typeName))
        }

        override fun maybeAttributeGroup(attributeGroupName: String): ResolvedGlobalAttributeGroup? {
            return maybeAttributeGroup(QName(XML_NAMESPACE, attributeGroupName))
        }

        override fun maybeGroup(groupName: String): ResolvedGlobalGroup? {
            return maybeGroup(QName(XML_NAMESPACE, groupName))
        }

        override fun maybeElement(elementName: String): ResolvedGlobalElement? {
            return maybeElement(QName(XML_NAMESPACE, elementName))
        }

        override fun maybeAttribute(attributeName: String): ResolvedGlobalAttribute? {
            return maybeAttribute(QName(XML_NAMESPACE, attributeName))
        }

        override fun maybeIdentityConstraint(constraintName: String): ResolvedIdentityConstraint? {
            return maybeIdentityConstraint(QName(XML_NAMESPACE, constraintName))
        }

        override fun maybeNotation(notationName: String): ResolvedNotation? {
            return maybeNotation(QName(XML_NAMESPACE, notationName))
        }
    }


    override val targetNamespace: VAnyURI get() = delegate.targetNamespace

    override val blockDefault: T_BlockSet get() = delegate.blockDefault

    override val finalDefault: Set<TypeModel.Derivation> get() = delegate.finalDefault

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
}
