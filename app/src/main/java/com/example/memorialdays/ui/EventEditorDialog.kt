package com.example.memorialdays.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.memorialdays.R
import com.example.memorialdays.data.EventRepository
import com.example.memorialdays.domain.DisplayFormat
import com.example.memorialdays.domain.EventType
import com.example.memorialdays.domain.MemorialEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val CardColorOptions = listOf(
    0,
    0xFFB3261E.toInt(),
    0xFFE37400.toInt(),
    0xFFF2B705.toInt(),
    0xFF2E7D32.toInt(),
    0xFF2F5D50.toInt(),
    0xFF00897B.toInt(),
    0xFF1A73E8.toInt(),
    0xFF3949AB.toInt(),
    0xFF7B1FA2.toInt(),
    0xFFAD1457.toInt(),
    0xFF6D4C41.toInt(),
    0xFF607D8B.toInt()
)

private val NamedColors = mapOf(
    "black" to 0xFF000000.toInt(),
    "white" to 0xFFFFFFFF.toInt(),
    "red" to 0xFFF44336.toInt(),
    "pink" to 0xFFE91E63.toInt(),
    "purple" to 0xFF9C27B0.toInt(),
    "blue" to 0xFF2196F3.toInt(),
    "green" to 0xFF4CAF50.toInt(),
    "yellow" to 0xFFFFEB3B.toInt(),
    "orange" to 0xFFFF9800.toInt(),
    "brown" to 0xFF795548.toInt(),
    "gray" to 0xFF9E9E9E.toInt(),
    "grey" to 0xFF9E9E9E.toInt(),
    "cyan" to 0xFF00BCD4.toInt(),
    "magenta" to 0xFFE91E63.toInt()
)

private fun parseColorOrNull(text: String): Int? {
    val normalized = text.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) return null
    NamedColors[normalized]?.let { return it }
    val hex = normalized.removePrefix("#")
    return when (hex.length) {
        6 -> hex.toLongOrNull(16)?.let { (0xFF000000L or it).toInt() }
        8 -> hex.toLongOrNull(16)?.toInt()
        3 -> {
            val r = hex[0].digitToIntOrNull(16) ?: return null
            val g = hex[1].digitToIntOrNull(16) ?: return null
            val b = hex[2].digitToIntOrNull(16) ?: return null
            (0xFF000000.toInt()) or (r * 17 shl 16) or (g * 17 shl 8) or (b * 17)
        }
        else -> null
    }
}

