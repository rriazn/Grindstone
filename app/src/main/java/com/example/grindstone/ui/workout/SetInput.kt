package com.example.grindstone.ui.workout

import java.math.BigDecimal

enum class InputError { Empty, NotANumber, OutOfRange, TooManyDecimals }

sealed interface Parsed<out T> {
    data class Valid<T>(val value: T) : Parsed<T>
    data class Invalid(val error: InputError) : Parsed<Nothing>
}

/** Parsing and validation of the raw text typed into the reps and weight fields. */
object SetInput {
    const val MIN_REPS = 1
    const val MAX_REPS = 999
    const val MIN_WEIGHT_KG = 0
    const val MAX_WEIGHT_KG = 1000
    const val MAX_WEIGHT_DECIMALS = 2

    private val integer = Regex("""-?\d+""")
    private val decimal = Regex("""-?(\d+\.?\d*|\.\d+)""")

    fun parseReps(input: String): Parsed<Int> {
        val text = input.trim()
        if (text.isEmpty()) return Parsed.Invalid(InputError.Empty)
        if (!integer.matches(text)) return Parsed.Invalid(InputError.NotANumber)
        val reps = text.toIntOrNull()
        return if (reps != null && reps in MIN_REPS..MAX_REPS) {
            Parsed.Valid(reps)
        } else {
            Parsed.Invalid(InputError.OutOfRange)
        }
    }

    /** Accepts both "," and "." as the decimal separator; 0 is allowed (bodyweight). */
    fun parseWeightKg(input: String): Parsed<Double> {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return Parsed.Invalid(InputError.Empty)
        if (!decimal.matches(text)) return Parsed.Invalid(InputError.NotANumber)
        val weight = BigDecimal(text)
        return when {
            weight < BigDecimal(MIN_WEIGHT_KG) || weight > BigDecimal(MAX_WEIGHT_KG) ->
                Parsed.Invalid(InputError.OutOfRange)
            weight.stripTrailingZeros().scale() > MAX_WEIGHT_DECIMALS ->
                Parsed.Invalid(InputError.TooManyDecimals)
            else -> Parsed.Valid(weight.toDouble())
        }
    }
}
