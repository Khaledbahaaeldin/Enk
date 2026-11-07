package app.enk.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val CourierPrime = FontFamily(
    Font(R.font.courier_prime_regular, FontWeight.Normal),
    Font(R.font.courier_prime_bold, FontWeight.Bold)
)

object EnkType {
    val paper = TextStyle(
        fontFamily = CourierPrime,
        letterSpacing = 0.em,
        color = EnkColors.ink
    )

    val wordmark = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.16.em,
        color = EnkColors.textOnChassis
    )

    val meta = TextStyle(
        fontFamily = CourierPrime,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.04.em,
        color = EnkColors.textMuted
    )

    val panelLabel = TextStyle(
        fontFamily = CourierPrime,
        fontSize = 9.sp,
        letterSpacing = 0.16.em,
        color = EnkColors.textMuted
    )

    val panelValue = TextStyle(
        fontFamily = CourierPrime,
        fontSize = 9.sp,
        letterSpacing = 0.04.em,
        color = EnkColors.textOnChassis
    )

    val hint = TextStyle(
        fontFamily = CourierPrime,
        fontSize = 10.sp,
        letterSpacing = 0.em,
        color = EnkColors.textMuted
    )

    val slug = TextStyle(
        fontFamily = CourierPrime,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        color = EnkColors.slugLetter
    )
}
