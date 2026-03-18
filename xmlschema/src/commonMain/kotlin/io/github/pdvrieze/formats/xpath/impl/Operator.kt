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

import io.github.pdvrieze.formats.xpath.XPathVersion

enum class Operator(val literal: String, val priority: Int, val minVersion: XPathVersion = XPathVersion.XPath3_1) {

    @NeedsXPath1 OR("or", 1, XPathVersion.XPath1_0),
    @NeedsXPath1 AND("and", 2, XPathVersion.XPath1_0),
    @NeedsXPath1 UNION("union", 8, XPathVersion.XPath1_0),
    @NeedsXPath2 INTERSECT("intersect", 9, XPathVersion.XPath2_0),
    @NeedsXPath2 EXCEPT("except", 9, XPathVersion.XPath2_0),
    @NeedsXPath1 EQ("=", 3, XPathVersion.XPath1_0),
    @NeedsXPath1 NEQ("!=", 3, XPathVersion.XPath1_0),
    @NeedsXPath1 LT("<", 4, XPathVersion.XPath1_0),
    @NeedsXPath1 LE("<=", 4, XPathVersion.XPath1_0),
    @NeedsXPath1 GT(">", 4, XPathVersion.XPath1_0),
    @NeedsXPath1 GE(">=", 4, XPathVersion.XPath1_0),
    @NeedsXPath2 VAL_EQ("eq", 3, XPathVersion.XPath2_0),
    @NeedsXPath2 VAL_NEQ("neq", 3, XPathVersion.XPath2_0),
    @NeedsXPath2 VAL_LT("lt", 4, XPathVersion.XPath2_0),
    @NeedsXPath2 VAL_LE("le", 4, XPathVersion.XPath2_0),
    @NeedsXPath2 VAL_GT("gt", 4, XPathVersion.XPath2_0),
    @NeedsXPath2 VAL_GE("ge", 4, XPathVersion.XPath2_0),
    @NeedsXPath1 ADD("+", 5, XPathVersion.XPath1_0),
    @NeedsXPath1 SUB("-", 5, XPathVersion.XPath1_0),
    @NeedsXPath1 MUL("*", 6, XPathVersion.XPath1_0),
    @NeedsXPath1 DIV("div", 6, XPathVersion.XPath1_0),
    @NeedsXPath2 IDIV("idiv", 6, XPathVersion.XPath2_0),
    @NeedsXPath1 MOD("mod", 6, XPathVersion.XPath1_0),
    @NeedsXPath2 PRECEDES("<<", 1, XPathVersion.XPath2_0),
    @NeedsXPath2 FOLLOWS(">>", 1, XPathVersion.XPath2_0),
    @NeedsXPath2 IS("is", 1, XPathVersion.XPath2_0),
    @NeedsXPath3_0 CONCAT("||", 7, XPathVersion.XPath3_0)
    ;

}
