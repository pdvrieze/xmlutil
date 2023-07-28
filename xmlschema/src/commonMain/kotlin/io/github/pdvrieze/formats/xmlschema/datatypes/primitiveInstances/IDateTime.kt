package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.DecimalType

interface IDateTime : VAnyAtomicType {
    /** any integer */
    val year: Int?

    /** 1..12 */
    val month: UInt?

    /** 1..31 or further restricted on month */
    val day: UInt?

    /** 0..23 */
    val hour: UInt?

    /** 0..59 */
    val minute: UInt?

    /** A decimal [0.0, 60.0> */
    val second: VDecimal?

    /**
     * Minutes offset from UTC
     */
    val timezoneOffset: Int?

    fun yearFrag(): String = year?.let{ // it must pad to at least 4 digits
        if(it<0) "-${(-it).toString().padStart(4, '0')}" else it.toString().padStart(4, '0')
    } ?: ""

    fun monthFrag(): String = month?.toString() ?: ""
    fun dayFrag(): String = day?.toString() ?: ""
    fun hourFrag(): String = hour?.toString() ?: ""
    fun minuteFrag(): String = minute?.toString() ?: ""
    fun secondFrag(): String =
        (second as? VInteger)?.run { toInt().toString() } ?: second?.run { toDouble().toString() } ?: ""

    fun timeZoneFrag(): String = when (val it = timezoneOffset) {
        null -> ""
        0 -> "Z"
        else -> {
            val sign = if (it >= 0) '+' else '-'
            val hours = (it / 60).toString().padStart(2, '0')
            val minutes = (it % 60).toString().padStart(2, '0')
            "$sign$hours:$minutes"
        }
    }

    companion object {
        fun yearFragValue(yr: String): Int = yr.toInt()
        fun monthFragValue(mo: String): UInt = mo.toUInt()
        fun dayFragValue(da: String): UInt = da.toUInt()
        fun hourFragValue(hr: String): UInt = hr.toUInt()
        fun minuteFragValue(mi: String): UInt = mi.toUInt()
        fun secondFragValue(se: String): VDecimal = DecimalType.value(VString(se))
        fun timezoneFragValue(tz: String): Int? {
            if (tz.isEmpty()) return null
            if (tz == "Z") return 0 // handle Z case differently
            if (tz.length != 6) throw NumberFormatException("Timezone fragments are 6 characters long: '$tz'")
            val sign = when (tz[0]) {
                '+' -> false
                '-' -> true
                else -> throw NumberFormatException("Missing sign in timezone, found ${tz[0]}")
            }
            val hours = tz[1].digitToInt() * 10 + tz[2].digitToInt()
            if (hours !in 0..14) throw NumberFormatException("Timezone hours must be between 0 and 14")
            if (tz[3] != ':') throw NumberFormatException("Missing : between hours and minutes in timezone")
            val minutes = tz[4].digitToInt() * 10 + tz[5].digitToInt()
            if (minutes !in 0..59) throw NumberFormatException("Minutes must be between 0 and 59")
            return (if (sign) -1 else 1) * ((hours * 60) + minutes)
        }
    }
}

internal fun Int.toLBits(bitCount: Int, shift: Int): ULong = toLBits(bitCount) shl shift

internal fun Int.toLBits(bitCount: Int): ULong {
    val ulValue = toULong()
    return (ulValue and (1uL shl (bitCount - 1)) - 1uL) or ((ulValue shr 63) shl (bitCount - 1))
}

internal fun Int.toIBits(bitCount: Int, shift: Int): UInt = toIBits(bitCount) shl shift

internal fun Int.toIBits(bitCount: Int): UInt {
    val uValue = toUInt()
    return (uValue and (1u shl (bitCount - 1)) - 1u) or ((uValue shr 31) shl (bitCount - 1))
}

internal fun UInt.toIBits(bitCount: Int, shift: Int): UInt = toIBits(bitCount) shl shift

internal fun UInt.toIBits(bitCount: Int): UInt {
    return toUInt() and (1u shl (bitCount)) - 1u
}

internal fun UInt.toLBits(bitCount: Int, shift: Int): ULong = toLBits(bitCount) shl shift

internal fun UInt.toLBits(bitCount: Int): ULong {
    return toULong() and (1uL shl (bitCount)) - 1uL
}

internal fun UInt.intFromBits(bitCount: Int): Int {
    val signMask: UInt = 1u shl (bitCount - 1)
    val mask = signMask - 1u
    return when {
        this and signMask == signMask ->  // negative
            (((-1).toUInt() xor mask) or
                    (this and mask)).toInt()

        else -> (this and mask).toInt()
    }
}

internal fun ULong.intFromBits(bitCount: Int): Int {
    val signMask: ULong = 1uL shl (bitCount - 1)
    val mask = signMask - 1uL
    return when {
        this and signMask == signMask ->  // negative
            (((-1).toULong() xor mask) or
                    (this and mask)).toInt()

        else -> (this and mask).toInt()
    }
}

internal fun UInt.uintFromBits(bitCount: Int): UInt {
    return this and ((1u shl bitCount) - 1u)
}

internal fun ULong.uintFromBits(bitCount: Int): UInt {
    return (this and ((1uL shl bitCount) - 1uL)).toUInt()
}

internal fun ULong.longFromBits(bitCount: Int): Long {
    val signMask: ULong = 1uL shl (bitCount - 1)
    val mask = signMask - 1uL
    return when {
        this and signMask == signMask ->  // negative
            (((-1).toULong() xor mask) or
                    (this and mask)).toLong()

        else -> (this and mask).toLong()
    }
}
