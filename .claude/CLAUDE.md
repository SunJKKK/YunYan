# SunJK's ToolBox — 项目文档

## 项目概览

- **应用名**: SunJK's ToolBox
- **包名**: `com.sunjk.sunjktool`
- **根项目名**: SunJKTool
- **技术栈**: Android (Kotlin 2.2.10), Jetpack Compose (BOM 2026.02.01), Material3, Room, Navigation Compose, Coil
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
│   └── components/
│       ├── HomeSection.kt             # 首页模块容器：粗体标题 + 圆角矩形卡片
│       ├── SunJKToolScaffold.kt     # 主 Scaffold + 底部导航栏
│       ├── CommonComponents.kt      # EmptyState, LoadingIndicator, ConfirmDialog
│       ├── HeatmapComposable.kt     # 学习热力图（12周，网格精确对齐）
│       ├── ExpandableFAB.kt         # 可展开 FAB（主按钮 + 二级菜单）
│       └── LogEntryCard.kt          # 学习日志卡片（瀑布流用）
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room 数据库单例 (sunjk_toolbox.db)
│   │   └── dao/
│   │       └── LogEntryDao.kt       # 日志 CRUD，返回 Flow（响应式）
│   └── model/
│       └── LogEntryEntity.kt        # Room @Entity
│
├── domain/
│   ├── model/
│   │   └── LogEntry.kt              # 领域模型
│   └── repository/
│       ├── LogRepository.kt         # 接口
│       └── LogRepositoryImpl.kt     # Entity ↔ Domain 映射
│
├── feature/
│   ├── home/                        # 首页：热力图 + 瀑布流 + 可展开 FAB
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── tools/                       # 工具 tab（工具卡片网格）
│   │   └── ToolsScreen.kt
│   ├── mine/                        # 我的 tab（占位，待开发）
│   │   └── MineScreen.kt
│   └── learninglog/
│       ├── edit/                    # 新建/编辑日志
│       │   ├── LogEditScreen.kt
│       │   └── LogEditViewModel.kt
│       └── detail/                  # 日志详情
│           ├── LogDetailScreen.kt
│           └── LogDetailViewModel.kt
│   └── countdown/
│       ├── list/                    # 倒数日列表
│       │   ├── CountdownListScreen.kt
│       │   └── CountdownListViewModel.kt
│       └── edit/                    # 添加/编辑倒数日
│           ├── CountdownEditScreen.kt
│           └── CountdownEditViewModel.kt
│
├── di/
│   ├── AppContainer.kt              # 手动 DI 容器（DB → DAO → Repository）
│   └── ViewModelFactory.kt          # 参数化 ViewModel 的 Factory
│
└── util/
    ├── DateUtil.kt                  # 日期格式化（今天/昨天/MM-dd 等）
    └── ImageUtil.kt                 # 图片存储（URI → 内部存储，删除）
