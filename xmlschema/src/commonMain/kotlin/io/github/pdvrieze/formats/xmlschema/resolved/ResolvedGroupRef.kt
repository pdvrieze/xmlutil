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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRef
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI

class ResolvedGroupRef(
    rawPart: XSGroupRef,
    schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE,
    override val mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE,
) : ResolvedGroupBase, ResolvedGroupParticle<ResolvedModelGroup> {

    init {
        require(mdlMinOccurs<=mdlMaxOccurs) { "Invalid bounds: ! (${mdlMinOccurs}<=$mdlMaxOccurs)" }
    }

    override val model: Model by lazy { Model(rawPart, schema) }

    override val mdlTerm: ResolvedModelGroup get() = model.referenced.mdlModelGroup

    override fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
        // global references should not collect
    }

    override fun checkParticle(checkHelper: CheckHelper) {
        check(mdlMinOccurs <= mdlMaxOccurs) { "MinOccurs should be <= than maxOccurs" }
        checkHelper.checkGroup(model.referenced)
    }

    class Model(rawPart: XSGroupRef, schema: ResolvedSchemaLike) : ResolvedAnnotated.Model(rawPart) {
        val referenced: ResolvedGlobalGroup = schema.modelGroup(rawPart.ref)
    }
}
