package com.example.aichat.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.aichat.system.SystemStatsManager

class SystemStatusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statsManager = SystemStatsManager(context)
        val stats = statsManager.getStats()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤖 OpenClaw Status",
                        style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFBB86FC)), fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔋 Battery: ", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White)))
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(text = "${stats.batteryPercentage}% ${if(stats.isCharging) "⚡" else ""}", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF03DAC6))))
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧠 RAM Free: ", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White)))
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(text = "${stats.availableRamMb} MB", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF03DAC6))))
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡ CPU Usage: ", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White)))
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(text = "${String.format("%.1f", stats.cpuUsagePercent)}%", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF03DAC6))))
                    }
                }
            }
        }
    }
}
