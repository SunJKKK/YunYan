# SunJK's ToolBox — 项目文档

## 项目概览

- **应用名**: SunJK's ToolBox
- **包名**: `com.sunjk.sunjktool`
- **根项目名**: SunJKTool
- **技术栈**: Android (Kotlin 2.2.10), Jetpack Compose (BOM 2026.02.01), Material3, Room, Navigation Compose, Coil, Ktor Client
- **版本**: 1.1 (versionCode 2)
- **Min SDK**: 28, **Target/Compile SDK**: 36
- **构建**: Gradle 9.3.1, AGP 9.1.1, KSP (Room compiler)

## 架构

### 分层

```
feature/   ← UI 层（Screen + ViewModel），按功能分包
domain/    ← 领域层（纯 Kotlin 模型 + Repository 接口）
data/      ← 数据层（Room Entity + DAO + Database）
```

### 包结构

```
com.sunjk.sunjktool/
├── SunJKToolApp.kt                  # Application，持有 AppContainer
├── MainActivity.kt                  # 单 Activity，enableEdgeToEdge + SunJKToolScaffold
│
├── navigation/
│   ├── Screen.kt                    # sealed class，所有路由集中管理
│   ├── TopLevelDestination.kt        # enum，底部导航 tab（首页 / 工具 / 我的）
│   └── SunJKToolNavHost.kt          # NavHost，路由 → Screen 映射
│
├── ui/
│   ├── theme/                       # Material3 主题，Monet 动态取色（Android 12+）
│   │   ├── AnimationConfig.kt         # LocalAnimationEnabled CompositionLocal
│   └── components/
│       ├── HomeSection.kt             # 首页模块容器：粗体标题 + 圆角矩形卡片
│       ├── SunJKToolScaffold.kt     # 主 Scaffold + 底部导航栏
│       ├── CommonComponents.kt      # EmptyState, LoadingIndicator, ConfirmDialog
│       ├── HeatmapComposable.kt     # 热力图：LearningHeatmap(12周方形) + CompactLearningHeatmap(6周) + HabitHeatmap(圆形)
│       ├── ExpandableFAB.kt         # 可展开 FAB（主按钮 + 二级菜单）
│       └── LogEntryCard.kt          # 学习记录卡片（瀑布流用）
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room 数据库单例 v19 (sunjk_toolbox.db)
│   │   ├── ApiPreferences.kt        # API key SharedPreferences 封装
│   │   └── dao/
│   │       ├── LogEntryDao.kt       # 日志 CRUD
│   │       ├── PomodoroRecordDao.kt # 番茄钟每日记录
│   │       ├── HabitDao.kt            # 习惯 CRUD
│   │       ├── HabitRecordDao.kt      # 习惯打卡记录
│   │       ├── ReviewNoteDao.kt       # 复盘心得 CRUD
│   │       └── ...                  # 其他 DAO
│   ├── remote/
│   │   ├── QWeatherApi.kt           # 和风天气 Ktor HTTP 客户端封装
│   │   └── QWeatherApiModels.kt     # @Serializable DTO（now / 7d / warning / indices / city）
│   └── model/
│       ├── LogEntryEntity.kt        # Room @Entity
│       ├── PomodoroRecordEntity.kt  # 番茄钟每日记录
│       └── ...                      # 其他 Entity
│
├── domain/
│   ├── model/
│   │   ├── LogEntry.kt              # 领域模型
│   │   └── WeatherModels.kt         # @Stable WeatherBundle / WeatherWarning / DayForecast
│   └── repository/
│       ├── LogRepository.kt         # 接口
│       ├── LogRepositoryImpl.kt     # Entity ↔ Domain 映射
│       ├── WeatherRepository.kt     # 天气数据接口
│       └── WeatherRepositoryImpl.kt # 并发调 5 个 API → 映射 WeatherBundle
│
├── feature/
│   ├── home/                        # 首页：可定制模块化页面 + 可展开 FAB
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   └── edit/                    # 编辑首页模块开关
│   │       ├── HomeEditScreen.kt
│   │       └── HomeEditViewModel.kt
│   ├── tools/                       # 工具 tab（工具卡片网格）
│   │   └── ToolsScreen.kt
│   ├── mine/                        # 我的 tab（占位，待开发）
│   │   └── MineScreen.kt
│   ├── learninglog/
│   │   ├── edit/                    # 新建/编辑日志
│   │   │   ├── LogEditScreen.kt
│   │   │   └── LogEditViewModel.kt
│   │   └── detail/                  # 日志详情
│   │       ├── LogDetailScreen.kt
│   │       └── LogDetailViewModel.kt
│   ├── countdown/
│   │   ├── list/                    # 倒数日列表
│   │   │   ├── CountdownListScreen.kt
│   │   │   └── CountdownListViewModel.kt
│   │   └── edit/                    # 添加/编辑倒数日
│   │       ├── CountdownEditScreen.kt
│   │       └── CountdownEditViewModel.kt
│   ├── weather/
│   │   ├── home/
│   │   │   └── WeatherHomeModule.kt # 首页天气卡片（权限→loading→error→success 四态）
│   │   └── detail/
│   │       ├── WeatherDetailScreen.kt    # 天气详情页（当前天气+预报+穿衣+预警）
│   │       └── WeatherDetailViewModel.kt
│   ├── deepseek/
│   │   ├── DeepSeekScreen.kt            # DeepSeek 额度详情页
│   │   ├── DeepSeekViewModel.kt
│   │   └── home/
│   │       └── DeepSeekHomeModule.kt     # 余额+迷你折线图
│   ├── habit/
│   │   ├── list/                       # 习惯列表 + 打卡
│   │   │   ├── HabitListScreen.kt
│   │   │   └── HabitListViewModel.kt
│   │   └── edit/                       # 新建/编辑习惯
│   │       ├── HabitEditScreen.kt
│   │       └── HabitEditViewModel.kt
│   ├── reviewnote/                     # 复盘心得
│   │   ├── list/                       # 心得列表页
│   │   │   ├── ReviewNoteListScreen.kt
│   │   │   └── ReviewNoteListViewModel.kt
│   │   ├── detail/                     # 心得详情页
│   │   │   ├── ReviewNoteDetailScreen.kt
│   │   │   └── ReviewNoteDetailViewModel.kt
│   │   └── edit/                       # 新建/编辑心得
│   │       ├── ReviewNoteEditScreen.kt
│   │       └── ReviewNoteEditViewModel.kt
│   ├── notebook/                       # 笔记本（多级目录）
│   │   ├── list/                       # 根级笔记本列表
│   │   │   ├── NotebookListScreen.kt
│   │   │   └── NotebookListViewModel.kt
│   │   ├── detail/                     # 笔记本内容（子笔记本+学习记录）
│   │   │   ├── NotebookDetailScreen.kt
│   │   │   └── NotebookDetailViewModel.kt
│   │   ├── edit/                       # 新建/编辑笔记本
│   │   │   ├── NotebookEditScreen.kt
│   │   │   └── NotebookEditViewModel.kt
│   │   └── picker/                     # 笔记本选择器弹窗
│   │       └── NotebookPickerDialog.kt
│   ├── settings/                     # 统一设置页
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── pomodoro/
│       ├── PomodoroScreen.kt
│       ├── PomodoroHistoryScreen.kt
│       ├── PomodoroService.kt
│       ├── PomodoroViewModel.kt
│       ├── PomodoroOverlayManager.kt   # 悬浮窗（开发中）
│       └── home/
│           └── PomodoroHomeModule.kt
│
├── di/
│   ├── AppContainer.kt              # 手动 DI 容器（DB → DAO → Repository）
│   └── ViewModelFactory.kt          # 参数化 ViewModel 的 Factory
│
└── util/
    ├── DateUtil.kt                  # 日期格式化（今天/昨天/MM-dd 等）
    ├── ImageUtil.kt                 # 图片存储（URI → 内部存储，删除）
    ├── LocationHelper.kt            # LocationManager 封装：权限检查 + getCurrentLocation()
    └── WeatherIconMapper.kt         # QWeather 图标 CDN URL 拼接 + 预警色映射
```

