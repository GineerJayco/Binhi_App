package com.example.binhi

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.binhi.ui.theme.BinhiTheme
import com.example.binhi.ui.theme.ThemeManager
import com.example.binhi.viewmodel.SoilDataViewModel
import com.example.binhi.data.database.SoilDataDatabase
import com.example.binhi.data.database.SessionRepository

@UnstableApi
class MainUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Tell Android to layout behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set transparent system bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Hide navigation bar with immersive sticky mode
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        setContent {
            val isDarkMode = remember { mutableStateOf(ThemeManager.getCurrentTheme()) }

            BinhiTheme(darkTheme = isDarkMode.value) {
                val navController = rememberNavController()
                // Initialize database and repository
                val database = SoilDataDatabase.getInstance(this@MainUI)
                val sessionRepository = SessionRepository(
                    database.sessionDao(),
                    database.soilDataPointDao()
                )
                // Create ViewModel with repository at NavHost level so it's shared across all screens
                val soilDataViewModel: SoilDataViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return SoilDataViewModel(sessionRepository) as T
                        }
                    }
                )

                NavHost(
                    navController = navController,
                    startDestination = "main",
                    modifier = Modifier.fillMaxSize()
                ) {
                    @Suppress("EXPERIMENTAL_API_USAGE")
                    composable("main") {
                        BinhiScreen(navController = navController, isDarkModeState = isDarkMode)
                    }
                    composable("input_land_area") {
                        InputLandAreaScreen(navController = navController)
                    }
                    composable("input_crop_quantity") {
                        InputCropQuantityScreen(navController = navController)
                    }
                    composable(
                        route = "visualize_la/{landArea}/{length}/{width}/{crop}",
                        arguments = listOf(
                            navArgument("landArea") { type = NavType.StringType },
                            navArgument("length") { type = NavType.StringType },
                            navArgument("width") { type = NavType.StringType },
                            navArgument("crop") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        VisualizeLA(
                            navController = navController,
                            landArea = backStackEntry.arguments?.getString("landArea"),
                            length = backStackEntry.arguments?.getString("length"),
                            width = backStackEntry.arguments?.getString("width"),
                            crop = backStackEntry.arguments?.getString("crop"),
                            isDarkModeState = isDarkMode
                        )
                    }
                    composable(
                        route = "visualize_cq/{crop}/{cropQuantity}",
                        arguments = listOf(
                            navArgument("crop") { type = NavType.StringType },
                            navArgument("cropQuantity") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        VisualizeCQ(
                            navController = navController,
                            crop = backStackEntry.arguments?.getString("crop"),
                            cropQuantity = backStackEntry.arguments?.getString("cropQuantity"),
                            isDarkModeState = isDarkMode
                        )
                    }
                    composable(
                        route = "visualizeCR/{crop}/{landArea}/{length}/{width}",
                        arguments = listOf(
                            navArgument("crop") { type = NavType.StringType },
                            navArgument("landArea") { type = NavType.StringType },
                            navArgument("length") { type = NavType.StringType },
                            navArgument("width") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        VisualizeCR(
                            navController = navController,
                            recommendedCrop = backStackEntry.arguments?.getString("crop"),
                            landArea = backStackEntry.arguments?.getString("landArea"),
                            length = backStackEntry.arguments?.getString("length"),
                            width = backStackEntry.arguments?.getString("width"),
                            isDarkModeState = isDarkMode
                        )
                    }
                    composable(
                        route = "get_soil_data/{landArea}/{length}/{width}/{crop}",
                        arguments = listOf(
                            navArgument("landArea") { type = NavType.StringType },
                            navArgument("length") { type = NavType.StringType },
                            navArgument("width") { type = NavType.StringType },
                            navArgument("crop") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        GetSoilData(
                            navController = navController,
                            landArea = backStackEntry.arguments?.getString("landArea"),
                            length = backStackEntry.arguments?.getString("length"),
                            width = backStackEntry.arguments?.getString("width"),
                            crop = backStackEntry.arguments?.getString("crop"),
                            soilDataViewModel = soilDataViewModel,
                            isDarkModeState = isDarkMode
                        )
                    }
                    composable("mapping_info") {
                        MappingInfo(navController = navController, soilDataViewModel = soilDataViewModel, isDarkModeState = isDarkMode)
                    }
                    composable(
                        route = "crop_recommendation/{skipStart}/{avgN}/{avgP}/{avgK}/{avgPH}/{avgMoist}/{avgTemp}",
                        arguments = listOf(
                            navArgument("skipStart") { type = NavType.BoolType; defaultValue = false },
                            navArgument("avgN") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("avgP") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("avgK") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("avgPH") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("avgMoist") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("avgTemp") { type = NavType.FloatType; defaultValue = 0f }
                        )
                    ) { backStackEntry ->
                        CropRecommendation(
                            navController = navController,
                            soilDataViewModel = soilDataViewModel,
                            skipStartScreen = backStackEntry.arguments?.getBoolean("skipStart") ?: false,
                            avgNitrogen = backStackEntry.arguments?.getFloat("avgN"),
                            avgPhosphorus = backStackEntry.arguments?.getFloat("avgP"),
                            avgPotassium = backStackEntry.arguments?.getFloat("avgK"),
                            avgPhLevel = backStackEntry.arguments?.getFloat("avgPH"),
                            avgMoisture = backStackEntry.arguments?.getFloat("avgMoist"),
                            avgTemperature = backStackEntry.arguments?.getFloat("avgTemp")
                        )
                    }
                    composable("crop_recommendation") {
                        CropRecommendation(navController = navController, soilDataViewModel = soilDataViewModel)
                    }
                    composable("saved_data") {
                        SavedDataScreen(navController = navController, soilDataViewModel = soilDataViewModel, isDarkModeState = isDarkMode)
                    }
                    composable("about") {
                        AboutScreen(navController = navController, isDarkModeState = isDarkMode)
                    }
                }
            }
        }
    }
}


