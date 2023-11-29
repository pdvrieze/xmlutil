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
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange

interface ResolvedTerm : ResolvedAnnotated {
    fun checkTerm(checkHelper: CheckHelper) {
        checkAnnotated(checkHelper.version)
    }

    fun <R> visit(visitor: Visitor<R>): R

    fun <T: MutableCollection<ResolvedIdentityConstraint>> collectConstraints(collector: T): T {
        visit(object : ElementVisitor() {
            override fun visitModelGroup(group: ResolvedModelGroup) {
                for (p in group.mdlParticles) {
                    when (p) {
                        is ResolvedElementRef,
                        is ResolvedProhibitedElement,
                        is ResolvedGroupRef -> {}

                        else -> p.mdlTerm.visit(this)
                    }
                }
            }

            override fun visitElement(element: ResolvedElement) {
                collector.addAll(element.mdlIdentityConstraints)
                (element.mdlTypeDefinition as? ResolvedLocalComplexType)?.collectConstraints(collector)
            }

            override fun visitAny(any: ResolvedAny) {} // no constraints
        })
        return collector
    }

    fun flatten(range: AllNNIRange, nameContext: ContextT, schema: ResolvedSchemaLike): FlattenedParticle =
        flatten(range, nameContext, schema)

    abstract class Visitor<R> {
        abstract fun visitElement(element: ResolvedElement): R

        abstract fun visitModelGroup(group : ResolvedModelGroup): R

        open fun visitAll(all: IResolvedAll): R =  visitModelGroup(all)
        open fun visitChoice(choice: IResolvedChoice): R = visitModelGroup(choice)
        open fun visitSequence(sequence: IResolvedSequence): R = visitModelGroup(sequence)
        abstract fun visitAny(any: ResolvedAny): R
    }

    abstract class ElementVisitor : Visitor<Unit>() {
        override fun visitModelGroup(group: ResolvedModelGroup) {
            for(p in group.mdlParticles) {
                when (p) {
                    is ResolvedProhibitedElement -> visitProhibited(p)
                    else -> p.mdlTerm.visit(this)
                }
            }
        }

        open fun visitProhibited(prohibitedElement: ResolvedProhibitedElement) {}
    }
}
