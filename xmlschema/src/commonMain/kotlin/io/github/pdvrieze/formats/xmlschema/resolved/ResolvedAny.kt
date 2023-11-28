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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAny
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedAny : ResolvedWildcardBase<VQNameListBase.Elem>, ResolvedParticle<ResolvedAny>, ResolvedBasicTerm {

    override val mdlMinOccurs: VNonNegativeInteger
    override val mdlMaxOccurs: VAllNNI

    constructor(
        mdlNamespaceConstraint: VNamespaceConstraint<VQNameListBase.Elem>,
        mdlProcessContents: VProcessContents,
        mdlMinOccurs: VNonNegativeInteger = VNonNegativeInteger.ONE,
        mdlMaxOccurs: VAllNNI = VAllNNI.ONE,
    ) : super(mdlNamespaceConstraint, mdlProcessContents) {
        require(mdlMinOccurs<=mdlMaxOccurs) { "Invalid bounds: ! (${mdlMinOccurs}<=$mdlMaxOccurs)" }
        this.mdlMinOccurs = mdlMinOccurs
        this.mdlMaxOccurs = mdlMaxOccurs
    }

    constructor(
        rawPart: XSAny,
        schema: ResolvedSchemaLike,
        localInContext: Boolean,
        mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE,
        mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE
    ) : super(
        rawPart,
        rawPart.toConstraint(schema),
        rawPart.processContents ?: VProcessContents.STRICT
    ) {
        require(mdlMinOccurs<=mdlMaxOccurs) { "Invalid bounds: ! (${mdlMinOccurs}<=$mdlMaxOccurs)" }
        this.mdlMinOccurs = mdlMinOccurs
        this.mdlMaxOccurs = mdlMaxOccurs
    }

    override val mdlTerm: ResolvedAny get() = this

    override fun flatten(range: AllNNIRange, typeContext: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle.Wildcard {
        return FlattenedParticle.Wildcard(range, this)
    }

    override fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {}

    override fun checkTerm(checkHelper: CheckHelper) {
        super.checkTerm(checkHelper)
    }

    fun intersects(other: ResolvedAny): Boolean = when (mdlMaxOccurs) {
        VAllNNI.ZERO -> false
        else -> this.mdlNamespaceConstraint.intersects(other.mdlNamespaceConstraint)
    }

    fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
        return mdlNamespaceConstraint.matches(name, context, schema)
    }
}
