package com.rickdnamps.glp1tracker.ui.screens

import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.PhotoEntity
import com.rickdnamps.glp1tracker.data.PhotoStore
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.ConfirmDialog
import com.rickdnamps.glp1tracker.ui.components.DatePickerDialogFor
import com.rickdnamps.glp1tracker.ui.components.GlassTile
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.Pill
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.weightText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

/** Poses offered for progress photos; the name is stored in [PhotoEntity.pose]. */
private enum class PhotoPose(@StringRes val label: Int) {
    FRONT(R.string.photos_pose_front),
    SIDE(R.string.photos_pose_side),
    BACK(R.string.photos_pose_back);

    companion object {
        fun fromName(name: String?): PhotoPose = entries.firstOrNull { it.name == name } ?: FRONT
    }
}

private const val THUMB_SIDE = 360
private const val COMPARE_SIDE = 1000
private const val FULL_SIDE = 1600
private const val DAY_MS = 86_400_000L

/** Nearest weigh-in within ±[maxDays] days of [timestamp], or null when none is close enough. */
internal fun nearestWeight(weights: List<WeightEntity>, timestamp: Long, maxDays: Int = 3): WeightEntity? =
    weights.asSequence()
        .filter { abs(it.timestamp - timestamp) <= maxDays * DAY_MS }
        .minByOrNull { abs(it.timestamp - timestamp) }

// --- Bitmap loading -------------------------------------------------------------------------

/** Decoded bitmaps kept in memory so thumbnails don't flicker when the grid scrolls back. */
private object PhotoCache {
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    operator fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}

/** Result of a finished load; [bitmap] is null when the file is missing or unreadable. */
private class LoadedPhoto(val bitmap: ImageBitmap?)

/** Null while loading. */
@Composable
private fun rememberLoadedPhoto(fileName: String, maxSide: Int): LoadedPhoto? {
    val context = LocalContext.current.applicationContext
    val key = "$fileName@$maxSide"
    return produceState(initialValue = PhotoCache[key]?.let { LoadedPhoto(it) }, key) {
        val cached = PhotoCache[key]
        if (cached != null) {
            value = LoadedPhoto(cached)
            return@produceState
        }
        value = null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching { PhotoStore.load(context, fileName, maxSide)?.asImageBitmap() }.getOrNull()
        }
        if (bitmap != null) PhotoCache.put(key, bitmap)
        value = LoadedPhoto(bitmap)
    }.value
}

/** Loads a private progress photo off the main thread, downscaled to about [maxSide] px. */
@Composable
fun rememberPhotoBitmap(fileName: String, maxSide: Int): ImageBitmap? = rememberLoadedPhoto(fileName, maxSide)?.bitmap

@Composable
private fun PhotoImage(
    photo: LoadedPhoto?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val bitmap = photo?.bitmap
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(placeholder), contentAlignment = Alignment.Center) {
            if (photo != null) {
                Icon(Icons.Outlined.BrokenImage, stringResource(R.string.photos_missing), tint = iconTint)
            }
        }
    }
}

// --- Screen ---------------------------------------------------------------------------------

