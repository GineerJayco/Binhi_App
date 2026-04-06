package com.example.binhi

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.binhi.data.SoilData
import com.example.binhi.viewmodel.SoilDataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Enum for crop recommendation flow steps
enum class CropRecommendationStep {
    START,
    LOADING,
    RESULTS
}

// Data class to hold crop recommendation result
data class CropPrediction(
    val cropName: String,
    val confidence: Float,
    val percentage: Int,
    val color: Color,
    val icon: String,
    val reasoning: String
)

// Object for crop-related constants and utility functions
object CropConstants {
    // IMPORTANT: Order MUST match LabelEncoder.classes_ from mango_mock_data.py
    // LabelEncoder uses alphabetical order: Banana, Cassava, Corn, Mango, Sweet Potato
    val CROP_NAMES = listOf(
        "Banana",           // Index 0 - Model output[0]
        "Cassava",          // Index 1 - Model output[1]
        "Corn",             // Index 2 - Model output[2]
        "Mango",            // Index 3 - Model output[3]
        "Sweet Potato"      // Index 4 - Model output[4]
    )

    // Map crop names to their display colors
    val CROP_COLORS = mapOf(
        "Banana" to Color(0xFFFFD700),      // Gold
        "Cassava" to Color(0xFFD2B48C),     // Tan
        "Sweet Potato" to Color(0xFFFF8C69),// Salmon
        "Corn" to Color(0xFFFFD700),        // Gold
        "Mango" to Color(0xFFFF6347)        // Tomato
    )

    // Map crop names to their icons (you can add actual drawable references)
    val CROP_ICONS = mapOf(
        "Banana" to "🍌",
        "Cassava" to "🌳",
        "Sweet Potato" to "🥔",
        "Corn" to "🌽",
        "Mango" to "🥭"
    )

    fun getCropColor(cropName: String): Color {
        return CROP_COLORS[cropName] ?: Color.Gray
    }

    fun getCropIcon(cropName: String): String {
        return CROP_ICONS[cropName] ?: "🌾"
    }

    fun getReasoningForConfidence(cropName: String, confidence: Float): String {
        return when {
            confidence >= 0.8 -> "Excellent match - highly recommended"
            confidence >= 0.6 -> "Good match - well-suited for conditions"
            confidence >= 0.4 -> "Moderate match - may require adjustments"
            confidence >= 0.2 -> "Fair match - consider other options"
            else -> "Low compatibility - not recommended"
        }
    }
}

/**
 * Runs ONNX model inference for crop recommendation
 * Inputs: Normalized soil data (N, P, K, pH, Temperature, Moisture)
 * Outputs: Confidence scores for each crop
 */