@UnstableApi
@Composable
fun BinhiScreen(navController: NavController, isDarkModeState: MutableState<Boolean> = mutableStateOf(false), modifier: Modifier = Modifier) {
    // ===== ADJUSTABLE UI POSITIONING CONSTANTS =====
    // Adjust these values to customize button and widget positions
    val topSpacerHeight = 100.dp              // Space above Weather Widget
    val weatherToButtonSpacing = 32.dp       // Space between Weather Widget and first button
    val buttonSpacing = 16.dp                // Space between buttons
    val inputLandAreaButtonHeight = 56.dp
    val inputCropQuantityButtonHeight = 56.dp
    val savedDataButtonHeight = 56.dp
    val inputLandAreaButtonColor = Color(0xFF4CAF50)
    val inputCropQuantityButtonColor = Color(0xFF1976D2)
    val savedDataButtonColor = Color(0xFFF57C00)
    // ================================================

    val logoVisible = remember { mutableStateOf(false) }
    val weatherVisible = remember { mutableStateOf(false) }
    val menuExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Trigger the animation when the screen is first composed
        logoVisible.value = true
        // Show weather widget after a short delay
        delay(300)
        weatherVisible.value = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        VideoBackground(modifier = Modifier.fillMaxSize())

        // Navigation bar with theme toggle and hamburger menu at far right
        // Using statusBarsPadding() to respect system insets (status bar)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Toggle Button
            IconButton(onClick = {
                isDarkModeState.value = !isDarkModeState.value
                ThemeManager.toggleTheme()
            }) {
                Icon(
                    imageVector = if (isDarkModeState.value) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                    contentDescription = if (isDarkModeState.value) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = Color.Black
                )
            }

            // Menu Button
            Box {
                IconButton(onClick = { menuExpanded.value = !menuExpanded.value }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.Black
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded.value,
                    onDismissRequest = { menuExpanded.value = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            menuExpanded.value = false
                            navController.navigate("about")
                        }
                    )
                }
            }
        }

        // Logo at top left
        AnimatedVisibility(
            visible = logoVisible.value,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 50.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.binhi_logo2),
                contentDescription = "Binhi Logo",
                modifier = Modifier.height(200.dp)
            )
        }

        // Main content centered
        Column(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacerHeight))

            // Weather Widget
            AnimatedVisibility(
                visible = weatherVisible.value,
                enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight })
            ) {
                WeatherWidget()
            }

            Spacer(modifier = Modifier.height(weatherToButtonSpacing))

            AnimatedVisibility(
                visible = logoVisible.value,
                enter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            ) {
                Button(
                    onClick = { navController.navigate("input_land_area") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(inputLandAreaButtonHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = inputLandAreaButtonColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Yard,
                        contentDescription = "Land Area Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Input Land Area", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(buttonSpacing))
            AnimatedVisibility(
                visible = logoVisible.value,
                enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            ) {
                Button(
                    onClick = { navController.navigate("input_crop_quantity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(inputCropQuantityButtonHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = inputCropQuantityButtonColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Agriculture,
                        contentDescription = "Crop Quantity Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Input Crop Quantity", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(buttonSpacing))
            AnimatedVisibility(
                visible = logoVisible.value,
                enter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            ) {
                Button(
                    onClick = { navController.navigate("saved_data") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(savedDataButtonHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = savedDataButtonColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Saved Data Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Saved Data", fontSize = 16.sp)
                }
            }
        }
    }
}


@UnstableApi
@Preview(showBackground = true)
@Composable
fun BinhiScreenPreview() {
    BinhiTheme {
        BinhiScreen(navController = rememberNavController())
    }
}
