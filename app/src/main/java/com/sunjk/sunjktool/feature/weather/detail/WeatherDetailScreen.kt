package com.sunjk.sunjktool.feature.weather.detail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.domain.model.DayForecast
import com.sunjk.sunjktool.domain.model.WeatherBundle
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.util.weatherIcon
import com.sunjk.sunjktool.util.warningLevelColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    viewModel: WeatherDetailViewModel,
    onNavigateBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        if (granted) viewModel.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(uiState.weatherDetail?.cityName ?: "天气详情")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.MyLocation, contentDescription = "重新定位")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "需要位置权限以获取天气数据",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "请授予位置权限后重试",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) {
                        Text("授予权限")
                    }
                }
            }
            return@Scaffold
        }

        when {
            uiState.isLoading && uiState.weatherDetail == null -> {
                LoadingIndicator(modifier = Modifier.padding(innerPadding))
            }
            uiState.error != null && uiState.weatherDetail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            uiState.error ?: "未知错误",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("重试")
                        }
                    }
                }
            }
            uiState.weatherDetail != null -> {
                WeatherDetailContent(
                    data = uiState.weatherDetail!!,
                    modifier = Modifier.padding(innerPadding),
                    currentWeatherModifier = sharedTransitionScope?.let { s ->
                        with(s) {
                            animatedVisibilityScope?.let { scope ->
                                Modifier.sharedBounds(rememberSharedContentState("weather_home_card"), scope)
                            } ?: Modifier
                        }
                    } ?: Modifier
                )
            }
        }
    }
}

@Composable
private fun WeatherDetailContent(data: WeatherBundle, modifier: Modifier = Modifier, currentWeatherModifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ---- Current Weather Card ----
        Card(
            modifier = currentWeatherModifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = MaterialTheme.typography.displayLarge.fontSize, fontWeight = FontWeight.Light)) {
                            append(data.currentTemp)
                        }
                        withStyle(
                            SpanStyle(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                fontWeight = FontWeight.Light,
                                baselineShift = BaselineShift.Superscript
                            )
                        ) {
                            append("°")
                        }
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = weatherIcon(data.currentIcon),
                        contentDescription = data.currentText,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.currentText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (data.feelsLike.isNotEmpty()) {
                    Text(
                        text = "体感 ${data.feelsLike}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (data.obsTime.isNotEmpty()) {
                    Text(
                        text = "${data.obsTime} 发布",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Info Grid ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WeatherInfoItem("风向", "${data.windDir} ${data.windScale}级", modifier = Modifier.weight(1f))
            WeatherInfoItem("湿度", "${data.humidity}%", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WeatherInfoItem("气压", "${data.pressure} hPa", modifier = Modifier.weight(1f))
            WeatherInfoItem("能见度", "${data.vis} km", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // ---- Tomorrow Forecast ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("明日预报", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = weatherIcon(data.tomorrowIconDay),
                        contentDescription = data.tomorrowTextDay,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(data.tomorrowTextDay, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${data.tomorrowTempMin}° ~ ${data.tomorrowTempMax}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---- 7-Day Forecast ----
        if (data.dailyForecast.size > 2) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("7天预报", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    data.dailyForecast.drop(1).forEachIndexed { idx, day ->
                        if (idx > 0) HorizontalDivider()
                        ForecastRow(day)
                    }
                }
            }
        }

        // ---- Warning Section ----
        if (data.warnings.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("预警信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            data.warnings.forEach { warning ->
                val color = warningLevelColor(warning.level)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = color, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                warning.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = color.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    warning.level,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            warning.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5
                        )
                        if (warning.pubTime.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "发布时间：${warning.pubTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ForecastRow(day: DayForecast) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.date,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(52.dp)
        )
        Icon(
            imageVector = weatherIcon(day.iconDay),
            contentDescription = day.textDay,
            modifier = Modifier.size(28.dp).padding(end = 4.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = day.textDay,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${day.tempMin}° ~ ${day.tempMax}°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
