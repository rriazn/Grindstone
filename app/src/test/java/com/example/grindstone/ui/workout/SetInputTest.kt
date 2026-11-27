package com.example.grindstone.ui.workout

import com.example.grindstone.ui.workout.InputError.Empty
import com.example.grindstone.ui.workout.InputError.NotANumber
import com.example.grindstone.ui.workout.InputError.OutOfRange
import com.example.grindstone.ui.workout.InputError.TooManyDecimals
import org.junit.Assert.assertEquals
import org.junit.Test

class SetInputTest {

    @Test
    fun repsAcceptsWholeNumbersWithinRange() {
        assertEquals(Parsed.Valid(1), SetInput.parseReps("1"))
        assertEquals(Parsed.Valid(12), SetInput.parseReps("12"))
        assertEquals(Parsed.Valid(999), SetInput.parseReps("999"))
        assertEquals(Parsed.Valid(8), SetInput.parseReps(" 8 "))
        assertEquals(Parsed.Valid(7), SetInput.parseReps("007"))
    }

    @Test
    fun repsRejectsValuesOutsideRange() {
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseReps("0"))
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseReps("1000"))
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseReps("-3"))
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseReps("99999999999999"))
    }

    @Test
    fun repsReportsEmptyInput() {
        assertEquals(Parsed.Invalid(Empty), SetInput.parseReps(""))
        assertEquals(Parsed.Invalid(Empty), SetInput.parseReps("   "))
    }

    @Test
    fun repsRejectsGarbageAndDecimals() {
        listOf("abc", "5a", "5.0", "5,5", "1 2", "+", "-", "٣").forEach { input ->
            assertEquals("input '$input'", Parsed.Invalid(NotANumber), SetInput.parseReps(input))
        }
    }

    @Test
    fun weightAcceptsDotAndCommaDecimalSeparators() {
        assertEquals(Parsed.Valid(62.5), SetInput.parseWeightKg("62.5"))
        assertEquals(Parsed.Valid(62.5), SetInput.parseWeightKg("62,5"))
        assertEquals(Parsed.Valid(2.25), SetInput.parseWeightKg("2,25"))
        assertEquals(Parsed.Valid(0.5), SetInput.parseWeightKg(",5"))
        assertEquals(Parsed.Valid(80.0), SetInput.parseWeightKg("80."))
        assertEquals(Parsed.Valid(100.0), SetInput.parseWeightKg(" 100 "))
    }

    @Test
    fun weightBoundariesAreInclusive() {
        assertEquals(Parsed.Valid(0.0), SetInput.parseWeightKg("0"))
        assertEquals(Parsed.Valid(1000.0), SetInput.parseWeightKg("1000"))
        assertEquals(Parsed.Valid(1000.0), SetInput.parseWeightKg("1000,00"))
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseWeightKg("1000.01"))
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseWeightKg("-0.5"))
        assertEquals(Parsed.Invalid(OutOfRange), SetInput.parseWeightKg("123456789012345678901234567890"))
    }

    @Test
    fun weightAllowsAtMostTwoDecimals() {
        assertEquals(Parsed.Valid(1.25), SetInput.parseWeightKg("1.250"))
        assertEquals(Parsed.Invalid(TooManyDecimals), SetInput.parseWeightKg("1.255"))
        assertEquals(Parsed.Invalid(TooManyDecimals), SetInput.parseWeightKg("0,001"))
    }

    @Test
    fun weightReportsEmptyInput() {
        assertEquals(Parsed.Invalid(Empty), SetInput.parseWeightKg(""))
        assertEquals(Parsed.Invalid(Empty), SetInput.parseWeightKg("  "))
    }

    @Test
    fun weightRejectsGarbage() {
        listOf("abc", ".", ",", "1,000.5", "1.2.3", "5kg", "1e3", "NaN", "Infinity", "--5", "5 5").forEach { input ->
            assertEquals("input '$input'", Parsed.Invalid(NotANumber), SetInput.parseWeightKg(input))
        }
    }
}
