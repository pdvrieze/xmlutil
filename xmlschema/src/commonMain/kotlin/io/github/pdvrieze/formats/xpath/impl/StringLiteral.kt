/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xpath.impl

import nl.adaptivity.xmlutil.XmlWriter

@XPathInternal
internal class StringLiteral(value: String) : LiteralExpr<String>(value) {
    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        var hasQuot = false
        var hasApos = false
        for (c in value) {
            when(c) {
                '\'' -> hasApos = true
                '"' -> hasQuot = true
            }
            if (hasQuot && hasApos) break
        }
        when {
            !hasQuot -> {
                builder.append('"').append(value).append('"')
                return
            }

            !hasApos -> {
                builder.append('\'').append(value).append('\'')
                return
            }
        }
        builder.append('"')
        for (c in value) {
            when(c) {
                '"' -> builder.append("\"\"")
                else -> builder.append(c)
            }
        }
        builder.append('"')
    }
}
