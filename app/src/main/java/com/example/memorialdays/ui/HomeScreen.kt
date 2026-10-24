@file:OptIn(ExperimentalFoundationApi::class)

package com.example.memorialdays.ui

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memorialdays.R
import com.example.memorialdays.domain.DayStatus
import com.example.memorialdays.domain.DisplayFormat
import com.example.memorialdays.domain.MemorialEvent
import com.example.memorialdays.share.PosterData
import com.example.memorialdays.share.PosterStyle
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class EditorTarget(val item: EventUi?)

@Composable
fun HomeScreen(viewModel: EventViewModel) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = rememberSafeHaptics()
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme

    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<MemorialEvent?>(null) }
    var actionTarget by remember { mutableStateOf<EventUi?>(null) }
    var showBackup by remember { mutableStateOf(false) }

    val posterStyle = remember(
        scheme.background,
        scheme.surface,
        scheme.primary,
        scheme.onSurfaceVariant,
        isDark
    ) {
        PosterStyle(
            background = scheme.background.toArgb(),
            surface = scheme.surface.toArgb(),
            text = if (isDark) Color.White.toArgb() else Color.Black.toArgb(),
            muted = scheme.onSurfaceVariant.toArgb(),
            accent = scheme.primary.toArgb()
        )
    }
    val shareTitle = stringResource(R.string.poster_share_title)
    val posterFailed = stringResource(R.string.poster_failed)
    val noShareApp = stringResource(R.string.poster_no_app)
    val exportFileName = stringResource(R.string.backup_export_file_name)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    val sharePoster: (PosterData) -> Unit = { data ->
        scope.launch {
            val result = viewModel.createPoster(data, posterStyle)
            val share = result.getOrNull()
            if (share == null) {
                snackbarHostState.showSnackbar(posterFailed)
            } else {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, share.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val launched = runCatching {
                    context.startActivity(Intent.createChooser(send, shareTitle))
                }.isSuccess
                if (!launched) {
                    snackbarHostState.showSnackbar(noShareApp)
                    viewModel.cleanupPoster(share.file)
                } else {
                    // Give the receiving app time to read the content:// URI,
                    // then delete the cache file instead of leaking it.
                    delay(60_000L)
                    viewModel.cleanupPoster(share.file)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = if (message.args.isEmpty()) {
                context.getString(message.messageRes)
            } else {
                context.getString(message.messageRes, *message.args.toTypedArray())
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    val background = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        background,
                        lerp(background, accent, 0.20f)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                HomeHeader(
                    today = (state as? HomeUiState.Ready)?.today ?: LocalDate.now(),
                    haptics = haptics,
                    onAdd = { editorTarget = EditorTarget(item = null) },
                    onBrandLongClick = { showBackup = true }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (val current = state) {
                        HomeUiState.Loading -> LoadingState()
                        is HomeUiState.Error -> ErrorState(onRetry = viewModel::retry)
                        is HomeUiState.Ready -> {
                            if (current.isEmpty) {
                                EmptyState(
                                    haptics = haptics,
                                    onAdd = { editorTarget = EditorTarget(item = null) }
                                )
                            } else {
                                SortableEventList(
                                    items = current.items,
                                    haptics = haptics,
                                    onEdit = { editorTarget = EditorTarget(item = it) },
                                    onOpenActions = { actionTarget = it },
                                    onReorder = viewModel::reorder,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editorTarget?.let { target ->
        val item = target.item
        EventEditorDialog(
            initial = item?.event,
            onDismiss = { editorTarget = null },
            onSave = { title, date, note, isYearly, isLunar, color ->
                viewModel.save(
                    id = item?.event?.id ?: 0L,
                    title = title,
                    date = date,
                    note = note,
                    isYearly = isYearly,
                    isLunar = isLunar,
                    color = color
                )
            }
        )
    }

    actionTarget?.let { item ->
        EventActionSheet(
            item = item,
            haptics = haptics,
            onDismiss = { actionTarget = null },
            onTogglePin = {
                actionTarget = null
                viewModel.togglePin(item.event)
            },
            onShare = { data ->
                actionTarget = null
                sharePoster(data)
            },
            onDelete = {
                actionTarget = null
                deleteTarget = item.event
            }
        )
    }

    if (showBackup) {
        BackupSheet(
            haptics = haptics,
            onDismiss = { showBackup = false },
            onExport = { exportLauncher.launch(exportFileName) },
            onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_confirm_message, target.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(target)
                        deleteTarget = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        )
    }
}

@Composable
private fun HomeHeader(
    today: LocalDate,
    haptics: SafeHaptics,
    onAdd: () -> Unit,
    onBrandLongClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthDayPattern = stringResource(R.string.pattern_month_day)
    val monthDay = remember(today, locale, monthDayPattern) {
        DisplayFormat.patternDate(today, monthDayPattern, locale)
    }
    val weekday = remember(today, locale) {
        DisplayFormat.weekday(today, locale)
    }
    val meta = stringResource(R.string.date_meta_format, weekday, today.year.toString())
    val brand = stringResource(R.string.brand_name).uppercase(Locale.ROOT)
    val addLabel = stringResource(R.string.content_desc_add)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = brand,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptics.longPress()
                            onBrandLongClick()
                        }
                    )
                    .padding(vertical = 10.dp)
            )
            AddButton(
                onClick = {
                    haptics.click()
                    onAdd()
                },
                description = addLabel
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = monthDay,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, description: String) {
    val iconColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                onClickLabel = description,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = 1.4.dp.toPx()
            drawLine(
                color = iconColor,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = iconColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SortablePage(
    items: List<EventUi>,
    haptics: SafeHaptics,
    onEdit: (EventUi) -> Unit,
    onOpenActions: (EventUi) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var fingerDeltaY by remember { mutableStateOf(0f) }
    var dragStartLayoutOffset by remember { mutableStateOf(0f) }
    val currentItems by rememberUpdatedState(items)
    val localItems = remember {
        mutableStateListOf<EventUi>().apply { addAll(items) }
    }
    val listState = rememberLazyListState()
    val overscroll = rememberOverscrollEffect()
    val overscrollEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        overscroll
    } else {
        null
    }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val baseConfiguration = LocalViewConfiguration.current
    val dragConfiguration = remember(baseConfiguration) {
        object : androidx.compose.ui.platform.ViewConfiguration {
            override val longPressTimeoutMillis: Long = 300L
            override val doubleTapTimeoutMillis: Long =
                baseConfiguration.doubleTapTimeoutMillis
            override val doubleTapMinTimeMillis: Long =
                baseConfiguration.doubleTapMinTimeMillis
            override val touchSlop: Float = baseConfiguration.touchSlop
            override val minimumTouchTargetSize: DpSize =
                baseConfiguration.minimumTouchTargetSize
        }
    }
    val itemSpacingPx = with(LocalDensity.current) { 14.dp.toPx() }
    val horizontalPadding = 24.dp
    val scope = rememberCoroutineScope()

    // Minimal-diff sync: move existing rows instead of clear()+addAll(), so
    // LazyColumn keeps the item nodes and animateItem can slide them smoothly
    // instead of teleporting.
    LaunchedEffect(items) {
        if (draggingId != null) return@LaunchedEffect
        val incomingIds = items.map { it.event.id }.toSet()
        localItems.removeAll { it.event.id !in incomingIds }
        items.forEachIndexed { index, item ->
            val current = localItems.indexOfFirst { it.event.id == item.event.id }
            if (current < 0) {
                localItems.add(index.coerceAtMost(localItems.size), item)
            } else {
                if (current != index) {
                    localItems.add(index, localItems.removeAt(current))
                }
                localItems[index] = item
            }
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides dragConfiguration) {
        Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 2.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    overscrollEffect = overscrollEffect
                ) {
                    itemsIndexed(
                        items = localItems,
                        key = { _, item -> item.event.id }
                    ) { _, item ->
                        val currentItem by rememberUpdatedState(item)
                        val currentOnEdit by rememberUpdatedState(onEdit)
                        val currentOnOpenActions by rememberUpdatedState(onOpenActions)
                        val currentOnReorder by rememberUpdatedState(onReorder)
                        val currentPinned by rememberUpdatedState(item.event.isPinned)
                        val isDragging = draggingId == item.event.id

                        val rowModifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
                            )
                            .zIndex(if (isDragging) 10f else 0f)
                            .then(
                                if (isDragging) {
                                    Modifier
                                } else {
                                    Modifier.animateItem(
                                        placementSpec = spring<IntOffset>(
                                            dampingRatio = 0.85f,
                                            stiffness = 300f
                                        )
                                    )
                                }
                            )
                            .pointerInput(item.event.id, currentPinned) {
                                val velocityTracker = VelocityTracker()
                                var moved = false

                                fun animateOverlayTo(
                                    targetOffset: Float,
                                    onSettled: () -> Unit
                                ) {
                                    val start = fingerDeltaY
                                    val targetDelta = targetOffset - dragStartLayoutOffset
                                    scope.launch {
                                        Animatable(start).animateTo(
                                            targetDelta,
                                            spring(dampingRatio = 0.85f, stiffness = 300f)
                                        ) {
                                            fingerDeltaY = value
                                        }
                                        onSettled()
                                        draggingId = null
                                        fingerDeltaY = 0f
                                    }
                                }

                                fun itemOffset(id: Long): Float? =
                                    listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == id }
                                        ?.let { it.offset.toFloat() }

                                fun itemBottom(id: Long): Float? =
                                    listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == id }
                                        ?.let { (it.offset + it.size).toFloat() }

                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = item.event.id
                                        fingerDeltaY = 0f
                                        velocityTracker.resetTracking()
                                        val info = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == item.event.id }
                                        dragStartLayoutOffset = (info?.offset ?: 0).toFloat()
                                        moved = false
                                        haptics.longPress()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        velocityTracker.addPosition(
                                            change.uptimeMillis,
                                            change.position
                                        )
                                        fingerDeltaY += amount.y
                                        if (!moved && abs(fingerDeltaY) > touchSlop) {
                                            moved = true
                                        }
                                        if (moved && !currentPinned) {
                                            val from = localItems.indexOfFirst {
                                                it.event.id == item.event.id
                                            }
                                            if (from >= 0) {
                                                val pinnedCount = localItems.count {
                                                    it.event.isPinned
                                                }
                                                val minIndex = pinnedCount.coerceAtMost(
                                                    localItems.lastIndex
                                                )
                                                val draggedTop = dragStartLayoutOffset +
                                                    fingerDeltaY
                                                val target = localItems.indexOfFirst { row ->
                                                    val info = listState.layoutInfo
                                                        .visibleItemsInfo
                                                        .firstOrNull {
                                                            it.key == row.event.id
                                                        }
                                                    info != null && draggedTop >=
                                                        info.offset &&
                                                        draggedTop < info.offset +
                                                        info.size + itemSpacingPx
                                                }
                                                if (target >= 0 && target != from &&
                                                    target in minIndex..localItems.lastIndex
                                                ) {
                                                    localItems.add(
                                                        target,
                                                        localItems.removeAt(from)
                                                    )
                                                    haptics.dragTick()
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (!moved) {
                                            currentOnOpenActions(currentItem)
                                            draggingId = null
                                            fingerDeltaY = 0f
                                        } else {
                                            val velocityY = velocityTracker
                                                .calculateVelocity().y
                                            val currentY = dragStartLayoutOffset +
                                                fingerDeltaY
                                            val pinnedCount = localItems.count {
                                                it.event.isPinned
                                            }
                                            val lastItem = localItems.lastOrNull()
                                            val pinnedBottom = if (pinnedCount > 0) {
                                                localItems[pinnedCount - 1]
                                                    .let { itemBottom(it.event.id) } ?: 0f
                                            } else {
                                                0f
                                            }
                                            val lastBottom = lastItem
                                                ?.let { itemBottom(it.event.id) } ?: 0f
                                            val fast = abs(velocityY) > 1200f

                                            if (currentPinned) {
                                                // Pinned card is fixed: pull it
                                                // down, then spring it back.
                                                animateOverlayTo(dragStartLayoutOffset) {}
                                            } else if (
                                                pinnedCount > 0 &&
                                                currentY < pinnedBottom
                                            ) {
                                                if (fast) {
                                                    // A quick fling into the
                                                    // hero block bounces back.
                                                    animateOverlayTo(
                                                        dragStartLayoutOffset
                                                    ) {}
                                                } else {
                                                    // A slow drag settles just
                                                    // below the hero block.
                                                    val targetIndex = pinnedCount
                                                        .coerceAtMost(localItems.lastIndex)
                                                    val targetOffset = localItems[
                                                        targetIndex
                                                    ].let { itemOffset(it.event.id) }
                                                        ?: dragStartLayoutOffset
                                                    animateOverlayTo(targetOffset) {
                                                        val from = localItems
                                                            .indexOfFirst {
                                                                it.event.id == item.event.id
                                                            }
                                                        if (from >= 0 &&
                                                            targetIndex != from
                                                        ) {
                                                            localItems.add(
                                                                targetIndex,
                                                                localItems.removeAt(from)
                                                            )
                                                        }
                                                        currentOnReorder(
                                                            localItems.map { it.event.id }
                                                        )
                                                    }
                                                }
                                            } else if (
                                                lastItem != null &&
                                                currentY > lastBottom
                                            ) {
                                                if (fast) {
                                                    // Fling past the bottom
                                                    // bounces back.
                                                    animateOverlayTo(
                                                        dragStartLayoutOffset
                                                    ) {}
                                                } else {
                                                    val targetIndex = localItems.lastIndex
                                                    val targetOffset =
                                                        itemOffset(lastItem.event.id)
                                                            ?: dragStartLayoutOffset
                                                    animateOverlayTo(targetOffset) {
                                                        val from = localItems
                                                            .indexOfFirst {
                                                                it.event.id == item.event.id
                                                            }
                                                        if (from >= 0 &&
                                                            targetIndex != from
                                                        ) {
                                                            localItems.add(
                                                                targetIndex,
                                                                localItems.removeAt(from)
                                                            )
                                                        }
                                                        currentOnReorder(
                                                            localItems.map { it.event.id }
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Normal in-range drop: glide to
                                                // the current slot, then commit.
                                                val targetOffset =
                                                    itemOffset(item.event.id)
                                                        ?: dragStartLayoutOffset
                                                animateOverlayTo(targetOffset) {
                                                    currentOnReorder(
                                                        localItems.map { it.event.id }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        val incomingIds = currentItems.map { it.event.id }
                                        localItems.sortBy { row ->
                                            incomingIds.indexOf(row.event.id)
                                                .takeIf { it >= 0 } ?: Int.MAX_VALUE
                                        }
                                        draggingId = null
                                        fingerDeltaY = 0f
                                    }
                                )
                            }

                        Box(modifier = rowModifier) {
                            val onCardClick = {
                                haptics.click()
                                currentOnEdit(currentItem)
                            }
                            AnimatedContent(
                                targetState = item.event.isPinned,
                                transitionSpec = {
                                    // Symmetric soft fade in both directions.
                                    // The height itself is animated by
                                    // animateContentSize on the row, so a hero
                                    // card shrinks continuously as it falls
                                    // into its compact slot instead of snapping
                                    // smaller only after it lands.
                                    fadeIn(
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = 300f
                                        )
                                    ).togetherWith(
                                        fadeOut(
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = 300f
                                            )
                                        )
                                    ).using(null)
                                },
                                label = "cardSize"
                            ) { pinned ->
                                if (isDragging) {
                                    // Invisible placeholder keeps the slot
                                    // height; the real card is drawn in the
                                    // overlay above the list.
                                    Box(modifier = Modifier.alpha(0f)) {
                                        if (pinned) {
                                            HeroCard(item = item, onClick = {})
                                        } else {
                                            EventCard(item = item, onClick = {})
                                        }
                                    }
                                } else {
                                    if (pinned) {
                                        HeroCard(item = item, onClick = onCardClick)
                                    } else {
                                        EventCard(item = item, onClick = onCardClick)
                                    }
                                }
                            }
                        }
                    }
                }

                // The dragged card is drawn above the list and follows the
                // finger from the date header all the way to the bottom, so it
                // stays whole with no clipping or edge.
                val draggingItem = localItems.firstOrNull { it.event.id == draggingId }
                if (draggingItem != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (dragStartLayoutOffset + fingerDeltaY).roundToInt()
                                )
                            }
                            .zIndex(100f)
                            .padding(horizontal = horizontalPadding)
                    ) {
                        if (draggingItem.event.isPinned) {
                            HeroCard(item = draggingItem, onClick = {})
                        } else {
                            EventCard(item = draggingItem, onClick = {})
                        }
                    }
                }
            }
    }
}

private const val PAGE_SIZE = 5

/**
 * Five events per page, swiped horizontally. Each page is a self-contained
 * sortable list; reordering inside a page is written back to the matching
 * slice of the global order.
 */
@Composable
private fun SortableEventList(
    items: List<EventUi>,
    haptics: SafeHaptics,
    onEdit: (EventUi) -> Unit,
    onOpenActions: (EventUi) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember(items) { items.chunked(PAGE_SIZE) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val globalIds = items.map { it.event.id }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val pageOffset = abs(
                (pagerState.currentPage - pageIndex) +
                    pagerState.currentPageOffsetFraction
            )
            val pageAlpha = (1f - pageOffset * 0.35f).coerceIn(0.55f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = pageAlpha }
            ) {
                val pageItems = pages.getOrNull(pageIndex).orEmpty()
                SortablePage(
                    items = pageItems,
                    haptics = haptics,
                    onEdit = onEdit,
                    onOpenActions = onOpenActions,
                    onReorder = { pageIds ->
                        val merged = globalIds.toMutableList()
                        val start = pageIndex * PAGE_SIZE
                        pageIds.forEachIndexed { i, id ->
                            if (start + i < merged.size) {
                                merged[start + i] = id
                            }
                        }
                        onReorder(merged)
                    }
                )
            }
        }
        if (pages.size > 1) {
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                isScrolling = pagerState.isScrollInProgress
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    isScrolling: Boolean
) {
    AnimatedVisibility(
        visible = isScrolling,
        enter = fadeIn(animationSpec = tween(durationMillis = 250)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { index ->
                val dotSize by animateDpAsState(
                    targetValue = if (index == currentPage) 8.dp else 6.dp,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                    label = "dotSize"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    haptics: SafeHaptics,
    onAdd: () -> Unit
) {
    val ringColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    val transition = rememberInfiniteTransition(label = "emptyBreath")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                1f at 0
                1.02f at 1100
                1f at 2200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "emptyBreathScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .graphicsLayer {
                scaleX = breath
                scaleY = breath
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            drawCircle(
                color = ringColor,
                radius = size.minDimension / 2f - 1.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = ringColor,
                start = Offset(size.width * 0.38f, size.height / 2f),
                end = Offset(size.width * 0.62f, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = {
                haptics.click()
                onAdd()
            },
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_action),
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.error_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.action_retry),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