fun runOnnxInference(
    context: Context,
    soilDataList: List<SoilData>
): List<CropPrediction> {
    return try {
        if (soilDataList.isEmpty()) {
            Log.w("CropRecommendation", "No soil data available")
            return getDefaultRecommendations()
        }

        // Calculate averages from all soil data
        val avgNitrogen = soilDataList.map { it.nitrogen }.average().toFloat()
        val avgPhosphorus = soilDataList.map { it.phosphorus }.average().toFloat()
        val avgPotassium = soilDataList.map { it.potassium }.average().toFloat()
        val avgPhLevel = soilDataList.map { it.phLevel }.average().toFloat()
        val avgTemperature = soilDataList.map { it.temperature }.average().toFloat()
        val avgMoisture = soilDataList.map { it.moisture }.average().toFloat()

        Log.d(
            "CropRecommendation",
            "Average Soil Data - N: $avgNitrogen, P: $avgPhosphorus, K: $avgPotassium, " +
                    "pH: $avgPhLevel, Temp: $avgTemperature, Moisture: $avgMoisture"
        )

        // Use raw input data WITHOUT normalization
        // The model was trained on raw data, not normalized data
        // Create 2D array [1, 6] - batch size 1, 6 features
        val rawInputData = arrayOf(
            floatArrayOf(
                avgNitrogen,      // Nitrogen: raw mg/kg value
                avgPhosphorus,    // Phosphorus: raw mg/kg value
                avgPotassium,     // Potassium: raw mg/kg value
                avgPhLevel,       // pH Level: raw value (3.0-9.0)
                avgTemperature,   // Temperature: raw °C value
                avgMoisture       // Moisture: raw % value
            )
        )

        Log.d("CropRecommendation", "Raw input shape [1, 6] (no normalization): ${rawInputData[0].contentToString()}")

        // Load and run ONNX model
        val predictions = try {
            val ortEnv = ai.onnxruntime.OrtEnvironment.getEnvironment()
            val sessionOptions = ai.onnxruntime.OrtSession.SessionOptions()

            // Load model from assets
            val modelAsset = context.assets.open("crop_recommendation_model.onnx")
            val modelBytes = modelAsset.readBytes()
            modelAsset.close()

            val session = ortEnv.createSession(modelBytes, sessionOptions)

            // Create input tensor - use simpler API without allocator
            val inputTensor = ai.onnxruntime.OnnxTensor.createTensor(ortEnv, rawInputData)

            // Get input and output names
            val inputName = session.inputNames?.firstOrNull()
                ?: throw IllegalStateException("No input names found in model")

            // The ONNX model has 2 outputs:
            // 0: output_label - predicted class index
            // 1: output_probability - probabilities dict {0: prob, 1: prob, ...}
            val outputNamesList: List<String> = session.outputNames?.toList() ?: emptyList()
            if (outputNamesList.size < 2) {
                throw IllegalStateException("Expected 2 outputs from model (label and probability), got ${outputNamesList.size}")
            }

            val labelOutputName: String = outputNamesList.getOrNull(0) ?: throw IllegalStateException("No label output found")
            val probOutputName: String = outputNamesList.getOrNull(1) ?: throw IllegalStateException("No probability output found")

            Log.d("CropRecommendation", "Input name: $inputName")
            Log.d("CropRecommendation", "Label output name: $labelOutputName")
            Log.d("CropRecommendation", "Probability output name: $probOutputName")

            // Run inference
            val results = session.run(mapOf(inputName to inputTensor))

            // Extract confidence scores from probability output
            val confidences: List<Float> = try {
                val output: Any? = results[probOutputName]
                Log.d("CropRecommendation", "Output is List: ${output is List<*>}, is Map: ${output is Map<*, *>}, is Tensor: ${output is ai.onnxruntime.OnnxTensor}")

                val scores: MutableList<Float> = mutableListOf()

                when {
                    output is List<*> && output.isNotEmpty() -> {
                        // output_probability is returned as List<Map<*, *>>
                        try {
                            val firstElement: Any? = output[0]
                            if (firstElement is Map<*, *>) {
                                for (i in 0 until CropConstants.CROP_NAMES.size) {
                                    val rawValue: Any? = (firstElement as Map<*, *>)[i]
                                    val floatValue: Float = when (rawValue) {
                                        is Double -> rawValue.toFloat()
                                        is Float -> rawValue
                                        is Number -> rawValue.toFloat()
                                        else -> 0f
                                    }
                                    scores.add(floatValue)
                                }
                                Log.d("CropRecommendation", "Extracted ${scores.size} probability scores from list of maps")
                            }
                        } catch (e: Exception) {
                            Log.e("CropRecommendation", "Error extracting from list: ${e.message}", e)
                        }
                    }
                    output is Map<*, *> -> {
                        // Fallback: direct map
                        try {
                            for (i in 0 until CropConstants.CROP_NAMES.size) {
                                val rawValue: Any? = output[i]
                                val floatValue: Float = when (rawValue) {
                                    is Double -> rawValue.toFloat()
                                    is Float -> rawValue
                                    is Number -> rawValue.toFloat()
                                    else -> 0f
                                }
                                scores.add(floatValue)
                            }
                            Log.d("CropRecommendation", "Extracted ${scores.size} probability scores from map")
                        } catch (e: Exception) {
                            Log.e("CropRecommendation", "Error extracting from map: ${e.message}", e)
                        }
                    }
                    output is ai.onnxruntime.OnnxTensor -> {
                        // Fallback: float tensor
                        try {
                            val floatBuffer = output.floatBuffer
                            while (floatBuffer.hasRemaining()) {
                                scores.add(floatBuffer.get())
                            }
                            Log.d("CropRecommendation", "Extracted ${scores.size} probability scores from tensor")
                        } catch (e: Exception) {
                            Log.e("CropRecommendation", "Error extracting from tensor: ${e.message}", e)
                        }
                    }
                    else -> {
                        Log.e("CropRecommendation", "Unexpected output type: ${output?.javaClass?.simpleName ?: "null"}")
                    }
                }
                scores
            } catch (e: Exception) {
                Log.e("CropRecommendation", "Error in confidence extraction: ${e.message}", e)
                emptyList()
            }

            Log.d("CropRecommendation", "Raw confidences: $confidences")

            // Create predictions
            confidences.mapIndexed { index, confidence ->
                val cropName = if (index < CropConstants.CROP_NAMES.size) {
                    CropConstants.CROP_NAMES[index]
                } else {
                    "Crop $index"
                }

                CropPrediction(
                    cropName = cropName,
                    confidence = confidence.coerceIn(0f, 1f),
                    percentage = (confidence * 100).roundToInt().coerceIn(0, 100),
                    color = CropConstants.getCropColor(cropName),
                    icon = CropConstants.getCropIcon(cropName),
                    reasoning = CropConstants.getReasoningForConfidence(cropName, confidence)
                )
            }
        } catch (e: Exception) {
            Log.e("CropRecommendation", "Error during model inference: ${e.message}", e)
            getDefaultRecommendations()
        }

        // Sort by confidence (highest first)
        predictions.sortedByDescending { it.confidence }
    } catch (e: Exception) {
        Log.e("CropRecommendation", "Error in runOnnxInference: ${e.message}", e)
        getDefaultRecommendations()
    }
}

