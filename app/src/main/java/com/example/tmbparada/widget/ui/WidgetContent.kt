package com.example.tmbparada.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import com.example.tmbparada.widget.RefreshAction

@Composable
fun WidgetContent(widgetId: Int, model: WidgetUiModel) {
    val context = LocalContext.current
    val openStopPicker = actionStartActivity(StopPickerActivity.intent(context, widgetId))
    val size = LocalSize.current
    val layout = WidgetLayout.forSize(size.width.value, size.height.value)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(layout.padding.dp),
    ) {
        when (model) {
            WidgetUiModel.NotConfigured -> NotConfiguredContent(openStopPicker)
            is WidgetUiModel.Stop -> StopContent(model, layout, openStopPicker)
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
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_choose_stop_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Text(
            text = context.getString(R.string.widget_choose_stop_body),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun StopContent(model: WidgetUiModel.Stop, layout: WidgetLayout, openStopPicker: Action) {
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
                    text = context.placeholderText(model.placeholder ?: Placeholder.TAP_TO_REFRESH),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
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
                    visible.forEach { row -> ArrivalListRow(row, isStale = model.isStale) }
                }
                WidgetLayout.Mode.GRID -> ArrivalGrid(visible, layout.columns, isStale = model.isStale)
            }
        }
    }
}

@Composable
private fun Header(model: WidgetUiModel.Stop, layout: WidgetLayout, openStopPicker: Action) {
    val context = LocalContext.current
    val stopLabel = context.getString(R.string.widget_stop_code, model.stopCode)
    val statusLabel = context.statusText(model.status)
    val headerHeight = layout.headerHeight.dp

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight().clickable(openStopPicker)) {
            Text(
                text = model.stopName ?: stopLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = if (layout.compactHeader) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            // Short widgets drop the status line; the refresh button still shows progress.
            if (!layout.compactHeader) {
                Text(
                    text = if (model.stopName != null) "$stopLabel · $statusLabel" else statusLabel,
                    style = TextStyle(
                        color = if (model.status is Status.Failed) {
                            GlanceTheme.colors.error
                        } else {
                            GlanceTheme.colors.onSurfaceVariant
                        },
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
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
            .background(GlanceTheme.colors.secondaryContainer)
            .clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(iconSize),
                color = GlanceTheme.colors.onSecondaryContainer,
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            )
        }
    }
}

/** Line badge, destination, then the following bus (dimmed) and the next bus, in fixed columns. */
@Composable
private fun ArrivalListRow(row: ArrivalRow, isStale: Boolean) {
    val context = LocalContext.current

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(WidgetLayout.ROW_HEIGHT.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(row.line, width = 44.dp, height = 24.dp, fontSize = 12)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.destination ?: context.getString(R.string.widget_destination_unknown),
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
            maxLines = 1,
        )
        Text(
            text = row.minutes.getOrNull(1)?.let { context.minutesText(it, approximate = isStale) }.orEmpty(),
            modifier = GlanceModifier.width(52.dp),
            style = followingTimeStyle(TextAlign.End),
            maxLines = 1,
        )
        Text(
            text = context.minutesText(row.minutes.first(), approximate = isStale),
            modifier = GlanceModifier.width(64.dp),
            style = nextTimeStyle(isStale, fontSize = 15, textAlign = TextAlign.End),
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
            .background(GlanceTheme.colors.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineBadge(row.line, width = 36.dp, height = 18.dp, fontSize = 11)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = row.destination ?: context.getString(R.string.widget_destination_unknown),
                style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = context.minutesText(row.minutes.first(), approximate = isStale),
                style = nextTimeStyle(isStale, fontSize = 18, textAlign = TextAlign.Start),
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

@Composable
private fun nextTimeStyle(isStale: Boolean, fontSize: Int, textAlign: TextAlign) = TextStyle(
    color = if (isStale) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.primary,
    fontSize = fontSize.sp,
    fontWeight = FontWeight.Bold,
    textAlign = textAlign,
)

@Composable
private fun followingTimeStyle(textAlign: TextAlign) = TextStyle(
    color = GlanceTheme.colors.onSurfaceVariant,
    fontSize = 12.sp,
    textAlign = textAlign,
)