## 导航路由

| 路由                                                     | 参数                        | Screen                                                     | 底部栏 |
| -------------------------------------------------------- | --------------------------- | ---------------------------------------------------------- | ------ |
| `home`                                                 | —                          | HomeScreen（可定制模块瀑布流+FAB）                         | ✅     |
| `tools`                                                | —                          | ToolsScreen（工具网格）                                    | ✅     |
| `mine`                                                 | —                          | MineScreen（我的，占位）                                   | ✅     |
| `home/edit`                                            | —                          | HomeEditScreen（模块开关多选 + ↑↓排序）                  | ❌     |
| `learning_log/edit?logId={logId}`                      | Long? logId                 | LogEditScreen                                              | ❌     |
| `learning_log/{logId}`                                 | Long logId                  | LogDetailScreen（学习记录详情+AI总结+闪卡入口）            | ❌     |
| `learning_log/{logId}/flashcard?sessionId={sessionId}` | Long logId, Long? sessionId | FlashcardScreen（互动答题+AI解析+总结）                    | ❌     |
| `learning_log/{logId}/flashcard/hub`                   | Long logId                  | FlashcardHubScreen（生成闪卡+历史记录+错题集）             | ❌     |
| `countdown/list`                                       | —                          | CountdownListScreen（卡片列表+FAB）                        | ❌     |
| `countdown/edit?countdownId={countdownId}`             | Long? countdownId           | CountdownEditScreen                                        | ❌     |
| `weather/detail`                                       | —                          | WeatherDetailScreen（当前天气+7天预报+穿衣+预警+重新定位） | ❌     |
| `learning_record/list`                                 | —                          | TimelineListScreen（时间轴列表，按日期分组+任务计数+搜索） | ❌     |
| `pomodoro`                                             | —                          | PomodoroScreen（进度环+预设+滑块+控制按钮+统计+历史按钮）  | ❌     |
| `pomodoro/history`                                     | —                          | PomodoroHistoryScreen（每日专注时长+完成数）               | ❌     |
| `review/list`                                          | —                          | ReviewListScreen（今日复盘任务列表，点对勾完成/撤销）      | ❌     |
| `review/history`                                       | —                          | ReviewHistoryScreen（历史复盘记录，按日期倒序）            | ❌     |
| `habit/list`                                           | —                          | HabitListScreen（习惯列表+打卡+热力图）                    | ❌     |
| `habit/edit?habitId={habitId}`                         | Long? habitId               | HabitEditScreen（新建/编辑习惯+颜色选择器）                | ❌     |
| `review_note/list/{logEntryId}`                        | Long logEntryId             | ReviewNoteListScreen（心得卡片列表，按时间降序）           | ❌     |
| `review_note/detail/{logEntryId}/{noteId}`             | Long logEntryId, Long noteId | ReviewNoteDetailScreen（心得详情，Markdown渲染+图片）     | ❌     |
| `review_note/edit/{logEntryId}?noteId={noteId}`       | Long logEntryId, Long? noteId | ReviewNoteEditScreen（MD编辑+图片选取+CropScreen）       | ❌     |
| `notebook/list`                                        | —                          | NotebookListScreen（根级笔记本横向卡片列表）               | ❌     |
| `notebook/detail/{notebookId}`                         | Long notebookId             | NotebookDetailScreen（子笔记本+学习记录+面包屑）           | ❌     |
| `notebook/edit?notebookId={notebookId}&parentId={parentId}` | Long? notebookId, Long? parentId | NotebookEditScreen（名称+父笔记本选择）                | ❌     |
| `deepseek/balance`                                     | —                          | DeepSeekScreen（余额+折线图+历史）                         | ❌     |
| `settings`                                             | —                          | SettingsScreen（API 密钥+WebDAV 同步设置+同步控制）        | ❌     |
| `sync/settings`                                        | —                          | SyncSettingsScreen（旧，保留向后兼容）                     | ❌     |

底部栏仅在一级页面（Home / Tools / Mine）显示。

### 页面切换动画

统一使用 **前进/返回** 两对动画，遵循 Google 原生 app 风格：

| 方向                                   | 进入动画                 | 退出动画                 |
| -------------------------------------- | ------------------------ | ------------------------ |
| **前进**（Tab 左滑、进入子页面） | 从右侧全屏滑入 + 淡入    | 向左 1/3 屏宽微移 + 淡出 |
| **返回**（Tab 右滑、返回上级）   | 从左 1/3 屏宽微移 + 淡入 | 向右全屏滑出 + 淡出      |

**方向判断**（`tabDirection()`）：

| 场景                   | fromIdx | toIdx                 | 方向                      |
| ---------------------- | ------- | --------------------- | ------------------------- |
| Tab 切换（0→1, 1→2） | ≥0     | ≥0, toIdx ≥ fromIdx | 前进                      |
| Tab 切换（1→0, 2→1） | ≥0     | ≥0, toIdx < fromIdx  | 返回                      |
| Tab → 子页面          | ≥0     | -1                    | 前进                      |
| 子页面 → Tab（返回）  | -1      | ≥0                   | 返回                      |
| 子页面 ↔ 子页面       | -1      | -1                    | enter/exit 前进，pop 返回 |

**时长与缓动**：

| 常量                    | 值                        | 说明                                                  |
| ----------------------- | ------------------------- | ----------------------------------------------------- |
| `TRANSITION_DURATION` | 300ms                     | 所有场景统一，`internal` 共享给 Scaffold 底部栏动画 |
| 进入缓动                | `FastOutSlowInEasing`   | 减速进入，自然落定                                    |
| 退出缓动                | `FastOutLinearInEasing` | 加速退出，干净利落                                    |

**底部栏动画**：`SunJKToolScaffold.kt` 使用 `AnimatedVisibility` + `slideInVertically`/`slideOutVertically`，进入时从下方滑入，退出时向下方滑出，与内容过渡同频 300ms。

**实现位置**：

- `navigation/SunJKToolNavHost.kt` — 4 个统一动画 `forwardEnter`、`forwardExit`、`backEnter`、`backExit` + 方向辅助函数 `tabDirection()`
- `ui/components/SunJKToolScaffold.kt` — `AnimatedVisibility` 包裹 `NavigationBar`

**新增页面时的规则**：

- 底部导航新 Tab → 在 `TopLevelDestination` 枚举末尾追加，`tabDirection()` 自动处理方向
- 层级子页面 → 使用 `forwardEnter` / `forwardExit` / `backEnter` / `backExit`

## 数据模型

### LogEntryEntity (Room)

| 字段        | 类型                    | 说明                                |
| ----------- | ----------------------- | ----------------------------------- |
| id          | Long (PK, autoGenerate) | 主键                                |
| subject     | String                  | 科目（可选）                        |
| title       | String                  | 标题（必填）                        |
| timeSpent   | Int                     | 花费时间，分钟（可选）              |
| imagePaths  | String (JSON array)     | 内部存储图片路径列表，JSON 数组编码 |
| description | String                  | 用户描述                            |
| aiSummary   | String                  | AI 生成总结                         |
| notebookId  | Long?                   | 归属笔记本 ID（可为空）             |
| createdDate | Long                    | 创建时间 (epoch millis)             |
| updatedDate | Long                    | 更新时间 (epoch millis)             |

### LogEntry (Domain)

- 同上，但 `createdDate`/`updatedDate` 为 `LocalDateTime` 类型，`imagePaths` 为 `List<String>`
- **必须标注 `@Stable`**（含 java.time 类型，Compose 编译器无法推断稳定性）

### CountdownEntity (Room)

| 字段        | 类型                    | 说明                              |
| ----------- | ----------------------- | --------------------------------- |
| id          | Long (PK, autoGenerate) | 主键                              |
| title       | String                  | 倒数日标题（必填）                |
| targetDate  | Long                    | 目标日期 epoch millis（当天零点） |
| note        | String                  | 备注（可选，默认空串）            |
| createdDate | Long                    | 创建时间 epoch millis             |
| updatedDate | Long                    | 更新时间 epoch millis             |

