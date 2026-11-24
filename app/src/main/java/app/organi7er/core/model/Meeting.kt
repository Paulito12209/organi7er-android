package app.organi7er.core.model

/** Plattform eines Termin-Links, erkannt am Host. */
enum class MeetingPlatform(val label: String) {
    GoogleMeet("Google Meet"),
    Zoom("Zoom"),
    Teams("Microsoft Teams"),
    Webex("Webex"),
    Other("Link"),
}

object Meeting {
    /** „meet.google.com/x“ → „https://meet.google.com/x“; vorhandenes Schema bleibt. */
    fun normalize(raw: String): String {
        val t = raw.trim()
        return if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(t)) t else "https://$t"
    }

    fun platform(link: String): MeetingPlatform {
        val host = runCatching { java.net.URI(normalize(link)).host }.getOrNull()?.lowercase().orEmpty()
        fun on(domain: String) = host == domain || host.endsWith(".$domain")
        return when {
            on("meet.google.com") -> MeetingPlatform.GoogleMeet
            on("zoom.us") || on("zoom.com") -> MeetingPlatform.Zoom
            on("teams.microsoft.com") || on("teams.live.com") -> MeetingPlatform.Teams
            on("webex.com") -> MeetingPlatform.Webex
            else -> MeetingPlatform.Other
        }
    }
}