private fun hexOf(color: Int): String =
    if (color == 0) "" else String.format(Locale.ROOT, "#%06X", 0xFFFFFF and color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorDialog(
    initial: MemorialEvent?,
    onDismiss: () -> Unit,
    onSave: suspend (
        title: String,
        date: LocalDate,
        note: String?,
        isYearly: Boolean,
        isLunar: Boolean,
        color: Int
    ) -> Result<Unit>,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var title by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.title.orEmpty())
    }
    var note by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.note.orEmpty())
    }
    var dateEpochDay by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.date?.toEpochDay() ?: LocalDate.now().toEpochDay())
    }
    var isYearly by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.isYearly == true || initial?.isLunar == true)
    }
    var isLunar by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.isLunar ?: false)
    }
    var color by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.color ?: 0)
    }
    var colorText by rememberSaveable(initial?.id) {
        mutableStateOf(hexOf(initial?.color ?: 0))
    }
    var nameError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val date = remember(dateEpochDay) { LocalDate.ofEpochDay(dateEpochDay) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DisplayFormat.mediumDateFormatter(locale) }
    val scope = rememberCoroutineScope()
    val detectedType = EventType.detect(title)

    fun persistYearlyState() {
        // 编辑已有事件时，开关切换立即写回 Room 并重发射 StateFlow，避免状态失效；
        // 新增或字段尚未合法时只更新本地状态，交由「保存」按钮统一提交。
        if (initial == null) return
        val safeTitle = title.trim()
        if (safeTitle.isEmpty()) return
        if (date.year !in EventRepository.MIN_YEAR..EventRepository.MAX_YEAR) return
        scope.launch {
            onSave(
                safeTitle,
                date,
                note.trim().takeIf { it.isNotEmpty() },
                isYearly,
                isLunar,
                color
            )
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (initial == null) R.string.add_event else R.string.edit_event
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { value ->
                            title = value.take(EventRepository.MAX_TITLE_LENGTH)
                            nameError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.event_name)) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.event_name_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        isError = nameError,
                        supportingText = {
                            if (nameError) {
                                Text(
                                    text = stringResource(R.string.error_name_required),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.char_count_format,
                                        title.length,
                                        EventRepository.MAX_TITLE_LENGTH
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.event_type_auto,
                            stringResource(detectedType.labelRes())
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.event_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = dateFormatter.format(date),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (dateError) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.error_date_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    SwitchRow(
                        title = stringResource(R.string.event_yearly),
                        hint = stringResource(R.string.event_yearly_hint),
                        checked = isYearly,
                        enabled = !isLunar,
                        onCheckedChange = { value ->
                            isYearly = value
                            persistYearlyState()
                        }
                    )
                    SwitchRow(
                        title = stringResource(R.string.event_lunar),
                        hint = stringResource(R.string.event_lunar_hint),
                        checked = isLunar,
                        onCheckedChange = { checked ->
                            isLunar = checked
                            if (checked) isYearly = true
                            persistYearlyState()
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { value ->
                            note = value.take(EventRepository.MAX_NOTE_LENGTH)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.event_note)) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.event_note_placeholder),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 2,
                        supportingText = {
                            Text(
                                text = stringResource(
                                    R.string.char_count_format,
                                    note.length,
                                    EventRepository.MAX_NOTE_LENGTH
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.event_color),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    CardColorOptions.chunked(7).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowColors.forEach { option ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (option == 0) {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            } else {
                                                Color(option)
                                            }
                                        )
                                        .border(
                                            width = if (color == option) 2.dp else 1.dp,
                                            color = if (color == option) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            color = option
                                            colorText = hexOf(option)
                                        }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (color == 0) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color(color or 0xFF000000.toInt())
                                    }
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (color == 0) {
                                stringResource(R.string.event_color_default)
                            } else {
                                hexOf(color)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = colorText,
                        onValueChange = { text ->
                            colorText = text
                            parseColorOrNull(text)?.let { color = it }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.event_color_code)) },
                        placeholder = { Text("#FFFFFF / black / pink") },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    val currentR = (color shr 16) and 0xFF
                    val currentG = (color shr 8) and 0xFF
                    val currentB = color and 0xFF
                    RgbSlider(
                        label = "R",
                        value = currentR,
                        onChange = { v ->
                            color = ((color or 0xFF000000.toInt()) and
                                0xFF00FFFF.toInt()) or (v shl 16)
                            colorText = hexOf(color)
                        }
                    )
                    RgbSlider(
                        label = "G",
                        value = currentG,
                        onChange = { v ->
                            color = ((color or 0xFF000000.toInt()) and
                                0xFFFF00FF.toInt()) or (v shl 8)
                            colorText = hexOf(color)
                        }
                    )
                    RgbSlider(
                        label = "B",
                        value = currentB,
                        onChange = { v ->
                            color = ((color or 0xFF000000.toInt()) and
                                0xFFFFFF00.toInt()) or v
                            colorText = hexOf(color)
                        }
                    )

                    if (saveError) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.error_save_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDelete != null) {
                            TextButton(
                                onClick = { onDelete.invoke() },
                                enabled = !isSaving,
                                contentPadding = PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (onShare != null) {
                            TextButton(
                                onClick = { onShare.invoke() },
                                enabled = !isSaving,
                                contentPadding = PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.action_share),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val safeTitle = title.trim()
                                if (safeTitle.isEmpty()) {
                                    nameError = true
                                    return@Button
                                }
                                if (date.year !in EventRepository.MIN_YEAR..EventRepository.MAX_YEAR) {
                                    dateError = true
                                    return@Button
                                }
                                scope.launch {
                                    isSaving = true
                                    saveError = false
                                    val result = onSave(
                                        safeTitle,
                                        date,
                                        note.trim().takeIf { it.isNotEmpty() },
                                        isYearly,
                                        isLunar,
                                        color
                                    )
                                    isSaving = false
                                    if (result.isSuccess) {
                                        onDismiss()
                                    } else {
                                        saveError = true
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 10.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = stringResource(
                                    if (isSaving) R.string.action_saving else R.string.action_save
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(date) {
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = EventRepository.MIN_YEAR..EventRepository.MAX_YEAR
        )
        val pickerFormatter = DatePickerDefaults.dateFormatter()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            val picked = Instant.ofEpochMilli(selected)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            if (picked.year in EventRepository.MIN_YEAR..EventRepository.MAX_YEAR) {
                                dateEpochDay = picked.toEpochDay()
                                dateError = false
                            } else {
                                dateError = true
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
                headline = {
                    val selected = datePickerState.selectedDateMillis
                    val mode = datePickerState.displayMode
                    val headlineText = if (selected != null) {
                        pickerFormatter.formatDate(selected, locale).orEmpty()
                    } else {
                        stringResource(
                            if (mode == DisplayMode.Input) {
                                R.string.date_picker_input_headline
                            } else {
                                R.string.date_picker_headline
                            }
                        )
                    }
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun RgbSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(16.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}
