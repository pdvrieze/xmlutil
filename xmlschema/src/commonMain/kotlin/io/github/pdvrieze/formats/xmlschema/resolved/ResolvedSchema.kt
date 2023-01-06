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
class ResolvedSchema(val rawPart: XSSchema, private val resolver: Resolver) : ResolvedSchemaLike() {
    override fun check() {
        super.check()

        for (rd in redefines) { rd.check() }
    }

    val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val simpleTypes: List<ResolvedToplevelSimpleType> = DelegateList(rawPart.simpleTypes) { ResolvedToplevelSimpleType(it, this) }
    override val complexTypes: List<ResolvedToplevelComplexType> = DelegateList(rawPart.complexTypes) { ResolvedToplevelComplexType(it, this) }

    val types: List<ResolvedToplevelType> get() = CombiningList(simpleTypes, complexTypes)
    override val attributes: List<ResolvedToplevelAttribute> get() = DelegateList(rawPart.attributes) { ResolvedToplevelAttribute(it, this) }
    val redefines: List<ResolvedRedefine> = DelegateList(rawPart.redefines) { ResolvedRedefine(it, this, resolver) }

    override val elements: List<ResolvedToplevelElement> = DelegateList(CombiningList(rawPart.elements)) { ResolvedToplevelElement(it, this) }

    override val attributeGroups: List<ResolvedDirectAttributeGroup> = DelegateList(CombiningList(rawPart.attributeGroups)) { ResolvedDirectAttributeGroup(it, this) }
    override val groups: List<ResolvedDirectGroup> get() = DelegateList(rawPart.groups) { ResolvedDirectGroup(it, this) }
    val notations: List<XSNotation> get() = rawPart.notations
    val identityConstraints: List<G_IdentityConstraint.Base> get() = TODO("Delegate list of identity constraints")

    val attributeFormDefault: T_FormChoice
        get() = rawPart.attributeFormDefault ?: T_FormChoice.UNQUALIFIED

    override val blockDefault: T_BlockSet get() = rawPart.blockDefault

    val defaultAttributes: QName? get() = rawPart.defaultAttributes

    val xPathDefaultNamespace: T_XPathDefaultNamespace
        get() = rawPart.xpathDefaultNamespace ?: T_XPathDefaultNamespace.LOCAL

    val elementFormDefault: T_FormChoice
        get() = rawPart.elementFormDefault ?: T_FormChoice.UNQUALIFIED

    override val finalDefault : Set<T_TypeDerivationControl> get() = rawPart.finalDefault ?: emptySet()

    val id: VID? get() = rawPart.id

    override val targetNamespace: VAnyURI get() = rawPart.targetNamespace ?: VAnyURI("")

    val version: VToken? get() = rawPart.version

    val lang: VLanguage? get() = rawPart.lang


    interface Resolver {
        fun readSchema(schemaLocation: VAnyURI): XSSchema

        /**
         * Create a delegate resolver for the schema
         */
        fun delegate(schemaLocation: VAnyURI): Resolver
    }
}