@Composable
fun PhotosScreen(state: UiState, vm: AppViewModel, contentPadding: PaddingValues) {
    val settings = state.settings
    val photos = state.photos
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var poseName by rememberSaveable { mutableStateOf(photos.lastOrNull()?.pose ?: PhotoPose.FRONT.name) }
    val pose = PhotoPose.fromName(poseName)
    val posePhotos = remember(photos, pose) { photos.filter { PhotoPose.fromName(it.pose) == pose } }
    val counts = remember(photos) { photos.groupingBy { PhotoPose.fromName(it.pose) }.eachCount() }

    // Before / after: user picks override the defaults (first and latest photo of the pose).
    var beforeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var afterId by rememberSaveable { mutableStateOf<Long?>(null) }
    val before = photos.firstOrNull { it.id == beforeId } ?: posePhotos.firstOrNull()
    val after = photos.firstOrNull { it.id == afterId } ?: posePhotos.lastOrNull()
    val comparing = before != null && after != null && before.id != after.id
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }

    var importing by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val targetPose = pose.name
        // ViewModel scope: the import must finish even if the screen is left meanwhile.
        vm.viewModelScope.launch {
            importing = true
            importFailed = false
            val result = runCatching {
                withContext(Dispatchers.IO) { PhotoStore.import(context.applicationContext, uri) }
            }
            importing = false
            result.onSuccess { fileName ->
                vm.savePhoto(PhotoEntity(timestamp = System.currentTimeMillis(), fileName = fileName, pose = targetPose))
                afterId = null // the new photo becomes the default "after"
            }.onFailure {
                if (it is CancellationException) throw it
                importFailed = true
            }
        }
    }
    val pick = {
        importFailed = false
        runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .onFailure { importFailed = true }
        Unit
    }
    val selectPose: (PhotoPose) -> Unit = {
        poseName = it.name
        beforeId = null
        afterId = null
    }

    val months = remember(posePhotos) {
        posePhotos.asReversed().groupBy { YearMonth.from(Format.toLocalDateTime(it.timestamp)) }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (photos.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyHero() }
            item(key = "pose", span = { GridItemSpan(maxLineSpan) }) { PoseSelector(pose, counts, selectPose) }
            item(key = "add", span = { GridItemSpan(maxLineSpan) }) { AddPhotoCard(pose, importing, importFailed, pick) }
        } else {
            item(key = "pose", span = { GridItemSpan(maxLineSpan) }) { PoseSelector(pose, counts, selectPose) }
            item(key = "compare", span = { GridItemSpan(maxLineSpan) }) {
                if (comparing) {
                    CompareHero(
                        before = before,
                        after = after,
                        weights = state.weights,
                        settings = settings,
                        customized = beforeId != null || afterId != null,
                        onReset = { beforeId = null; afterId = null },
                    )
                } else {
                    NeedTwoHero(posePhotos.lastOrNull())
                }
            }
            item(key = "add", span = { GridItemSpan(maxLineSpan) }) { AddPhotoCard(pose, importing, importFailed, pick) }
            item(key = "timeline", span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.photos_timeline_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        pluralStringResource(R.plurals.photos_count, posePhotos.size, posePhotos.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (posePhotos.isEmpty()) {
                item(key = "timeline-empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.photos_timeline_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            months.forEach { (month, list) ->
                item(key = "month-$month", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        monthLabel(month),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
                items(list, key = { it.id }) { photo ->
                    PhotoThumb(
                        photo = photo,
                        weight = remember(state.weights, photo.timestamp) { nearestWeight(state.weights, photo.timestamp) },
                        settings = settings,
                        badge = when {
                            !comparing -> null
                            photo.id == before?.id -> stringResource(R.string.photos_before)
                            photo.id == after?.id -> stringResource(R.string.photos_after)
                            else -> null
                        },
                        onClick = { openId = photo.id },
                    )
                }
            }
        }
        item(key = "privacy", span = { GridItemSpan(maxLineSpan) }) { PrivacyNote() }
    }

    val open = photos.firstOrNull { it.id == openId }
    if (open != null) {
        PhotoViewer(
            photo = open,
            weights = state.weights,
            settings = settings,
            isBefore = comparing && before?.id == open.id,
            isAfter = comparing && after?.id == open.id,
            onDismiss = { openId = null },
            onUseAsBefore = {
                if (after?.id == open.id) afterId = before?.id
                beforeId = open.id
            },
            onUseAsAfter = {
                if (before?.id == open.id) beforeId = after?.id
                afterId = open.id
            },
            onSave = { vm.savePhoto(it) },
            onDelete = {
                openId = null
                if (beforeId == open.id) beforeId = null
                if (afterId == open.id) afterId = null
                vm.deletePhoto(open)
            },
        )
    }
}

private fun monthLabel(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

// --- Sections -------------------------------------------------------------------------------

@Composable
private fun EmptyHero() {
    HeroCard {
        Pill(stringResource(R.string.photos_hero_badge))
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.photos_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.photos_hero_text),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.photos_hero_tip_1, R.string.photos_hero_tip_2, R.string.photos_hero_tip_3).forEachIndexed { i, tip ->
                GlassTile(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(tip), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PoseSelector(selected: PhotoPose, counts: Map<PhotoPose, Int>, onSelect: (PhotoPose) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.photos_pose_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PhotoPose.entries.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = p == selected,
                    onClick = { onSelect(p) },
                    shape = SegmentedButtonDefaults.itemShape(i, PhotoPose.entries.size),
                ) {
                    val n = counts[p] ?: 0
                    val label = stringResource(p.label)
                    Text(if (n > 0) "$label · $n" else label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AddPhotoCard(pose: PhotoPose, importing: Boolean, failed: Boolean, onPick: () -> Unit) {
    SectionCard(title = stringResource(R.string.photos_add_title), icon = Icons.Outlined.AddPhotoAlternate) {
        Text(
            stringResource(R.string.photos_add_hint, stringResource(pose.label)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onPick, enabled = !importing, modifier = Modifier.fillMaxWidth()) {
            if (importing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (importing) R.string.photos_importing else R.string.photos_choose_gallery))
        }
        if (failed) {
            Text(
                stringResource(R.string.photos_import_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NeedTwoHero(only: PhotoEntity?) {
    HeroCard {
        Text(
            stringResource(R.string.photos_compare_title).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (only != null) {
                PhotoImage(
                    rememberLoadedPhoto(only.fileName, THUMB_SIDE),
                    contentDescription = null,
                    modifier = Modifier
                        .width(72.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(16.dp)),
                    placeholder = Color.White.copy(alpha = 0.14f),
                    iconTint = Color.White,
                )
                Spacer(Modifier.width(14.dp))
            }
            Text(
                stringResource(R.string.photos_need_two),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompareHero(
    before: PhotoEntity,
    after: PhotoEntity,
    weights: List<WeightEntity>,
    settings: UserSettings,
    customized: Boolean,
    onReset: () -> Unit,
) {
    var sideBySide by rememberSaveable { mutableStateOf(false) }
    val beforeWeight = remember(weights, before.timestamp) { nearestWeight(weights, before.timestamp) }
    val afterWeight = remember(weights, after.timestamp) { nearestWeight(weights, after.timestamp) }
    val beforeImage = rememberLoadedPhoto(before.fileName, COMPARE_SIDE)
    val afterImage = rememberLoadedPhoto(after.fileName, COMPARE_SIDE)
    val days = abs(
        ChronoUnit.DAYS.between(
            Format.toLocalDateTime(before.timestamp).toLocalDate(),
            Format.toLocalDateTime(after.timestamp).toLocalDate(),
        ),
    ).toInt()
    val diff = if (beforeWeight != null && afterWeight != null) afterWeight.weightKg - beforeWeight.weightKg else null

    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.photos_compare_title).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            ModeToggle(sideBySide) { sideBySide = it }
        }
        Spacer(Modifier.height(8.dp))
        if (diff != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                val sign = when {
                    diff < 0 -> "−"
                    diff > 0 -> "+"
                    else -> ""
                }
                Text(
                    sign + weightText(abs(diff), settings),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    pluralStringResource(R.plurals.photos_in_days, days, days),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        } else {
            Text(
                pluralStringResource(R.plurals.photos_days_apart, days, days),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (sideBySide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(beforeImage to R.string.photos_before, afterImage to R.string.photos_after).forEach { (image, label) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(18.dp)),
                    ) {
                        PhotoImage(
                            image,
                            contentDescription = stringResource(label),
                            modifier = Modifier.fillMaxSize(),
                            placeholder = Color.White.copy(alpha = 0.14f),
                            iconTint = Color.White,
                        )
                        Pill(
                            stringResource(label),
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            container = Color.Black.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        } else {
            CompareSlider(beforeImage, afterImage)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhotoCaption(stringResource(R.string.photos_before), before, beforeWeight, settings, Modifier.weight(1f))
            PhotoCaption(stringResource(R.string.photos_after), after, afterWeight, settings, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.photos_compare_hint),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            if (customized) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.photos_compare_reset), color = Color.White)
                }
            }
        }
    }
}

/** Two-option "Slider / Side by side" switch styled for the hero gradient. */
@Composable
private fun ModeToggle(sideBySide: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(3.dp)
            .selectableGroup(),
    ) {
        listOf(false to R.string.photos_mode_slider, true to R.string.photos_mode_side_by_side).forEach { (value, label) ->
            val selected = value == sideBySide
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onChange(value) })
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Both photos stacked: "after" fills the frame, "before" is drawn on top and clipped to the left of a
 * draggable vertical divider.
 */
@Composable
private fun CompareSlider(before: LoadedPhoto?, after: LoadedPhoto?) {
    var fraction by remember { mutableFloatStateOf(0.5f) }
    val description = stringResource(R.string.photos_cd_compare)
    // Read in composition so the accessibility value follows the divider.
    val progress = fraction
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset -> fraction = (offset.x / size.width).coerceIn(0f, 1f) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> fraction = (offset.x / size.width).coerceIn(0f, 1f) },
                ) { change, dragAmount ->
                    change.consume()
                    fraction = (fraction + dragAmount / size.width).coerceIn(0f, 1f)
                }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { target ->
                    fraction = target.coerceIn(0f, 1f)
                    true
                }
            },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val placeholder = Color.White.copy(alpha = 0.14f)
        PhotoImage(after, null, Modifier.fillMaxSize(), placeholder = placeholder, iconTint = Color.White)
        PhotoImage(
            before,
            null,
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
                },
            placeholder = placeholder,
            iconTint = Color.White,
        )
        Box(
            Modifier
                .offset { IntOffset((widthPx * fraction - 1.dp.toPx()).roundToInt(), 0) }
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((widthPx * fraction - 20.dp.toPx()).roundToInt(), 0) }
                .size(40.dp)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
        }
        Pill(
            stringResource(R.string.photos_before),
            Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            container = Color.Black.copy(alpha = 0.35f),
        )
        Pill(
            stringResource(R.string.photos_after),
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            container = Color.Black.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun PhotoCaption(label: String, photo: PhotoEntity, weight: WeightEntity?, settings: UserSettings, modifier: Modifier) {
    GlassTile(modifier) {
        Column {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Text(
                Format.date(photo.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                weight?.let { weightText(it.weightKg, settings) } ?: stringResource(R.string.photos_no_weight),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PhotoThumb(photo: PhotoEntity, weight: WeightEntity?, settings: UserSettings, badge: String?, onClick: () -> Unit) {
    val image = rememberLoadedPhoto(photo.fileName, THUMB_SIDE)
    Column(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            PhotoImage(
                image,
                contentDescription = stringResource(
                    R.string.photos_cd_photo,
                    stringResource(PhotoPose.fromName(photo.pose).label),
                    Format.date(photo.timestamp),
                ),
                modifier = Modifier.fillMaxSize(),
            )
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            Format.shortDate(photo.timestamp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp, top = 4.dp),
        )
        Text(
            weight?.let { weightText(it.weightKg, settings) } ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.photos_privacy_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.photos_privacy_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Full-screen viewer ---------------------------------------------------------------------

@Composable
private fun PhotoViewer(
    photo: PhotoEntity,
    weights: List<WeightEntity>,
    settings: UserSettings,
    isBefore: Boolean,
    isAfter: Boolean,
    onDismiss: () -> Unit,
    onUseAsBefore: () -> Unit,
    onUseAsAfter: () -> Unit,
    onSave: (PhotoEntity) -> Unit,
    onDelete: () -> Unit,
) {
    val image = rememberLoadedPhoto(photo.fileName, FULL_SIDE)
    val weight = remember(weights, photo.timestamp) { nearestWeight(weights, photo.timestamp) }
    val pose = PhotoPose.fromName(photo.pose)
    var notes by remember(photo.id) { mutableStateOf(photo.notes) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black, contentColor = Color.White) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.photos_close), tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(Format.date(photo.timestamp), style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            listOfNotNull(stringResource(pose.label), weight?.let { weightText(it.weightKg, settings) }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = Color.White)
                    }
                }
                ZoomableImage(
                    image,
                    contentDescription = stringResource(R.string.photos_cd_photo, stringResource(pose.label), Format.date(photo.timestamp)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            CheckChip(stringResource(R.string.photos_use_as_before), isBefore, onUseAsBefore, Modifier.weight(1f))
                            CheckChip(stringResource(R.string.photos_use_as_after), isAfter, onUseAsAfter, Modifier.weight(1f))
                        }
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            PhotoPose.entries.forEachIndexed { i, p ->
                                SegmentedButton(
                                    selected = p == pose,
                                    onClick = { if (p != pose) onSave(photo.copy(pose = p.name)) },
                                    shape = SegmentedButtonDefaults.itemShape(i, PhotoPose.entries.size),
                                ) { Text(stringResource(p.label), maxLines = 1) }
                            }
                        }
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.photos_notes_label)) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { pickDate = true }) {
                                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.photos_change_date))
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = { onSave(photo.copy(notes = notes.trim())) },
                                enabled = notes.trim() != photo.notes,
                            ) { Text(stringResource(R.string.photos_save_notes)) }
                        }
                    }
                }
            }
        }

        if (confirmDelete) {
            ConfirmDialog(
                title = stringResource(R.string.photos_delete_title),
                text = stringResource(R.string.photos_delete_text),
                confirmLabel = stringResource(R.string.delete),
                onDismiss = { confirmDelete = false },
                onConfirm = {
                    confirmDelete = false
                    onDelete()
                },
            )
        }
        if (pickDate) {
            val taken = Format.toLocalDateTime(photo.timestamp)
            DatePickerDialogFor(
                initial = taken.toLocalDate(),
                onDismiss = { pickDate = false },
                onPick = { date ->
                    pickDate = false
                    onSave(photo.copy(timestamp = Format.toMillis(date.atTime(taken.toLocalTime()))))
                },
            )
        }
    }
}

@Composable
private fun CheckChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
        } else null,
        modifier = modifier,
    )
}

/** Pinch to zoom, drag to pan, double-tap to toggle zoom. */
@Composable
private fun ZoomableImage(photo: LoadedPhoto?, contentDescription: String?, modifier: Modifier = Modifier) {
    var scale by remember(photo) { mutableFloatStateOf(1f) }
    var offset by remember(photo) { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .clipToBounds()
            .pointerInput(photo) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.5f
                    offset = Offset.Zero
                })
            }
            .pointerInput(photo) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxX = size.width * (scale - 1f) / 2f
                    val maxY = size.height * (scale - 1f) / 2f
                    offset = if (scale <= 1f) Offset.Zero
                    else Offset((offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = photo?.bitmap
        when {
            photo == null -> CircularProgressIndicator(color = Color.White)
            bitmap == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.BrokenImage, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.photos_missing), color = Color.White)
            }
            else -> Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}

// --- Entry point on the Progress screen -----------------------------------------------------

/** Card linking to [PhotosScreen]: latest thumbnail, count and date of the last photo. */
@Composable
fun PhotosEntryCard(photos: List<PhotoEntity>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val latest = photos.lastOrNull()
    SectionCard(
        modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = stringResource(R.string.photos_open), role = Role.Button, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (latest != null) {
                    PhotoImage(
                        rememberLoadedPhoto(latest.fileName, THUMB_SIDE),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.photos_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (latest == null) stringResource(R.string.photos_entry_empty)
                    else pluralStringResource(R.plurals.photos_entry_count, photos.size, photos.size, Format.date(latest.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
