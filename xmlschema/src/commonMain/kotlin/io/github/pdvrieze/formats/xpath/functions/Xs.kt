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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.functions.impl.AbstractFunctionObject
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.RangeException
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.*
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.InfBigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdQNameImpl
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
object Xs: AbstractFunctionObject() {

    object constructAnyType: ConstructorBase<Nothing>("anyType") {
        override val returnSchemaType: Nothing
            get() = throw UnsupportedOperationException("AnyType cannot be instantiated")

        context(ctx: ExprEvalContext)
        override fun invoke(arg: XdmAtomic<*>): Nothing {
            throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH, "Function not found with the given arguments")
        }
    }

    object constructAnySimpleType: ConstructorBase<Nothing>("anySimpleType") {
        override val returnSchemaType: AnySimpleType<*> get() = AnySimpleType.Instance

        context(ctx: ExprEvalContext)
        override fun invoke(arg: XdmAtomic<*>): Nothing {
            throw EvaluationException("Cannot instantiate anySimpleType, use a subtype")
        }
    }

    object constructAnyAtomicType: ConstructorBase<Nothing>("anyAtomicType") {
        override val returnSchemaType: AnyAtomicType<*> get() = AnyAtomicType.Instance

        context(ctx: ExprEvalContext)
        override fun invoke(arg: XdmAtomic<*>): Nothing {
            throw EvaluationException("Cannot instantiate anyAtomicType, use a subtype")
        }

    }

    object constructUntypedAtomic: AtomicConstructor<XsdAtomic>(UntypedAtomicType.Instance)

    object constructAnyURI: AtomicConstructor<XsdAnyURI>(AnyURIType.Instance)

    object constructBase64Binary: AtomicConstructor<XsdBase64Binary>(Base64BinaryType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdBase64Binary {
            return try {
                XsdBase64Binary(arg.value.xmlString)
            } catch (e: IllegalArgumentException) {
                throw EvaluationException(ErrorCodes.FORG0001, "Cannot convert '${arg.value.xmlString}' to a base64Binary")
            }
        }
    }

    object constructBoolean: AtomicConstructor<XsdBoolean>(BooleanType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdBoolean = XsdBoolean(arg.toBoolean())
    }

    object constructDate: AtomicConstructor<XsdDate>(DateType.Instance)

    object constructDateTime: AtomicConstructor<XsdDateTime>(DateTimeType.Instance)

    object constructDateTimeStamp: AtomicConstructor<XsdDateTime>(DateTimeStampType.Instance)

    object constructNumeric: ConstructorBase<XdmAtomicOrEmpty<XdmAtomic<XsdNumeric<*>>>>("numeric") {
        override val returnSchemaType: NumericType<*> get() = NumericType.Instance

        context(ctx: ExprEvalContext)
        override fun invoke(arg: XdmAtomic<*>): XdmAtomicOrEmpty<XdmAtomic<XsdNumeric<*>>> {
            val v = arg.value
            when {
                v is XsdBoolean -> return when (v.value) {
                    true -> XdmAtomic(XsdDouble(1.0))
                    else -> XdmAtomic(XsdDouble(0.0))
                }
                arg.dynamicType.schemaType == UntypedAtomicType.Instance ||
                v is XsdString -> {
                    val d = v.xmlString.toDoubleOrNull()
                        ?: throw EvaluationException(ErrorCodes.FORG0001, "Cannot convert '${v.xmlString}' to a double")
                    return XdmAtomic(XsdDouble(d))
                }

                else -> throw EvaluationException(ErrorCodes.FORG0001, "Cannot convert '${v.xmlString}' of type ${arg.dynamicType} to a number")
            }
        }
    }

    object constructDecimal: AtomicConstructor<XsdDecimal>(DecimalType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdDecimal {
            when (val origVal = arg.value) {
                is InfBigDecimal -> return origVal
                is XsdDecimal -> return origVal.toBigDecimal()
                is XsdFloat -> {
                    if (!origVal.value.isFinite()) throw EvaluationException(
                        ErrorCodes.FORG0001,
                        "Value '${arg.value.xmlString}' out of supported range for decimal"
                    )
                    return BigDecimal(origVal.xmlString)
                }
                is XsdDouble -> {
                    if (!origVal.value.isFinite()) throw EvaluationException(
                        ErrorCodes.FORG0001,
                        "Value '${arg.value.xmlString}' out of supported range for decimal"
                    )
                    return BigDecimal(origVal.xmlString)
                }
                else -> {
                    val infDecimal = InfBigDecimal(origVal.xmlString)
                    if (!infDecimal.isFinite) {
                        throw EvaluationException(ErrorCodes.FORG0001, "Decimal does not support NaN or Infinite values")
                    }
                    return BigDecimal(infDecimal)
                }
            }
            val value = super.constructXsd(arg)
            if (! value.isFinite) throw EvaluationException(ErrorCodes.FORG0001, "Value '${arg.value.xmlString}' out of supported range for decimal")
            return value
        }
    }

    object constructInteger: AtomicConstructor<XsdInteger>(IntegerType.Instance)

    object constructLong: AtomicConstructor<XsdLong>(LongType.Instance)

    object constructInt: AtomicConstructor<XsdInt>(IntType.Instance)

    object constructShort: AtomicConstructor<XsdShort>(ShortType.Instance)

    object constructByte: AtomicConstructor<XsdByte>(ByteType.Instance)

    object constructNonNegativeInteger: AtomicConstructor<XsdNonNegativeInteger>(NonNegativeIntegerType.Instance)

    object constructPositiveInteger: AtomicConstructor<XsdPositiveInteger>(PositiveIntegerType.Instance)

    object constructUnsignedLong: AtomicConstructor<XsdUnsignedLong>(UnsignedLongType.Instance)

    object constructUnsignedInt: AtomicConstructor<XsdUnsignedInt>(UnsignedIntType.Instance)

    object constructUnsignedShort: AtomicConstructor<XsdUnsignedShort>(UnsignedShortType.Instance)

    object constructUnsignedByte: AtomicConstructor<XsdUnsignedByte>(UnsignedByteType.Instance)

    object constructNonPositiveInteger: AtomicConstructor<XsdNonPositiveInteger>(NonPositiveIntegerType.Instance)

    object constructNegativeInteger: AtomicConstructor<XsdNegativeInteger>(NegativeIntegerType.Instance)

    object constructDouble: AtomicConstructor<XsdDouble>(DoubleType.Instance)

    object constructDuration: AtomicConstructor<XsdDuration>(DurationType.Instance)

    object constructDayTimeDuration: AtomicConstructor<XsdDayTimeDuration>(DayTimeDurationType.Instance)

    object constructYearMonthDuration: AtomicConstructor<XsdYearMonthDuration>(YearMonthDurationType.Instance)

    object constructFloat: AtomicConstructor<XsdFloat>(FloatType.Instance)

    object constructGDay: AtomicConstructor<XsdGDay>(GDayType.Instance)

    object constructGMonth: AtomicConstructor<XsdGMonth>(GMonthType.Instance)

    object constructGMonthDay: AtomicConstructor<XsdGMonthDay>(GMonthDayType.Instance)

    object constructGYear: AtomicConstructor<XsdGYear>(GYearType.Instance)

    object constructGYearMonth: AtomicConstructor<XsdGYearMonth>(GYearMonthType.Instance)

    object constructHexBinary: AtomicConstructor<XsdHexBinary>(HexBinaryType.Instance)

    object constructNOTATION: AtomicConstructor<XsdNotation>(NotationType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): Nothing =
            throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH, "NOTATION cannot be instantiated")
    }

    object constructQName: AtomicConstructor<XsdQName>(QNameType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdQName {
            if (arg.value !is XsdString) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)
            val str = arg.value.xmlString
            val cIdx = str.indexOf(':')
            val prefix: String
            val localName: String
            if (cIdx < 0) { prefix = ""; localName = str } else { prefix = str.substring(0, cIdx); localName = str.substring(cIdx + 1) }

            val namespace = ctx.namepaceContext.getNamespaceURI(prefix)
                ?: throw EvaluationException(ErrorCodes.FONS0004, "Namespace prefix '$prefix' not bound to namespace")

            return XsdQName(namespace, localName, prefix)
        }
    }

    object constructString: AtomicConstructor<XsdString>(StringType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdString = XsdString(arg.value.xmlString)
    }

    object constructNormalizedString: AtomicConstructor<XsdNormalizedString>(NormalizedStringType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdNormalizedString = XsdNormalizedString(arg.value.xmlString)
    }

    object constructToken: AtomicConstructor<XsdToken>(TokenType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdToken = XsdToken(arg.value.xmlString)
    }

    object constructLanguage: AtomicConstructor<XsdLanguage>(LanguageType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdLanguage = XsdLanguage(arg.value.xmlString)
    }

    object constructName: AtomicConstructor<XsdName>(NameType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdName = XsdName(arg.value.xmlString)
    }

    object constructNCName: AtomicConstructor<XsdNCName>(NCNameType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdNCName = XsdNCName(arg.value.xmlString)
    }

    object constructENTITY: AtomicConstructor<XsdEntity>(EntityType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdEntity = XsdEntity(arg.value.xmlString)
    }

    object constructID: AtomicConstructor<XsdID>(IDType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdID = XsdID(arg.value.xmlString)
    }

    object constructIDREF: AtomicConstructor<XsdIDRef>(IDRefType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdIDRef = XsdIDRef(arg.value.xmlString)
    }

    object constructNMTOKEN: AtomicConstructor<XsdNMToken>(NMTokenType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdNMToken = XsdNMToken(arg.value.xmlString)
    }

    object constructTime: AtomicConstructor<XsdTime>(TimeType.Instance) {
        context(ctx: ExprEvalContext)
        override fun constructXsd(arg: XdmAtomic<*>): XsdTime = XsdTime(arg.value.xmlString)
    }

    object constructENTITIES: ListConstructor<XsdEntities, XsdEntity>("ENTITIES", EntitiesType.Instance) {
        override fun invoke(arg: String): XsdEntity = XsdEntity(arg)
    }

    object constructIDREFS: ListConstructor<XsdIDRefs, XsdIDRef>("IDREFS", IDRefsType.Instance) {
        override fun invoke(arg: String): XsdIDRef = XsdIDRef(arg)
    }

    object constructNMTOKENS: ListConstructor<XsdNMTokens, XsdNMToken>("NMTOKENS", NMTokensType.Instance) {
        override fun invoke(arg: String): XsdNMToken = XsdNMToken(arg)
    }


    context(ctx: ExprEvalContext)
    fun createFromSchemaType(name: QName): BuiltinFunction<*> {
        /*
        val type = ctx.resolveTypeOrNull(name) as AnyAtomicType<*>
        */
        if (name.namespaceURI == XMLConstants.XSD_NS_URI) {
            val resolvedConstructor = knownTypes[name.localPart]
                ?: throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH, "Constructor function $name not found with the given arguments")

            return resolvedConstructor
        }
        throw EvaluationException("Cannot contstruct instances in namespace ${name.namespaceURI}")
    }


    val knownTypes: Map<String, ConstructorBase<*>> = arrayOf(
        constructAnyType, constructAnySimpleType, constructAnyAtomicType, constructAnyURI, constructBase64Binary,
        constructBoolean, constructDate, constructDateTime, constructDateTimeStamp, constructDecimal,
        constructInteger, constructLong, constructInt, constructShort, constructByte,
        constructNonNegativeInteger, constructPositiveInteger, constructUnsignedLong, constructUnsignedInt,
        constructUnsignedShort, constructUnsignedByte, constructNonPositiveInteger, constructNegativeInteger,
        constructDouble, constructDuration, constructDayTimeDuration, constructYearMonthDuration,
        constructFloat, constructGDay, constructGMonth, constructGMonthDay, constructGYear,
        constructGYearMonth, constructHexBinary, constructNOTATION, constructQName, constructString,
        constructNormalizedString, constructToken, constructLanguage, constructName, constructNCName,
        constructENTITY, constructID, constructIDREF, constructNMTOKEN, constructTime, constructENTITIES,
        constructIDREFS, constructNMTOKENS, constructNumeric, constructUntypedAtomic,
    ).groupBy { it.localName }
        .mapValues { (k, v) -> v.singleOrNull() ?: throw IllegalArgumentException("Multiple bindings to type $k") }

    abstract class ConstructorBase<out XR : XdmAtomicOrSequence<*>>(localName: String) :
        BuiltinFunction<XR> {

        final override val functionName: QName = XsdQNameImpl(XMLConstants.XSD_NS_URI, localName)
        val localName: String get() = functionName.localPart

        abstract val returnSchemaType: AnySimpleType<*>

        final override val functionTypes: List<XdmFunctionType>
            get() = listOf(functionType(returnSchemaType, ATOMIC.opt))

        context(ctx: ExprEvalContext)
        abstract fun invoke(arg: XdmAtomic<*>): XR

        context(ctx: ExprEvalContext)
        final override fun invoke(args: List<XdmValue<*>>): XR {
            val arg = when (val arg = args.singleArg<XdmAtomicOrEmpty<*>>()) {
                is XdmSequence.EMPTY -> {
                    @Suppress("UNCHECKED_CAST")
                    return XdmSequence.EMPTY as XR
                }

                else -> arg as XdmAtomic<*>
            }
            // Must be the same type, not a derived one.
            if (arg.value.schemaType.name isEquivalent returnSchemaType.name) {
                @Suppress("UNCHECKED_CAST")
                return arg as XR
            }

            return invoke(arg)
        }
    }

    abstract class AtomicConstructor<R : XsdAtomic>(override val returnSchemaType: AnyAtomicType<R>) :
        ConstructorBase<XdmAtomicOrEmpty<XdmAtomic<R>>>(returnSchemaType.name!!.localPart) {

        context(ctx: ExprEvalContext)
        open fun constructXsd(arg: XdmAtomic<*>): R {
            try {
                return returnSchemaType.castFrom(arg.value)
            } catch (e: RangeException) {
                val errorCode = rangeErrorCodeFor(returnSchemaType)
                throw EvaluationException(errorCode, "Value '${arg.value.xmlString}' out of supported range for ${returnSchemaType.name}", e)
            } catch (e: NumberFormatException) {
                val errorCode = rangeErrorCodeFor(returnSchemaType)
                throw EvaluationException(ErrorCodes.FORG0001, "Cannot convert '${arg.value.xmlString}' to a ${returnSchemaType.name}", e)
            }
        }

        context(ctx: ExprEvalContext)
        final override fun invoke(arg: XdmAtomic<*>): XdmAtomic<R> = XdmAtomic(constructXsd(arg))

        fun rangeErrorCodeFor(type: AnyAtomicType<*>): ErrorCodes = when (type) {
            is IntegerType -> ErrorCodes.FOCA0003
            is DecimalType<*> -> ErrorCodes.FOCA0001
            is DateType,
            is TimeType,
            is DateTimeType -> ErrorCodes.FODT0001
            else -> ErrorCodes.FORG0001
        }

    }

    abstract class ListConstructor<T: XsdAnySimple, E: XsdAtomic>(localName: String, override val returnSchemaType: AnySimpleListType<T, E>):
        ConstructorBase<XdmAtomicOrSequence<XdmAtomic<*>>>(localName) {

        abstract fun invoke(arg: String): E

        context(ctx: ExprEvalContext)
        override fun invoke(arg: XdmAtomic<*>): XdmAtomicOrSequence<XdmAtomic<E>> {
            return XdmSequence.buildAtomic {
                arg.value.xmlString.splitToSequence(' ')
                    .filter { it.isNotEmpty() }
                    .forEach { this.add(XdmAtomic(invoke(it))) }
            }
        }

    }

}
