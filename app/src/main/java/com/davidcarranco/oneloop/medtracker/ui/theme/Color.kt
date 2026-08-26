package com.davidcarranco.oneloop.medtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WCAG 2.2 AA, color-blind safe: blue is primary, orange is warning,
 * green is success only (never the global brand). Matches iOS AppTheme.
 */
object OneLoopColors {
    val NavyLight = Color(0xFF171E2E)
    val NavyDark = Color(0xFFF0F2FA)

    val BlueLight = Color(0xFF1F52B8)
    val BlueDark = Color(0xFF8CB3FF)

    val TealLight = Color(0xFF47597A)
    val TealDark = Color(0xFFB2C2DB)

    val LimeLight = Color(0xFFD1E0FA)
    val LimeDark = Color(0xFF2E477A)

    val ScheduleSelectionTextLight = Color(0xFF171E2E)
    val ScheduleSelectionTextDark = Color(0xFFF0F2FA)

    val ActionTextLight = Color(0xFFFCFCFF)
    val ActionTextDark = Color(0xFF121A2E)

    val OrangeLight = Color(0xFFB86114)
    val OrangeDark = Color(0xFFFFAD52)

    val SoftBackgroundLight = Color(0xFFF5F5F7)
    val SoftBackgroundDark = Color(0xFF12141C)

    val CardBackgroundLight = Color(0xFFFFFFFF)
    val CardBackgroundDark = Color(0xFF212633)

    val ElevatedCardLight = Color(0xFFE6EBF5)
    val ElevatedCardDark = Color(0xFF2E3647)

    val CardBorderLight = Color(0x1A000000)
    val CardBorderDark = Color(0x24FFFFFF)

    val FieldFillLight = Color(0xFFEDF0F7)
    val FieldFillDark = Color(0xFF292E3D)

    val MutedTextLight = Color(0xFF52617A)
    val MutedTextDark = Color(0xFFADBAD1)

    val SuccessLight = Color(0xFF1F7347)
    val SuccessDark = Color(0xFF73D194)

    val SplashFillLight = Color(0xFF1A2947)
    val SplashFillDark = Color(0xFF141F38)

    val SplashWordmark = Color(0xFFE6EDFA)
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
    val splashFill: Color,
    val splashWordmark: Color,
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
    fieldFill = OneLoopColors.FieldFillLight,
    splashFill = OneLoopColors.SplashFillLight,
    splashWordmark = OneLoopColors.SplashWordmark,
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
    fieldFill = OneLoopColors.FieldFillDark,
    splashFill = OneLoopColors.SplashFillDark,
    splashWordmark = OneLoopColors.SplashWordmark,
)
