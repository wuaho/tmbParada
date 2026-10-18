package com.example.tmbparada.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.tmbparada.R
import com.example.tmbparada.config.StopPickerActivity
import com.example.tmbparada.domain.BusLines
import com.example.tmbparada.domain.model.WidgetTheme
import com.example.tmbparada.widget.RefreshAction

@Composable
fun WidgetContent(widgetId: Int, model: WidgetUiModel) {
    val context = LocalContext.current
    val openStopPicker = actionStartActivity(StopPickerActivity.intent(context, widgetId))
    val size = LocalSize.current
    val layout = WidgetLayout.forSize(size.width.value, size.height.value)
    val theme = when (model) {
        is WidgetUiModel.Active -> model.theme
        is WidgetUiModel.Idle -> model.theme
        WidgetUiModel.NotConfigured -> WidgetTheme.SYSTEM
    }
    val palette = widgetPalette(theme)

    CompositionLocalProvider(LocalWidgetPalette provides palette) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(palette.background)
                .cornerRadius(20.dp)
                .padding(layout.padding.dp),
        ) {
            when (model) {
                WidgetUiModel.NotConfigured -> NotConfiguredContent(openStopPicker)
                is WidgetUiModel.Idle -> IdleContent(model, layout, openStopPicker)
                is WidgetUiModel.Active -> ActiveContent(model, layout, openStopPicker)
            }
        }
    }
}

@Composable
private fun NotConfiguredContent(openStopPicker: Action) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().clickable(openStopPicker),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_bus),
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp),
            colorFilter = ColorFilter.tint(LocalWidgetPalette.current.accent),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_choose_stop_title),
            style = TextStyle(
                color = LocalWidgetPalette.current.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Text(
            text = context.getString(R.string.widget_choose_stop_body),
            style = TextStyle(
                color = LocalWidgetPalette.current.secondaryText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun ActiveContent(model: WidgetUiModel.Active, layout: WidgetLayout, openStopPicker: Action) {
    val context = LocalContext.current

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(model, layout, openStopPicker)
        Spacer(GlanceModifier.height(layout.headerSpacing.dp))
        if (model.rows.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.placeholderText(model.placeholder ?: Placeholder.LOADING),
                    style = TextStyle(
                        color = LocalWidgetPalette.current.secondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                )
            }
        } else {
            val visible = model.rows.take(layout.capacity)
            when (layout.mode) {
                WidgetLayout.Mode.LIST -> Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    visible.forEach { row -> ArrivalListRow(row, layout.listColumns, isStale = model.isStale) }
                }
                WidgetLayout.Mode.GRID -> ArrivalGrid(visible, layout.columns, isStale = model.isStale)
                WidgetLayout.Mode.HERO -> HeroContent(visible, isStale = model.isStale)
            }
        }
    }
}

/**
 * Shown when no live session runs: old arrivals would only mislead, so the widget offers to
 * start a session instead. The whole body is the button; the title still changes the stop.
 */
