package com.davidcarranco.oneloop.medtracker.ui.theme

import androidx.compose.ui.graphics.Color

object OneLoopColors {
    val NavyLight = Color(0xFF0F141F)
    val NavyDark = Color(0xFFF2F7FF)

    val BlueLight = Color(0xFF0D47B8)
    val BlueDark = Color(0xFF528AFF)

    val TealLight = Color(0xFF006B70)
    val TealDark = Color(0xFF26C7B8)

    val LimeLight = Color(0xFF7ABD1F)
    val LimeDark = Color(0xFFB3ED33)

    val ScheduleSelectionTextLight = Color(0xFF0D1A29)
    val ScheduleSelectionTextDark = Color(0xFF080D05)

    val ActionTextLight = Color(0xFF0F2408)
    val ActionTextDark = Color(0xFF0A1205)

    val OrangeLight = Color(0xFFD13D0A)
    val OrangeDark = Color(0xFFFF7A33)

    val SoftBackgroundLight = Color(0xFFFFFFFF)
    val SoftBackgroundDark = Color(0xFF090B11)

    val CardBackgroundLight = Color(0xFFE6EBF2)
    val CardBackgroundDark = Color(0xFF171B24)

    val ElevatedCardLight = Color(0xFFD6E0F0)
    val ElevatedCardDark = Color(0xFF212633)

    val CardBorderLight = Color(0x1A000000)
    val CardBorderDark = Color(0x1AFFFFFF)

    val MutedTextLight = Color(0xFF424D61)
    val MutedTextDark = Color(0xFF9EABBF)

    val SuccessLight = Color(0xFF2E8C33)
    val SuccessDark = Color(0xFFB3ED33)
}

data class OneLoopPalette(
    val navy: Color,
    val blue: Color,
    val teal: Color,
    val lime: Color,
    val scheduleSelectionText: Color,
    val actionText: Color,
    val orange: Color,
    val softBackground: Color,
    val cardBackground: Color,
    val elevatedCard: Color,
    val cardBorder: Color,
    val mutedText: Color,
    val success: Color,
    val warning: Color,
    val fieldFill: Color,
)

val LightPalette = OneLoopPalette(
    navy = OneLoopColors.NavyLight,
    blue = OneLoopColors.BlueLight,
    teal = OneLoopColors.TealLight,
    lime = OneLoopColors.LimeLight,
    scheduleSelectionText = OneLoopColors.ScheduleSelectionTextLight,
    actionText = OneLoopColors.ActionTextLight,
    orange = OneLoopColors.OrangeLight,
    softBackground = OneLoopColors.SoftBackgroundLight,
    cardBackground = OneLoopColors.CardBackgroundLight,
    elevatedCard = OneLoopColors.ElevatedCardLight,
    cardBorder = OneLoopColors.CardBorderLight,
    mutedText = OneLoopColors.MutedTextLight,
    success = OneLoopColors.SuccessLight,
    warning = OneLoopColors.OrangeLight,
    fieldFill = Color(0xFFF0F2F8),
)

val DarkPalette = OneLoopPalette(
    navy = OneLoopColors.NavyDark,
    blue = OneLoopColors.BlueDark,
    teal = OneLoopColors.TealDark,
    lime = OneLoopColors.LimeDark,
    scheduleSelectionText = OneLoopColors.ScheduleSelectionTextDark,
    actionText = OneLoopColors.ActionTextDark,
    orange = OneLoopColors.OrangeDark,
    softBackground = OneLoopColors.SoftBackgroundDark,
    cardBackground = OneLoopColors.CardBackgroundDark,
    elevatedCard = OneLoopColors.ElevatedCardDark,
    cardBorder = OneLoopColors.CardBorderDark,
    mutedText = OneLoopColors.MutedTextDark,
    success = OneLoopColors.SuccessDark,
    warning = OneLoopColors.OrangeDark,
    fieldFill = Color(0xFF1F242E),
)
