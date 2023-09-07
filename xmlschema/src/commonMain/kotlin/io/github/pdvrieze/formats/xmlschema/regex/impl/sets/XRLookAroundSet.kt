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
 * Abstract class for lookahead and lookbehind nodes.
 */
internal abstract class XRLookAroundSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRAtomicJointSet(children, fSet) {
    protected abstract fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int

    /** Returns startIndex+shift, the next position to match */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        matchResult.saveState()
        return tryToMatch(startIndex, testString, matchResult).also { if (it < 0) matchResult.rollbackState() }
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
}
