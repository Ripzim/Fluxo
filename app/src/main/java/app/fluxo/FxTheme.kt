package app.fluxo

import org.json.JSONObject

val HEX_REGEX = Regex("^#[0-9a-fA-F]{6}$")

data class FxTheme(
    val name: String,
    val bg: String,
    val surface: String,
    val text: String,
    val textSecondary: String,
    val accent: String,
    val font: String = "sans",
    val radius: Int = 12,
    val density: Float = 1f,
    val textScale: Float = 1f,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("bg", bg)
        .put("surface", surface)
        .put("text", text)
        .put("textSecondary", textSecondary)
        .put("accent", accent)
        .put("font", font)
        .put("radius", radius)
        .put("density", density.toDouble())
        .put("textScale", textScale.toDouble())

    companion object {
        const val CUSTOM = "Personalizado"
        val FONTS = listOf("sans", "serif", "mono")

        /** Lê um tema de JSON validando tudo (cores só em #RRGGBB, números com limites). */
        fun fromJson(o: JSONObject): FxTheme {
            require(o.has("bg")) { "tema inválido" }
            val base = presets.first()
            fun hex(key: String, default: String): String =
                o.optString(key, default).takeIf { HEX_REGEX.matches(it) } ?: default
            return FxTheme(
                name = o.optString("name", CUSTOM).take(40).ifBlank { CUSTOM },
                bg = hex("bg", base.bg),
                surface = hex("surface", base.surface),
                text = hex("text", base.text),
                textSecondary = hex("textSecondary", base.textSecondary),
                accent = hex("accent", base.accent),
                font = o.optString("font", "sans").takeIf { it in FONTS } ?: "sans",
                radius = o.optInt("radius", 12).coerceIn(0, 28),
                density = o.optDouble("density", 1.0).toFloat().coerceIn(0.8f, 1.2f),
                textScale = o.optDouble("textScale", 1.0).toFloat().coerceIn(0.8f, 1.4f),
            )
        }

        val presets: List<FxTheme> = listOf(
            FxTheme("AMOLED", "#000000", "#0D0D0D", "#F2F2F2", "#9A9A9A", "#3EA6FF"),
            FxTheme("Claro", "#FFFFFF", "#F2F2F2", "#0F0F0F", "#606060", "#CC0000"),
            FxTheme("Terminal", "#000000", "#0A120A", "#C8FFC8", "#5FA85F", "#33FF66", font = "mono", radius = 0),
            FxTheme("Dracula", "#282A36", "#343746", "#F8F8F2", "#A0A4B8", "#BD93F9"),
            FxTheme("Nord", "#2E3440", "#3B4252", "#ECEFF4", "#A3ABBA", "#88C0D0"),
            FxTheme("Sépia", "#F4ECD8", "#EADFC4", "#3B2F1E", "#7A6A50", "#B5651D", font = "serif"),
        )
    }
}
