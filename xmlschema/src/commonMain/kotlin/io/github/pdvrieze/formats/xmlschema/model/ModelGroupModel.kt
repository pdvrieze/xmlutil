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

package io.github.pdvrieze.formats.xmlschema.model

/**
 * Base interface for types that hold particles (all, choice, sequence)
 */
interface ModelGroupModel : IAnnotated, Term {
    val mdlCompositor: Compositor
    val mdlParticles: List<ParticleModel<Term>>

    enum class Compositor { ALL, CHOICE, SEQUENCE }
}

/**
 * Base interface for terms that can be defined in a group: all, choice, sequence
 * @param M The kind of particles contained
 */
interface GroupMember : ModelGroupModel, Term {
}

/** Base interface for terms that can be defined in a choice or sequence: */
interface ChoiceSeqMember : Term

/** Base interface for terms that can be defined in an all: */
interface AllMember : ChoiceSeqMember

interface GroupRefModel : IAnnotated {
    val mdlTerm: GroupDefModel
}

interface GroupRefParticleModel : GroupRefModel, ParticleModel<GroupDefModel>, AllMember {

    override fun mdlIsEmptiable(): Boolean {
        return super.mdlIsEmptiable() || effectiveTotalRange.start.toUInt() == 0u
    }

}
