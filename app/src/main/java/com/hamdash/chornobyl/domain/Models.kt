package com.hamdash.chornobyl.domain

enum class RadioMode(val label: String) {
    DMR("DMR"),
    FM("FM"),
    DSTAR("D-STAR"),
    C4FM("C4FM"),
    OTHER("Other");

    companion object {
        fun parse(raw: String?): RadioMode {
            val key = raw?.uppercase()?.filter { it.isLetterOrDigit() } ?: return OTHER
            return when (key) {
                "DMR" -> DMR
                "FM", "NFM", "ANALOG" -> FM
                "DSTAR" -> DSTAR
                "C4FM", "YSF", "FUSION", "SYSTEMFUSION", "C4FMSYSTEMFUSION" -> C4FM
                else -> OTHER
            }
        }
    }
}

/**
 * Whether a repeater is on the air, as stated by the catalog source. UNKNOWN is the default: the app never
 * assumes a repeater works.
 */
enum class RepeaterStatus {
    ACTIVE, OFF_AIR, UNKNOWN;

    companion object {
        fun parse(raw: String?): RepeaterStatus =
            when (raw?.uppercase()?.filter { it.isLetter() }) {
                "ACTIVE", "ON", "ONAIR", "OPERATIONAL" -> ACTIVE
                "OFFAIR", "OFF", "INACTIVE", "DOWN", "SUSPENDED" -> OFF_AIR
                else -> UNKNOWN
            }
    }
}

/**
 * RX is the frequency the user listens on (repeater output); TX is the frequency the user
 * transmits on (repeater input). offset = TX - RX.
 */
data class Repeater(
    val id: String,
    val name: String,
    val callsign: String,
    val rxMHz: Double,
    val txMHz: Double,
    val mode: RadioMode,
    val ctcssHz: Double?,
    val colorCode: Int?,
    val timeslot: Int?,
    val talkgroupIds: List<Int>,
    val latitude: Double,
    val longitude: Double,
    val elevationM: Int?,
    val source: String,
    val lastUpdated: String,
    val status: RepeaterStatus = RepeaterStatus.UNKNOWN,
) {
    val offsetMHz: Double get() = txMHz - rxMHz
    val bandName: String? get() = bandNameFor(rxMHz)
}

data class Talkgroup(
    val id: Int,
    val name: String,
    val description: String,
    val source: String,
)

data class Band(
    val name: String,
    val startMHz: Double,
    val endMHz: Double,
    val typicalModes: String,
    val purpose: String,
    val notes: String,
)

data class DocLink(val title: String, val url: String)

data class DigitalMode(
    val id: String,
    val name: String,
    val summary: String,
    val principle: String,
    val uses: String,
    val equipment: String,
    val links: List<DocLink>,
)

data class RadioCatalog(
    val updated: String? = null,
    val repeaters: List<Repeater> = emptyList(),
    val talkgroups: List<Talkgroup> = emptyList(),
    val bands: List<Band> = emptyList(),
    val digitalModes: List<DigitalMode> = emptyList(),
) {
    val isEmpty: Boolean
        get() = repeaters.isEmpty() && talkgroups.isEmpty() && bands.isEmpty() && digitalModes.isEmpty()
}
