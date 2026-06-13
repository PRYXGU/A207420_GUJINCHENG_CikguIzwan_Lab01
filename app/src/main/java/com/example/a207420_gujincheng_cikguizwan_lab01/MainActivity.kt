package com.example.a207420_gujincheng_cikguizwan_lab01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

// 【技术点 1：数据模型】
// 定义一个数据类，用于在不同界面之间传递和存储翻译或单词的信息
@Entity(tableName = "history")
data class TranslationData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val original: String,
    val result: String,
    val type: String = "Translation"
)

// 【技术点 2：共享状态管理 - ViewModel】
// 按照 Tutorial 要求，ViewModel 调用 Repository 的方法，并以 StateFlow 形式向 UI 暴露数据。
// 数据来自 Room 数据库，实现持久化（关闭 App 后数据依然存在）。
class TranssistantViewModel(private val repository: TranslationRepository) : ViewModel() {
    // Exposes StateFlow to UI：把 Repository 的 Flow 转换成 StateFlow。
    // 数据库一变化，StateFlow 就会发新值，所有观察它的 UI 自动刷新。
    val historyList: StateFlow<List<TranslationData>> =
        repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // 提供一个公共方法，让不同页面（如翻译页、生词页）通过 Repository 向数据库添加数据
    fun addHistory(q: String, r: String, type: String = "Translation") {
        if (q.isNotBlank()) {
            // insert 是 suspend 方法，Room 会自动在后台线程执行
            viewModelScope.launch {
                repository.insert(TranslationData(original = q, result = r, type = type))
            }
        }
    }

    // 删除指定记录
    fun deleteItem(item: TranslationData) {
        viewModelScope.launch {
            repository.deleteById(item.id)
        }
    }

    // ViewModel 工厂，用于注入 Repository
    class Factory(private val repository: TranslationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TranssistantViewModel::class.java)) {
                return TranssistantViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// UI 样式定义：主色调和全局字体大小
val primaryLight = Color(0xFFC73243)
val cardBackgroundColor = Color.White.copy(alpha = 0.2f)
val globalFontSize = 14

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 应用自定义主题颜色
            MaterialTheme(colorScheme = lightColorScheme(primary = primaryLight)) {
                MainNavigation()
            }
        }
    }
}

// 【技术点 3：导航系统 - Navigation】
// 这里管理着至少 5 个界面，符合 Project 1 关于多页面流转（Multi-screen flow）的要求
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    // 在此处初始化一次 ViewModel，并将其注入到下方所有子页面中，实现“单源数据”
    // ViewModel 通过 Repository 访问数据（UI -> ViewModel -> Repository -> DAO -> Room）
    val context = LocalContext.current.applicationContext as AppApplication
    val vm: TranssistantViewModel = viewModel(
        factory = TranssistantViewModel.Factory(context.repository)
    )

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, vm) }
        composable("history") { HistoryScreen(navController, vm) }
        composable("me") { MeScreen(navController, vm) }
        composable("add_word") { AddWordScreen(navController, vm) }
        composable("check_in") { CheckInScreen(navController, vm) }
        composable("photo_translate") { PhotoTranslateScreen(navController, vm) }
        composable("community") { CommunityScreen(navController) }
    }
}

// --- 界面 1：主页（核心翻译与导航入口） ---
@Composable
fun HomeScreen(navController: NavController, viewModel: TranssistantViewModel) {
    var inputQuery by remember { mutableStateOf("") }
    var displayMessage by remember { mutableStateOf("Waiting for input...") }
    var isTranslating by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片展示
        Image(painter = painterResource(id = R.drawable.bg_app), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(80.dp))
            Text("Transsistant", fontSize = (globalFontSize * 3).sp, color = Color.White)
            Text("Learning Assistant", fontSize = globalFontSize.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(40.dp))

            // 【功能展示：文本输入】使用 Card 和 TextField 构建输入区域
            Card(colors = CardDefaults.cardColors(containerColor = cardBackgroundColor), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(value = inputQuery, onValueChange = { inputQuery = it }, placeholder = { Text("Enter text to translate...", color = Color.White.copy(alpha = 0.6f)) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (inputQuery.isNotBlank() && !isTranslating) {
                                isTranslating = true
                                displayMessage = "Translating..."
                                scope.launch(Dispatchers.IO) {
                                    val result = try {
                                        RetrofitClient.api.translate(inputQuery).responseData.translatedText
                                    } catch (e: Exception) {
                                        "Translation failed: ${e.message}"
                                    }
                                    withContext(Dispatchers.Main) {
                                        displayMessage = result
                                        viewModel.addHistory(inputQuery, result, "Translation")
                                        isTranslating = false
                                    }
                                }
                            }
                        },
                        enabled = !isTranslating,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryLight.copy(alpha = 0.6f)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isTranslating) "..." else "Translate", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            // 展示处理后的结果
            Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)) {
                Text(text = displayMessage, color = Color.White, modifier = Modifier.padding(20.dp), fontSize = (globalFontSize + 4).sp)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 快捷功能图标区域：使用自定义组件 FeatureIcon 实现代码复用
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FeatureIcon("📷", "PHOTO") { navController.navigate("photo_translate") }
                FeatureIcon("📝", "ADD WORD") { navController.navigate("add_word") }
                FeatureIcon("📅", "SIGN IN") { navController.navigate("check_in") }
                FeatureIcon("📓", "WORDS") { navController.navigate("community") }

                // 下拉菜单展示：导航到历史页面
                Box {
                    FeatureIcon("➕", "MORE") { showMenu = true }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = {
                                showMenu = false
                                navController.navigate("history")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部导航栏模拟
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("Home", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Translate", color = Color.White)
                Text("Study", color = Color.White)
                Text("Me", color = Color.White, modifier = Modifier.clickable { navController.navigate("me") })
            }
        }
    }
}

