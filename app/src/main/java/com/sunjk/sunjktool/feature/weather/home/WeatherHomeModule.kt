package com.sunjk.sunjktool.feature.weather.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sunjk.sunjktool.domain.model.WeatherBundle
import com.sunjk.sunjktool.domain.model.WeatherResult
import com.sunjk.sunjktool.util.weatherIcon
import com.sunjk.sunjktool.util.warningLevelColor

@Composable
fun WeatherHomeModule(
    weatherResult: WeatherResult,
    onRefresh: () -> Unit,
    onNavigateToDetail: () -> Unit,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val context = LocalContext.current
    var hasPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted
        if (granted) onRefresh()
    }

    Surface(
        onClick = { onNavigateToDetail() },
        enabled = weatherResult is WeatherResult.Success,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        when {
            !hasPermission -> {
                WeatherPromptCard(
                    icon = { Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary) },
                    title = "需要位置权限",
                    subtitle = "授权后可查看本地天气",
                    buttonText = "授予权限",
                    onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                )
            }
            weatherResult is WeatherResult.Idle -> {
                WeatherPromptCard(
                    icon = { Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(28.dp)) },
                    title = "天气",
                    subtitle = "点击加载天气数据",
                    buttonText = "加载",
                    onClick = onRefresh
                )
            }
            weatherResult is WeatherResult.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("加载天气中…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            weatherResult is WeatherResult.Error -> {
                WeatherPromptCard(
                    icon = { Icon(Icons.Default.Error, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error) },
                    title = "获取失败",
                    subtitle = weatherResult.message,
                    buttonText = "重试",
                    onClick = onRefresh
                )
            }
            weatherResult is WeatherResult.Success -> {
                if (isLarge) WeatherLargeSuccessContent(data = weatherResult.data)
                else WeatherSuccessContent(data = weatherResult.data)
            }
        }
    }
}

@Composable
private fun WeatherPromptCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClick, contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Text(buttonText)
        }
    }
}

@Composable
private fun WeatherLargeSuccessContent(data: WeatherBundle) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        // Row 1: City + current temp + condition
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = data.cityName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = weatherIcon(data.currentIcon),
                        contentDescription = data.currentText,
                        modifier = Modifier.size(28.dp),
                        tint = primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = data.currentText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = MaterialTheme.typography.displaySmall.fontSize, fontWeight = FontWeight.Bold, color = primary)) {
                        append(data.currentTemp)
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                            baselineShift = BaselineShift.Superscript
                        )
                    ) {
                        append("°")
                    }
                }
            )
        }

        // Row 2: detail chips
        Spacer(Modifier.height(10.dp))
        val details = listOf(
            "体感" to data.feelsLike,
            "湿度" to data.humidity,
            data.windDir to "${data.windScale}级",
            "能见度" to data.vis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            details.forEach { (label, value) ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        // Row 3: 5-day forecast strip
        val forecast = data.dailyForecast.take(5)
        if (forecast.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                forecast.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = forecastDayLabel(day.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            imageVector = weatherIcon(day.iconDay),
                            contentDescription = day.textDay,
                            modifier = Modifier.size(22.dp),
                            tint = primary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${day.tempMax.removeSuffix("°")}°",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${day.tempMin.removeSuffix("°")}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Clothing index
        if (data.clothingName.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${data.clothingName}：${data.clothingText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth()
                )
            }
        }

        // Warning badge
        if (data.warnings.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val warningCount = data.warnings.size
            Surface(
                shape = MaterialTheme.shapes.small,
                color = warningLevelColor(data.warnings.first().level).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = warningLevelColor(data.warnings.first().level)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (warningCount > 1) "${data.warnings.first().title}等${warningCount}条预警" else data.warnings.first().title,
                        style = MaterialTheme.typography.labelSmall,
                        color = warningLevelColor(data.warnings.first().level)
                    )
                }
            }
        }
    }
}

/** "今天 / 明天 / 周X" label from a yyyy-MM-dd forecast date. */
private fun forecastDayLabel(date: String): String = try {
    val d = java.time.LocalDate.parse(date)
    val today = java.time.LocalDate.now()
    when (d) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.CHINESE)
    }
} catch (e: Exception) {
    date.takeLast(5)
}

@Composable
private fun WeatherSuccessContent(data: WeatherBundle) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        // Row 1: City + Current Temperature
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = data.cityName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold, color = primary)) {
                        append(data.currentTemp)
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                            baselineShift = BaselineShift.Superscript
                        )
                    ) {
                        append("°")
                    }
                }
            )
        }

        // Row 2: Weather icon + text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Icon(
                imageVector = weatherIcon(data.currentIcon),
                contentDescription = data.currentText,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = data.currentText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Row 3: Tomorrow forecast
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = weatherIcon(data.tomorrowIconDay),
                contentDescription = data.tomorrowTextDay,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = "明天 ${data.tomorrowTextDay}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${data.tomorrowTempMin}° ~ ${data.tomorrowTempMax}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Warning badge
        if (data.warnings.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            val warningCount = data.warnings.size
            Surface(
                shape = MaterialTheme.shapes.small,
                color = warningLevelColor(data.warnings.first().level).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = warningLevelColor(data.warnings.first().level)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (warningCount > 1) "${data.warnings.first().title}等${warningCount}条预警" else data.warnings.first().title,
                        style = MaterialTheme.typography.labelSmall,
                        color = warningLevelColor(data.warnings.first().level)
                    )
                }
            }
        }
    }
}
