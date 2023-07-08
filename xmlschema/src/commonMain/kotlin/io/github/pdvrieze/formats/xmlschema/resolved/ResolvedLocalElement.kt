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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.model.ElementModel
import io.github.pdvrieze.formats.xmlschema.model.ValueConstraintModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_FormChoice
import io.github.pdvrieze.formats.xmlschema.types.T_LocalElement
import io.github.pdvrieze.formats.xmlschema.types.T_Scope
import nl.adaptivity.xmlutil.QName

class ResolvedLocalElement(
    override val parent: ResolvedComplexType,
    override val rawPart: XSLocalElement,
    schema: ResolvedSchemaLike
) : ResolvedElement(schema),
    ResolvedParticle<ResolvedLocalElement>,
    T_LocalElement,
    ElementModel.Local<ResolvedLocalElement>,
    ResolvedComplexTypeContext,
    ElementModel.Scope.Local,
    ResolvedAllMember {

    override val id: VID? get() = rawPart.id
    override val annotation: XSAnnotation? get() = rawPart.annotation
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val scope: T_Scope get() = T_Scope.LOCAL

    override val ref: QName? get() = rawPart.ref

    val referenced: ResolvedElement by lazy {
        ref?.let { schema.element(it) } ?: this
    }
    override val mdlName: VNCName? get() = rawPart.name

    override val minOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger(1)

    override val maxOccurs: T_AllNNI
        get() = rawPart.maxOccurs ?: T_AllNNI(1)

    override val form: T_FormChoice?
        get() = rawPart.form

    override val targetNamespace: VAnyURI?
        get() = rawPart.targetNamespace ?: schema.targetNamespace


    override val keyrefs: List<ResolvedKeyRef> = DelegateList(rawPart.keyrefs) { ResolvedKeyRef(it, schema, this) }
    override val uniques: List<ResolvedUnique> = DelegateList(rawPart.uniques) { ResolvedUnique(it, schema, this) }
    override val keys: List<ResolvedKey> = DelegateList(rawPart.keys) { ResolvedKey(it, schema, this) }

    override val model: Model by lazy { ModelImpl(rawPart, schema, this) }

    override val mdlScope: ElementModel.Scope.Local get() = model.mdlScope
    override val mdlTerm: ResolvedLocalElement get() = model.mdlTerm
    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace
    override val mdlMinOccurs: VNonNegativeInteger get() = model.mdlMinOccurs
    override val mdlMaxOccurs: T_AllNNI get() = model.mdlMaxOccurs

    override fun check() {
        super<ResolvedElement>.check()
        if (rawPart.ref != null) {
            referenced// Don't check as that would already be done at top level
            check(name == null) { "Local elements can not have both a name and ref attribute specified" }
            check(block.isEmpty()) { "Local element references cannot have the block attribute specified" }
            check(type == null) { "Local element references cannot have the type attribute specified" }
            check(rawPart.nillable == null) {
                "Local element references cannot have the nillable attribute specified"
            }
            check(rawPart.default == null) { "Local element references cannot have the default attribute specified" }
            check(rawPart.fixed == null) { "Local element references cannot have the default attribute specified" }
            check(rawPart.form == null) { "Local element references cannot have the default attribute specified" }
        } else {
            check(name != null) { "Missing name for local (non-referencing) element" }
            checkSingleType()
        }

        keyrefs.forEach { it.check() }
        uniques.forEach { it.check() }
        keys.forEach { it.check() }
    }

    interface Model: ResolvedElement.Model, ElementModel.Local<ResolvedLocalElement>

    private inner class ModelImpl(rawPart: XSLocalElement, schema: ResolvedSchemaLike, context: ResolvedLocalElement) :
        ResolvedElement.ModelImpl(rawPart, schema, context), Model {

        override val mdlName: VNCName? = rawPart.name

        override val mdlScope: ElementModel.Scope.Local
            get() = this@ResolvedLocalElement

        override val mdlTerm: ResolvedLocalElement get() = this@ResolvedLocalElement

        override val mdlTargetNamespace: VAnyURI? get() = rawPart.targetNamespace ?: schema.targetNamespace

        override val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE

        override val mdlMaxOccurs: T_AllNNI = rawPart.maxOccurs ?: T_AllNNI.ONE

        override val mdlTypeTable: ElementModel.TypeTable?
            get() = TODO("not implemented")

        override val mdlValueConstraint: ValueConstraintModel?
            get() = TODO("not implemented")
    }

}
