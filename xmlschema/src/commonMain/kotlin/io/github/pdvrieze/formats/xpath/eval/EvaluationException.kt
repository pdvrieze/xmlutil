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
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdQName

open class EvaluationException : Exception {
    val expr: Expr
    val errorCode: ErrorCodes?

    constructor(expr: Expr, message: String?) : super(message) {
        this.expr = expr
        this.errorCode = null
    }

    constructor(expr: Expr, message: String?, cause: Throwable?) : super(message, cause) {
        this.expr = expr
        this.errorCode = null
    }

    constructor(expr: Expr, cause: Throwable?) : super("Evaluation of expression $expr failed", cause) {
        this.expr = expr
        this.errorCode = null
    }

    constructor(expr: Expr) : super("Evaluation of expression $expr failed") {
        this.expr = expr
        this.errorCode = null
    }

    constructor(errorCode: ErrorCodes, expr: Expr, message: String?) : super(message ?: errorCode.message) {
        this.expr = expr
        this.errorCode = errorCode
    }

    constructor(qName: XsdQName, expr: Expr, message: String) : super(message) {
        this.expr = expr
        this.errorCode = ErrorCodes.lookup(qName)
    }

    constructor(errorCode: ErrorCodes, expr: Expr, message: String?, cause: Throwable?) : super(message ?: errorCode.message, cause) {
        this.expr = expr
        this.errorCode = errorCode
    }

    constructor(errorCode: ErrorCodes, expr: Expr, cause: Throwable?) : super(errorCode.message, cause) {
        this.expr = expr
        this.errorCode = errorCode
    }

    constructor(errorCode: ErrorCodes, expr: Expr) : super(errorCode.message) {
        this.expr = expr
        this.errorCode = errorCode
    }

    @Suppress("NOTHING_TO_INLINE")
    @OptIn(XPathInternal::class)
    companion object {

        context(ctx: ExprEvalContext)
        inline operator fun invoke(message: String?): EvaluationException {
            return EvaluationException(ctx.expr, message)
        }

        context(ctx: ExprEvalContext)
        inline operator fun invoke(errorCode: ErrorCodes, message: String?): EvaluationException {
            return EvaluationException(errorCode, ctx.expr, message)
        }

        context(ctx: ExprEvalContext)
        inline operator fun invoke(errorCode: ErrorCodes, message: String?, cause: Throwable?): EvaluationException {
            return EvaluationException(errorCode, ctx.expr, message, cause)
        }

        context(ctx: ExprEvalContext)
        inline operator fun invoke(errorCode: ErrorCodes, cause: Throwable?): EvaluationException {
            return EvaluationException(errorCode, ctx.expr, cause)
        }

        context(ctx: ExprEvalContext)
        inline operator fun invoke(errorCode: ErrorCodes): EvaluationException {
            return EvaluationException(errorCode, ctx.expr)
        }

    }

}

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
