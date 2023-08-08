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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

class ResolvedElementRef constructor(
    rawPart: XSLocalElement,
    schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE,
    override val mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE,
) : IResolvedElementUse {
    override val model: Model by lazy { Model(rawPart, schema) }

    override val mdlQName: QName get() = mdlTerm.mdlQName
    override val mdlTerm: ResolvedGlobalElement get() = model.term


    init {
        invariantNotNull(rawPart.ref) { "Element references must have a ref property" }

        require(mdlMinOccurs<=mdlMaxOccurs) { "Invalid bounds: ! (${mdlMinOccurs}<=$mdlMaxOccurs)" }
        require(rawPart.name == null) { "3.3.3(2.1) - A local element declaration must have exactly one of name or ref specified" }
        require(rawPart.block == null) { "3.3.3(2.2) - References may not specify block" }
        require(rawPart.default == null) { "3.3.3(2.2) - References may not specify default" }
        require(rawPart.fixed == null) { "3.3.3(2.2) - References may not specify fixed" }
        require(rawPart.form == null) { "3.3.3(2.2) - References may not specify form" }
        require(rawPart.nillable == null) { "3.3.3(2.2) - References may not specify nillable" }
        require(rawPart.targetNamespace == null) { "3.3.3(2.2) - References may not specify target namespace" }
        require(rawPart.type == null) { "3.3.3(2.2) - References may not specify type" }
        require(rawPart.localType == null) { "3.3.3(2.2) - References may not specify inline type" }
        require(rawPart.identityConstraints.isEmpty()) { "3.3.3(2.2) - References may not specify identity constraints" }
        require(rawPart.alternatives.isEmpty()) { "3.3.3(2.2) - References may not specify alternatives" }
    }

    override fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
        // References don't need collecting
    }

    override fun checkParticle(checkHelper: CheckHelper) {
        check(mdlMinOccurs <= mdlMaxOccurs) { "MinOccurs should be <= than maxOccurs" }
        checkHelper.checkElement(mdlTerm)
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedLocalElement(")
            append("mdlName=$mdlQName, ")
            if (mdlMinOccurs != VNonNegativeInteger.ONE) append("minOccurs=$mdlMinOccurs, ")
            if (mdlMaxOccurs != VAllNNI.ONE) append("maxOccurs=$mdlMaxOccurs, ")
            append("type=${mdlTerm.mdlTypeDefinition}")
            append(")")
        }
    }

    class Model: ResolvedAnnotated.Model {

        val term: ResolvedGlobalElement

        constructor(rawPart: XSLocalElement, schema: ResolvedSchemaLike) : super(rawPart) {
            term = schema.element(invariantNotNull(rawPart.ref) { "Element references must have a ref property" })
        }
    }

}