@Composable
private fun IdleContent(model: WidgetUiModel.Idle, layout: WidgetLayout, openStopPicker: Action) {
    val context = LocalContext.current
    val palette = LocalWidgetPalette.current
    val stopLabel = context.getString(R.string.widget_stop_code, model.stopCode)
    val note = model.error?.let { context.errorText(it) }
        ?: context.getString(R.string.widget_start_hint).takeIf { !layout.compactHeader }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = GlanceModifier.fillMaxWidth().clickable(openStopPicker)) {
            Text(
                text = model.stopName ?: stopLabel,
                style = TextStyle(
                    color = palette.text,
                    fontSize = if (layout.compactHeader) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            if (!layout.compactHeader && model.stopName != null) {
                Text(text = stopLabel, style = TextStyle(color = palette.secondaryText, fontSize = 11.sp), maxLines = 1)
            }
        }
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StartButton(compact = layout.compactHeader)
            note?.let {
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = it,
                    style = TextStyle(
                        color = if (model.error != null) palette.error else palette.secondaryText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun StartButton(compact: Boolean) {
    val context = LocalContext.current
    val palette = LocalWidgetPalette.current
    val height = if (compact) 30.dp else 40.dp
    Row(
        modifier = GlanceModifier
            .height(height)
            .cornerRadius(height / 2)
            .background(palette.accent)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_play),
            contentDescription = null,
            modifier = GlanceModifier.size(if (compact) 16.dp else 18.dp),
            colorFilter = ColorFilter.tint(palette.onAccent),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = context.getString(R.string.widget_start),
            style = TextStyle(
                color = palette.onAccent,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun Header(model: WidgetUiModel.Active, layout: WidgetLayout, openStopPicker: Action) {
    val context = LocalContext.current
    val stopLabel = context.getString(R.string.widget_stop_code, model.stopCode)
    val statusLabel = context.statusText(model.status)
    val headerHeight = layout.headerHeight.dp
    val palette = LocalWidgetPalette.current

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight().clickable(openStopPicker)) {
            Text(
                text = model.stopName ?: stopLabel,
                style = TextStyle(
                    color = palette.text,
                    fontSize = if (layout.compactHeader) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            // Short widgets drop the status line; the refresh button still shows progress.
            if (!layout.compactHeader) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (model.status is Status.Live) {
                        LiveDot()
                        Spacer(GlanceModifier.width(5.dp))
                    }
                    Text(
                        text = if (model.stopName != null) "$stopLabel · $statusLabel" else statusLabel,
                        style = TextStyle(
                            color = if (model.status is Status.Failed) palette.error else palette.secondaryText,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                    )
                }
            } else if (model.status is Status.Live) {
                // Compact headers have no status line, so the live dot sits under the title.
                LiveDot()
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        RefreshButton(isRefreshing = model.isRefreshing, size = headerHeight)
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, size: Dp) {
    val context = LocalContext.current
    val iconSize = size / 2
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 2)
            .background(LocalWidgetPalette.current.chip)
            .clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(iconSize),
                color = LocalWidgetPalette.current.onChip,
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(LocalWidgetPalette.current.onChip),
            )
        }
    }
}

/**
 * Line badge, destination, then the following bus (dimmed) and the next bus, in fixed columns
 * so times line up. Narrow widgets drop the destination and then the following bus.
 */
@Composable
private fun ArrivalListRow(row: ArrivalRow, columns: WidgetLayout.ListColumns, isStale: Boolean) {
    val context = LocalContext.current
    val following = row.minutes.getOrNull(1)
        ?.takeIf { columns != WidgetLayout.ListColumns.NEXT_ONLY }
        ?.let { context.minutesText(it, approximate = isStale) }

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(WidgetLayout.ROW_HEIGHT.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(row.line, width = 44.dp, height = 24.dp, fontSize = 12)
        Spacer(GlanceModifier.width(8.dp))
        if (columns == WidgetLayout.ListColumns.FULL) {
            Text(
                text = row.destination ?: context.getString(R.string.widget_destination_unknown),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = LocalWidgetPalette.current.text, fontSize = 13.sp),
                maxLines = 1,
            )
        } else {
            Spacer(GlanceModifier.defaultWeight())
        }
        if (columns != WidgetLayout.ListColumns.NEXT_ONLY) {
            Text(
                text = following.orEmpty(),
                modifier = GlanceModifier.width(WidgetLayout.FOLLOWING_WIDTH.dp),
                style = followingTimeStyle(TextAlign.End),
                maxLines = 1,
            )
        }
        Text(
            text = context.minutesText(row.minutes.first(), approximate = isStale),
            modifier = GlanceModifier.width(WidgetLayout.NEXT_WIDTH.dp),
            style = nextTimeStyle(row.minutes.first(), isStale, fontSize = 15, textAlign = TextAlign.End),
            maxLines = 1,
        )
    }
}

@Composable
private fun ArrivalGrid(rows: List<ArrivalRow>, columns: Int, isStale: Boolean) {
    val gap = WidgetLayout.TILE_GAP.dp
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        rows.chunked(columns).forEachIndexed { index, line ->
            if (index > 0) Spacer(GlanceModifier.height(gap))
            Row(modifier = GlanceModifier.fillMaxWidth().height(WidgetLayout.TILE_HEIGHT.dp)) {
                repeat(columns) { column ->
                    if (column > 0) Spacer(GlanceModifier.width(gap))
                    val tileModifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    val row = line.getOrNull(column)
                    if (row != null) ArrivalTile(row, isStale, tileModifier) else Box(tileModifier) {}
                }
            }
        }
    }
}

/** Grid cell: badge and destination on top, next bus large with the following one dimmed. */
@Composable
private fun ArrivalTile(row: ArrivalRow, isStale: Boolean, modifier: GlanceModifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .cornerRadius(14.dp)
            .background(LocalWidgetPalette.current.chip)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineBadge(row.line, width = 36.dp, height = 18.dp, fontSize = 11)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = row.destination ?: context.getString(R.string.widget_destination_unknown),
                style = TextStyle(color = LocalWidgetPalette.current.onChip, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = context.minutesText(row.minutes.first(), approximate = isStale),
                style = nextTimeStyle(row.minutes.first(), isStale, fontSize = 18, textAlign = TextAlign.Start),
                maxLines = 1,
            )
            row.minutes.getOrNull(1)?.let { following ->
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = context.minutesText(following, approximate = isStale),
                    style = followingTimeStyle(TextAlign.Start),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LineBadge(line: String, width: Dp, height: Dp, fontSize: Int) {
    Box(
        modifier = GlanceModifier
            .width(width)
            .height(height)
            .cornerRadius(6.dp)
            .background(ColorProvider(Color(BusLines.colorFor(line)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = line,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

/** An arriving bus is highlighted, and in a smaller size so "Arriving" fits the time column. */
@Composable
private fun nextTimeStyle(minutes: Int, isStale: Boolean, fontSize: Int, textAlign: TextAlign): TextStyle {
    val palette = LocalWidgetPalette.current
    val arriving = minutes == 0 && !isStale
    return TextStyle(
        color = when {
            isStale -> palette.secondaryText
            arriving -> palette.arriving
            else -> palette.accent
        },
        fontSize = if (minutes == 0) (fontSize - 2).sp else fontSize.sp,
        fontWeight = FontWeight.Bold,
        textAlign = textAlign,
    )
}

@Composable
private fun followingTimeStyle(textAlign: TextAlign) = TextStyle(
    color = LocalWidgetPalette.current.secondaryText,
    fontSize = 12.sp,
    textAlign = textAlign,
)

@Composable
private fun LiveDot() {
    Box(
        modifier = GlanceModifier
            .size(7.dp)
            .cornerRadius(4.dp)
            .background(LocalWidgetPalette.current.live),
    ) {}
}

/**
 * Large view for small square widgets: the next bus (the favorite line first) in big type,
 * then one compact line per other route if there is room.
 */
@Composable
private fun HeroContent(rows: List<ArrivalRow>, isStale: Boolean) {
    val context = LocalContext.current
    val palette = LocalWidgetPalette.current
    val row = rows.first()
    val minutes = row.minutes.first()

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineBadge(row.line, width = 40.dp, height = 22.dp, fontSize = 12)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = row.destination ?: context.getString(R.string.widget_destination_unknown),
                style = TextStyle(color = palette.secondaryText, fontSize = 12.sp),
                maxLines = 1,
            )
        }
        if (minutes == 0) {
            Text(
                text = context.minutesText(0, approximate = isStale),
                style = nextTimeStyle(0, isStale, fontSize = 28, textAlign = TextAlign.Start),
                maxLines = 1,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isStale) "~$minutes" else minutes.toString(),
                    style = nextTimeStyle(minutes, isStale, fontSize = 36, textAlign = TextAlign.Start),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = context.getString(R.string.widget_minutes_unit),
                    modifier = GlanceModifier.padding(bottom = 7.dp),
                    style = nextTimeStyle(minutes, isStale, fontSize = 15, textAlign = TextAlign.Start),
                    maxLines = 1,
                )
            }
        }
        row.minutes.getOrNull(1)?.let { following ->
            Text(
                text = context.getString(R.string.widget_then, context.minutesText(following, approximate = isStale)),
                style = followingTimeStyle(TextAlign.Start),
                maxLines = 1,
            )
        }
        rows.drop(1).forEach { other -> HeroExtraLine(other, isStale) }
    }
}

@Composable
private fun HeroExtraLine(row: ArrivalRow, isStale: Boolean) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(WidgetLayout.HERO_EXTRA_ROW_HEIGHT.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(row.line, width = 32.dp, height = 16.dp, fontSize = 10)
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = context.minutesText(row.minutes.first(), approximate = isStale),
            style = nextTimeStyle(row.minutes.first(), isStale, fontSize = 13, textAlign = TextAlign.End),
            maxLines = 1,
        )
    }
}

