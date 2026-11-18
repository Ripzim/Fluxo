package app.fluxo

/** Gera o CSS do tema que é injetado no m.youtube.com. */
object Injector {

    fun themeCss(t: FxTheme): String {
        val family = when (t.font) {
            "serif" -> "Georgia, 'Times New Roman', serif"
            "mono" -> "ui-monospace, 'SFMono-Regular', Menlo, Consolas, monospace"
            else -> ""
        }
        val sb = StringBuilder()

        // Variáveis nativas do YouTube
        sb.append(":root,html,html[dark],html[light],body{")
        fun v(name: String, value: String) {
            sb.append("--").append(name).append(":").append(value).append(" !important;")
        }
        v("yt-spec-base-background", t.bg)
        v("yt-spec-general-background-a", t.bg)
        v("yt-spec-general-background-b", t.surface)
        v("yt-spec-general-background-c", t.surface)
        v("yt-spec-raised-background", t.surface)
        v("yt-spec-menu-background", t.surface)
        v("yt-spec-badge-chip-background", t.surface)
        v("yt-spec-brand-background-solid", t.bg)
        v("yt-spec-brand-background-primary", t.bg)
        v("yt-spec-text-primary", t.text)
        v("yt-spec-text-secondary", t.textSecondary)
        v("yt-spec-text-disabled", t.textSecondary)
        v("yt-spec-icon-active-other", t.text)
        v("yt-spec-icon-inactive", t.textSecondary)
        v("yt-spec-call-to-action", t.accent)
        v("yt-spec-themed-blue", t.accent)
        v("yt-spec-static-brand-red", t.accent)
        v("yt-spec-10-percent-layer", "rgba(128,128,128,0.25)")
        v("fx-radius", "${t.radius}px")
        sb.append("}")

        // Fundos principais
        sb.append("html,body,ytm-app{background:").append(t.bg)
            .append(" !important;color:").append(t.text).append(" !important;}")
        sb.append(".mobile-topbar-header,.mobile-topbar-header-content,ytm-mobile-topbar-renderer,")
            .append("ytm-pivot-bar-renderer,.pivot-bar-wrapper,ytm-feed-filter-chip-bar-renderer{background:")
            .append(t.bg).append(" !important;}")
        sb.append("ytm-bottom-sheet-renderer,ytm-menu-popup-renderer{background:")
            .append(t.surface).append(" !important;}")

        // Destaque (barra de progresso das miniaturas)
        sb.append(".thumbnail-overlay-resume-playback-progress{background-color:")
            .append(t.accent).append(" !important;}")

        // Arredondamento das miniaturas
        sb.append(".video-thumbnail-container-large,.video-thumbnail-container-compact,")
            .append(".media-item-thumbnail-container,ytm-thumbnail-cover{")
            .append("border-radius:var(--fx-radius) !important;overflow:hidden;}")

        // Fonte
        if (family.isNotEmpty()) {
            sb.append("html,body,ytm-app,ytm-app *{font-family:").append(family).append(" !important;}")
        }

        // Densidade (zoom da página, experimental)
        if (t.density != 1f) {
            sb.append("html{zoom:").append(t.density).append(";}")
        }
        return sb.toString()
    }
}