### Countdown (Domain)

- 同上，但 `targetDate` 为 `LocalDate`，`createdDate`/`updatedDate` 为 `LocalDateTime`
- **必须标注 `@Stable`**

### HabitEntity (Room)

| 字段        | 类型                    | 说明                           |
| ----------- | ----------------------- | ------------------------------ |
| id          | Long (PK, autoGenerate) | 主键                           |
| name        | String                  | 习惯名称（必填）               |
| description | String                  | 描述（可选）                   |
| colorArgb   | Int                     | 习惯颜色（`Color.toArgb()`） |
| createdAt   | Long                    | 创建时间 epoch millis          |
| updatedAt   | Long                    | 更新时间 epoch millis          |

### HabitRecordEntity (Room)

| 字段        | 类型        | 说明                                |
| ----------- | ----------- | ----------------------------------- |
| date        | String (PK) | `"{habitId}_yyyy-MM-dd"` 复合主键 |
| habitId     | Long        | 关联习惯 ID                         |
| isCompleted | Boolean     | 当天是否已完成打卡                  |
| updatedAt   | Long        | 更新时间 epoch millis（LWW 同步）   |

### ReviewNoteEntity (Room)

| 字段                | 类型                    | 说明                                      |
| ------------------- | ----------------------- | ----------------------------------------- |
| id                  | Long (PK, autoGenerate) | 主键                                      |
| logEntryId          | Long                    | 关联学习记录 ID                           |
| content             | String                  | Markdown 内容                             |
| imagePaths          | String? (JSON array)    | 图片路径列表                              |
| sourceType          | String                  | 来源：manual / flashcard                  |
| flashcardSessionId  | Long?                   | 闪卡导出时关联的会话 ID                   |
| createdDate         | Long                    | 创建时间 epoch millis                     |
| updatedDate         | Long                    | 更新时间 epoch millis（LWW 同步）         |

### NotebookEntity (Room)

| 字段        | 类型                    | 说明                                   |
| ----------- | ----------------------- | -------------------------------------- |
| id          | Long (PK, autoGenerate) | 主键                                   |
| name        | String                  | 笔记本名称                             |
| parentId    | Long?                   | 父笔记本 ID，null = 根级               |
| sortOrder   | Int                     | 同级排序                               |
| createdDate | Long                    | 创建时间 epoch millis                  |
| updatedDate | Long                    | 更新时间 epoch millis                  |

**邻接表模型**：通过 `parentId` 自引用实现多级目录。`LogEntryEntity` 新增 `notebookId: Long?` 字段关联笔记本（可为空，删除笔记本时解绑而非级联删除）。

### HomeModuleEntity (Room)

| 字段                | 类型        | 说明                                               |
| ------------------- | ----------- | -------------------------------------------------- |
| moduleKey           | String (PK) | 模块标识：heatmap / today_logs / countdown / habit |
| enabled             | Boolean     | 是否在首页显示                                     |
| sortOrder           | Int         | 排序顺序                                           |
| selectedCountdownId | Long?       | 倒数日模块专用：用户选中的倒数日 ID                |

## 首页功能

首页可定制，用户通过 FAB → "编辑首页" 选择展示哪些模块并排序。所有模块使用 `HomeSection` 容器（粗体标题在卡片外左上角，圆角 16dp），在 `LazyVerticalStaggeredGrid` 中以**双列瀑布流**排列。

### 可用模块

| 模块 key           | 标题            | 说明                                                                                          | 默认启用 |
| ------------------ | --------------- | --------------------------------------------------------------------------------------------- | -------- |
| `heatmap`        | 学习热力图      | `CompactLearningHeatmap`：紧凑 6 周，`CELL_SIZE=14dp`，`CELL_GAP=3dp`，无月份标题和图例 | ✅       |
| `today_logs`     | 今日学习记录    | 展示当天日志列表（标题+科目），无日志显示"今天还没有学习记录"                                 | ✅       |
| `countdown_{id}` | 倒数日: {title} | 每个倒数日独立为一个模块，可单独开关和排序                                                    | ❌       |
| `habit_{id}`     | 习惯: {name}    | 打卡按钮+圆形热力图，每习惯一卡片，可单独开关和排序                                           | ❌       |
| `weather`        | 天气            | 展示当前天气、明天预报、穿衣建议与预警信息；点击跳转详情页                                    | ❌       |
| `pomodoro`       | 番茄钟          | 迷你倒计时圆环+暂停/停止按钮+今日专注时长；点击跳转番茄钟页                                   | ❌       |

### 倒数日模块

每个倒数日独立为一个模块（key=`countdown_{id}`），与热力图/天气/番茄钟同级别，可单独开关和排序。添加或删除倒数日后编辑首页自动同步。

### 习惯模块

每个习惯独立为一个模块（key=`habit_{id}`），卡片内含习惯名+颜色圆点+圆形热力图+M3 打卡按钮。添加或删除习惯后编辑首页自动同步。打卡状态由 `combine(habits flow, records flow)` 驱动，打卡后 UI 立即刷新。

### 模块开关持久化

Room 表 `home_modules`（`HomeModuleEntity`）存储各模块的 `enabled`、`sortOrder` 和 `selectedCountdownId`。`SunJKToolApp.onCreate` 首次启动时插入默认值。

### HomeSection 样式规范

- 粗体标题位于圆角矩形卡片左上角**外部**（`start=8dp, bottom=6dp`）
- 卡片为 `surfaceVariant` 背景 + `alpha=0.45` + `tonalElevation=2dp`，圆角 16dp

### 可展开 FAB

- 点击右下角 + 按钮展开二级菜单
- 菜单项：添加学习记录 / 编辑首页
- 点击遮罩层可收起菜单

### 天气模块

- **API**: 和风天气 devapi.qweather.com，Key 在 `QWeatherApi.kt` 中
- **定位**: `LocationHelper` 封装 `LocationManager.getLastKnownLocation()`，优先 GPS → Network → Passive
- **数据流**: `WeatherRepository.refresh()` → `coroutineScope { 5 个 async }` 并发调用 now/7d/warning/indices/city API → 映射为 `WeatherBundle` → emit 到 `StateFlow<WeatherResult>`
- **权限**: Composable 中 `rememberLauncherForActivityResult` 申请 `ACCESS_FINE_LOCATION`，ViewModel 不持有 Context
- **首页模块**: `WeatherHomeModule` 处理四态（无权限 / loading / error / success），展示城市+温度+图标+明天预报+穿衣+预警标记
- **详情页**: `WeatherDetailScreen` — TopAppBar 含重新定位按钮，内容区滚动展示当前天气大卡片、信息网格（风向/湿度/气压/能见度）、明日预报、7天逐日预报、穿衣建议、预警卡片（蓝/黄/橙/红色）

## 状态管理模式

每个 Screen 遵循统一模式：

```kotlin
// ViewModel
class XxxViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(XxxUiState())
    val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()
}

// Screen Composable
@Composable
fun XxxScreen(onXxx: () -> Unit, viewModel: XxxViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 处理 Loading / Empty / Error / Content 状态
}
```

**约束**：

- ViewModel 不持有 NavController、Context 引用
- Screen 接收 lambda 回调，不直接拿 NavController
- 导航副作用通过 `LaunchedEffect` 触发
- 数据流：Room Flow → Repository.map → ViewModel.collect → Compose.render
- **UiState data class 必须标注 `@Immutable`**

## 主题

- 默认启用 Monet 动态取色（`dynamicColor = true`），Android 12+ 从壁纸提取色彩
- Android 12 以下回退到紫色调色板（Color.kt）
- 支持深色模式跟随系统（`isSystemInDarkTheme()`）

### 启动器图标