/**
 * Provides default recommendations when model is not available
 */
fun getDefaultRecommendations(): List<CropPrediction> {
    return listOf(
        CropPrediction(
            cropName = "Banana",
            confidence = 0.85f,
            percentage = 85,
            color = Color(0xFFFFD700),
            icon = "🍌",
            reasoning = "Well-suited for tropical climate"
        ),
        CropPrediction(
            cropName = "Cassava",
            confidence = 0.72f,
            percentage = 72,
            color = Color(0xFFD2B48C),
            icon = "🌳",
            reasoning = "Good drought tolerance"
        ),
        CropPrediction(
            cropName = "Sweet Potato",
            confidence = 0.68f,
            percentage = 68,
            color = Color(0xFFFF8C69),
            icon = "🥔",
            reasoning = "Moderate soil nutrient requirements"
        ),
        CropPrediction(
            cropName = "Corn",
            confidence = 0.60f,
            percentage = 60,
            color = Color(0xFFFFD700),
            icon = "🌽",
            reasoning = "Good temperature adaptability"
        ),
        CropPrediction(
            cropName = "Mango",
            confidence = 0.55f,
            percentage = 55,
            color = Color(0xFFFF6347),
            icon = "🥭",
            reasoning = "Well-drained soil preference"
        )
    )
}

/**
 * CropRecommendation Composable - Main UI Screen with three-step flow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropRecommendationScreen(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel = viewModel(),
    skipStartScreen: Boolean = false,
    avgNitrogen: Float? = null,
    avgPhosphorus: Float? = null,
    avgPotassium: Float? = null,
    avgPhLevel: Float? = null,
    avgMoisture: Float? = null,
    avgTemperature: Float? = null
) {
    val context = LocalContext.current
    var currentStep by remember {
        mutableStateOf(if (skipStartScreen) CropRecommendationStep.LOADING else CropRecommendationStep.START)
    }
    var predictions by remember { mutableStateOf<List<CropPrediction>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Step 1: Start Screen (skipped if parameters provided)
    if (currentStep == CropRecommendationStep.START && !skipStartScreen) {
        // Auto-transition to loading when start screen would be shown
        LaunchedEffect(Unit) {
            currentStep = CropRecommendationStep.LOADING
        }
        return
    }

    // Step 2 & 3: Loading and Results
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crop Recommendation",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == CropRecommendationStep.LOADING) {
                            currentStep = CropRecommendationStep.START
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            when (currentStep) {
                CropRecommendationStep.LOADING -> {
                    // Load and process data when entering loading state
                    LaunchedEffect(Unit) {
                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                // Check if we have direct parameters passed from MappingInfo
                                if (skipStartScreen && avgNitrogen != null && avgPhosphorus != null &&
                                    avgPotassium != null && avgPhLevel != null && avgMoisture != null &&
                                    avgTemperature != null) {

                                    Log.d("CropRecommendation", "Using passed average parameters for inference")
                                    Log.d("CropRecommendation",
                                        "Avg - N: $avgNitrogen, P: $avgPhosphorus, K: $avgPotassium, " +
                                        "pH: $avgPhLevel, Temp: $avgTemperature, Moisture: $avgMoisture")

                                    // Create a synthetic SoilData list with single entry for inference
                                    val syntheticSoilData = listOf(
                                        SoilData(
                                            nitrogen = avgNitrogen.toInt(),
                                            phosphorus = avgPhosphorus.toInt(),
                                            potassium = avgPotassium.toInt(),
                                            phLevel = avgPhLevel,
                                            temperature = avgTemperature,
                                            moisture = avgMoisture.toInt(),
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )

                                    predictions = runOnnxInference(context, syntheticSoilData)
                                    Log.d("CropRecommendation", "Generated ${predictions.size} predictions from passed parameters")

                                } else {
                                    // Fallback: Load from ViewModel if no parameters provided
                                    Log.d("CropRecommendation", "Loading data from ViewModel as fallback")
                                    val allLocations = soilDataViewModel.getAllStoredLocations().toList()
                                    val soilDataList = allLocations.mapNotNull { location ->
                                        soilDataViewModel.getSoilData(location)
                                    }

                                    Log.d("CropRecommendation", "Loaded ${soilDataList.size} soil data samples")

                                    if (soilDataList.isEmpty()) {
                                        error = "No soil data collected. Please collect soil samples first."
                                        showErrorDialog = true
                                        currentStep = CropRecommendationStep.START
                                        return@launch
                                    }

                                    predictions = runOnnxInference(context, soilDataList)
                                    Log.d("CropRecommendation", "Generated ${predictions.size} predictions")
                                }

                                // Simulate some processing time for better UX
                                kotlinx.coroutines.delay(1500)
                                currentStep = CropRecommendationStep.RESULTS

                            } catch (e: Exception) {
                                Log.e("CropRecommendation", "Error loading data: ${e.message}", e)
                                error = "Error loading crop recommendations: ${e.message}"
                                showErrorDialog = true
                                currentStep = CropRecommendationStep.START
                            }
                        }
                    }
                    LoadingScreen()
                }
                CropRecommendationStep.RESULTS -> {
                    if (predictions.isNotEmpty()) {
                        ResultsScreen(predictions = predictions)
                    } else {
                        EmptyScreen()
                    }
                }
                else -> {}
            }
        }
    }

    // Error Dialog
    if (showErrorDialog && error != null) {
        Dialog(
            onDismissRequest = { showErrorDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        error!!,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            showErrorDialog = false
                            currentStep = CropRecommendationStep.START
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }
    }
}


/**
 * Loading Screen Composable
 */