// --- 界面 2：历史记录（数据展示） ---
@Composable
fun HistoryScreen(navController: NavController, viewModel: TranssistantViewModel) {
    // 用 collectAsState 订阅 ViewModel 暴露的 StateFlow，数据变化时自动重组
    val historyList by viewModel.historyList.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_app), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(modifier = Modifier.height(80.dp))
            Text("History & Vocabulary", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Personal Learning List", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(20.dp))

            // 【技术点 4：列表展示】使用 LazyColumn 渲染 ViewModel 中的 historyList
            // 这证明了数据已经在不同页面之间成功共享
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(historyList) { item ->
                    var shareStatus by remember { mutableStateOf("") }
                    Card(colors = CardDefaults.cardColors(containerColor = cardBackgroundColor), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(15.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.original, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(item.result, color = Color.White.copy(alpha = 0.8f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.type, color = primaryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.clickable { viewModel.deleteItem(item) })
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        val entry = hashMapOf(
                                            "word" to item.original,
                                            "meaning" to item.result,
                                            "author" to "A207420",
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        Firebase.firestore.collection("community").add(entry)
                                            .addOnSuccessListener { shareStatus = "Shared!" }
                                            .addOnFailureListener { shareStatus = "Failed" }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("🌐 Share", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                                if (shareStatus.isNotBlank()) {
                                    Text(shareStatus, color = primaryLight, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryLight)) {
                Text("Back to Home", color = Color.White)
            }
        }
    }
}

// --- 界面 3：个人信息（身份验证） ---
@Composable
fun MeScreen(navController: NavController, viewModel: TranssistantViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_app), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(modifier = Modifier.height(100.dp))
            Text("Profile", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(40.dp))
            // 展示作者信息，确保作业归属明确
            Card(colors = CardDefaults.cardColors(containerColor = cardBackgroundColor), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(25.dp)) {
                    Text("NAME: GU JINCHENG", color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("MATRIC NO: A207420", color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("MORE SETTINGS", color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("CHANGE PASSWORD", color = Color.White, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryLight)) {
                Text("Back to Home", color = Color.White)
            }
        }
    }
}

// --- 界面 4：添加单词（表单输入 Form Input） ---
@Composable
fun AddWordScreen(navController: NavController, viewModel: TranssistantViewModel) {
    var wordInput by remember { mutableStateOf("") }
    var meaningInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_app), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(modifier = Modifier.height(80.dp))
            Text("Add New Word", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Build your own dictionary", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(30.dp))

            // 【技术点 5：表单处理】处理多字段输入并保存到全局状态
            Card(colors = CardDefaults.cardColors(containerColor = cardBackgroundColor), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TextField(value = wordInput, onValueChange = { wordInput = it }, placeholder = { Text("Enter Word", color = Color.White.copy(alpha = 0.6f)) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(value = meaningInput, onValueChange = { meaningInput = it }, placeholder = { Text("Enter Meaning", color = Color.White.copy(alpha = 0.6f)) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (wordInput.isNotBlank() && meaningInput.isNotBlank()) {
                                // 保存数据至全局 ViewModel
                                viewModel.addHistory(wordInput, meaningInput, "Word")
                                message = "Successfully added!"
                                wordInput = ""
                                meaningInput = ""
                            } else {
                                message = "Please fill in all fields."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Word", color = Color.White)
                    }
                }
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(message, color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                Text("Back to Home", color = Color.White)
            }
        }
    }
}

// --- 界面 5：每日签到（逻辑处理 Processing） ---
@Composable
fun CheckInScreen(navController: NavController, viewModel: TranssistantViewModel) {
    var checkInDays by remember { mutableStateOf(0) }
    var hasCheckedIn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_app), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(80.dp))
            Text("Study Check-in", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Keep learning every day", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(50.dp))

            // 【技术点 6：逻辑计算】实现签到逻辑计数，并同步状态
            Card(colors = CardDefaults.cardColors(containerColor = cardBackgroundColor), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Streak", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("$checkInDays Days", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (!hasCheckedIn) {
                                checkInDays += 1
                                hasCheckedIn = true
                                // 将逻辑处理的结果（签到成功）反馈到 ViewModel 的历史列表中
                                viewModel.addHistory("Day $checkInDays", "Attendance recorded", "Check-In")
                            }
                        },
                        enabled = !hasCheckedIn,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasCheckedIn) "Checked In" else "Record Attendance", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                Text("Back to Home", color = Color.White)
            }
        }
    }
}

// 通用组件：特征图标按钮，用于代码复用和整洁
@Composable
fun FeatureIcon(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(icon, fontSize = (globalFontSize * 2).sp)
        Text(label, color = Color.White, fontSize = (globalFontSize - 2).sp)
    }
}