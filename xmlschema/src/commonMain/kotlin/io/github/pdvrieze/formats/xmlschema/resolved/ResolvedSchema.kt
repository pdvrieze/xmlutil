/*
 * Copyright (c) 2021.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VLanguage
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_IdentityConstraint
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName

// TODO("Support resolving documents that are external to the original/have some resolver type")
class ResolvedSchema(val rawPart: XSSchema, private val resolver: Resolver) {
    fun check() {
        for(element in elements) { element.check() }
    }

    fun type(typeName: QName): T_Type {
        return types.first { it.qName == typeName }
    }

    fun element(elementName: QName): ResolvedToplevelElement {
        return elements.first { it.qName == elementName }
    }

    val annotations: List<XSAnnotation> get() = TODO("Resolve annotations if needed")
    val types: List<ResolvedType> get() = TODO("Delegate list of resolved types")
    val attributes: List<XSAttribute> get() = TODO("Delegate list of resolved attributes")
    val redefines: List<ResolvedRedefine> = DelegateList(rawPart.redefines) { ResolvedRedefine(it, this, resolver) }

    val elements: List<ResolvedToplevelElement> = DelegateList(CombiningList(rawPart.elements)) { ResolvedToplevelElement(it, this) }

    val attributeGroups: List<XSAttributeGroup> get() = TODO("Delegate list of attribute groups")
    val modelGroups: List<XSGroup> get() = TODO("Delegate list of model groups")
    val notations: List<XSNotation> get() = TODO("Delegate list of notation declarations")
    val identityConstraints: List<G_IdentityConstraint.Types> get() = TODO("Delegate list of identity constraints")

    val attributeFormDefault: T_FormChoice
        get() = rawPart.attributeFormDefault ?: T_FormChoice.UNQUALIFIED

    val blockDefault: T_BlockSet get() = rawPart.blockDefault

    val defaultAttributes: QName? get() = rawPart.defaultAttributes

    val xPathDefaultNamespace: T_XPathDefaultNamespace
        get() = rawPart.xpathDefaultNamespace ?: T_XPathDefaultNamespace.LOCAL

    val elementFormDefault: T_FormChoice
        get() = rawPart.elementFormDefault ?: T_FormChoice.UNQUALIFIED

    val finalDefault : Set<T_TypeDerivationControl> get() = rawPart.finalDefault ?: emptySet()

    val id: VID? get() = rawPart.id

    val targetNamespace: VAnyURI? get() = rawPart.targetNamespace

    val version: VToken? get() = rawPart.version

    val lang: VLanguage? get() = rawPart.lang


    interface Resolver {
        fun readSchema(schemaLocation: VAnyURI): XSSchema
    }
}