- 使用 Adaptive Icon（`mipmap-anydpi/ic_launcher.xml` + `ic_launcher_round.xml`）
- 背景：`drawable/ic_launcher_background.xml` — 纯色深灰 (`#0F172A`)
- 前景：`drawable/ic_launcher_foreground.xml` — 白色图标轮廓，108dp viewport，缩放到 72dp 安全区内
- **支持 Monet 主题图标**：Android 13+ 通过 `<monochrome>` 层指向前景矢量，系统根据壁纸自动着色

## 依赖

| 库                          | 用途                                                |
| --------------------------- | --------------------------------------------------- |
| Navigation Compose          | 路由 + 底部导航                                     |
| Room + KSP                  | 本地数据库 + 编译时处理                             |
| Lifecycle ViewModel Compose | `viewModel()` + `collectAsStateWithLifecycle()` |
| Coil Compose                | 异步图片加载（含天气图标 CDN）                      |
| Material3                   | UI 组件 + Monet 取色                                |
| Ktor Client (Android)       | 和风天气 HTTP API 调用                              |
| kotlinx-serialization       | JSON 反序列化天气 DTO                               |

## 扩展规范

新增功能时按以下模式：

```
feature/{feature_name}/
├── list/           # 列表页（如有）
├── detail/         # 详情页（如有）
└── edit/           # 编辑页（如有）
```

同时更新：

1. `navigation/Screen.kt` — 新增路由
2. `navigation/SunJKToolNavHost.kt` — 注册 composable
3. `data/local/dao/` — 新增 DAO
4. `domain/repository/` — 新增 Repository
5. `di/AppContainer.kt` — 注入新依赖
6. `di/ViewModelFactory.kt` — 新增 Factory

## 计划功能

- [X] 学习记录（首期） — CRUD + 图片 + 热力图（精确对齐）+ 瀑布流 + 模块化首页布局
- [X] "我的"底部 Tab（占位）
- [X] 倒数日 — 列表卡片 + FAB + 添加/编辑
- [X] 首页可定制化 — 模块开关 + 紧凑热力图 + 今日日志模块 + FAB 菜单扩展
- [X] 天气 — 和风天气 API + Ktor Client + 首页模块 + 详情页 + 穿衣指南 + 预警 + Monet 主题图标
- [X] 番茄钟 — PomodoroManager + Foreground Service + 小米超级岛通知 + 广播控制暂停/停止 + 首页模块 + 休息循环 + 停止时询问计入专注时长
- [X] DeepSeek API 额度追踪 — 余额卡片+折线图+历史记录+首页模块
- [X] 动态问候语 — DeepSeek API 生成上下文感知建议/励志语录 + Room 缓存 + 首页刷新按钮
- [X] 复盘 — ReviewHelper 自动生成复盘任务（每日21:00/次日7:30/周日/月末）+ 时间轴列表 + 打勾撤销 + 首页模块
- [X] WebDAV 同步 — 时间戳 cursor + mutation counter + 孤儿文件清理 + 客户端侧全量表清理
- [X] AI 闪卡 — 4 种题型 + OCR 生成 + 风格选择 + 错题集 + 持久化 + 闪卡中枢管理 + 答对自动下一张 + 做完循环再做
- [X] API 密钥配置化 — DeepSeek / 和风天气 key 从硬编码改为工具页齿轮→设置页用户输入
- [X] 番茄钟历史 — 每日专注时长持久化（Room）+ 支持 WebDAV 同步 + 历史页面
- [X] 番茄钟悬浮窗 — 开发中（开关已置灰）
- [X] 习惯 — 习惯 CRUD + 首页打卡卡片（含圆形热力图）+ 打卡列表页 + 颜色选择器 + WebDAV 同步
- [X] 历史复盘 — 历史复盘记录独立页面，右上角 History 图标入口
- [X] 复盘任务同步修复 — `review_status` mutation counter bump + upload 条件改为本地 cursor 比较
- [X] 首页复盘卡片优化 — 标题改"今日复盘任务"，点标题跳转详情，仅复选框切换完成状态
- [X] 首页 FAB 简化 — 去掉二级菜单，直接打开添加学习记录；编辑首页入口移至工具页
- [X] AI 总结覆盖确认 — 已有总结时点击 AutoAwesome 弹出确认对话框
- [X] 学习记录详情性能优化 — 移除组合期间 `File.exists()` I/O，Markdown 解析缓存
- [X] 复盘心得 — MD笔记+图片附件+闪卡错题导出，列表→详情→编辑三级页面，WebDAV同步
- [X] 笔记本 — 多级目录结构（邻接表），逐级 drill-down 导航，面包屑，学习记录可选择归属笔记本，对话框式选择器（内部 drill-down + 滑动动画），后续将承载错题集
- [X] 笔记本 WebDAV 同步 — 增量式逐文件同步（notebooks/\<id\>.json），孤儿清理，LWW 冲突；SyncLogEntry 新增 notebookId 字段同步关联关系
- [X] 动画开关 — 设置页 Switch，CompositionLocal 全局注入，关闭后禁用导航过渡/底部栏/首页错开/FAB/闪卡切换等所有动画，面向墨水屏用户
- [X] 概览 — 底部栏 Tab，当月日历 + 日期选中 + 每日摘要（学习记录/专注时长/习惯打卡/复盘/生活记录）
- [X] 笔记本底部栏 — 笔记本从工具页移到底部栏（首页 | 笔记本 | 概览 | 工具），TOPLevelDestination 枚举新增 NOTEBOOK，NavHost 注册改为 tabDirection 动画
- [X] 闪卡单题正确率 — UserAnswerJson/AnswerRecord 加 totalAttempts/correctCount；recordAttempt 每次答题累加；restart 不清 DB；预览对话框显示题型+正确率+知识点+解析；答题页显示正确率
- [X] 生活记录 — 时间轴列表 + 搜索 + 编辑(内容+心情多选+图片) + 详情(Markdown渲染) + WebDAV同步；12种Material图标心情预设
- [X] 详情页 Tab 布局 — ScrollableTabRow + HorizontalPager 四Tab（AI总结/描述+图片/复盘心得/知识短板），支持左右滑动切换
- [X] 知识短板 — 闪卡加 knowledgePoint 字段；答题后按知识点分组统计正确率；AI 自动分析每题错题提炼薄弱点列表并持久化；详情短板 Tab 展示进度条+趋势图标+薄弱分析
- [X] AI总结检索增强 — 选择生成模式（标准/检索增强），三阶段 Agent（缺口分析→知识检索→总结生成）
- [X] AI总结渲染增强 — 泛化 HTML `<span>` 解析（color/background/font-size/font-weight/font-style/text-decoration + 嵌套 span 平衡闭合）；流式安全（未闭合标签不闪白）；高危标签剥离（script/iframe/…）；3 色高亮（蓝概念/黄知识点/红易错点，Material 200级）；提示词重构（课堂字幕/指示识别、夸张样式支持）
- [X] AI总结入口重构 — TopAppBar AutoAwesome 按钮删除；SummaryTab 自包含化（空态生成按钮/有内容重新生成按钮/内部对话框），与自检标签页交互一致
- [X] 闪卡检索增强 — "检索增强"风格，同样的三阶段多 Agent 流水线生成闪卡
- [X] 闪卡题型选择 — 生成对话框可选判断/单选/多选/记忆四种题型
- [X] 统一超时 — 所有 API 调用 withTimeout 改为 300s（5min）；移除 maxTokens 限制
- [X] AI 模型切换 — 设置页"AI 模型"卡片选择 deepseek-v4-flash（默认）/ deepseek-v4-pro，存于 ApiPreferences，DeepSeekApi 每次请求读取，作用于所有 DeepSeek 生成功能
- [X] PP-OCRv6 Small — 离线 OCR 引擎，ONNX Runtime 推理（det DB 后处理 + rec CTC 解码），设置页"OCR 引擎"切换 ML Kit / PP-OCR；OcrManager 统一入口，OcrEngine 接口 + MlKitOcrEngine + PpOcrEngine
- [X] 题集（Question Bank）— 底部栏 Tab（首页|笔记本|题集|概览|工具），复用 Notebook 邻接表模型（独立 question_bank_categories 表）；题目 AI 解析（结构化闪卡式：type/options/answer/explanation）；批量拆题（多题粘贴/拍照自动拆分）；展开折叠解析 + 全局切换 + 长按删除；"一键直达"设置跳过拆分确认/解析预览
- [X] AI 总结多 Agent 长输入模式 — 三阶段流水线（预处理分块→逐主题总结→整合流式），跨来源内容合并（字幕+讲义对应内容归并）；分块策略三选（按章/按节/自动）；各阶段模型独立选择并持久化；主题总结顺序/并行由设置页开关控制
- [X] AI 总结对话框合并 — 生成模式/分块策略/模型选择统一到单个 AlertDialog，UI 用 SegmentedButton 风格（与设置页一致）
- [X] AI 总结 OCR 进度 + 流式输出 — OcrManager.recognizeWithProgress 逐图回调"识别 N/M"；生成阶段 summary/integrate 相位实时渲染 MarkdownRenderer(summaryText)

