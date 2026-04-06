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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.checkArgCount
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.XsdDayTimeDuration
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdLanguage
import io.github.pdvrieze.xml.schematypes.values.XsdString

@XPathInternal
object ContextFunctions : AbstractFunctionObject() {

    /** Returns the context position from the dynamic context.*/
    val fnPosition = BuiltinFunctionImpl("position", functionType(INTEGER)) { args ->
        args.checkArgCount(0)
        val item = contextOf<ExprEvalContext>().contextItem ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        XdmAtomic(XsdInteger(item.position))
    }

    /** Returns the context size from the dynamic context.*/
    val fnLast = BuiltinFunctionImpl("last", functionType(INTEGER)) { args ->
        args.checkArgCount(0)
        val item = contextOf<ExprEvalContext>().contextItem ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        XdmAtomic(XsdInteger(item.last))
    }

    /** Returns the current date and time (with timezone).*/
    val fnCurrentDateTime = BuiltinFunctionImpl("current-dateTime", functionType(t(DateTimeStampType.Instance))) { args ->
        args.checkArgCount(0)
        XdmAtomic(contextOf<ExprEvalContext>().currentTimeStamp)
    }

    /** Returns the current date.*/
    val fnCurrentDate = BuiltinFunctionImpl("current-date", functionType(t(DateType.Instance))) { args ->
        args.checkArgCount(0)
        XdmAtomic(contextOf<ExprEvalContext>().currentTimeStamp.toDate())
    }

    /** Returns the current time.*/
    val fnCurrentTime = BuiltinFunctionImpl("current-time", functionType(t(TimeType.Instance))) { args ->
        args.checkArgCount(0)
        XdmAtomic(contextOf<ExprEvalContext>().currentTimeStamp.toTime())
    }

    /** Returns the value of the implicit timezone property from the dynamic context.*/
    val fnImplicitTimezone = BuiltinFunctionImpl("implicit-timezone", functionType(t(DayTimeDurationType.Instance))) { args ->
        args.checkArgCount(0)
        val timezoneOffset = contextOf<ExprEvalContext>().currentTimeStamp.timezoneOffset
        XdmAtomic(XsdDayTimeDuration.ofMinutes(timezoneOffset))
    }

    /** Returns the value of the default collation property from the static context.*/
    val fnDefaultCollation = BuiltinFunctionImpl("default-collation", functionType(STRING)) { args ->
        XdmAtomic(XsdString(contextOf<ExprEvalContext>().defaultCollation.uri))
    }

    /** Returns the value of the default language property from the dynamic context.*/
    val fnDefaultLanguage = BuiltinFunctionImpl("default-language", functionType(t(LanguageType.Instance))) { args ->
        args.checkArgCount(0)
        XdmAtomic(XsdLanguage("en"))
    }

    /** This function returns the value of the static base URI property from the static context.*/
    val fnStaticBaseUri = BuiltinFunctionImpl("static-base-uri", functionType(t(AnyURIType.Instance))) { args ->
        args.checkArgCount(0)
        contextOf<ExprEvalContext>().baseUri?.let{ XdmAtomic(it) } ?: XdmSequence.EMPTY
    }

}
