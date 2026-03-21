/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.XPathVersion

enum class Keywords(override val literal: String, override val minVersion: XPathVersion = XPathVersion.XPath1_0) : WordToken {
    AS("as"),
    EVERY("every", XPathVersion.XPath2_0),
    FOR("for", XPathVersion.XPath2_0),
    IF("if", XPathVersion.XPath2_0),
    THEN("then", XPathVersion.XPath2_0),
    ELSE("else", XPathVersion.XPath2_0),
    IN("in"),
    LET("let", XPathVersion.XPath3_0),
    RETURN("return"),
    SOME("some", XPathVersion.XPath2_0),
    SATISFIES("satisfies", XPathVersion.XPath2_0),
    INSTANCE("instance", XPathVersion.XPath2_0),
    OF("of", XPathVersion.XPath2_0),
    TREAT("treat", XPathVersion.XPath2_0),
    CASTABLE("castable", XPathVersion.XPath2_0),
    CAST("cast", XPathVersion.XPath2_0),
    ;

    override val isDelimiting: Boolean get() = false
}

internal interface WordToken: Token {
    val literal: String
    val minVersion: XPathVersion get() = XPathVersion.XPath1_0
}
