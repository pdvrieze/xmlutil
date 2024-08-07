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

import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.namespaceURI

sealed interface ResolvedModelGroup : ResolvedTerm {
    val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

    val mdlCompositor: Compositor

    override fun checkTerm(checkHelper: CheckHelper) {
        super.checkTerm(checkHelper)
        for(particle in mdlParticles) {
            particle.checkParticle(checkHelper)
        }
    }

    fun definesElement(name: QName): Boolean {
        for (particle in mdlParticles) {
            when (val t = particle.mdlTerm) {
                is ResolvedModelGroup -> if(t.definesElement(name)) return true
                is ResolvedElement -> if(t.mdlQName.isEquivalent(name)) return true
            }
        }
        return false
    }

    fun hasLocalNsInContext(): Boolean {
        for (particle in mdlParticles) {
            when (val t = particle.mdlTerm) {
                is ResolvedModelGroup -> if(t.hasLocalNsInContext()) return true
                is ResolvedElement -> if(t.mdlQName.namespaceURI.isEmpty()) return true
            }
        }
        return false
    }

    enum class Compositor { ALL, CHOICE, SEQUENCE }
}
