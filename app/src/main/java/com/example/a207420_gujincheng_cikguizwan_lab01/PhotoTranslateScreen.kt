package com.example.a207420_gujincheng_cikguizwan_lab01

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoTranslateScreen(navController: NavController, viewModel: TranssistantViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_app),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text("Photo Translate", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Take a photo to translate text", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(20.dp))

            if (!hasCameraPermission) {
                Text("Camera permission is required", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryLight)
                ) {
                    Text("Grant Camera Permission", color = Color.White)
                }
            } else if (capturedBitmap == null) {
                // Camera preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isLoading = true
                        statusMessage = "Capturing..."
                        val executor = ContextCompat.getMainExecutor(context)
                        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                image.close()
                                capturedBitmap = bitmap
                                statusMessage = "Recognizing text..."

                                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                val inputImage = InputImage.fromBitmap(bitmap, 0)
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        recognizedText = visionText.text.trim()
                                        if (recognizedText.isNotBlank()) {
                                            statusMessage = "Translating..."
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    val response = RetrofitClient.api.translate(recognizedText)
                                                    withContext(Dispatchers.Main) {
                                                        translatedText = response.responseData.translatedText
                                                        isLoading = false
                                                        statusMessage = ""
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        translatedText = "Translation failed: ${e.message}"
                                                        isLoading = false
                                                        statusMessage = ""
                                                    }
                                                }
                                            }
                                        } else {
                                            recognizedText = "(no text detected in image)"
                                            isLoading = false
                                            statusMessage = ""
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        recognizedText = "Recognition failed: ${e.message}"
                                        isLoading = false
                                        statusMessage = ""
                                    }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                statusMessage = "Capture failed: ${exception.message}"
                                isLoading = false
                            }
                        })
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📷 Capture & Translate", color = Color.White, fontSize = 16.sp)
                }
            } else {
                // Results view
                if (isLoading) {
                    Spacer(modifier = Modifier.height(40.dp))
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(statusMessage, color = Color.White, fontSize = 16.sp)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Recognized Text", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                recognizedText.ifBlank { "(no text)" },
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Translation (EN → ZH)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                translatedText.ifBlank { "(no translation)" },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (recognizedText.isNotBlank() && translatedText.isNotBlank()) {
                        Button(
                            onClick = {
                                viewModel.addHistory(recognizedText, translatedText, "Photo")
                                Toast.makeText(context, "Saved to history!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💾 Save to History", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            capturedBitmap = null
                            recognizedText = ""
                            translatedText = ""
                            statusMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄 Retake Photo", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home", color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
