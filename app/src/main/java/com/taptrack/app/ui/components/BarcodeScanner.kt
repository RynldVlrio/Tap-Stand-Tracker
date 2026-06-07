package com.taptrack.app.ui.components

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8
                )
                .build()
        )
    }
    DisposableEffect(Unit) { onDispose { scanner.close() } }

    LaunchedEffect(previewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(executor) { proxy ->
                        val media = proxy.image
                        if (media != null && !scanned) {
                            val img = InputImage.fromMediaImage(
                                media,
                                proxy.imageInfo.rotationDegrees
                            )
                            scanner.process(img)
                                .addOnSuccessListener { barcodes ->
                                    val raw = barcodes.firstOrNull()?.rawValue
                                    if (!raw.isNullOrBlank() && !scanned) {
                                        scanned = true
                                        ContextCompat.getMainExecutor(context).execute {
                                            onBarcodeDetected(raw)
                                        }
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        } else {
                            proxy.close()
                        }
                    }
                }
            try {
                provider.unbindAll()
                cameraRef.value = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(torchOn) {
        cameraRef.value?.cameraControl?.enableTorch(torchOn)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.92f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // ── Header ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Meter Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    // Torch toggle
                    IconButton(onClick = { torchOn = !torchOn }) {
                        Icon(
                            imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (torchOn) "Torch on" else "Torch off",
                            tint = if (torchOn) Color(0xFFFFD740)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close scanner")
                    }
                }

                // ── Camera preview with viewfinder overlay ───────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RectangleShape)
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Darkened surround + white corner brackets
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val hPad = size.width * 0.10f
                        val vPad = size.height * 0.10f
                        val fl = hPad
                        val ft = vPad
                        val fr = size.width - hPad
                        val fb = size.height - vPad
                        val fh = fb - ft
                        val fw = fr - fl
                        val corner = 28.dp.toPx()
                        val sw = 3.5.dp.toPx()

                        // Vignette
                        val dim = Color.Black.copy(alpha = 0.55f)
                        drawRect(dim, size = Size(size.width, ft))
                        drawRect(dim, topLeft = Offset(0f, fb), size = Size(size.width, size.height - fb))
                        drawRect(dim, topLeft = Offset(0f, ft), size = Size(fl, fh))
                        drawRect(dim, topLeft = Offset(fr, ft), size = Size(size.width - fr, fh))

                        val w = Color.White
                        val cap = StrokeCap.Round
                        // top-left
                        drawLine(w, Offset(fl, ft + corner), Offset(fl, ft), sw, cap)
                        drawLine(w, Offset(fl, ft), Offset(fl + corner, ft), sw, cap)
                        // top-right
                        drawLine(w, Offset(fr - corner, ft), Offset(fr, ft), sw, cap)
                        drawLine(w, Offset(fr, ft), Offset(fr, ft + corner), sw, cap)
                        // bottom-left
                        drawLine(w, Offset(fl, fb - corner), Offset(fl, fb), sw, cap)
                        drawLine(w, Offset(fl, fb), Offset(fl + corner, fb), sw, cap)
                        // bottom-right
                        drawLine(w, Offset(fr - corner, fb), Offset(fr, fb), sw, cap)
                        drawLine(w, Offset(fr, fb), Offset(fr, fb - corner), sw, cap)
                    }
                }

                // ── Instruction ──────────────────────────────────────────
                Text(
                    text = "Point at the meter's QR code or barcode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}
