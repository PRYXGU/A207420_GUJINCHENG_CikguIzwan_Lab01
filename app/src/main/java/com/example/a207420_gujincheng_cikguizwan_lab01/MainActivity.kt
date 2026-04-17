package com.example.a207420_gujincheng_cikguizwan_lab01 // 请确保包名符合实验要求

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 颜色变量定义区 ---
val primaryLight = Color(0xFFC73243) // 主题色

// 【关键修改】：定义一个变量控制两个卡片的半透明背景颜色
// 你可以修改这个颜色，或者调整 .copy(alpha = 0.2f) 改变透明度
val cardBackgroundColor = Color.White.copy(alpha = 0.2f)

// 定义一个变量改变所有的字体大小
val globalFontSize = 14

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = primaryLight)
            ) {
                TranssistantApp()
            }
        }
    }
}

@Composable
fun TranssistantApp() {
    var inputQuery by remember { mutableStateOf("") }
    var displayMessage by remember { mutableStateOf("Waiting for input...") }
    var showMenu by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_app),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Text("Transsistant", fontSize = (globalFontSize * 3).sp, color = Color.White)
            Text("demo", fontSize = globalFontSize.sp, color = Color.White.copy(alpha = 0.7f))

            Spacer(modifier = Modifier.height(40.dp))

            // --- 1. 输入框卡片 ---
            Card(
                // 使用上面定义的 cardBackgroundColor
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        placeholder = { Text("Enter text to translate...", color = Color.White.copy(alpha = 0.6f), fontSize = globalFontSize.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { if(inputQuery.isNotBlank()) displayMessage = "Result: $inputQuery" },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryLight.copy(alpha = 0.6f)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Translate", color = Color.White, fontSize = globalFontSize.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 2. 结果展示卡片 (任务 3: 动画) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                // 同样使用 cardBackgroundColor 变量，保持视觉统一
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = displayMessage,
                        color = Color.White,
                        fontSize = (globalFontSize + 4).sp
                    )

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HISTORY.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = globalFontSize.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 底部图标和导航栏保持不变...
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = (globalFontSize * 2).sp, color = Color.White)
                    Text("PHOTO", color = Color.White, fontSize = (globalFontSize - 2).sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = (globalFontSize * 2).sp, color = Color.White)
                    Text("CHAT", color = Color.White, fontSize = (globalFontSize - 2).sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📄", fontSize = (globalFontSize * 2).sp, color = Color.White)
                    Text("DOCUMENT", color = Color.White, fontSize = (globalFontSize - 2).sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📓", fontSize = (globalFontSize * 2).sp, color = Color.White)
                    Text("WORDS", color = Color.White, fontSize = (globalFontSize - 2).sp)
                }

                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showMenu = true }
                    ) {
                        Text("➕", fontSize = (globalFontSize * 2).sp, color = Color.White)
                        Text("MORE", color = Color.White, fontSize = (globalFontSize - 2).sp)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.9f))
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚙️ Settings", fontSize = globalFontSize.sp) },
                            onClick = { showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("⏳ History", fontSize = globalFontSize.sp) },
                            onClick = { showMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("Home", color = Color.White, fontSize = globalFontSize.sp)
                Text("Translate", color = Color.White, fontSize = globalFontSize.sp)
                Text("Study", color = Color.White, fontSize = globalFontSize.sp)
                Text("Me", color = Color.White, fontSize = globalFontSize.sp)
            }
        }
    }
}