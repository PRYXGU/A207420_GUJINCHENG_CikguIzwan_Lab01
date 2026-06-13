package com.example.a207420_gujincheng_cikguizwan_lab01

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

data class SharedWord(
    val word: String = "",
    val meaning: String = "",
    val author: String = "Anonymous",
    val timestamp: Long = 0L
)

@Composable
fun CommunityScreen(navController: NavController) {
    var wordInput by remember { mutableStateOf("") }
    var meaningInput by remember { mutableStateOf("") }
    var sharedWords by remember { mutableStateOf<List<SharedWord>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(true) }

    val db = Firebase.firestore

    // Real-time listener from Firestore
    DisposableEffect(Unit) {
        val listener = db.collection("community")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isConnected = false
                    statusMessage = "Could not connect to Firebase. Check google-services.json."
                    return@addSnapshotListener
                }
                isConnected = true
                sharedWords = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SharedWord::class.java)
                } ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_app),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(modifier = Modifier.height(60.dp))
            Text("Community", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Share vocabulary with everyone", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))

            if (!isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("⚠️ Firebase not configured", color = Color(0xFFFFCC00), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share form
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Share a Word", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    TextField(
                        value = wordInput,
                        onValueChange = { wordInput = it },
                        placeholder = { Text("Word / Phrase", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = meaningInput,
                        onValueChange = { meaningInput = it },
                        placeholder = { Text("Meaning / Translation", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (wordInput.isNotBlank() && meaningInput.isNotBlank()) {
                                val entry = hashMapOf(
                                    "word" to wordInput,
                                    "meaning" to meaningInput,
                                    "author" to "A207420",
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("community").add(entry)
                                    .addOnSuccessListener {
                                        statusMessage = "Shared successfully!"
                                        wordInput = ""
                                        meaningInput = ""
                                    }
                                    .addOnFailureListener { e ->
                                        statusMessage = "Share failed: ${e.message}"
                                    }
                            } else {
                                statusMessage = "Please fill in both fields."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🌐 Share to Community", color = Color.White)
                    }
                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(statusMessage, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Community Words", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("(${sharedWords.size})", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sharedWords.isEmpty()) {
                    item {
                        Text(
                            if (isConnected) "No words shared yet. Be the first!"
                            else "Connect to Firebase to see shared words.",
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                items(sharedWords) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(item.word, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.meaning, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("shared by ${item.author}", color = primaryLight, fontSize = 11.sp)
                        }
                    }
                }
            }

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Back to Home", color = Color.White)
            }
        }
    }
}