## Compos 性能规范

### 稳定性注解

领域模型和 UI State 类**必须**添加 `@Stable` 或 `@Immutable` 注解，确保 Compose 编译器启用重组跳过优化：

- **领域模型**（`domain/model/`）→ `@Stable`：`LogEntry` 等含 `LocalDateTime` 字段的 data class
- **UI State**（ViewModel 中的 state data class）→ `@Immutable`：`HomeUiState`、`LogEditUiState`、`LogDetailUiState`

原因：Compose 编译器无法推断 `java.time.*` 类型的稳定性，缺少注解会导致动画期间列表全量重组。

### 组合期间禁止 I/O

Composable 函数中**禁止**在组合期间执行同步 I/O（`File.exists()`、`File.listFiles()` 等）。若需判断文件是否存在，用 `remember(key) { }` 包裹并在其中执行。

### 重计算移至后台

ViewModel 中对集合的过滤/分组/排序等操作在 `Dispatchers.Default` 中执行：

```kotlin
val result = withContext(Dispatchers.Default) { heavyComputation(data) }
```

### 计算记忆化

Composable 中的 `LocalDate.now()`、`maxOrNull()` 等计算用 `remember` 包裹，避免每次重组重新执行。

## 构建命令

```bash
./gradlew assembleDebug          # 编译 debug
./gradlew test                    # 运行单元测试
./gradlew connectedAndroidTest    # 运行设备测试
```

## 复盘功能

### 数据层

- **`ReviewStatusEntity`** — `review_status` 表：`id`/`logEntryId`/`reviewDate`/`reviewType`/`isCompleted`
- **`ReviewStatusDao`** — 按日期、按记录查询；插入/更新（REPLACE）；标记完成；按记录删除；获取待复盘日期列表
- 所有查询返回 `Flow`，由 ViewModel 响应式收集

### 复盘任务生成（`ReviewHelper.generateFor()`）

- **触发时机**：保存学习记录后即时调用（`LogEditViewModel.save()`）
- **编辑时**：先 `deleteByEntryId` 清除旧任务，再重新生成
- **删除时**：`LogDetailViewModel.deleteEntry()` 先清理复盘任务再删记录
- 每条记录生成 4 个任务：当天（daily）、次日（daily）、当周周日（weekly）、当月最后一天（monthly）
- 任务日期逻辑：`todayReviewDates()` — 始终返回今天，首页卡片展示所有 `reviewDate == 今天` 的复盘任务

### 首页复盘模块

- `HomeViewModel` 以 `reviewDao.getAll()` 驱动，每次 emit 用 `logRepository.getAllEntries().first()` 获取日志快照关联标题/科目
- 避免 `combine` 两边 Flow 不同步导致的间歇性空白（reviewDao emit 时 logRepository 可能还是旧数据）
- `HomeReviewItem` 展示数据结构：`statusId`/`title`/`subject`/`isCompleted`/`reviewType`
- 首页卡片显示待复盘数量 + 全量任务复选框（已完成项灰色区分）

### 复盘列表页（`ReviewListScreen`）

- 路由：`review/list`
- 按日期分组时间轴（竖线+圆点），每项显示科目标签+标题
- **点击卡片** → 跳转学习记录详情（`onNavigateToDetail`）
- **点击对勾图标** → 标记完成/撤销（`toggleReview`）

## 学习记录列表（TimelineListScreen）

- 按日期分组排列（`createdDate` 为准），无竖线时间轴，日期前有主题色圆点
- **搜索功能**：右上角搜索图标 → 切换为 Material 3 `SearchBar`（`FocusRequester` 自动弹键盘）
- 搜索范围：标题、科目、描述、AI 总结，不区分大小写
- `TimelineEntryCard` 含缩略图行（`ImageThumbnailRow`），`BoxWithConstraints` 计算 72dp 方形缩略图
- 超出宽度显示 `+N` 覆盖层，Coil `AsyncImage` 加载，`ContentScale.Crop`

## 图片查看器（`FullscreenImageGallery`）

- 替换旧的单图 `Dialog` 为 `HorizontalPager` 多图滑动查看器
- **左右滑动**：`HorizontalPager` + `rememberPagerState`
- **双指缩放**：自定义 `pointerInput` + `awaitEachGesture` 仅响应多指手势（不拦截单指滑动给 Pager）
- **双击缩放**：`detectTapGestures.onDoubleTap` → 1x ↔ 3x 切换
- **单指平移**（已缩放时）：`offsetX/Y += position - previousPosition`
- 缩放时 `userScrollEnabled = false` 禁用 Pager 翻页
- 切页自动重置缩放
- 底部页码指示器（多图时），底部保存按钮 + **裁剪按钮**（Crop 图标）
- 点击裁剪按钮弹出 `CropScreen`（四角拖拽手柄 + 预设比例 + 网格线）
- 裁剪后替换原图路径，保持图片顺序不变（`viewModel.replaceImagePath()`）
- 黑色全屏背景

## 番茄钟

### 状态持久化（`PomodoroManager`）

- 使用 `SharedPreferences`（`pomodoro_prefs`）保存完整 `PomodoroState`
- **保存时机**：start / pause / resume / preStop / confirmStop / stop / 计时器每30秒
- **恢复**：`init` 中读取，若 `isRunning == true` 则计算 wall-clock elapsed，重算 `remainingSecs`
  - 若剩余 > 0：恢复计时器 + 前台服务 + 广播接收器
  - 若已到期：直接触发 `onTimerFinished()`
- 保存为异步 I/O（`Dispatchers.IO`），不阻塞主线程

### 通知（弃用 HyperIsland）

- **删除** `hyperisland-kit` 依赖（`build.gradle.kts` + `libs.versions.toml`）
- **删除** `NotificationHelper` 中所有 HyperIsland 代码
- 标准通知增强：
  - `setOngoing(true)` — 不可划走
  - `addAction` — "暂停" / "结束" 快捷操作按钮
  - `setProgress` — 环形进度条
  - 频道 `IMPORTANCE_DEFAULT`（确保操作按钮可见）
- **更新频率**：通知每 30 秒更新一次（避免每秒弹出），UI 仍每秒刷新

### 广播接收器

- 程序化注册（非清单声明），`ACTION_PAUSE` / `ACTION_STOP`
- 通知操作按钮和 `PomodoroManager.controlReceiver` 共享这两个 action

### 每日专注记录

- **`PomodoroRecordEntity`** — Room 表 `pomodoro_records`：`date`(PK)、`focusSecs`、`completedCount`、`updatedDate`
- `PomodoroManager` 在专注计时完成/停止时自动 `upsertTodayRecord()`
- `PomodoroHistoryScreen` — M3 风格历史页：总汇总卡片 + 每日记录卡片列表
- 番茄钟页面右上角 `DateRange` 图标进入历史页
- 数据支持 WebDAV 同步（全量 JSON 文件 `pomodoro_records.json`）

