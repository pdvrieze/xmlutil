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

package io.github.pdvrieze.formats.xpath.impl

enum class Operator(val literal: String, val priority: Int) {

    OR("or", 1),
    AND("and", 2),
    UNION("union", 7),
    EQ("=", 3),
    NEQ("!=", 3),
    LT("<", 4),
    LE("<=", 4),
    GT(">", 4),
    GE(">=", 4),
    VAL_EQ("eq", 3),
    VAL_NEQ("neq", 3),
    VAL_LT("lt", 4),
    VAL_LE("le", 4),
    VAL_GT("gt", 4),
    VAL_GE("ge", 4),
    ADD("+", 5),
    SUB("-", 5),
    MUL("*", 6),
    DIV("div", 6),
    IDIV("idiv", 6),
    MOD("mod", 6),
    PRECEDES("<<", 1),
    FOLLOWS(">>", 1),
    IS("is", 1),
    ;

}
