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

class ResolvedElementRef private constructor(
    val parent: VElementScope.Member,
    override val rawPart: XSLocalElement,
    override val schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger,
    override val mdlMaxOccurs: VAllNNI,
) : IResolvedElementUse,
    ResolvedParticle<ResolvedElement> {

    override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

    val ref: QName = invariantNotNull(rawPart.ref) { "Element references must have a ref property" }

    init {
        require(rawPart.name == null) { "3.3.3(2.1) - A local element declaration must have exactly one of name or ref specified" }
        require(rawPart.block==null) { "3.3.3(2.2) - References may not specify block" }
        require(rawPart.default==null) { "3.3.3(2.2) - References may not specify default" }
        require(rawPart.fixed==null) { "3.3.3(2.2) - References may not specify fixed" }
        require(rawPart.form==null) { "3.3.3(2.2) - References may not specify form" }
        require(rawPart.nillable==null) { "3.3.3(2.2) - References may not specify nillable" }
        require(rawPart.targetNamespace==null) { "3.3.3(2.2) - References may not specify target namespace" }
        require(rawPart.type==null) { "3.3.3(2.2) - References may not specify type" }
        require(rawPart.localType==null) { "3.3.3(2.2) - References may not specify inline type" }
        require(rawPart.identityConstraints.isEmpty()) { "3.3.3(2.2) - References may not specify identity constraints" }
        require(rawPart.alternatives.isEmpty()) { "3.3.3(2.2) - References may not specify alternatives" }
    }

    override val mdlQName: QName get() = mdlTerm.mdlQName

    override val mdlTerm: ResolvedGlobalElement by lazy {
        schema.element(invariantNotNull(rawPart.ref) { "Element references must have a ref property" })
    }

    constructor(
        parent: VElementScope.Member,
        rawPart: XSLocalElement,
        schema: ResolvedSchemaLike,
    ) : this(
        parent,
        rawPart,
        schema,
        rawPart.minOccurs ?: VNonNegativeInteger.ONE,
        rawPart.maxOccurs ?: VAllNNI.ONE,
    )

    override fun checkParticle(checkHelper: CheckHelper) {
        checkHelper.checkElement(mdlTerm)
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): ResolvedElementRef = when {
        minMultiplier != VNonNegativeInteger.ONE || maxMultiplier != VAllNNI.ONE -> {
            ResolvedElementRef(
                parent,
                rawPart,
                schema,
                mdlMinOccurs * minMultiplier,
                mdlMaxOccurs * maxMultiplier,
            )
        }

        else -> this
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedLocalElement(")
            append("mdlName=$mdlQName, ")
            if (rawPart.minOccurs != null) append("minOccurs=${rawPart.minOccurs}, ")
            if (rawPart.maxOccurs != null) append("maxOccurs=${rawPart.maxOccurs}, ")
            append("type=${mdlTerm.mdlTypeDefinition}")
            append(")")
        }
    }

}
