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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAny
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.WildcardModel
import io.github.pdvrieze.formats.xmlschema.types.*

class ResolvedAny(
    override val rawPart: XSAny,
    override val schema: ResolvedSchemaLike
) : ResolvedPart, ResolvedParticle<ResolvedAny>, T_AnyElement, WildcardModel, ResolvedBasicTerm {
    override val minOccurs: VNonNegativeInteger?
        get() = mdlMinOccurs
    override val maxOccurs: T_AllNNI?
        get() = mdlMaxOccurs

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val namespace: T_NamespaceList
        get() = rawPart.namespace ?: T_NamespaceList.ANY

    override val mdlAnnotations: AnnotationModel? get() = rawPart.annotation.models()
    override val mdlMinOccurs: VNonNegativeInteger get() = rawPart.minOccurs ?: VNonNegativeInteger(1)
    override val mdlMaxOccurs: T_AllNNI get() = rawPart.maxOccurs ?: T_AllNNI(1)
    override val mdlTerm: ResolvedAny get() = this

    override val mdlNamespaceConstraint: Set<WildcardModel.NamespaceConstraint>
        get() = TODO("not implemented")

    override val mdlProcessContents: T_ProcessContents
        get() = processContents

    override val notNamespace: T_NotNamespaceList
        get() = rawPart.notNamespace ?: T_NotNamespaceList()

    override val notQName: T_QNameList
        get() = T_QNameList()

    override val processContents: T_ProcessContents
        get() = rawPart.processContents ?: T_ProcessContents.STRICT
}
