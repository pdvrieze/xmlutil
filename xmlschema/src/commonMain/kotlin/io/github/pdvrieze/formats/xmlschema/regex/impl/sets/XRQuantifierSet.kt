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

package io.github.pdvrieze.formats.xmlschema.regex.impl

/**
 * Base class for quantifiers.
 */
internal abstract class XRQuantifierSet(open var innerSet: XRAbstractSet, override var next: XRAbstractSet, type: Int)
    : XRSimpleSet(type) {

    override fun first(set: XRAbstractSet): Boolean =
        innerSet.first(set) || next.first(set)

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true

    override fun processSecondPassInternal(): XRAbstractSet {
        val innerSet = this.innerSet
        if (innerSet.secondPassVisited) {
            this.innerSet = innerSet.processSecondPass()
        }

        return super.processSecondPassInternal()
    }
}
