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

package io.github.pdvrieze.formats.xpath.functions

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.functions.impl.*
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.localPart

@OptIn(XPathInternal::class)
enum class Fn(
    localName: String? = null,
    override val functionTypes: List<XdmFunctionType>,
    val implementation: context(ExprEvalContext) (List<XdmValue<*>>) -> XdmValue<*>,
) : XFunction<XdmValue<*>> {
    //region Accessors (2)
    nodeName(Accessors.fnNodeName),
    nilled(Accessors.fnNilled),
    string(Accessors.fnString),
    Data(Accessors.fnData),
    baseUri(Accessors.fnBaseUri),
    documentUri(Accessors.fnDocumentUri),
    //endregion

    //region Errors and diagnostics (3)
    error(ErrorDiagnosticFunctions.fnError),
    trace(ErrorDiagnosticFunctions.fnTrace),
    //endregion

    //region Number functions (4)
    abs(NumericFunctions.fnAbs),
    ceiling(NumericFunctions.fnCeiling),
    floor(NumericFunctions.fnFloor),
    round(NumericFunctions.fnRound),
    roundToHalfEven(NumericFunctions.fnRoundHalfToEven),

    number(NumericFunctions.fnNumber),

    formatInteger(NumericFunctions.fnFormatInteger),
    formatNumber(NumericFunctions.fnFormatNumber),

    //endregion

    //region String functions (5)
    codepointsToString(StringFunctions.fnCodepointsToString),
    stringToCodepoints(StringFunctions.fnStringToCodepoints),

    concat(StringFunctions.fnConcat),
    stringJoin(StringFunctions.fnStringJoin),
    substring(StringFunctions.fnSubstring),
    stringLength(StringFunctions.fnStringLength),
    normalizeSpace(StringFunctions.fnNormalizeSpace),
    normalizeUnicode(StringFunctions.fnNormalizeUnicode),
    upperCase(StringFunctions.fnUpperCase),
    lowerCase(StringFunctions.fnLowerCase),
    translate(StringFunctions.fnTranslate),

    matches(StringFunctions.fnMatches),
    //endregion

    //region Boolean functions (7)
    True(BooleanFunctions.fnTrue),
    False(BooleanFunctions.fnFalse),
    boolean(BooleanFunctions.fnBoolean),
    not(BooleanFunctions.fnNot),
    //endregion

    //region Date/Time functions (9)
    dateTime(DateTimeFunctions.fnDateTime),
    yearFromDateTime(DateTimeFunctions.fnYearFromDateTime),
    monthFromDateTime(DateTimeFunctions.fnMonthFromDateTime),
    dayFromDateTime(DateTimeFunctions.fnDayFromDateTime),
    hoursFromDateTime(DateTimeFunctions.fnHoursFromDateTime),
    minutesFromDateTime(DateTimeFunctions.fnMinutesFromDateTime),
    secondsFromDateTime(DateTimeFunctions.fnSecondsFromDateTime),
    timezoneFromDateTime(DateTimeFunctions.fnTimezoneFromDateTime),
    yearFromDate(DateTimeFunctions.fnYearFromDate),
    monthFromDate(DateTimeFunctions.fnMonthFromDate),
    dayFromDate(DateTimeFunctions.fnDayFromDate),
    timezoneFromDate(DateTimeFunctions.fnTimezoneFromDate),
    hoursFromTime(DateTimeFunctions.fnHoursFromTime),
    minutesFromTime(DateTimeFunctions.fnMinutesFromTime),
    secondsFromTime(DateTimeFunctions.fnSecondsFromTime),
    timezoneFromTime(DateTimeFunctions.fnTimezoneFromTime),

    adjustDateTimeToTimezone(DateTimeFunctions.fnAdjustDateTimeToTimezone),
    adjustDateToTimezone(DateTimeFunctions.fnAdjustDateToTimezone),
    adjustTimeToTimezone(DateTimeFunctions.fnAdjustTimeToTimezone),

    formatDateTime(DateTimeFunctions.fnFormatDateTime),

    //endregion

    //region Node Operations (13)
    Name(NodeFunctions.fnName),
    localName(NodeFunctions.fnLocalName),
    namespaceUri(NodeFunctions.fnNamespaceUri),
    lang(NodeFunctions.fnLang),
    root(NodeFunctions.fnRoot),
    path(NodeFunctions.fnPath),
    hasChildren(NodeFunctions.fnHasChildren),
    innermost(NodeFunctions.fnInnermost),
    outermost(NodeFunctions.fnOutermost),
    //endregion

    //region Sequence operations (14)
    empty(SequenceFunctions.fnEmpty),
    exists(SequenceFunctions.fnExists),
    head(SequenceFunctions.fnHead),
    tail(SequenceFunctions.fnTail),
    insertBefore(SequenceFunctions.fnInsertBefore),
    remove(SequenceFunctions.fnRemove),
    reverse(SequenceFunctions.fnReverse),
    subsequence(SequenceFunctions.fnSubsequence),
    unordered(SequenceFunctions.fnUnordered),

    distinctValues(SequenceFunctions.fnDistincValues),
    indexOf(SequenceFunctions.fnIndexOf),
    deepEqual(SequenceFunctions.fnDeepEqual),

    zeroOrOne(SequenceFunctions.fnZeroOrOne),
    oneOrMore(SequenceFunctions.fnOneOrMore),
    exactlyOne(SequenceFunctions.fnExactlyOne),

    Count(SequenceFunctions.fnCount),
    avg(SequenceFunctions.fnAvg),
    max(SequenceFunctions.fnMax),
    min(SequenceFunctions.fnMin),
    sum(SequenceFunctions.fnSum),
    //endregion

    //region Context functions (15)
    position(ContextFunctions.fnPosition),
    last(ContextFunctions.fnLast),
    currentDateTime(ContextFunctions.fnCurrentDateTime),
    currentDate(ContextFunctions.fnCurrentDate),
    currentTime(ContextFunctions.fnCurrentTime),
    implicitTimezone(ContextFunctions.fnImplicitTimezone),
    defaultCollation(ContextFunctions.fnDefaultCollation),
    defaultLanguage(ContextFunctions.fnDefaultLanguage),
    staticBaseUri(ContextFunctions.fnStaticBaseUri),
    //endregion

    //region higher order functions (16)
    //endregion
    ;

    override val functionName: QName = QName(XMLConstants.XPATH_FUNCTIONS_NAMESPACE, localName ?: name)

    constructor(builtinFunction: BuiltinFunctionImpl<XdmValue<*>>): this(builtinFunction.functionName.localPart, builtinFunction.functionTypes, builtinFunction.evalFunction)

    constructor(
        types: List<XdmFunctionType>,
        implementation: context(ExprEvalContext) (List<XdmValue<*>>) -> XdmValue<*>,
    ): this(null, types, implementation)

    constructor(type: XdmFunctionType, implementation: context(ExprEvalContext) (List<XdmValue<*>>) -> XdmValue<*>):
            this(null, listOf(type), implementation)

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun invoke(args: List<XdmValue<*>>): XdmValue<*> {
        return implementation(args)
    }

    companion object {
        private val functionMap = entries.groupBy { it.functionName.localPart }
            .mapValues { (k, v) -> v.singleOrNull() ?: error("Multiple functions with name $k") }

        fun of(localName: String): Fn? = functionMap[localName]
    }
}
