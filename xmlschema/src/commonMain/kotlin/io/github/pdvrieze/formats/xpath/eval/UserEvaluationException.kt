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

package io.github.pdvrieze.formats.xpath.eval

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.xml.schematypes.values.XsdQName

class UserEvaluationException: EvaluationException {
    val qName: XsdQName
    val context: XdmValue<*>?

    constructor(qName: XsdQName, expr: Expr, description: String, context: XdmValue<*>? = null) :
            super(qName, expr, description) {
        this.qName = qName
        this.context = context
    }

    constructor(errorCode: ErrorCodes, expr: Expr, description: String, context: XdmValue<*>? = null) : super(errorCode, expr, description) {
        this.qName = errorCode.qName
        this.context = context
    }

}