@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color(0xFF2196F3),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Analyzing Soil Data...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Running ML model inference",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}


/**
 * Empty State Screen Composable
 */
@Composable
fun EmptyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "No Data",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Recommendations Available",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Please collect soil samples first",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Results Screen Composable - Shows all crop recommendations
 */
@Composable
fun ResultsScreen(predictions: List<CropPrediction>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Top Recommendation",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val topCrop = predictions.firstOrNull()
                    if (topCrop != null) {
                        Text(
                            topCrop.icon,
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            topCrop.cropName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${topCrop.percentage}% Match",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = topCrop.color
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            topCrop.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Summary Stats Row
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${predictions.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                "Options",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(predictions.map { it.confidence }.average() * 100).roundToInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                "Avg. Confidence",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Recommendations Title
        item {
            Text(
                "All Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
        }

        // Predictions List
        items(predictions) { prediction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                CropPredictionCard(prediction = prediction)
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Individual Crop Prediction Card
 */
@Composable
fun CropPredictionCard(prediction: CropPrediction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Crop Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = prediction.color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    prediction.icon,
                    fontSize = 32.sp
                )
            }

            // Crop Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        prediction.cropName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "${prediction.percentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = prediction.color,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(3.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(prediction.confidence.coerceIn(0f, 1f))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(prediction.color, prediction.color.copy(alpha = 0.7f))
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Reasoning
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = prediction.color.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Info",
                        modifier = Modifier.size(16.dp),
                        tint = prediction.color
                    )
                    Text(
                        prediction.reasoning,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Extension function to display the Crop Recommendation Screen
 */
@Composable
fun CropRecommendation(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel = viewModel(),
    skipStartScreen: Boolean = false,
    avgNitrogen: Float? = null,
    avgPhosphorus: Float? = null,
    avgPotassium: Float? = null,
    avgPhLevel: Float? = null,
    avgMoisture: Float? = null,
    avgTemperature: Float? = null
) {
    CropRecommendationScreen(
        navController = navController,
        soilDataViewModel = soilDataViewModel,
        skipStartScreen = skipStartScreen,
        avgNitrogen = avgNitrogen,
        avgPhosphorus = avgPhosphorus,
        avgPotassium = avgPotassium,
        avgPhLevel = avgPhLevel,
        avgMoisture = avgMoisture,
        avgTemperature = avgTemperature
    )
}

