package com.example.agriplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.agriplanner.ui.theme.AgriplannerTheme

class MainUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgriplannerTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            AgriPlannerScreen(navController = navController)
                        }
                        composable("input_land_area") {
                            InputLandAreaScreen(navController = navController)
                        }
                        composable("input_crop_quantity") {
                            InputCropQuantityScreen(navController = navController)
                        }
                        composable(
                            route = "visualize_analyze/{landArea}",
                            arguments = listOf(navArgument("landArea") { type = NavType.StringType })
                        ) { backStackEntry ->
                            VisualizeAndAnalyzeScreen(
                                navController = navController,
                                landArea = backStackEntry.arguments?.getString("landArea")
                            )
                        }
                        composable("receive_data") {
                            ReceiveDataScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AgriPlannerScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "aGRiPLANNER",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Get smart crop recommendations for your land.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { navController.navigate("input_land_area") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(
                imageVector = Icons.Default.Yard,
                contentDescription = "Land Area Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Input Land Area", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("input_crop_quantity") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
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
}

@Preview(showBackground = true)
@Composable
fun AgriPlannerScreenPreview() {
    AgriplannerTheme {
        val navController = rememberNavController()
        AgriPlannerScreen(navController = navController)
    }
}
