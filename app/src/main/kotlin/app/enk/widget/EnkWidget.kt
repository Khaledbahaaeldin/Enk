package app.enk.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.enk.DefaultAppContainer
import app.enk.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnkWidget : GlanceAppWidget() {

    companion object {
        val SMALL_BOX = DpSize(100.dp, 100.dp)
        val WIDE_BOX = DpSize(250.dp, 100.dp)

        val PaperColor = Color(0xFFF2ECDF)
        val PaperTextColor = Color(0xFF14110D)
        val PaperSubtextColor = Color(0xFF5C574F)
        val PaperButtonFill = Color(0xFFE5DECF)

        val GraphiteBg = Color(0xFF1C1A18)
        val GraphiteTextPrimary = Color(0xFFEDE8DF)
        val GraphiteTextSecondary = Color(0xFF8C867E)
        val GraphiteRail = Color(0xFF2C2825)
        val GraphiteCarriage = Color(0xFF24211E)
        val AccentOrange = Color(0xFFFF5A1F)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_BOX, WIDE_BOX))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = DefaultAppContainer(context.applicationContext)
        val model = buildWidgetModel(container.repo)

        provideContent {
            val size = androidx.glance.LocalSize.current
            val isWide = size.width >= WIDE_BOX.width

            val newNoteIntent = Intent(context, MainActivity::class.java).apply {
                action = WidgetIntentParser.ACTION_NEW_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val openNoteIntent = Intent(context, MainActivity::class.java).apply {
                if (model.noteId != null) {
                    action = WidgetIntentParser.ACTION_OPEN_NOTE
                    putExtra(WidgetIntentParser.EXTRA_NOTE_ID, model.noteId)
                } else {
                    action = WidgetIntentParser.ACTION_NEW_NOTE
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val talkBackDescription = if (model.noteId != null) {
                "${model.meta ?: ""}. ${model.excerpt}"
            } else {
                "${model.title ?: ""}. ${model.excerpt}"
            }

            if (model.noteId == null) {
                // Empty state card
                EmptyWidgetCard(
                    model = model,
                    onClick = actionStartActivity(newNoteIntent),
                    contentDesc = talkBackDescription
                )
            } else if (isWide) {
                // Wide layout (360 x 160 dp)
                WideWidgetCard(
                    model = model,
                    onNoteClick = actionStartActivity(openNoteIntent),
                    onNewNoteClick = actionStartActivity(newNoteIntent),
                    contentDesc = talkBackDescription
                )
            } else {
                // Small layout (170 x 170 dp)
                SmallWidgetCard(
                    model = model,
                    onNoteClick = actionStartActivity(openNoteIntent),
                    onNewNoteClick = actionStartActivity(newNoteIntent),
                    contentDesc = talkBackDescription
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptyWidgetCard(
    model: WidgetModel,
    onClick: androidx.glance.action.Action,
    contentDesc: String
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(26.dp)
            .background(ColorProvider(EnkWidget.PaperColor))
            .clickable(onClick)
            .semantics { contentDescription = contentDesc }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Text(
                text = model.title ?: "Nothing yet",
                style = TextStyle(
                    color = ColorProvider(EnkWidget.PaperTextColor),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = model.excerpt,
                style = TextStyle(
                    color = ColorProvider(EnkWidget.PaperSubtextColor),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun WideWidgetCard(
    model: WidgetModel,
    onNoteClick: androidx.glance.action.Action,
    onNewNoteClick: androidx.glance.action.Action,
    contentDesc: String
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(26.dp)
            .background(ColorProvider(EnkWidget.PaperColor))
            .clickable(onNoteClick)
            .semantics { contentDescription = contentDesc }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Meta header row + "New note" button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (model.meta != null) {
                Text(
                    text = model.meta,
                    style = TextStyle(
                        color = ColorProvider(EnkWidget.PaperSubtextColor),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            } else {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }

            // Paper pill "New note" button (min touch height 48 dp)
            Box(
                modifier = GlanceModifier
                    .height(48.dp)
                    .clickable(onNewNoteClick)
                    .semantics { contentDescription = "New note" }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .cornerRadius(14.dp)
                    .background(ColorProvider(EnkWidget.PaperButtonFill)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ New note",
                    style = TextStyle(
                        color = ColorProvider(EnkWidget.PaperTextColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Excerpt in system monospace (up to 2 lines)
        Text(
            text = model.excerpt,
            maxLines = 2,
            style = TextStyle(
                color = ColorProvider(EnkWidget.PaperTextColor),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}

@androidx.compose.runtime.Composable
private fun SmallWidgetCard(
    model: WidgetModel,
    onNoteClick: androidx.glance.action.Action,
    onNewNoteClick: androidx.glance.action.Action,
    contentDesc: String
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(26.dp)
            .background(ColorProvider(EnkWidget.GraphiteBg))
            .clickable(onNoteClick)
            .semantics { contentDescription = contentDesc }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Miniature rail and carriage
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rail rod
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(ColorProvider(EnkWidget.GraphiteRail))
            ) {}

            // Miniature carriage with orange index mark
            Box(
                modifier = GlanceModifier
                    .size(width = 24.dp, height = 18.dp)
                    .cornerRadius(3.dp)
                    .background(ColorProvider(EnkWidget.GraphiteCarriage)),
                contentAlignment = Alignment.Center
            ) {
                // Orange caret strip
                Box(
                    modifier = GlanceModifier
                        .width(2.dp)
                        .height(10.dp)
                        .background(ColorProvider(EnkWidget.AccentOrange))
                ) {}
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Note excerpt (truncated to 1-2 lines)
        Text(
            text = model.excerpt,
            maxLines = 2,
            style = TextStyle(
                color = ColorProvider(EnkWidget.GraphiteTextPrimary),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Small "New note" pill button (48 dp minimum touch target)
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(onNewNoteClick)
                .semantics { contentDescription = "New note" }
                .padding(vertical = 6.dp)
                .cornerRadius(18.dp)
                .background(ColorProvider(EnkWidget.PaperButtonFill)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ New",
                style = TextStyle(
                    color = ColorProvider(EnkWidget.PaperTextColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

class EnkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EnkWidget()
}

object EnkWidgetManager {
    private val gate = UpdateGate()

    fun requestUpdate(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        gate.schedule(scope) {
            EnkWidget().updateAll(context)
        }
    }
}
