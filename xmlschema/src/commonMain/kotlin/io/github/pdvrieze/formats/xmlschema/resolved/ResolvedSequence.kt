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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSequence
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

class ResolvedSequence(
    parent: VElementScope.Member,
    rawPart: XSSequence,
    schema: ResolvedSchemaLike
) : ResolvedGroupParticleTermBase<IResolvedSequence>(
    parent,
    rawPart,
    schema,
    rawPart.minOccurs ?: VNonNegativeInteger.ONE,
    rawPart.maxOccurs ?: VAllNNI.ONE
), IResolvedSequence {

    override val mdlTerm: ResolvedSequence get() = this

    override fun checkTerm(checkHelper: CheckHelper) {
        super.checkTerm(checkHelper)
        val names = mutableSetOf<QName>()
        for (elem in mdlParticles) {
            if (elem is ResolvedLocalElement) {
                check(names.add(elem.mdlQName)) { "Duplicate element with name ${elem.mdlQName} found in sequence" }
            }
        }
    }


    override fun toString(): String {
        return buildString {
            append("ResolvedSequence(")
            if (mdlMinOccurs != VNonNegativeInteger.ONE) append("minOccurs=$mdlMinOccurs, ")
            if (mdlMaxOccurs != VAllNNI.ONE) append("maxOccurs=$mdlMaxOccurs, ")
            append(mdlParticles)
            append(")")
        }
    }


}

