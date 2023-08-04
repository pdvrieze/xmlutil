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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAny
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedAny(
    override val rawPart: XSAny,
    override val schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE,
    override val mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE,
) : ResolvedParticle<ResolvedAny>, ResolvedBasicTerm {

    override val mdlAnnotations: ResolvedAnnotation?
        get() = rawPart.annotation.models()

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    val namespace: VNamespaceList
        get() = rawPart.namespace ?: VNamespaceList.ANY

    override val mdlTerm: ResolvedAny get() = this

    val mdlNamespaceConstraint: Set<VNamespaceConstraint>
        get() = TODO("not implemented")

    val mdlProcessContents: VProcessContents
        get() = processContents

    val notNamespace: VNotNamespaceList
        get() = rawPart.notNamespace ?: VNotNamespaceList()

    val notQName: VQNameList
        get() = VQNameList()

    val processContents: VProcessContents
        get() = rawPart.processContents ?: VProcessContents.STRICT

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {}

    override fun check(checkedTypes: MutableSet<QName>) {
//        TODO("not implemented")
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): ResolvedAny {
        return when {
            minMultiplier != VNonNegativeInteger.ONE || maxMultiplier != VAllNNI.ONE -> {
                ResolvedAny(
                    rawPart,
                    schema,
                    mdlMinOccurs * minMultiplier,
                    mdlMaxOccurs * maxMultiplier,
                )
            }

            else -> this
        }
    }
}
