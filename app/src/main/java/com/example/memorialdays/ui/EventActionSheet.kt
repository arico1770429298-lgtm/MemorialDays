package com.example.memorialdays.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.memorialdays.R
import com.example.memorialdays.domain.DayStatus
import com.example.memorialdays.share.PosterData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventActionSheet(
    item: EventUi,
    haptics: SafeHaptics,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: (PosterData) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val posterData = PosterData(
        title = item.event.title,
        number = item.displayDays.toString(),
        status = item.heroStatus(),
        type = stringResource(item.typeLabelRes),
        primaryDate = item.baseDateInfoCompact(),
        secondaryDate = item.nextOccurrenceCopy().orEmpty(),
        isToday = item.count.status == DayStatus.TODAY
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = item.event.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.heroStatus(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            SheetActionRow(
                label = stringResource(
                    if (item.event.isPinned) R.string.action_unpin else R.string.action_pin
                ),
                color = MaterialTheme.colorScheme.onSurface,
                haptics = haptics
            ) {
                onDismiss()
                onTogglePin()
            }
            SheetActionRow(
                label = stringResource(R.string.action_share),
                color = MaterialTheme.colorScheme.onSurface,
                haptics = haptics
            ) {
                onDismiss()
                onShare(posterData)
            }
            SheetActionRow(
                label = stringResource(R.string.action_delete),
                color = MaterialTheme.colorScheme.error,
                haptics = haptics
            ) {
                onDismiss()
                onDelete()
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