### 悬浮窗（开发中，暂不可用）

- `PomodoroOverlayManager` — `WindowManager.addView()` + `ComposeView` 方案
- 迷你进度环 + 倒计时 + 暂停/停止按钮，可拖动
- 已知问题：`ComposeView` 主线程创建 + 权限处理待修复
- 番茄钟页面开关已置灰（`enabled = false`，"开发中，暂不可用"）

## 首页动画

### 卡片入场动画

- **冷启动交错**：`LaunchedEffect` 控制 `visibleCount` 计数器，每 60ms 释放一个 item
- **位移动画**：`Modifier.animateItem(placementSpec = tween(300ms, FastOutSlowInEasing))` — 瀑布流重排时卡片平滑移动到新位置
- **增删动画**：`fadeInSpec(300ms)` / `fadeOutSpec(250ms)` — 模块开关后淡入/淡出
- **M3 缓动**：进入/位移用 `FastOutSlowInEasing`（减速落定），退出用 `FastOutLinearInEasing`（加速消失），与导航过渡动画一致
- 问候横幅同样带 `animateItem()`

### 数据自动刷新

- `HomeViewModel.refreshAll()` — 刷新 DeepSeek + 副标题
- 天气按日缓存（`weatherLoadedDate`），当日获取后不再更新，进入天气详情页才刷新
- `LaunchedEffect` + `repeatOnLifecycle(RESUMED)` — 每次回到首页自动触发
- Room 数据（日志/模块/复盘）本身是 Flow 响应式，无需手动刷新

## 动态文案（AI 问候语）

### 生成机制

- `GreetingHelper.generateSubtitle()` — 50% 概率调用 AI 个性化建议，50% 概率返回励志名言
- `DeepSeekApi.chatCompletion()` — 调用 DeepSeek Chat API，可配置 temperature

### 上下文数据

发送给 AI 的完整上下文（`generateSuggestion()`）：

- 当前时间（日期 + 星期 + 小时）
- 天气（文本 + 温度）
- 📖 今日学习记录详情：每条记录的科目+标题+耗时（最多 5 条）
- 📝 待复盘任务：数量 + 标题列表（最多 5 条）
- 🍅 番茄钟状态：完成次数 + 专注分钟数
- 📅 最近倒数日：标题 + 剩余天数

### 关键参数

- 建议类 temperature=0.5（低随机性，确保数据准确），名言类 temperature=0.9
- 系统提示词强调"数据显示有学习记录就一定不要说还没学习"
- 默认回退：有记录 → "学习打卡完成，继续保持 💪"；无记录 → 按时间段提示

### 触发时机

- `refreshAll()` — 每次进入首页（`repeatOnLifecycle(RESUMED)`）
- 用户点击问候横幅上的刷新按钮

## LSPosed 模块 — 娱乐时长悬浮窗 ⚠️ 有 Bug

### 功能设计

- 当用户打开配置的目标应用时，屏幕左上角显示半透明 "注意娱乐时长 ⏳" 悬浮窗
- 关闭目标应用后悬浮窗自动消失
- 悬浮窗不可交互（`FLAG_NOT_TOUCHABLE`），不拦截触摸

### 架构

```
ModuleEntry (Xposed Hook, 目标应用进程)
  → contentResolver.call("show"/"hide") (跨进程 IPC)
    → OverlayProvider (ContentProvider, 本 app 进程)
      → WindowManager.addView/removeView
```

### 文件结构

- `resources/META-INF/xposed/` — 模块元数据（`module.prop`、`java_init.list`、`scope.list`）
- `xposed/ModuleEntry.kt` — LSPosed Hook 入口，Hook Activity 生命周期
- `xposed/OverlayProvider.kt` — ContentProvider 承载悬浮窗
- `xposed/TargetPackages.kt` — SharedPreferences 管理目标包名列表（默认 `tv.danmaku.bili`）

### 配置方式

- "我的" tab → 目标应用包名：添加/删除包名
- "我的" tab → 悬浮窗调试：本地测试悬浮窗

### ⚠️ 已知 Bug

**悬浮窗无法正常弹出。** 已确认：

- 模块被 LSPosed 正确加载（`handleLoadPackage` 触发）
- `contentResolver.call()` 跨进程调用正常
- `OverlayProvider.call("show")` 被执行
- 但 `WindowManager.addView()` 未成功显示悬浮窗

可能原因：

- Android 12+ 对 `TYPE_APPLICATION_OVERLAY` 的权限检查更严格
- 从 ContentProvider（非 Activity 上下文）添加 overlay 窗口可能受限
- 某些 ROM 对非前台进程的悬浮窗有额外限制

**当前状态："我的" 入口已隐藏（`TopLevelDestination.MINE` 注释掉），相关代码保留。**

### 依赖

- `compileOnly(files("libs/api-82.jar"))` — Xposed API（本地 jar）

## AI 总结学习记录

### 数据模型

- `LogEntry`/`LogEntryEntity` 新增 `description: String`（用户描述）、`aiSummary: String`（AI 生成总结）
- 数据库迁移 11→12：`ALTER TABLE ADD COLUMN`

### 生成流程

1. 详情页 TopAppBar `AutoAwesome` 按钮触发 `generateSummary()`
2. ML Kit 中文 OCR 识别所有图片文字
3. 拼接 OCR 文本 + 描述 + 标题 + 科目 → DeepSeek API（`chatCompletionStream`, temperature=0.3, maxTokens=1024）
4. **流式输出**：SSE 逐 token 累积到 `summaryText`，UI 实时显示
5. 结果保存到 `aiSummary` 字段

### 展示

- `primaryContainer` 淡色圆角卡片，标题 "AI 总结"
- **折叠/展开**：首次生成默认展开，再次访问默认折叠，`AnimatedVisibility` 动画
- **流式动画**：`animateContentSize()` 卡片随 token 增长，底部进度条闪烁
- **编辑**：点击编辑按钮 → `OutlinedTextField` → 确认保存

### Markdown 渲染（`SummaryMarkdown`）

- 多级标题 `#`~`####`、列表 `-` / `1.`、嵌套列表缩进
- **粗体** `**`、*斜体* `*`、~~删除线~~ `~~`、行内代码 `` ` ``
- 代码块 ` ``` `、块引用 `>`、水平线 `---`、链接 `[text](url)`
- **表格** `| col | col |` 带表头和数据行

### 图片处理

- **自适应显示**：`ContentScale.FillWidth` + `BoxWithConstraints`，填满宽度、高度按比例，不裁切
- **拍照裁剪**：`CropScreen` — 全屏裁剪界面，底部比例选择（自由/1:1/4:3/3:4/16:9/9:16），双指缩放拖动，M3 风格
- 裁剪确认后保存 `_crop.jpg`，替换原照片路径

## WebDAV 同步（坚果云）

### 架构

```
SyncEngine (data/sync/)
  ├── KtorWebDavClient — Ktor HTTP 客户端（PROPFIND/PUT/GET/DELETE/MKCOL）
  ├── SyncPreferencesManager — SharedPreferences 封装（凭证、光标、auto-sync 开关）
  ├── SyncModels — 同步数据模型（@Serializable）+ SyncStatus/SyncException
  ├── SyncTrigger — 全局单例，避免将 SyncEngine 注入每个 ViewModel
  └── WebDavClient interface — WebDAV 操作抽象
```

### 远程目录结构

```
/sunjk_toolbox/
├── sync_meta.json              # {deviceId, lastSyncEpochMs, entityCursors(时间戳)}
├── log_entries/<id>.json       # 学习记录（单文件增量）
├── countdowns/<id>.json        # 倒数日（单文件增量）
├── review_status.json          # 复盘状态（全量）
├── greeting_quotes.json        # 问候语（全量）
├── balance_records.json        # 余额记录（全量）
├── flashcard_sessions.json     # 闪卡会话（全量）
├── pomodoro_records.json       # 番茄钟每日记录（全量）
├── habits.json                 # 习惯定义（全量）
├── habit_records.json          # 习惯打卡记录（全量）
├── review_notes.json           # 复盘心得（全量）
├── notebooks/<id>.json         # 笔记本（单文件增量）
├── prefs/
│   ├── pomodoro_prefs.json     # 番茄钟设置（hash 比较）
│   └── overlay_targets.json    # Xposed 目标应用（hash 比较）
└── images/img_<uuid>.<ext>     # 图片文件
```

