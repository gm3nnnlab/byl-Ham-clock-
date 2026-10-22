package com.hamdash.chornobyl.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.RepeaterStatus

@Composable
fun RepeaterStatus.displayName(): String = stringResource(
    when (this) {
        RepeaterStatus.ACTIVE -> R.string.status_active
        RepeaterStatus.OFF_AIR -> R.string.status_off_air
        RepeaterStatus.UNKNOWN -> R.string.status_unknown
    },
)

fun RepeaterStatus.color(): Color = when (this) {
    RepeaterStatus.ACTIVE -> Color(0xFF2EBD6B)
    RepeaterStatus.OFF_AIR -> Color(0xFFE5484D)
    RepeaterStatus.UNKNOWN -> Color(0xFF7A8794)
}

@Composable
fun RepeaterStatusBadge(status: RepeaterStatus, modifier: Modifier = Modifier) {
    Surface(color = status.color(), shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            status.displayName(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
