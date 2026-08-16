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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.type.XdmMapType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmMapTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction.Companion.MAP_NAMESPACE
import io.github.pdvrieze.formats.xpath.functions.xdmArg
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType.SINGLE
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import io.github.pdvrieze.xml.schematypes.values.*
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import nl.adaptivity.xmlutil.QName

@XPathInternal
class XdmMap private constructor(
    private val content: HashMap<XsdAtomic, Pair<XdmAtomic<*>, XdmValue<*>>>,
    override val staticType: XdmMapType,
    private val _dynamicType: Lazy<XdmMapType>,
) : XdmFunction<XdmMap>() {
    override fun asT(): XdmMap = this

    override val maybeName: QName get() = QName(MAP_NAMESPACE, "get", "map")

    override val dynamicType: XdmFunctionType
        get() = _dynamicType.value

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue<*> {
        if (type !is XdmMapTypeTest) throw EvaluationException("Cannot cast map to $type")
        return XdmMap(content, type.toValueType(staticType).single as XdmMapType, _dynamicType)
    }

    val mapSize: Int get() = content.size

    val mapIsEmpty: Boolean get() = content.isEmpty()

    val entries: Collection<Pair<XdmAtomic<*>, XdmValue<*>>>
        get() = content.values

    val keys: Collection<XdmAtomic<*>>
        get() = content.values.map { it.first }

    val values: Collection<XdmValue<*>>
        get() = content.values.map { it.second }

    context(ctx: ExprEvalContext)
    fun get(key: XdmAtomic<*>): XdmValue<*>? {
        return content[normalizeKey(key.value)]?.second
    }

    context(ctx: ExprEvalContext)
    fun put(key: XdmAtomic<*>, value: XdmValue<*>): XdmMap {
        val newMap = HashMap(content)
        newMap[normalizeKey(key.value)] = Pair(key, value)
        return XdmMap(newMap, staticType, _dynamicType)
    }

    context(ctx: ExprEvalContext)
    fun remove(keys: XdmValue<XdmAtomic<*>>): XdmMap {
        val normalizedKeys = keys.mapTo(HashSet()) { normalizeKey(it.value) }
        val changed = content.filterTo(HashMap()) { (k, _) -> k !in normalizedKeys }
        when {
            changed.size == content.size -> return this
            else -> return XdmMap(changed, staticType, _dynamicType)
        }
    }

    context(ctx: ExprEvalContext)
    fun containsKey(key: XdmAtomic<*>): Boolean {
        return content.containsKey(normalizeKey(key.value))
    }

    fun containsValue(value: XdmValue<*>): Boolean {
        return content.any { (_, v) -> v.second == value }
    }

    context(ctx: ExprEvalContext)
    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        if (expected !is XdmMap) return false
        if (expected.content.size != content.size) return false

        for ((k, v) in content) {
            if (expected.content[k] != v) return false
        }
        return true
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(other: XdmValue<*>, collation: Collation?): Boolean {
        if (other !is XdmMap) return false
        if (other.content.size != content.size) return false

        for ((k, kv) in content) {
            val (_, otherVal) = other.content[k] ?: return false
            if (!kv.second.isDeepEqual(otherVal, collation)) return false
        }
        return true
    }

    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: XdmSequence.XdmSequenceBuilder<XdmAtomic<*>>) {
        throw EvaluationException(ErrorCodes.FOTY0013, "Cannot atomize a map")
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        throw EvaluationException(ErrorCodes.FOTY0013, "Cannot atomize a map")
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Nothing {
        throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE, "Cannot cast maps to boolean")
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun normalizeToArithmetic(): XdmValue<*> {
        throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Maps are not compatible with an arithmetic operator")
    }

    context(ctx: ExprEvalContext)
    override fun invoke(args: List<XdmValue<*>>): XdmValue<*> {
        if (args.size!= 1) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val arg = args.xdmArg<XdmAtomic<*>>(0)
        val k = normalizeKey(arg.value)
        return content[k]?.second ?: XdmSequence.EMPTY
    }

    private object NaN: XsdAtomic {
        override val schemaType: AnyAtomicType<XsdAtomic> get() = DoubleType.Instance
        override val xmlString: String get() = "NaN"
    }




    companion object {
        context(ctx: ExprEvalContext)
        operator fun invoke(content: Map<XdmAtomic<*>, XdmValue<*>>, staticType: XdmMapType): XdmMap {
            val newMap = content.entries.associateTo(HashMap()) { (k, v) -> normalizeKey(k.value) to Pair(k, v) }
            return XdmMap(newMap, staticType, lazy { dynamicMapType(content) })
        }


        context(ctx: ExprEvalContext)
        fun merge(maps: Iterable<XdmMap>, duplicates: XdmMap.DuplicateHandling): XdmMap {
            val mapIt = maps.iterator()

            val resultMap = HashMap<XsdAtomic, Pair<XdmAtomic<*>, XdmValue<*>>>()

            while (mapIt.hasNext()) {
                for ((key, value) in mapIt.next().content.entries) {
                    when (duplicates) {
                        DuplicateHandling.REJECT -> if(resultMap.put(key, value)!=null) throw EvaluationException(ErrorCodes.FOJS0003, "Duplicate key $key already exists")
                        DuplicateHandling.USE_FIRST -> resultMap.put(key, value)?.let { resultMap.put(key, it) } // revert back to original
                        DuplicateHandling.USE_LAST,
                        DuplicateHandling.USE_ANY -> resultMap[key] = value
                        DuplicateHandling.COMBINE -> resultMap.put(key, value)?.let {
                            val combined = XdmSequence.build<XdmSingleValue<*>> {
                                add(it.second)
                                add(value.second)
                            }
                            resultMap.put(key, Pair(it.first, combined))
                        }
                    }
                }
            }


            val mapType = maps.first().staticType
            return XdmMap(resultMap, mapType, lazy { mapType })
        }

        context(ctx: ExprEvalContext)
        private fun normalizeKey(key: XsdAtomic): XsdAtomic = when(key) {
            is XsdDouble if key.value.isNaN() -> NaN
            is XsdDouble -> {
                val lVal = key.toLong()
                val bd = BigDecimal(key.value)
                when {
                    bd.toLong() != lVal -> bd
                    lVal in Int.MIN_VALUE..Int.MAX_VALUE -> XsdInt(lVal.toInt())
                    else -> XsdLong(lVal)
                }
            }

            is XsdFloat if key.value.isNaN() -> NaN
            is XsdFloat -> {
                val lVal = key.toLong()
                val bd = BigDecimal(key.value)
                when {
                    bd.toLong() != lVal -> bd
                    lVal in Int.MIN_VALUE..Int.MAX_VALUE -> XsdInt(lVal.toInt())
                    else -> XsdLong(lVal)
                }
            }

            is XsdShort, is XsdUnsignedShort,
            is XsdByte, is XsdUnsignedShort, -> XsdInt(key.toInt())
            is XsdInt -> key
            is XsdUnsignedInt -> when {
                key.uIntValue <= Int.MAX_VALUE.toUInt() -> XsdInt(key.toInt())
                else -> XsdLong(key.toLong())
            }

            is XsdLong -> key.longValue.let {
                if (it in Int.MIN_VALUE..Int.MAX_VALUE) XsdInt(it.toInt()) else key
            }

            is XsdUnsignedLong -> when {
                key.uLongValue < Int.MAX_VALUE.toULong() -> XsdInt(key.toInt())
                key.uLongValue < Long.MAX_VALUE.toULong() -> XsdLong(key.toLong())
                else -> BigInt(key)
            }

            is XsdInteger -> when (key.significantBitsFromZero()) {
                in 0uL..31uL -> XsdInt(key.toInt())
                in 33uL..63uL -> XsdLong(key.toLong())
                else if (key is BigInt) -> key
                else -> BigInt(key)
            }

            is XsdDecimal -> {
                val r = key.toBigDecimal().reduceDecimalDigits()
                when {
                    r.exponent < 0 -> r
                    else -> {
                        val i = r.roundToInteger()
                        when (i.significantBitsFromZero()) {
                            in 0uL..31uL -> XsdInt(i.toInt())
                            in 33uL..63uL -> XsdLong(i.toLong())
                            else -> i
                        }
                    }
                }
            }

            is XsdString, is XsdAnyURI, is UntypedAtomicType.XsdUntyped -> XsdString(key.xmlString)

            is XsdYearMonthDuration -> XsdDuration(key.months, key.millis)

            is XsdDayTimeDuration -> XsdDuration(key.months, key.millis)

            is XsdDateTime -> key

            is IXsdDateTime -> key.toStandardDateTime()

            is XsdNotation -> XsdNotation(key.getNamespaceURI(), key.getLocalPart())

            is XsdQName -> XsdQName(key.getNamespaceURI(), key.getLocalPart())

            else -> key
        }


        context(ctx: ExprEvalContext)
        fun dynamicMapType(content: Map<XdmAtomic<*>, XdmValue<*>>): XdmMapType {
            if (content.isEmpty()) {
                return XdmMapType(XdmSchemaTypeTest.ANY_ATOMIC, XdmTypeTest.ANY_ITEM.any)
            }

            val it = content.entries.iterator()

            var keyBaseSchemaType: AnySimpleType<*>
            var valueBaseType: XdmSequenceTypeTest
            it.next().let { (k, v) ->
                keyBaseSchemaType = k.dynamicType.schemaType as AnySimpleType<*>
                valueBaseType = v.staticType.toTypeTest()
            }

            while (it.hasNext()) {
                val (k, v) = it.next()
                val ks = k.dynamicType.schemaType
                val vt = v.staticType.toTypeTest()
                while (! ks.derivesFrom(keyBaseSchemaType)) {
                    keyBaseSchemaType = keyBaseSchemaType.baseType as AnySimpleType<*> // cast must work as all types must derive from AnySimple
                }
                while (! valueBaseType.isAssignableFrom(vt)) {
                    valueBaseType = valueBaseType.sharedBaseType(vt)
                }

            }

            return XdmMapType(XdmSchemaTypeTest(keyBaseSchemaType, SINGLE), valueBaseType)
        }
    }

    enum class DuplicateHandling(val txt: String) {
        REJECT("reject"),
        USE_FIRST("use-first"),
        USE_LAST("use-last"),
        USE_ANY("use-any"),
        COMBINE("combine"),
        ;

        companion object {
            fun fromString(str: String): DuplicateHandling? =
                entries.firstOrNull { it.txt == str }
        }
    }

}