### 同步算法（时间戳 cursor + mutation counter）

**核心**：`sync_meta.json` 中 `EntityCursors` 每个字段存储每类数据的 `max(updatedDate/id, mutationCounter)`。同步时比较本地和服务端的 cursor 值决定方向。

**Upload**:

1. 计算 `localMax = maxOf(dataMax, mutationCounter)`（mutationCounter 由 `SyncTrigger.bumpEntity()` 在每次增删改时递增）
2. 若 `localMax > serverCursor` → 上传
3. **增量表**（log_entries/countdowns/notebooks）：`filter { updatedDate > serverCursor }` 逐条 PUT + 孤儿文件清理（服务端有但本地无的 ID → DELETE）
4. **全量表**（review/greeting/balance/flashcard/pomodoro/habits/habit_records/review_notes）：全量 PUT JSON
5. 图片：检测远程是否存在 → 上传缺失的
6. 更新远程 `sync_meta.json`（cursor = `maxOf(dataMax, mutationCounter)`）

**Download**:

1. GET 远程 `sync_meta.json`
2. **增量表**：PROPFIND 列目录 → 按 `getlastmodified` 下载 → LWW（`res.modified > local.updatedDate`）
3. **全量表**：若 `serverCursor > localDataMax` → GET JSON → merge（per-id upsert）→ 客户端侧清理（本地有但远程无的 → deleteById）
4. prefs JSON → hash 比较 → 写入本地 SharedPreferences
5. 下载缺失图片

**冲突**: Last-Write-Wins（比较 `updatedDate` 毫秒精度）

### 触发方式

- **手动**：设置页 → 点击"立即同步"
- **自动**（3 秒 debounce）：日志保存/删除、倒数日保存/删除、笔记本增删改、复盘勾选、番茄钟完成/停止、闪卡生成/删除、DeepSeek 余额刷新、AI 名言生成
- 自动同步默认开启（配置凭证后）

### API 密钥管理

- `ApiPreferences` — SharedPreferences 封装 DeepSeek / QWeather API key
- `DeepSeekApi` / `QWeatherApi` 构造函数接受 `ApiPreferences`，每次请求读取最新 key
- 设置页（工具页齿轮图标 → `SettingsScreen`）统一管理 API key + WebDAV
- 未配置 key 时 API 调用静默失败，UI 已有 error 处理

### WebDAV 客户端：OkHttp 实现

- 使用 OkHttp 直接支持非标准 HTTP 方法（PROPFIND、MKCOL）
- PROPFIND XML 响应用 Android 内置 `XmlPullParser` 解析
- 错误处理：401 AuthFailure / 507 QuotaExceeded / NetworkError / NotFound

### 配置存储

- SharedPreferences: `sync_prefs` — webdav_url, username, password, auto_sync, sync_meta, device_id, mutation_<entity></entity>
- SharedPreferences: `api_prefs` — deepseek_key, qweather_key
- 密码明文存储

### 核心文件清单

| 文件                                      | 说明                                                  |
| ----------------------------------------- | ----------------------------------------------------- |
| `data/sync/SyncEngine.kt`               | 同步引擎：时间戳 cursor + mutation counter + 孤儿清理 |
| `data/sync/SyncModels.kt`               | 同步数据模型（SyncLogEntry 含 notebookId, SyncNotebook 等）+ SyncStatus + EntityCursors(12 字段) |
| `data/sync/SyncPreferencesManager.kt`   | SharedPreferences + mutation counter 管理             |
| `data/sync/SyncTrigger.kt`              | 全局触发器 + bumpEntity(entity)                       |
| `data/local/ApiPreferences.kt`          | API key SharedPreferences 封装                        |
| `feature/settings/SettingsScreen.kt`    | 统一设置页（API key + WebDAV + 动画开关）             |
| `feature/settings/SettingsViewModel.kt` | 设置页 ViewModel                                      |
| `data/model/PomodoroRecordEntity.kt`    | 番茄钟每日记录 Room 实体                              |
| `data/local/dao/PomodoroRecordDao.kt`   | 每日记录 DAO                                          |

## AI 闪卡

### 数据模型

- **`FlashcardSessionEntity`** — `flashcard_sessions` 表：`id`/`logEntryId`/`cardsJson`/`answersJson`/`style`/`createdDate`
- **`Flashcard`**（Domain sealed class）：`TrueFalse`、`SingleChoice`、`MultiChoice`、`Memory`
- **`AnswerRecord`** — 存储每道题的答题结果（`isCorrect`/`userChoice`）
- 数据库迁移链：12→13（建表）、13→14（answersJson）、14→15（style）

### 生成流程

1. 用户点击 🧠 → 跳转 `FlashcardHubScreen`
2. 点击"生成新闪卡" → 弹出对话框选择风格（核心/易错/详解/混淆/拓展/自定义）+ 卡片数量（AI决定/自定义）
3. ML Kit OCR 识别图片文字 → 拼接描述/科目/标题 → DeepSeek API 非流式生成 JSON
4. 解析 JSON → 保存 session（含风格标签）→ 自动跳转 `FlashcardScreen`
5. 完成所有卡片后答案自动持久化到 `answersJson`

### 中枢页面

- 生成新闪卡入口（`ElevatedCard`）
- 历史会话列表（按 `createdDate DESC`，最旧=第1组，每个卡片带风格标签）
- 错题集（汇总所有已完成会话的错误题目，点击跳转到对应会话）
- 每个会话支持删除（确认对话框）

### 闪卡答题页（FlashcardScreen）

- 判断题：两个 `OutlinedButton`（正确/错误），选中后高亮绿色/红色
- 单选题：`Surface` 列表，点击后高亮正确答案 + 标记错误选择
- 多选题：`Checkbox` 列表，多选后点"确认答案"
- 记忆卡：正反面翻转，翻转后自评"记住了"/"没记住"
- 答后显示 AI 解析（`MarkdownRenderer`），右上角 🧠 图标切换
- 完成后自动保存答案，总结页显示环形进度 + 答题详情 + 错题回顾

### 文件清单

| 文件                                                       | 说明                                                         |
| ---------------------------------------------------------- | ------------------------------------------------------------ |
| `data/model/FlashcardSessionEntity.kt`                   | Room 实体                                                    |
| `data/model/FlashcardModels.kt`                          | JSON DTO +`UserAnswerJson`                                 |
| `data/local/dao/FlashcardSessionDao.kt`                  | DAO（支持按 entry、按 id、全量查询）                         |
| `domain/model/Flashcard.kt`                              | Domain sealed class +`FlashcardSession` + `AnswerRecord` |
| `domain/repository/FlashcardRepository.kt`               | Repository 接口                                              |
| `domain/repository/FlashcardRepositoryImpl.kt`           | JSON ↔ Domain 映射实现                                      |
| `feature/learninglog/flashcard/FlashcardScreen.kt`       | 答题页 UI（4 种题型 + 总结）                                 |
| `feature/learninglog/flashcard/FlashcardViewModel.kt`    | 答题逻辑 + 答案持久化                                        |
| `feature/learninglog/flashcard/FlashcardHubScreen.kt`    | 中枢页 UI（生成+历史+错题）                                  |
| `feature/learninglog/flashcard/FlashcardHubViewModel.kt` | 中枢逻辑（含 OCR + API 生成）                                |
| `ui/components/MarkdownRenderer.kt`                      | 共享 Markdown 渲染（从 LogDetailScreen 提取）                |

### 同步支持

