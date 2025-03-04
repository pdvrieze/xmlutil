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

@file:Suppress("DEPRECATION") // Char.toInt()
package io.github.pdvrieze.formats.xmlschema.regex.impl

import io.github.pdvrieze.formats.xmlschema.regex.impl.sets.XBitSet
import io.github.pdvrieze.formats.xmlschema.regex.impl.sets.set
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import nl.adaptivity.xmlutil.XmlUtilInternal

/**
 * Unicode category (i.e. Ll, Lu).
 */
internal open class XRUnicodeCategory(protected val category: Int) : XRAbstractCharClass() {
    override fun contains(ch: Int): Boolean = alt xor (ch.toChar().category.ordinal == category)
}

/**
 * Unicode category scope (i.e IsL, IsM, ...)
 */
internal class XRUnicodeCategoryScope(category: Int) : XRUnicodeCategory(category) {
    override fun contains(ch: Int): Boolean {
        return alt xor (((category shr ch.toChar().category.ordinal) and 1) != 0)
    }
}

/**
 * This class represents character classes, i.e. sets of character either predefined or user defined.
 * Note: this class represent a token, not node, so being constructed by lexer.
 */
@OptIn(XmlUtilInternal::class)
internal abstract class XRAbstractCharClass : XRSpecialToken() {
    /**
     * Show if the class has alternative meaning:
     * if the class contains character 'a' and alt == true then the class will contains all characters except 'a'.
     */
    internal var alt: Boolean = false
    internal var altSurrogates: Boolean = false

    /**
     * For each unpaired surrogate char indicates whether it is contained in this char class.
     */
    internal val lowHighSurrogates = XBitSet(SURROGATE_CARDINALITY) // Bit set for surrogates?

    /**
     * Indicates if this class may contain supplementary Unicode codepoints.
     * If this flag is specified it doesn't mean that this class contains supplementary characters but may contain.
     */
    var mayContainSupplCodepoints = false
        protected set

    /** Returns true if this char class contains character specified. */
    abstract operator fun contains(ch: Int): Boolean
    open fun contains(ch: Char): Boolean = contains(ch.toInt())

    /**
     * Returns BitSet representing this character class or `null`
     * if this character class does not have character representation;
     */
    open internal val bits: XBitSet?
        get() = null

    fun hasLowHighSurrogates(): Boolean {
        return if (altSurrogates)
            lowHighSurrogates.nextClearBit(0) != -1
        else
            lowHighSurrogates.nextSetBit(0) != -1
    }

    override val type: Type = Type.CHARCLASS

    open val instance: XRAbstractCharClass
        get() = this


