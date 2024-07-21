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

import kotlin.IllegalArgumentException

/**
 * Represents RE quantifier; contains two fields responsible for min and max number of repetitions.
 * -1 as a maximum number of repetition represents infinity(i.e. +,*).
 */
internal class XRQuantifier(val min: Int, val max: Int = min) : XRSpecialToken() {

    init {
        if (min < 0 || max < -1) {
            throw IllegalArgumentException("Incorrect quantifier value: $this")
        }
    }

    override fun toString() = "{$min, ${if (max == INF) "" else max}}"

    override val type: Type = XRSpecialToken.Type.QUANTIFIER

    companion object {
        val starQuantifier = XRQuantifier(0, -1)
        val plusQuantifier = XRQuantifier(1, -1)
        val altQuantifier  = XRQuantifier(0,  1)

        val INF = -1

        fun fromLexerToken(token: Int) = when(token) {
            XRLexer.QUANT_STAR, XRLexer.QUANT_STAR_P, XRLexer.QUANT_STAR_R -> starQuantifier
            XRLexer.QUANT_ALT, XRLexer.QUANT_ALT_P, XRLexer.QUANT_ALT_R -> altQuantifier
            XRLexer.QUANT_PLUS, XRLexer.QUANT_PLUS_P, XRLexer.QUANT_PLUS_R -> plusQuantifier
            else -> throw IllegalArgumentException("Unknown quantifier token: $token")
        }
    }
}