- SyncEngine 已集成闪卡会话同步（全量 JSON 文件 `flashcard_sessions.json`）
- 生成/删除闪卡时自动触发 `SyncTrigger.requestAutoSync()`

### 预览功能

- 中枢页面每个会话卡片新增 👁 **预览按钮**（删除按钮左侧）
- 弹出全屏 `Dialog`，从上到下展示该组所有卡片，带正确答案标注
- 识记卡片同时显示正反面，无需交互

### 风格与数量

- 生成风格使用 `LazyRow` 横向滑动选择（核心/易错/详解/混淆/拓展）+ 自定义输入
- 卡片数量可选"由AI决定"（不设上限，力求覆盖所有知识点）或"自定义"精确数量

### 答题增强

- **答对自动下一张**：闪卡中枢页提供 Switch 开关，答对后自动跳转下一张（持久化到 `flashcard_prefs`）
- **循环再做**：做完所有卡片后总结页显示"再做一次"按钮（`OutlinedButton` + `Refresh` 图标），清除答案重新开始
- **错题回顾**：总结页错题改为完整闪卡卡片样式（`ElevatedCard` + 类型标签 + 题目 + 正确答案✓ + 错误答案✗ + 解析默认展开）

## 复盘心得

### 入口

学习记录详情页 TopAppBar 闪卡按钮右侧 `RateReview` 图标按钮 → 心得列表页。

### 数据层

- **`ReviewNoteEntity`** — `review_notes` 表：`id`/`logEntryId`/`content`/`imagePaths`/`sourceType`/`flashcardSessionId`/`createdDate`/`updatedDate`
- **`ReviewNoteDao`** — 按 logEntryId 查询（Flow，createdDate DESC）、按 id 查询、插入、删除
- **`ReviewNote`**（Domain）— `@Stable`，sourceType 为 `ReviewNoteSource` 枚举（MANUAL / FLASHCARD）
- 数据库迁移 v18→v19：`CREATE TABLE IF NOT EXISTS review_notes (...)`

### 页面结构

```
feature/reviewnote/
├── list/      # 心得列表页 — 圆角卡片 + 来源标签 + 时间 + 内容预览 + 缩略图
├── detail/    # 心得详情页 — 完整 Markdown 渲染 + 图片附件 + 编辑/删除按钮
└── edit/      # 编辑页 — MD 输入 + 相册/拍照 + CropScreen + 图片缩略图行
```

### 流程图

```
详情页 RateReview 按钮
  → ReviewNoteListScreen（心得卡片列表）
      → 点击 + → ReviewNoteEditScreen（新建心得）
      → 点击卡片 → ReviewNoteDetailScreen（查看心得）
          → 编辑按钮 → ReviewNoteEditScreen（编辑心得）
          → 删除按钮 → 确认对话框 → 删除并返回
```

### 闪卡错题导出

- 闪卡总结页（`FlashcardSummaryView`）底部新增"导出错题至复盘心得"按钮（仅在有错题时显示）
- `FlashcardViewModel.exportWrongCardsToReviewNote()` — 将错题格式化为 Markdown（题目+选项+用户答案+正确答案+解析），调用 `ReviewNoteRepository.save()` 保存，按钮变为"已导出"禁用态

### UI 复用

- `ImageUtil` / `CropScreen` / `MarkdownRenderer` / `ConfirmDialog` / `LoadingIndicator` — 全部直接复用
- 图片选取模式（`PickMultipleVisualMedia` / `PickVisualMedia` / `TakePicture` + `FileProvider`）— 从 `LogEditScreen` 复制到 `ReviewNoteEditScreen`

### 同步支持

- SyncEngine 已集成复盘心得同步（全量 JSON 文件 `review_notes.json`）
- 增删改时自动触发 `SyncTrigger.requestAutoSync()` + `bumpEntity("review_notes")`

### 文件清单

| 文件 | 说明 |
|------|------|
| `data/model/ReviewNoteEntity.kt` | Room 实体 |
| `data/local/dao/ReviewNoteDao.kt` | DAO |
| `domain/model/ReviewNoteModels.kt` | Domain 模型 + ReviewNoteSource 枚举 |
| `domain/repository/ReviewNoteRepository.kt` | 接口 |
| `domain/repository/ReviewNoteRepositoryImpl.kt` | 实现 |
| `feature/reviewnote/list/ReviewNoteListScreen.kt` | 列表页 |
| `feature/reviewnote/list/ReviewNoteListViewModel.kt` | 列表 ViewModel |
| `feature/reviewnote/detail/ReviewNoteDetailScreen.kt` | 详情页 |
| `feature/reviewnote/detail/ReviewNoteDetailViewModel.kt` | 详情 ViewModel |
| `feature/reviewnote/edit/ReviewNoteEditScreen.kt` | 编辑页 |
| `feature/reviewnote/edit/ReviewNoteEditViewModel.kt` | 编辑 ViewModel |

## 笔记本功能

### 数据模型

- **`NotebookEntity`** — `notebooks` 表：`id`/`name`/`parentId`/`sortOrder`/`createdDate`/`updatedDate`
- **`Notebook`**（Domain）— `@Stable`，邻接表模型（`parentId` 自引用）实现多级目录
- `LogEntryEntity` 新增 `notebookId: Long?` — 可选归属笔记本
- 数据库迁移 v19→v20：`CREATE TABLE notebooks` + `ALTER TABLE log_entries ADD COLUMN notebookId`

### 导航

- **drill-down 模式**：每层只显示当前笔记本的直接子节点，点击进入下一层，系统返回键逐级回退
- **面包屑**：详情页顶部 `AssistChip` 横向滚动，点击可快速跳转到任意层级
- **选择器**：`NotebookPickerDialog` — 居中卡片式 Dialog，内部 drill-down + `AnimatedContent` 滑动切换动画

### 页面结构

```
feature/notebook/
├── list/      # 根级笔记本卡片列表 + TopAppBar [+] 按钮
├── detail/    # 面包屑 + 子笔记本卡片 + 学习记录列表 + TopAppBar [+][编辑][删除]
├── edit/      # 名称输入 + 父笔记本选择 + 保存按钮
└── picker/    # NotebookPickerDialog（居中卡片式 Dialog，内部 drill-down + 滑动动画）
```

### 关键设计

- **删除安全**：删除笔记本时子节点重新挂载到被删节点的父级，学习记录 `notebookId` 设为 NULL（不解绑数据）
- **防循环**：编辑笔记本选择父节点时通过 `getDescendantIds()` 排除自身及所有后代
- **LogEdit 集成**：新建/编辑学习记录可在"花费时间"下方选择归属笔记本，弹出 NotebookPickerDialog 选择
- **详情页 [+] 按钮**：TopAppBar 右上角 "+" 点击弹出 AlertDialog，选择添加"学习记录"或"子笔记本"
- NotebookListScreen 和 NotebookDetailScreen **无 FAB**，操作入口在 TopAppBar

### 文件清单

| 文件 | 说明 |
|------|------|
| `data/model/NotebookEntity.kt` | Room 实体 |
| `data/local/dao/NotebookDao.kt` | DAO |
| `domain/model/Notebook.kt` | @Stable 领域模型 |
| `domain/repository/NotebookRepository.kt` | 接口 |
| `domain/repository/NotebookRepositoryImpl.kt` | 实现 + Entity↔Domain 映射 + getBreadcrumbs + getDescendantIds |
| `feature/notebook/list/NotebookListScreen.kt` | 根级列表页 |
| `feature/notebook/list/NotebookListViewModel.kt` | 列表 ViewModel |
| `feature/notebook/detail/NotebookDetailScreen.kt` | 详情页 |
| `feature/notebook/detail/NotebookDetailViewModel.kt` | 详情 ViewModel |
| `feature/notebook/edit/NotebookEditScreen.kt` | 编辑页 |
| `feature/notebook/edit/NotebookEditViewModel.kt` | 编辑 ViewModel |
| `feature/notebook/picker/NotebookPickerDialog.kt` | 共享选择器弹窗 |