    private val surrogates_: XRAbstractCharClass by lazy(LazyThreadSafetyMode.NONE) {
        val surrogates = lowHighSurrogates
        val result = object : XRAbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                return if (index >= 0 && index < XRAbstractCharClass.SURROGATE_CARDINALITY) {
                    this.altSurrogates xor surrogates[index]
                } else {
                    false
                }
            }
        }
        result.alt = this.alt
        result.altSurrogates = this.altSurrogates
        result.mayContainSupplCodepoints = this.mayContainSupplCodepoints
        result
    }
    /**
     * Returns a char class that contains only unpaired surrogate chars from this char class.
     *
     * Consider the following char class: `[a\uD801\uDC00\uD800]`.
     * This function returns a char class that contains only `\uD800`: `[\uD800]`.
     * [classWithoutSurrogates] returns a char class that does not contain `\uD800`: `[a\uD801\uDC00]`.
     *
     * The returned char class is used to create [XRSurrogateRangeSet] node
     * that matches any unpaired surrogate from this char class. [XRSurrogateRangeSet]
     * doesn't match a surrogate that is paired with the char before or after it.
     * The result of [classWithoutSurrogates] is used to create [XRSupplementaryRangeSet]
     * or [XRRangeSet] depending on [mayContainSupplCodepoints].
     * The two nodes are then combined in [XRCompositeRangeSet] node to fully represent this char class.
     */
    fun classWithSurrogates(): XRAbstractCharClass {
        return surrogates_
    }


    /**
     * Returns a char class that contains all chars from this char class excluding the unpaired surrogate chars.
     *
     * See [classWithSurrogates] for details.
     */
    // We cannot cache this class as we've done with surrogates above because
    // here is a circular reference between it and AbstractCharClass.
    fun classWithoutSurrogates(): XRAbstractCharClass {
        val result = object : XRAbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                val containslHS = if (index >= 0 && index < XRAbstractCharClass.SURROGATE_CARDINALITY)
                    this.altSurrogates xor this@XRAbstractCharClass.lowHighSurrogates.get(index)
                else
                    false

                return this@XRAbstractCharClass.contains(ch) && !containslHS
            }
        }
        result.alt = this.alt
        result.altSurrogates = this.altSurrogates
        result.mayContainSupplCodepoints = this.mayContainSupplCodepoints
        return result
    }

    /**
     * Sets this CharClass to negative form, i.e. if they will add some characters and after that set this
     * class to negative it will accept all the characters except previously set ones.
     *
     * Although this method will not alternate all the already set characters,
     * just overall meaning of the class.
     */
    fun setNegative(value: Boolean): XRAbstractCharClass {
        if (alt xor value) {
            alt = !alt
            altSurrogates = !altSurrogates

            if (!mayContainSupplCodepoints) {
                mayContainSupplCodepoints = true
            }
        }
        return this
    }

    fun isNegative(): Boolean {
        return alt
    }

    internal abstract class CachedCharClass {
        lateinit private var posValue: XRAbstractCharClass

        lateinit private var negValue: XRAbstractCharClass

        // Somewhat ugly init sequence, as computeValue() may depend on fields, initialized in subclass ctor.
        protected fun initValues() {
            posValue = computeValue()
            negValue = computeValue().setNegative(true)
        }

        fun getValue(negative: Boolean): XRAbstractCharClass = if (!negative) posValue else negValue
        protected abstract fun computeValue(): XRAbstractCharClass
    }

    internal class CachedDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            CachedCategory(CharCategory.DECIMAL_DIGIT_NUMBER.ordinal, true).getValue(false)
    }

    internal class CachedNonDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            CachedCategory(CharCategory.DECIMAL_DIGIT_NUMBER.ordinal, true).getValue(negative = true)
    }

    internal class CachedSpace : CachedCharClass() {
        init {
            initValues()
        }
        /* 9-13 - \t\n\x0B\f\r; 32 - ' ' */
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(9, 13).add(32)
    }

    internal class CachedNonSpace : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                CachedSpace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedWord : CachedCharClass() {
        init {
            initValues()
        }

        override fun computeValue(): XRAbstractCharClass =
            CachedNonWord().getValue(false).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedNonWord : CachedCharClass() {
        init {
            initValues()
        }

        override fun computeValue(): XRAbstractCharClass {
            val result = XRCharClass()
            result.add(CachedPunct().getValue(false))
            result.add(CharClasses.ISZ.factory().getValue(false))
            result.add(CharClasses.ISC.factory().getValue(false))
            return result
        }

    }

    internal class CachedNameStartChar : CachedCharClass() {
        init {
            initValues()
        }
        public override fun computeValue(): XRAbstractCharClass = XRCharClass()
            .add(':')
            .add('A', 'Z')
            .add('_')
            .add('a', 'z')
            .add(0xC0, 0xD6)
            .add(0xD8, 0xF6)
            .add(0xF8, 0x2FF)
            .add(0x370, 0x37D)
            .add(0x37F, 0x1FFF)
            .add(0x200C, 0x200D)
            .add(0x2070, 0x218F)
            .add(0x2C00, 0x2FEF)
            .add(0x3001, 0xD7FF)
            .add(0xF900, 0xFDCF)
            .add(0xFDF0, 0xFFFD)
            .add(0x10000, 0xEFFFF)
    }

    internal class CachedNonNameStartChar : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            CachedNameStartChar().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedNameChar : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass {
            return (CachedNameStartChar().computeValue() as XRCharClass)
                .add('-')
                .add('.')
                .add('0', '9')
                .add(0xB7)
                .add(0x300,0x36f)
                .add(0x203f,0x2040)
        }
    }

    internal class CachedNonNameChar : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                CachedWord().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    /** Supports unknown classes, in xsd 1.1 those are allowed, but match any character at all. */
    internal class CachedUnknownClass: CachedCharClass() {
        init {
            initValues()
        }

        override fun computeValue(): XRAbstractCharClass {
            return XRCharClass().apply { inverted=true }
        }
    }

    internal class CachedLower : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('a', 'z')
    }

    internal class CachedUpper : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('A', 'Z')
    }

    internal class CachedASCII : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0x00, 0x7F)
    }

    internal class CachedAlpha : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('a', 'z').add('A', 'Z')
    }

    internal class CachedAlnum : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                (CachedAlpha().getValue(negative = false) as XRCharClass).add('0', '9')
    }

    internal class CachedPunct : CachedCharClass() {
        init {
            initValues()
        }
        /* Punctuation !"#$%&'()*+,-./:;<=>?@ [\]^_` {|}~ */
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0x21, 0x40).add(0x5B, 0x60).add(0x7B, 0x7E)
    }

    internal class CachedGraph : CachedCharClass() {
        init {
            initValues()
        }
        /* plus punctuation */
        override fun computeValue(): XRAbstractCharClass =
                (CachedAlnum().getValue(negative = false) as XRCharClass)
                        .add(0x21, 0x40)
                        .add(0x5B, 0x60)
                        .add(0x7B, 0x7E)
    }

    internal class CachedPrint : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                (CachedGraph().getValue(negative = true) as XRCharClass).add(0x20)
    }

    internal class CachedBlank : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(' ').add('\t')
    }

    internal class CachedCntrl : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0x00, 0x1F).add(0x7F)
    }

    internal class CachedXDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('0', '9').add('a', 'f').add('A', 'F')
    }

    internal class CachedRange(var start: Int, var end: Int) : CachedCharClass() {
        init {
            initValues()
        }

        override fun computeValue(): XRAbstractCharClass =
                object: XRAbstractCharClass() {
                    override fun contains(ch: Int): Boolean = alt xor (ch in start..end)
                }.apply {
                    if (end >= MIN_SUPPLEMENTARY_CODE_POINT) {
                        mayContainSupplCodepoints = true
                    }
                    val minSurrogate = Char.MIN_SURROGATE.toInt()
                    val maxSurrogate = Char.MAX_SURROGATE.toInt()
                    // There is an intersection with surrogate characters.
                    if (end >= minSurrogate && start <= maxSurrogate && start <= end) {
                        val surrogatesStart = maxOf(start, minSurrogate) - minSurrogate
                        val surrogatesEnd = minOf(end, maxSurrogate) - minSurrogate
                        lowHighSurrogates.set(surrogatesStart..surrogatesEnd)
                    }
                }
    }

    internal class CachedSpecialsBlock : CachedCharClass() {
        init {
            initValues()
        }
        public override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0xFEFF, 0xFEFF).add(0xFFF0, 0xFFFD)
    }

    internal class CachedCategoryScope(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass {
            val result = XRUnicodeCategoryScope(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY, true)
            }

            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    internal class CachedCategory(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass {
            val result = XRUnicodeCategory(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY, true)
            }
            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    companion object {
        //Char.MAX_SURROGATE - Char.MIN_SURROGATE + 1
        const val SURROGATE_CARDINALITY = 2048

        /**
         * Character classes.
         * See http://www.unicode.org/reports/tr18/, http://www.unicode.org/Public/4.1.0/ucd/Blocks.txt
         */
        enum class CharClasses(val regexName : String, val factory: () -> CachedCharClass) {
            LOWER("Lower", ::CachedLower),
            UPPER("Upper", ::CachedUpper),
            ASCII("ASCII", ::CachedASCII),
            ALPHA("Alpha", ::CachedAlpha),
            DIGIT("Digit", ::CachedDigit),
            ALNUM("Alnum", :: CachedAlnum),
            PUNCT("Punct", ::CachedPunct),
            GRAPH("Graph", ::CachedGraph),
            PRINT("Print", ::CachedPrint),
            BLANK("Blank", ::CachedBlank),
            CNTRL("Cntrl", ::CachedCntrl),
            XDIGIT("XDigit", ::CachedXDigit),
            SPACE("Space", ::CachedSpace),
            NAMECHAR("c", ::CachedNameChar),
            NON_NAMECHAR("C", ::CachedNonNameChar),
            NAMESTARTCHAR("i", ::CachedNameStartChar),
            NON_NAMESTARTCHAR("I", ::CachedNonNameStartChar),
            WORD("w", ::CachedWord),
            NON_WORD("W", ::CachedNonWord),
            SPACE_SHORT("s", ::CachedSpace),
            NON_SPACE("S", ::CachedNonSpace),
            DIGIT_SHORT("d", ::CachedDigit),
            NON_DIGIT("D", ::CachedNonDigit),
            BASIC_LATIN("BasicLatin", { CachedRange(0x0000, 0x007F) }),
            LATIN1_SUPPLEMENT("Latin-1Supplement", { CachedRange(0x0080, 0x00FF) }),
            LATIN_EXTENDED_A("LatinExtended-A", { CachedRange(0x0100, 0x017F) }),
            LATIN_EXTENDED_B("LatinExtended-B", { CachedRange(0x0180, 0x024F) }),
            IPA_EXTENSIONS("IPAExtensions", { CachedRange(0x0250, 0x02AF) }),
            SPACING_MODIFIER_LETTERS("SpacingModifierLetters", { CachedRange(0x02B0, 0x02FF) }),
            COMBINING_DIACRITICAL_MARKS("CombiningDiacriticalMarks", { CachedRange(0x0300, 0x036F) }),
            GREEK("Greek", { CachedRange(0x0370, 0x03FF) }),
            CYRILLIC("Cyrillic", { CachedRange(0x0400, 0x04FF) }),
            CYRILLIC_SUPPLEMENT("CyrillicSupplement", { CachedRange(0x0500, 0x052F) }),
            ARMENIAN("Armenian", { CachedRange(0x0530, 0x058F) }),
            HEBREW("Hebrew", { CachedRange(0x0590, 0x05FF) }),
            ARABIC("Arabic", { CachedRange(0x0600, 0x06FF) }),
            SYRIAC("Syriac", { CachedRange(0x0700, 0x074F) }),
            ARABICSUPPLEMENT("ArabicSupplement", { CachedRange(0x0750, 0x077F) }),
            THAANA("Thaana", { CachedRange(0x0780, 0x07BF) }),
            NKO("NKo", { CachedRange(0x07C0, 0x07FF) }),
            SAMARITAN("Samaritan", { CachedRange(0x0800, 0x083F) }),
            MANDAIC("Mandaic", { CachedRange(0x0840, 0x085F) }),
            SYRIAC_SUPPLEMENT("SyriacSupplement", { CachedRange(0x0860, 0x086F) }),
            ARABIC_EXTENDED_B("ArabicExtended-B", { CachedRange(0x0870, 0x089F) }),
            ARABIC_EXTENDED_A("ArabicExtended-A", { CachedRange(0x08A0, 0x08FF) }),
            DEVANAGARI("Devanagari", { CachedRange(0x0900, 0x097F) }),
            BENGALI("Bengali", { CachedRange(0x0980, 0x09FF) }),
            GURMUKHI("Gurmukhi", { CachedRange(0x0A00, 0x0A7F) }),
            GUJARATI("Gujarati", { CachedRange(0x0A80, 0x0AFF) }),
            ORIYA("Oriya", { CachedRange(0x0B00, 0x0B7F) }),
            TAMIL("Tamil", { CachedRange(0x0B80, 0x0BFF) }),
            TELUGU("Telugu", { CachedRange(0x0C00, 0x0C7F) }),
            KANNADA("Kannada", { CachedRange(0x0C80, 0x0CFF) }),
            MALAYALAM("Malayalam", { CachedRange(0x0D00, 0x0D7F) }),
            SINHALA("Sinhala", { CachedRange(0x0D80, 0x0DFF) }),
            THAI("Thai", { CachedRange(0x0E00, 0x0E7F) }),
            LAO("Lao", { CachedRange(0x0E80, 0x0EFF) }),
            TIBETAN("Tibetan", { CachedRange(0x0F00, 0x0FFF) }),
            MYANMAR("Myanmar", { CachedRange(0x1000, 0x109F) }),
            GEORGIAN("Georgian", { CachedRange(0x10A0, 0x10FF) }),
            HANGULJAMO("HangulJamo", { CachedRange(0x1100, 0x11FF) }),
            ETHIOPIC("Ethiopic", { CachedRange(0x1200, 0x137F) }),
            ETHIOPICSUPPLEMENT("EthiopicSupplement", { CachedRange(0x1380, 0x139F) }),
            CHEROKEE("Cherokee", { CachedRange(0x13A0, 0x13FF) }),
            UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS("UnifiedCanadianAboriginalSyllabics", { CachedRange(0x1400, 0x167F) }),
            OGHAM("Ogham", { CachedRange(0x1680, 0x169F) }),
            RUNIC("Runic", { CachedRange(0x16A0, 0x16FF) }),
            TAGALOG("Tagalog", { CachedRange(0x1700, 0x171F) }),
            HANUNOO("Hanunoo", { CachedRange(0x1720, 0x173F) }),
            BUHID("Buhid", { CachedRange(0x1740, 0x175F) }),
            TAGBANWA("Tagbanwa", { CachedRange(0x1760, 0x177F) }),
            KHMER("Khmer", { CachedRange(0x1780, 0x17FF) }),
            MONGOLIAN("Mongolian", { CachedRange(0x1800, 0x18AF) }),
            LIMBU("Limbu", { CachedRange(0x1900, 0x194F) }),
            TAI_LE("TaiLe", { CachedRange(0x1950, 0x197F) }),
            NEW_TAI_LUE("NewTaiLue", { CachedRange(0x1980, 0x19DF) }),
            KHMERSYMBOLS("KhmerSymbols", { CachedRange(0x19E0, 0x19FF) }),
            BUGINESE("Buginese", { CachedRange(0x1A00, 0x1A1F) }),
            TAI_THAM("TaiTham", { CachedRange(0x1A20, 0x1AAF) }),
            COMBINING_DIACRITICAL_MARKS_EXTENDED("CombiningDiacriticalMarksExtended", { CachedRange(0x1AB0, 0x1AFF) }),
            BALINESE("Balinese", { CachedRange(0x1B00, 0x1B7F) }),
            SUNDANESE("Sundanese", { CachedRange(0x1B80, 0x1BBF) }),
            BATAK("Batak", { CachedRange(0x1BC0, 0x1BFF) }),
            LEPCHA("Lepcha", { CachedRange(0x1C00, 0x1C4F) }),
            OL_CHIKI("OlChiki", { CachedRange(0x1C50, 0x1C7F) }),
            CYRILLIC_EXTENDED_C("CyrillicExtended-C", { CachedRange(0x1C80, 0x1C8F) }),
            GEORGIAN_EXTENDED("GeorgianExtended", { CachedRange(0x1C90, 0x1CBF) }),
            SUNDANESE_SUPPLEMENT("SundaneseSupplement", { CachedRange(0x1CC0, 0x1CCF) }),
            VEDIC_EXTENSIONS("VedicExtensions", { CachedRange(0x1CD0, 0x1CFF) }),
            PHONETICEXTENSIONS("PhoneticExtensions", { CachedRange(0x1D00, 0x1D7F) }),
            PHONETICEXTENSIONSSUPPLEMENT("PhoneticExtensionsSupplement", { CachedRange(0x1D80, 0x1DBF) }),
            COMBININGDIACRITICALMARKSSUPPLEMENT("CombiningDiacriticalMarksSupplement", { CachedRange(0x1DC0, 0x1DFF) }),
            LATIN_EXTENDED_ADDITIONAL("LatinExtendedAdditional", { CachedRange(0x1E00, 0x1EFF) }),
            GREEKEXTENDED("GreekExtended", { CachedRange(0x1F00, 0x1FFF) }),
            GENERALPUNCTUATION("GeneralPunctuation", { CachedRange(0x2000, 0x206F) }),
            SUPERSCRIPTSANDSUBSCRIPTS("SuperscriptsandSubscripts", { CachedRange(0x2070, 0x209F) }),
            CURRENCYSYMBOLS("CurrencySymbols", { CachedRange(0x20A0, 0x20CF) }),
            COMBINING_DIACRITICAL_MARKS_FOR_SYMBOLS("CombiningDiacriticalMarksforSymbols", { CachedRange(0x20D0, 0x20FF) }),
            COMBININGMARKSFORSYMBOLS("CombiningMarksforSymbols", { CachedRange(0x20D0, 0x20FF) }),
            LETTERLIKESYMBOLS("LetterlikeSymbols", { CachedRange(0x2100, 0x214F) }),
            NUMBERFORMS("NumberForms", { CachedRange(0x2150, 0x218F) }),
            ARROWS("Arrows", { CachedRange(0x2190, 0x21FF) }),
            MATHEMATICALOPERATORS("MathematicalOperators", { CachedRange(0x2200, 0x22FF) }),
            MISCELLANEOUSTECHNICAL("MiscellaneousTechnical", { CachedRange(0x2300, 0x23FF) }),
            CONTROLPICTURES("ControlPictures", { CachedRange(0x2400, 0x243F) }),
            OPTICALCHARACTERRECOGNITION("OpticalCharacterRecognition", { CachedRange(0x2440, 0x245F) }),
            ENCLOSEDALPHANUMERICS("EnclosedAlphanumerics", { CachedRange(0x2460, 0x24FF) }),
            BOXDRAWING("BoxDrawing", { CachedRange(0x2500, 0x257F) }),
            BLOCKELEMENTS("BlockElements", { CachedRange(0x2580, 0x259F) }),
            GEOMETRICSHAPES("GeometricShapes", { CachedRange(0x25A0, 0x25FF) }),
            MISCELLANEOUSSYMBOLS("MiscellaneousSymbols", { CachedRange(0x2600, 0x26FF) }),
            DINGBATS("Dingbats", { CachedRange(0x2700, 0x27BF) }),
            MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A("MiscellaneousMathematicalSymbols-A", { CachedRange(0x27C0, 0x27EF) }),
            SUPPLEMENTALARROWS_A("SupplementalArrows-A", { CachedRange(0x27F0, 0x27FF) }),
            BRAILLEPATTERNS("BraillePatterns", { CachedRange(0x2800, 0x28FF) }),
            SUPPLEMENTALARROWS_B("SupplementalArrows-B", { CachedRange(0x2900, 0x297F) }),
            MISCELLANEOUSMATHEMATICALSYMBOLS_B("MiscellaneousMathematicalSymbols-B", { CachedRange(0x2980, 0x29FF) }),
            SUPPLEMENTAL_MATHEMATICAL_OPERATORS("SupplementalMathematicalOperators", { CachedRange(0x2A00, 0x2AFF) }),
            MISCELLANEOUS_SYMBOLS_AND_ARROWS("MiscellaneousSymbolsandArrows", { CachedRange(0x2B00, 0x2BFF) }),
            GLAGOLITIC("Glagolitic", { CachedRange(0x2C00, 0x2C5F) }),
            LATIN_EXTENDED_C("LatinExtended-C", { CachedRange(0x2C60, 0x2C7F) }),
            COPTIC("Coptic", { CachedRange(0x2C80, 0x2CFF) }),
            GEORGIANSUPPLEMENT("GeorgianSupplement", { CachedRange(0x2D00, 0x2D2F) }),
            TIFINAGH("Tifinagh", { CachedRange(0x2D30, 0x2D7F) }),
            ETHIOPICEXTENDED("EthiopicExtended", { CachedRange(0x2D80, 0x2DDF) }),
            CYRILLIC_EXTENDED_A("CyrillicExtended-A", { CachedRange(0x2DE0, 0x2DFF) }),
            SUPPLEMENTALPUNCTUATION("SupplementalPunctuation", { CachedRange(0x2E00, 0x2E7F) }),
            CJKRADICALSSUPPLEMENT("CJKRadicalsSupplement", { CachedRange(0x2E80, 0x2EFF) }),
            KANGXIRADICALS("KangxiRadicals", { CachedRange(0x2F00, 0x2FDF) }),
            IDEOGRAPHIC_DESCRIPTION_CHARACTERS("IdeographicDescriptionCharacters", { CachedRange(0x2FF0, 0x2FFF) }),
            CJK_SYMBOLS_AND_PUNCTUATION("CJKSymbolsandPunctuation", { CachedRange(0x3000, 0x303F) }),
            HIRAGANA("Hiragana", { CachedRange(0x3040, 0x309F) }),
            KATAKANA("Katakana", { CachedRange(0x30A0, 0x30FF) }),
            BOPOMOFO("Bopomofo", { CachedRange(0x3100, 0x312F) }),
            HANGUL_COMPATIBILITY_JAMO("HangulCompatibilityJamo", { CachedRange(0x3130, 0x318F) }),
            KANBUN("Kanbun", { CachedRange(0x3190, 0x319F) }),
            BOPOMOFOEXTENDED("BopomofoExtended", { CachedRange(0x31A0, 0x31BF) }),
            CJKSTROKES("CJKStrokes", { CachedRange(0x31C0, 0x31EF) }),
            KATAKANA_PHONETIC_EXTENSIONS("KatakanaPhoneticExtensions", { CachedRange(0x31F0, 0x31FF) }),
            ENCLOSED_CJK_LETTERS_AND_MONTHS("EnclosedCJKLettersandMonths", { CachedRange(0x3200, 0x32FF) }),
            CJK_COMPATIBILITY("CJKCompatibility", { CachedRange(0x3300, 0x33FF) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A("CJKUnifiedIdeographsExtensionA", { CachedRange(0x3400, 0x4DB5) }),
            YIJING_HEXAGRAM_SYMBOLS("YijingHexagramSymbols", { CachedRange(0x4DC0, 0x4DFF) }),
            CJKUNIFIEDIDEOGRAPHS("CJKUnifiedIdeographs", { CachedRange(0x4E00, 0x9FFF) }),
            YI_SYLLABLES("YiSyllables", { CachedRange(0xA000, 0xA48F) }),
            YI_RADICALS("YiRadicals", { CachedRange(0xA490, 0xA4CF) }),
            LISU("Lisu", { CachedRange(0xA4D0, 0xA4FF) }),
            VAI("Vai", { CachedRange(0xA500, 0xA63F) }),
            CYRILLIC_EXTENDED_B("CyrillicExtended-B", { CachedRange(0xA640, 0xA69F) }),
            BAMUM("Bamum", { CachedRange(0xA6A0, 0xA6FF) }),
            MODIFIER_TONE_LETTERS("ModifierToneLetters", { CachedRange(0xA700, 0xA71F) }),
            LATIN_EXTENDED_D("LatinExtended-D", { CachedRange(0xA720, 0xA7FF) }),
            SYLOTI_NAGRI("SylotiNagri", { CachedRange(0xA800, 0xA82F) }),
            COMMON_INDIC_NUMBER_FORMS("CommonIndicNumberForms", { CachedRange(0xA830, 0xA83F) }),
            PHAGS_PA("Phags-pa", { CachedRange(0xA840, 0xA87F) }),
            SAURASHTRA("Saurashtra", { CachedRange(0xA880, 0xA8DF) }),
            DEVANAGARI_EXTENDED("DevanagariExtended", { CachedRange(0xA8E0, 0xA8FF) }),
            KAYAH_LI("KayahLi", { CachedRange(0xA900, 0xA92F) }),
            REJANG("Rejang", { CachedRange(0xA930, 0xA95F) }),
            HANGUL_JAMO_EXTENDED_A("HangulJamoExtended-A", { CachedRange(0xA960, 0xA97F) }),
            JAVANESE("Javanese", { CachedRange(0xA980, 0xA9DF) }),
            MYANMAR_EXTENDED_B("MyanmarExtended-B", { CachedRange(0xA9E0, 0xA9FF) }),
            CHAM("Cham", { CachedRange(0xAA00, 0xAA5F) }),
            MYANMAR_EXTENDED_A("MyanmarExtended-A", { CachedRange(0xAA60, 0xAA7F) }),
            TAI_VIET("TaiViet", { CachedRange(0xAA80, 0xAADF) }),
            MEETEI_MAYEK_EXTENSIONS("MeeteiMayekExtensions", { CachedRange(0xAAE0, 0xAAFF) }),
            ETHIOPIC_EXTENDED_A("EthiopicExtended-A", { CachedRange(0xAB00, 0xAB2F) }),
            LATIN_EXTENDED_E("LatinExtended-E", { CachedRange(0xAB30, 0xAB6F) }),
            CHEROKEE_SUPPLEMENT("CherokeeSupplement", { CachedRange(0xAB70, 0xABBF) }),
            MEETEI_MAYEK("MeeteiMayek", { CachedRange(0xABC0, 0xABFF) }),
            HANGULSYLLABLES("HangulSyllables", { CachedRange(0xAC00, 0xD7A3) }),
            HANGUL_JAMO_EXTENDED_B("HangulJamoExtended-B", { CachedRange(0xD7B0, 0xD7FF) }),
            HIGHSURROGATES("HighSurrogates", { CachedRange(0xD800, 0xDB7F) }),
            HIGHPRIVATEUSESURROGATES("HighPrivateUseSurrogates", { CachedRange(0xDB80, 0xDBFF) }),
            LOWSURROGATES("LowSurrogates", { CachedRange(0xDC00, 0xDFFF) }),
            PRIVATEUSEAREA("PrivateUse", { CachedRange(0xE000, 0xF8FF) }),
            CJK_COMPATIBILITY_IDEOGRAPHS("CJKCompatibilityIdeographs", { CachedRange(0xF900, 0xFAFF) }),
            ALPHABETICPRESENTATIONFORMS("AlphabeticPresentationForms", { CachedRange(0xFB00, 0xFB4F) }),
            ARABICPRESENTATIONFORMS_A("ArabicPresentationForms-A", { CachedRange(0xFB50, 0xFDFF) }),
            VARIATIONSELECTORS("VariationSelectors", { CachedRange(0xFE00, 0xFE0F) }),
            VERTICALFORMS("VerticalForms", { CachedRange(0xFE10, 0xFE1F) }),
            COMBININGHALFMARKS("CombiningHalfMarks", { CachedRange(0xFE20, 0xFE2F) }),
            CJK_COMPATIBILITY_FORMS("CJKCompatibilityForms", { CachedRange(0xFE30, 0xFE4F) }),
            SMALL_FORM_VARIANTS("SmallFormVariants", { CachedRange(0xFE50, 0xFE6F) }),
            ARABIC_PRESENTATION_FORMS_B("ArabicPresentationForms-B", { CachedRange(0xFE70, 0xFEFF) }),
            HALFWIDTH_AND_FULLWIDTH_FORMS("HalfwidthandFullwidthForms", { CachedRange(0xFF00, 0xFFEF) }),
            LINEAR_B_SYLLABARY("LinearBSyllabary", { CachedRange(0x10000, 0x1007F) }),
            LINEAR_B_IDEOGRAMS("LinearBIdeograms", { CachedRange(0x10080, 0x100FF) }),
            AEGEAN_NUMBERS("AegeanNumbers", { CachedRange(0x10100, 0x1013F) }),
            ANCIENT_GREEK_NUMBERS("AncientGreekNumbers", { CachedRange(0x10140, 0x1018F) }),
            ANCIENT_SYMBOLS("AncientSymbols", { CachedRange(0x10190, 0x101CF) }),
            PHAISTOS_DISC("PhaistosDisc", { CachedRange(0x101D0, 0x101FF) }),
            LYCIAN("Lycian", { CachedRange(0x10280, 0x1029F) }),
            CARIAN("Carian", { CachedRange(0x102A0, 0x102DF) }),
            COPTIC_EPACT_NUMBERS("CopticEpactNumbers", { CachedRange(0x102E0, 0x102FF) }),
            OLD_ITALIC("OldItalic", { CachedRange(0x10300, 0x1032F) }),
            GOTHIC("Gothic", { CachedRange(0x10330, 0x1034F) }),
            OLD_PERMIC("OldPermic", { CachedRange(0x10350, 0x1037F) }),
            UGARITIC("Ugaritic", { CachedRange(0x10380, 0x1039F) }),
            OLD_PERSIAN("OldPersian", { CachedRange(0x103A0, 0x103DF) }),
            DESERET("Deseret", { CachedRange(0x10400, 0x1044F) }),
            SHAVIAN("Shavian", { CachedRange(0x10450, 0x1047F) }),
            OSMANYA("Osmanya", { CachedRange(0x10480, 0x104AF) }),
            OSAGE("Osage", { CachedRange(0x104B0, 0x104FF) }),
            ELBASAN("Elbasan", { CachedRange(0x10500, 0x1052F) }),
            CAUCASIAN_ALBANIAN("CaucasianAlbanian", { CachedRange(0x10530, 0x1056F) }),
            VITHKUQI("Vithkuqi", { CachedRange(0x10570, 0x105BF) }),
            LINEAR_A("LinearA", { CachedRange(0x10600, 0x1077F) }),
            LATIN_EXTENDED_F("LatinExtended-F", { CachedRange(0x10780, 0x107BF) }),
            CYPRIOT_SYLLABARY("CypriotSyllabary", { CachedRange(0x10800, 0x1083F) }),
            IMPERIAL_ARAMAIC("ImperialAramaic", { CachedRange(0x10840, 0x1085F) }),
            PALMYRENE("Palmyrene", { CachedRange(0x10860, 0x1087F) }),
            NABATAEAN("Nabataean", { CachedRange(0x10880, 0x108AF) }),
            HATRAN("Hatran", { CachedRange(0x108E0, 0x108FF) }),
            PHOENICIAN("Phoenician", { CachedRange(0x10900, 0x1091F) }),
            LYDIAN("Lydian", { CachedRange(0x10920, 0x1093F) }),
            MEROITIC_HIEROGLYPHS("MeroiticHieroglyphs", { CachedRange(0x10980, 0x1099F) }),
            MEROITIC_CURSIVE("MeroiticCursive", { CachedRange(0x109A0, 0x109FF) }),
            KHAROSHTHI("Kharoshthi", { CachedRange(0x10A00, 0x10A5F) }),
            OLD_SOUTH_ARABIAN("OldSouthArabian", { CachedRange(0x10A60, 0x10A7F) }),
            OLD_NORTH_ARABIAN("OldNorthArabian", { CachedRange(0x10A80, 0x10A9F) }),
            MANICHAEAN("Manichaean", { CachedRange(0x10AC0, 0x10AFF) }),
            AVESTAN("Avestan", { CachedRange(0x10B00, 0x10B3F) }),
            INSCRIPTIONAL_PARTHIAN("InscriptionalParthian", { CachedRange(0x10B40, 0x10B5F) }),
            INSCRIPTIONAL_PAHLAVI("InscriptionalPahlavi", { CachedRange(0x10B60, 0x10B7F) }),
            PSALTER_PAHLAVI("PsalterPahlavi", { CachedRange(0x10B80, 0x10BAF) }),
            OLD_TURKIC("OldTurkic", { CachedRange(0x10C00, 0x10C4F) }),
            OLD_HUNGARIAN("OldHungarian", { CachedRange(0x10C80, 0x10CFF) }),
            HANIFI_ROHINGYA("HanifiRohingya", { CachedRange(0x10D00, 0x10D3F) }),
            RUMI_NUMERAL_SYMBOLS("RumiNumeralSymbols", { CachedRange(0x10E60, 0x10E7F) }),
            YEZIDI("Yezidi", { CachedRange(0x10E80, 0x10EBF) }),
            ARABIC_EXTENDED_C("ArabicExtended-C", { CachedRange(0x10EC0, 0x10EFF) }),
            OLD_SOGDIAN("OldSogdian", { CachedRange(0x10F00, 0x10F2F) }),
            SOGDIAN("Sogdian", { CachedRange(0x10F30, 0x10F6F) }),
            OLD_UYGHUR("OldUyghur", { CachedRange(0x10F70, 0x10FAF) }),
            CHORASMIAN("Chorasmian", { CachedRange(0x10FB0, 0x10FDF) }),
            ELYMAIC("Elymaic", { CachedRange(0x10FE0, 0x10FFF) }),
            BRAHMI("Brahmi", { CachedRange(0x11000, 0x1107F) }),
            KAITHI("Kaithi", { CachedRange(0x11080, 0x110CF) }),
            SORA_SOMPENG("SoraSompeng", { CachedRange(0x110D0, 0x110FF) }),
            CHAKMA("Chakma", { CachedRange(0x11100, 0x1114F) }),
            MAHAJANI("Mahajani", { CachedRange(0x11150, 0x1117F) }),
            SHARADA("Sharada", { CachedRange(0x11180, 0x111DF) }),
            SINHALA_ARCHAIC_NUMBERS("SinhalaArchaicNumbers", { CachedRange(0x111E0, 0x111FF) }),
            KHOJKI("Khojki", { CachedRange(0x11200, 0x1124F) }),
            MULTANI("Multani", { CachedRange(0x11280, 0x112AF) }),
            KHUDAWADI("Khudawadi", { CachedRange(0x112B0, 0x112FF) }),
            GRANTHA("Grantha", { CachedRange(0x11300, 0x1137F) }),
            NEWA("Newa", { CachedRange(0x11400, 0x1147F) }),
            TIRHUTA("Tirhuta", { CachedRange(0x11480, 0x114DF) }),
            SIDDHAM("Siddham", { CachedRange(0x11580, 0x115FF) }),
            MODI("Modi", { CachedRange(0x11600, 0x1165F) }),
            MONGOLIAN_SUPPLEMENT("MongolianSupplement", { CachedRange(0x11660, 0x1167F) }),
            TAKRI("Takri", { CachedRange(0x11680, 0x116CF) }),
            AHOM("Ahom", { CachedRange(0x11700, 0x1174F) }),
            DOGRA("Dogra", { CachedRange(0x11800, 0x1184F) }),
            WARANG_CITI("WarangCiti", { CachedRange(0x118A0, 0x118FF) }),
            DIVES_AKURU("DivesAkuru", { CachedRange(0x11900, 0x1195F) }),
            NANDINAGARI("Nandinagari", { CachedRange(0x119A0, 0x119FF) }),
            ZANABAZAR_SQUARE("ZanabazarSquare", { CachedRange(0x11A00, 0x11A4F) }),
            SOYOMBO("Soyombo", { CachedRange(0x11A50, 0x11AAF) }),
            UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED_A("UnifiedCanadianAboriginalSyllabicsExtended-A", { CachedRange(0x11AB0, 0x11ABF) }),
            PAU_CIN_HAU("PauCinHau", { CachedRange(0x11AC0, 0x11AFF) }),
            DEVANAGARI_EXTENDED_A("DevanagariExtended-A", { CachedRange(0x11B00, 0x11B5F) }),
            BHAIKSUKI("Bhaiksuki", { CachedRange(0x11C00, 0x11C6F) }),
            MARCHEN("Marchen", { CachedRange(0x11C70, 0x11CBF) }),
            MASARAM_GONDI("MasaramGondi", { CachedRange(0x11D00, 0x11D5F) }),
            GUNJALA_GONDI("GunjalaGondi", { CachedRange(0x11D60, 0x11DAF) }),
            MAKASAR("Makasar", { CachedRange(0x11EE0, 0x11EFF) }),
            KAWI("Kawi", { CachedRange(0x11F00, 0x11F5F) }),
            LISU_SUPPLEMENT("LisuSupplement", { CachedRange(0x11FB0, 0x11FBF) }),
            TAMIL_SUPPLEMENT("TamilSupplement", { CachedRange(0x11FC0, 0x11FFF) }),
            CUNEIFORM("Cuneiform", { CachedRange(0x12000, 0x123FF) }),
            CUNEIFORM_NUMBERS_AND_PUNCTUATION("CuneiformNumbersandPunctuation", { CachedRange(0x12400, 0x1247F) }),
            EARLY_DYNASTIC_CUNEIFORM("EarlyDynasticCuneiform", { CachedRange(0x12480, 0x1254F) }),
            CYPRO_MINOAN("Cypro-Minoan", { CachedRange(0x12F90, 0x12FFF) }),
            EGYPTIAN_HIEROGLYPHS("EgyptianHieroglyphs", { CachedRange(0x13000, 0x1342F) }),
            EGYPTIAN_HIEROGLYPH_FORMAT_CONTROLS("EgyptianHieroglyphFormatControls", { CachedRange(0x13430, 0x1345F) }),
            ANATOLIAN_HIEROGLYPHS("AnatolianHieroglyphs", { CachedRange(0x14400, 0x1467F) }),
            BAMUM_SUPPLEMENT("BamumSupplement", { CachedRange(0x16800, 0x16A3F) }),
            MRO("Mro", { CachedRange(0x16A40, 0x16A6F) }),
            TANGSA("Tangsa", { CachedRange(0x16A70, 0x16ACF) }),
            BASSA_VAH("BassaVah", { CachedRange(0x16AD0, 0x16AFF) }),
            PAHAWH_HMONG("PahawhHmong", { CachedRange(0x16B00, 0x16B8F) }),
            MEDEFAIDRIN("Medefaidrin", { CachedRange(0x16E40, 0x16E9F) }),
            MIAO("Miao", { CachedRange(0x16F00, 0x16F9F) }),
            IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION("IdeographicSymbolsandPunctuation", { CachedRange(0x16FE0, 0x16FFF) }),
            TANGUT("Tangut", { CachedRange(0x17000, 0x187FF) }),
            TANGUT_COMPONENTS("TangutComponents", { CachedRange(0x18800, 0x18AFF) }),
            KHITAN_SMALL_SCRIPT("KhitanSmallScript", { CachedRange(0x18B00, 0x18CFF) }),
            TANGUT_SUPPLEMENT("TangutSupplement", { CachedRange(0x18D00, 0x18D7F) }),
            KANA_EXTENDED_B("KanaExtended-B", { CachedRange(0x1AFF0, 0x1AFFF) }),
            KANA_SUPPLEMENT("KanaSupplement", { CachedRange(0x1B000, 0x1B0FF) }),
            KANA_EXTENDED_A("KanaExtended-A", { CachedRange(0x1B100, 0x1B12F) }),
            SMALL_KANA_EXTENSION("SmallKanaExtension", { CachedRange(0x1B130, 0x1B16F) }),
            NUSHU("Nushu", { CachedRange(0x1B170, 0x1B2FF) }),
            DUPLOYAN("Duployan", { CachedRange(0x1BC00, 0x1BC9F) }),
            SHORTHAND_FORMAT_CONTROLS("ShorthandFormatControls", { CachedRange(0x1BCA0, 0x1BCAF) }),
            ZNAMENNY_MUSICAL_NOTATION("ZnamennyMusicalNotation", { CachedRange(0x1CF00, 0x1CFCF) }),
            BYZANTINE_MUSICAL_SYMBOLS("ByzantineMusicalSymbols", { CachedRange(0x1D000, 0x1D0FF) }),
            MUSICAL_SYMBOLS("MusicalSymbols", { CachedRange(0x1D100, 0x1D1FF) }),
            ANCIENT_GREEK_MUSICAL_NOTATION("AncientGreekMusicalNotation", { CachedRange(0x1D200, 0x1D24F) }),
            KAKTOVIK_NUMERALS("KaktovikNumerals", { CachedRange(0x1D2C0, 0x1D2DF) }),
            MAYAN_NUMERALS("MayanNumerals", { CachedRange(0x1D2E0, 0x1D2FF) }),
            TAI_XUAN_JING_SYMBOLS("TaiXuanJingSymbols", { CachedRange(0x1D300, 0x1D35F) }),
            COUNTING_ROD_NUMERALS("CountingRodNumerals", { CachedRange(0x1D360, 0x1D37F) }),
            MATHEMATICAL_ALPHANUMERIC_SYMBOLS("MathematicalAlphanumericSymbols", { CachedRange(0x1D400, 0x1D7FF) }),
            SUTTON_SIGNWRITING("SuttonSignWriting", { CachedRange(0x1D800, 0x1DAAF) }),
            LATIN_EXTENDED_G("LatinExtended-G", { CachedRange(0x1DF00, 0x1DFFF) }),
            GLAGOLITIC_SUPPLEMENT("GlagoliticSupplement", { CachedRange(0x1E000, 0x1E02F) }),
            CYRILLIC_EXTENDED_D("CyrillicExtended-D", { CachedRange(0x1E030, 0x1E08F) }),
            NYIAKENG_PUACHUE_HMONG("NyiakengPuachueHmong", { CachedRange(0x1E100, 0x1E14F) }),
            TOTO("Toto", { CachedRange(0x1E290, 0x1E2BF) }),
            WANCHO("Wancho", { CachedRange(0x1E2C0, 0x1E2FF) }),
            NAG_MUNDARI("NagMundari", { CachedRange(0x1E4D0, 0x1E4FF) }),
            ETHIOPIC_EXTENDED_B("EthiopicExtended-B", { CachedRange(0x1E7E0, 0x1E7FF) }),
            MENDE_KIKAKUI("MendeKikakui", { CachedRange(0x1E800, 0x1E8DF) }),
            ADLAM("Adlam", { CachedRange(0x1E900, 0x1E95F) }),
            INDIC_SIYAQ_NUMBERS("IndicSiyaqNumbers", { CachedRange(0x1EC70, 0x1ECBF) }),
            OTTOMAN_SIYAQ_NUMBERS("OttomanSiyaqNumbers", { CachedRange(0x1ED00, 0x1ED4F) }),
            ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS("ArabicMathematicalAlphabeticSymbols", { CachedRange(0x1EE00, 0x1EEFF) }),
            MAHJONG_TILES("MahjongTiles", { CachedRange(0x1F000, 0x1F02F) }),
            DOMINO_TILES("DominoTiles", { CachedRange(0x1F030, 0x1F09F) }),
            PLAYING_CARDS("PlayingCards", { CachedRange(0x1F0A0, 0x1F0FF) }),
            ENCLOSED_ALPHANUMERIC_SUPPLEMENT("EnclosedAlphanumericSupplement", { CachedRange(0x1F100, 0x1F1FF) }),
            ENCLOSED_IDEOGRAPHIC_SUPPLEMENT("EnclosedIdeographicSupplement", { CachedRange(0x1F200, 0x1F2FF) }),
            MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS("MiscellaneousSymbolsandPictographs", { CachedRange(0x1F300, 0x1F5FF) }),
            EMOTICONS("Emoticons", { CachedRange(0x1F600, 0x1F64F) }),
            ORNAMENTAL_DINGBATS("OrnamentalDingbats", { CachedRange(0x1F650, 0x1F67F) }),
            TRANSPORT_AND_MAP_SYMBOLS("TransportandMapSymbols", { CachedRange(0x1F680, 0x1F6FF) }),
            ALCHEMICAL_SYMBOLS("AlchemicalSymbols", { CachedRange(0x1F700, 0x1F77F) }),
            GEOMETRIC_SHAPES_EXTENDED("GeometricShapesExtended", { CachedRange(0x1F780, 0x1F7FF) }),
            SUPPLEMENTAL_ARROWS_C("SupplementalArrows-C", { CachedRange(0x1F800, 0x1F8FF) }),
            SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS("SupplementalSymbolsandPictographs", { CachedRange(0x1F900, 0x1F9FF) }),
            CHESS_SYMBOLS("ChessSymbols", { CachedRange(0x1FA00, 0x1FA6F) }),
            SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A("SymbolsandPictographsExtended-A", { CachedRange(0x1FA70, 0x1FAFF) }),
            SYMBOLS_FOR_LEGACY_COMPUTING("SymbolsforLegacyComputing", { CachedRange(0x1FB00, 0x1FBFF) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B("CJKUnifiedIdeographsExtensionB", { CachedRange(0x20000, 0x2A6DF) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C("CJKUnifiedIdeographsExtensionC", { CachedRange(0x2A700, 0x2B73F) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D("CJKUnifiedIdeographsExtensionD", { CachedRange(0x2B740, 0x2B81F) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E("CJKUnifiedIdeographsExtensionE", { CachedRange(0x2B820, 0x2CEAF) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F("CJKUnifiedIdeographsExtensionF", { CachedRange(0x2CEB0, 0x2EBEF) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_I("CJKUnifiedIdeographsExtensionI", { CachedRange(0x2EBF0, 0x2EE5F) }),
            CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT("CJKCompatibilityIdeographsSupplement", { CachedRange(0x2F800, 0x2FA1F) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G("CJKUnifiedIdeographsExtensionG", { CachedRange(0x30000, 0x3134F) }),
            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H("CJKUnifiedIdeographsExtensionH", { CachedRange(0x31350, 0x323AF) }),
            TAGS("Tags", { CachedRange(0xE0000, 0xE007F) }),
            VARIATION_SELECTORS_SUPPLEMENT("VariationSelectorsSupplement", { CachedRange(0xE0100, 0xE01EF) }),
            SUPPLEMENTARY_PRIVATE_USE_AREA_A("SupplementaryPrivateUseArea-A", { CachedRange(0xF0000, 0xFFFFF) }),
            SUPPLEMENTARY_PRIVATE_USE_AREA_B("SupplementaryPrivateUseArea-B", { CachedRange(0x100000, 0x10FFFF) }),
            ALL("all", { CachedRange(0x00, 0x10FFFF) }),
            SPECIALS("Specials", ::CachedSpecialsBlock),
            CN("Cn", { CachedCategory(CharCategory.UNASSIGNED.ordinal, true) }),
            ISL("IsL", { CachedCategoryScope(0x3E, true) }),
            LU("Lu", { CachedCategory(CharCategory.UPPERCASE_LETTER.ordinal, true) }),
            LL("Ll", { CachedCategory(CharCategory.LOWERCASE_LETTER.ordinal, true) }),
            LT("Lt", { CachedCategory(CharCategory.TITLECASE_LETTER.ordinal, false) }),
            LM("Lm", { CachedCategory(CharCategory.MODIFIER_LETTER.ordinal, false) }),
            LO("Lo", { CachedCategory(CharCategory.OTHER_LETTER.ordinal, true) }),
            ISM("IsM", { CachedCategoryScope(0x1C0, true) }),
            MN("Mn", { CachedCategory(CharCategory.NON_SPACING_MARK.ordinal, true) }),
            ME("Me", { CachedCategory(CharCategory.ENCLOSING_MARK.ordinal, false) }),
            MC("Mc", { CachedCategory(CharCategory.COMBINING_SPACING_MARK.ordinal, true) }),
            N("IsN", { CachedCategoryScope(0xE00, true) }),
            ND("Nd", { CachedCategory(CharCategory.DECIMAL_DIGIT_NUMBER.ordinal, true) }),
            NL("Nl", { CachedCategory(CharCategory.LETTER_NUMBER.ordinal, true) }),
            NO("No", { CachedCategory(CharCategory.OTHER_NUMBER.ordinal, true) }),
            ISZ("IsZ", { CachedCategoryScope(0x7000, false) }),
            ZS("Zs", { CachedCategory(CharCategory.SPACE_SEPARATOR.ordinal, false) }),
            ZL("Zl", { CachedCategory(CharCategory.LINE_SEPARATOR.ordinal, false) }),
            ZP("Zp", { CachedCategory(CharCategory.PARAGRAPH_SEPARATOR.ordinal, false) }),
            ISC("IsC", { CachedCategoryScope(0xF0000, true, true) }),
            CC("Cc", { CachedCategory(CharCategory.CONTROL.ordinal, false) }),
            CF("Cf", { CachedCategory(CharCategory.FORMAT.ordinal, true) }),
            CO("Co", { CachedCategory(CharCategory.PRIVATE_USE.ordinal, true) }),
            CS("Cs", { CachedCategory(CharCategory.SURROGATE.ordinal, false, true) }),
            ISP("IsP", { CachedCategoryScope((1 shl CharCategory.DASH_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.START_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.END_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.CONNECTOR_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.OTHER_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.INITIAL_QUOTE_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.FINAL_QUOTE_PUNCTUATION.ordinal), true) }),
            PD("Pd", { CachedCategory(CharCategory.DASH_PUNCTUATION.ordinal, false) }),
            PS("Ps", { CachedCategory(CharCategory.START_PUNCTUATION.ordinal, false) }),
            PE("Pe", { CachedCategory(CharCategory.END_PUNCTUATION.ordinal, false) }),
            PC("Pc", { CachedCategory(CharCategory.CONNECTOR_PUNCTUATION.ordinal, false) }),
            PO("Po", { CachedCategory(CharCategory.OTHER_PUNCTUATION.ordinal, true) }),
            ISS("IsS", { CachedCategoryScope(0x7E000000, true) }),
            SM("Sm", { CachedCategory(CharCategory.MATH_SYMBOL.ordinal, true) }),
            SC("Sc", { CachedCategory(CharCategory.CURRENCY_SYMBOL.ordinal, false) }),
            SK("Sk", { CachedCategory(CharCategory.MODIFIER_SYMBOL.ordinal, false) }),
            SO("So", { CachedCategory(CharCategory.OTHER_SYMBOL.ordinal, true) }),
            PI("Pi", { CachedCategory(CharCategory.INITIAL_QUOTE_PUNCTUATION.ordinal, false) }),
            PF("Pf", { CachedCategory(CharCategory.FINAL_QUOTE_PUNCTUATION.ordinal, false)  }),
        }

        private val classCache = Array<Lazy<CachedCharClass>>(CharClasses.entries.size) { idx ->
            lazy {
                CharClasses.entries[idx].factory()
            }
        }
        private val classCacheMap = CharClasses.entries.associate { it -> it.regexName to it }

        fun intersects(ch1: Int, ch2: Int): Boolean = ch1 == ch2
        fun intersects(cc: XRAbstractCharClass, ch: Int): Boolean = cc.contains(ch)

        fun intersects(cc1: XRAbstractCharClass, cc2: XRAbstractCharClass): Boolean {
            if (cc1.bits == null || cc2.bits == null) {
                return true
            }
            return cc1.bits!!.intersects(cc2.bits!!)
        }

        fun getPredefinedClass(name: String, negative: Boolean, version: SchemaVersion): XRAbstractCharClass {
            val charClass = classCacheMap[name] ?: when(version) {
                SchemaVersion.V1_0 -> throw XRPatternSyntaxException("No such character class ($name)")
                else -> classCacheMap["all"]!! // xsd 1.1 allows unknown classes }
            }
            val cachedClass = classCache[charClass.ordinal].value
            return cachedClass.getValue(negative)
        }
    }
}