```

## 导航路由

| 路由 | 参数 | Screen | 底部栏 |
|------|------|--------|--------|
| `home` | — | HomeScreen（热力图+瀑布流+FAB） | ✅ |
| `tools` | — | ToolsScreen（工具网格） | ✅ |
| `mine` | — | MineScreen（我的，占位） | ✅ |
| `learning_log/edit?logId={logId}` | Long? logId | LogEditScreen | ❌ |
| `learning_log/{logId}` | Long logId | LogDetailScreen | ❌ |
| `countdown/list` | — | CountdownListScreen（卡片列表+FAB） | ❌ |
| `countdown/edit?countdownId={countdownId}` | Long? countdownId | CountdownEditScreen | ❌ |

底部栏仅在一级页面（Home / Tools / Mine）显示。

### 页面切换动画

统一使用 **前进/返回** 两对动画，遵循 Google 原生 app 风格：

| 方向 | 进入动画 | 退出动画 |
|------|---------|---------|
| **前进**（Tab 左滑、进入子页面） | 从右侧全屏滑入 + 淡入 | 向左 1/3 屏宽微移 + 淡出 |
| **返回**（Tab 右滑、返回上级） | 从左 1/3 屏宽微移 + 淡入 | 向右全屏滑出 + 淡出 |

**方向判断**（`tabDirection()`）：

| 场景 | fromIdx | toIdx | 方向 |
|------|---------|-------|------|
| Tab 切换（0→1, 1→2） | ≥0 | ≥0, toIdx ≥ fromIdx | 前进 |
| Tab 切换（1→0, 2→1） | ≥0 | ≥0, toIdx < fromIdx | 返回 |
| Tab → 子页面 | ≥0 | -1 | 前进 |
| 子页面 → Tab（返回） | -1 | ≥0 | 返回 |
| 子页面 ↔ 子页面 | -1 | -1 | enter/exit 前进，pop 返回 |

**时长与缓动**：

| 常量 | 值 | 说明 |
|------|-----|------|
| `TRANSITION_DURATION` | 300ms | 所有场景统一，`internal` 共享给 Scaffold 底部栏动画 |
| 进入缓动 | `FastOutSlowInEasing` | 减速进入，自然落定 |
| 退出缓动 | `FastOutLinearInEasing` | 加速退出，干净利落 |

**底部栏动画**：`SunJKToolScaffold.kt` 使用 `AnimatedVisibility` + `slideInVertically`/`slideOutVertically`，进入时从下方滑入，退出时向下方滑出，与内容过渡同频 300ms。

**实现位置**：
- `navigation/SunJKToolNavHost.kt` — 4 个统一动画 `forwardEnter`、`forwardExit`、`backEnter`、`backExit` + 方向辅助函数 `tabDirection()`
- `ui/components/SunJKToolScaffold.kt` — `AnimatedVisibility` 包裹 `NavigationBar`

**新增页面时的规则**：
- 底部导航新 Tab → 在 `TopLevelDestination` 枚举末尾追加，`tabDirection()` 自动处理方向
- 层级子页面 → 使用 `forwardEnter` / `forwardExit` / `backEnter` / `backExit`

## 数据模型

### LogEntryEntity (Room)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, autoGenerate) | 主键 |
| subject | String | 科目（可选） |
| title | String | 标题（必填） |
| timeSpent | Int | 花费时间，分钟（可选） |
| imagePath | String? | 内部存储图片路径（可选） |
| createdDate | Long | 创建时间 (epoch millis) |
| updatedDate | Long | 更新时间 (epoch millis) |

### LogEntry (Domain)
- 同上，但 `createdDate`/`updatedDate` 为 `LocalDateTime` 类型
- **必须标注 `@Stable`**（含 java.time 类型，Compose 编译器无法推断稳定性）

### CountdownEntity (Room)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, autoGenerate) | 主键 |
| title | String | 倒数日标题（必填） |
| targetDate | Long | 目标日期 epoch millis（当天零点） |
| note | String | 备注（可选，默认空串） |
| createdDate | Long | 创建时间 epoch millis |
| updatedDate | Long | 更新时间 epoch millis |

### Countdown (Domain)
- 同上，但 `targetDate` 为 `LocalDate`，`createdDate`/`updatedDate` 为 `LocalDateTime`
- **必须标注 `@Stable`**

## 首页功能

首页通过 `LazyVerticalStaggeredGrid` 实现，热力图等模块以 `StaggeredGridItemSpan.FullLine` 全宽显示，日志条目以双列瀑布流排列。

### 模块化布局规范

所有首页模块使用统一的 `HomeSection` 容器：

```kotlin
@Composable
fun HomeSection(title: String, modifier: Modifier, content: @Composable () -> Unit)
```

**样式规范**：
- 粗体标题位于圆角矩形卡片左上角**外部**（`start=8dp, bottom=6dp`）
- 卡片为 `surfaceVariant` 背景 + `alpha=0.45` + `tonalElevation=2dp`，圆角 16dp
- 所有 HomeSection 在 `LazyVerticalStaggeredGrid` 中边距对齐（统一 `contentPadding=16dp`）

**新增首页模块时**，在 `LazyVerticalStaggeredGrid` 中添加 `item(span = FullLine)`：

```kotlin
item(span = StaggeredGridItemSpan.FullLine) {
    HomeSection(title = "模块标题") {
        // 模块内容
    }
}
```

### 热力图
- 位于首页顶部，展示最近 **12 周**的学习活跃度
- **7 行**（周一至周日）+ 周列，单元格尺寸固定 `14dp`，间距 `3dp`
- 顶部月份标签通过 `Arrangement.spacedBy` 与下方网格精确对齐
- 左侧显示中文星期标签（一~日）
- 颜色：零条目显示 `surfaceVariant`，有数据按 `primary` 透明度梯度（12%~100%）
- 底部图例：少 → 多

### 学习日志瀑布流
- 使用 `LazyVerticalStaggeredGrid`（双列 `StaggeredGridCells.Fixed(2)`）
- 每张卡片显示：科目标签、图片缩略图（如有）、标题、花费时间、日期
- 按创建日期降序排列

### 可展开 FAB
- 点击右下角 + 按钮展开二级菜单
- 目前有"添加学习日志"选项
- 点击遮罩层可收起菜单
- 未来可扩展更多选项（添加倒数日、添加番茄钟等）

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

## 依赖

| 库 | 用途 |
|----|------|
| Navigation Compose | 路由 + 底部导航 |
| Room + KSP | 本地数据库 + 编译时处理 |
| Lifecycle ViewModel Compose | `viewModel()` + `collectAsStateWithLifecycle()` |
| Coil Compose | 异步图片加载 |
| Material3 | UI 组件 + Monet 取色 |

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

- [x] 学习日志（首期） — CRUD + 图片 + 热力图（精确对齐）+ 瀑布流 + 模块化首页布局
- [x] "我的"底部 Tab（占位）
- [x] 倒数日 — 列表卡片 + FAB + 添加/编辑
- [ ] 番茄钟
- [ ] WebDAV 同步
- [ ] FAB 菜单扩展（添加其他类型记录）

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
