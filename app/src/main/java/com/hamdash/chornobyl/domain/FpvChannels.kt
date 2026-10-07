package com.hamdash.chornobyl.domain

/** Analog 5.8 GHz FPV video channels as published by VTX makers (Boscam A/B/E, Fatshark, Raceband). */
data class FpvChannel(val band: String, val number: Int, val mhz: Int) {
    val label: String get() = "$band$number"

    /** Inside the 5725-5875 MHz short-range-device band (low power, license-exempt in most of Europe). */
    val inSrdBand: Boolean get() = mhz in SRD_START..SRD_END

    /** Inside the 5650-5850 MHz amateur allocation (IARU Region 1); needs an amateur license. */
    val inAmateurBand: Boolean get() = mhz in AMATEUR_START..AMATEUR_END

    companion object {
        const val SRD_START = 5725
        const val SRD_END = 5875
        const val AMATEUR_START = 5650
        const val AMATEUR_END = 5850
    }
}

object FpvChannels {
    private val bands = linkedMapOf(
        "A" to listOf(5865, 5845, 5825, 5805, 5785, 5765, 5745, 5725),
        "B" to listOf(5733, 5752, 5771, 5790, 5809, 5828, 5847, 5866),
        "E" to listOf(5705, 5685, 5665, 5645, 5885, 5905, 5925, 5945),
        "F" to listOf(5740, 5760, 5780, 5800, 5820, 5840, 5860, 5880),
        "R" to listOf(5658, 5695, 5732, 5769, 5806, 5843, 5880, 5917),
    )

    val all: List<FpvChannel> = bands.flatMap { (band, freqs) ->
        freqs.mapIndexed { index, mhz -> FpvChannel(band, index + 1, mhz) }
    }

    fun band(name: String): List<FpvChannel> = all.filter { it.band == name }

    val bandNames: List<String> get() = bands.keys.toList()
}
