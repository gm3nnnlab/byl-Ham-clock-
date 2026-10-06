package com.hamdash.chornobyl.domain

enum class RepeaterFilter(val label: String, val mode: RadioMode?) {
    ALL("All", null),
    DMR("DMR", RadioMode.DMR),
    FM("FM", RadioMode.FM),
    DSTAR("D-STAR", RadioMode.DSTAR),
    C4FM("C4FM", RadioMode.C4FM),
    OTHER("Other", RadioMode.OTHER);

    fun matches(repeater: Repeater): Boolean = mode == null || repeater.mode == mode
}

fun List<Repeater>.filterBy(filter: RepeaterFilter, query: String = ""): List<Repeater> {
    val q = query.trim()
    return filter { repeater ->
        filter.matches(repeater) && (q.isEmpty() || repeater.matchesQuery(q))
    }
}

private fun Repeater.matchesQuery(q: String): Boolean =
    callsign.contains(q, ignoreCase = true) ||
        name.contains(q, ignoreCase = true) ||
        formatMHz(rxMHz).contains(q) ||
        formatMHz(txMHz).contains(q)

fun List<Repeater>.countByMode(): Map<RadioMode, Int> =
    RadioMode.entries.associateWith { mode -> count { it.mode == mode } }
