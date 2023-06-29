/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroup
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRefParticle
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.GroupDefModel
import io.github.pdvrieze.formats.xmlschema.model.ModelGroupModel
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedGroupBase(override val schema: ResolvedSchemaLike): T_RealGroup, ResolvedPart, ResolvedAnnotated {
    abstract override val rawPart: XSI_Annotated

    override val annotation: XSAnnotation? get() = rawPart.annotation
}



class ResolvedGroupRef(
    override val rawPart: XSGroupRef,
    schema: ResolvedSchemaLike
): ResolvedGroupBase(schema), ResolvedGroupParticle<ResolvedGroupRef>, T_GroupRef {
    val referencedGroup: ResolvedToplevelGroup by lazy { schema.modelGroup(rawPart.ref) }
    override val minOccurs: VNonNegativeInteger? get() = rawPart.minOccurs
    override val mdlMinOccurs: VNonNegativeInteger get() = minOccurs ?: VNonNegativeInteger.ONE

    override val maxOccurs: T_AllNNI? get() = rawPart.maxOccurs
    override val mdlMaxOccurs: T_AllNNI get() = maxOccurs ?: T_AllNNI.ONE

    override val mdlAnnotations: AnnotationModel? get() = rawPart.annotation.models()

    override val mdlTerm: ResolvedGroupRef get() = this

    override val ref: QName get() = rawPart.ref

    override val particle: T_RealGroup.Particle
        get() = referencedGroup.particle

    override fun check() {
        referencedGroup.check()
    }
}

class ResolvedGroupRefParticle(
    override val rawPart: XSGroupRefParticle,
    schema: ResolvedSchemaLike
): ResolvedGroupBase(schema), ResolvedParticle<ResolvedAllTerm>, ResolvedAllTerm, T_GroupRef {
    val referencedGroup: ResolvedToplevelGroup by lazy { schema.modelGroup(rawPart.ref) }

    override val ref: QName get() = rawPart.ref

    override val annotation: XSAnnotation?
        get() = referencedGroup.annotation

    override val particle: T_RealGroup.Particle
        get() = referencedGroup.particle

    override val minOccurs: VNonNegativeInteger?
        get() = rawPart.minOccurs

    override val mdlMinOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger.ONE

    override val mdlMaxOccurs: T_AllNNI
        get() = rawPart.maxOccurs ?: T_AllNNI.ONE

    override val maxOccurs: T_AllNNI?
        get() = rawPart.maxOccurs

    override val mdlAnnotations: AnnotationModel? get() = rawPart.annotation.models()

    override val mdlTerm: ResolvedGroupRefParticle get() = this

    override fun check() {
        referencedGroup.check()
    }
}

class ResolvedToplevelGroup(
    override val rawPart: XSGroup,
    schema: ResolvedSchemaLike
): ResolvedGroupBase(schema), NamedPart, T_NamedGroup, GroupDefModel, ModelGroupModel {
    override val mdlName: VNCName
        get() = rawPart.name

    override val mdlTargetNamespace: Nothing?
        get() = rawPart.targetNamespace

    override val mdlModelGroup: ResolvedToplevelGroup
        get() = this

    override val mdlAnnotations: AnnotationModel?
        get() = rawPart.annotation.models()


    override fun check() {
//        TODO("not implemented")
    }

    @Deprecated("incorrect")
    override val particle: T_NamedGroup.Particle
        get() = TODO()

    override val name: VNCName
        get() = rawPart.name

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace
}
