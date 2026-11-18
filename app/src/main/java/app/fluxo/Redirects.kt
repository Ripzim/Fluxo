package app.fluxo

import android.net.Uri

object Redirects {
    const val HOME = "https://m.youtube.com/"

    private val INTERNAL = listOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com",
        "google.com", "gstatic.com", "googleusercontent.com",
        "ggpht.com", "googlevideo.com", "ytimg.com",
    )

    /** true se o link deve abrir dentro do app (YouTube + login do Google). */
    fun isInternal(uri: Uri): Boolean {
        if (uri.scheme != "https" && uri.scheme != "http") return false
        val host = uri.host ?: return false
        return INTERNAL.any { host == it || host.endsWith(".$it") }
    }

    /**
     * Se for um link de Short, devolve a URL equivalente como vídeo normal
     * (/shorts/ID -> /watch?v=ID) ou a home (/shorts). Senão, null.
     */
    fun rewriteShorts(uri: Uri): String? {
        val host = uri.host ?: return null
        if (host != "youtube.com" && !host.endsWith(".youtube.com")) return null
        val segments = uri.pathSegments
        if (segments.isEmpty() || segments[0] != "shorts") return null
        val id = segments.getOrNull(1)
        return if (id.isNullOrBlank()) HOME else "https://m.youtube.com/watch?v=$id"
    }
}
