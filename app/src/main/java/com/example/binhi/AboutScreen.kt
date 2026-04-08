package com.example.binhi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.foundation.Image

data class CropInfo(
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val drawableId: Int? = null,
    val nitrogen: String,
    val phosphorus: String,
    val potassium: String,
    val phLevel: String,
    val temperature: String,
    val moisture: String,
    val plantingDistance: String,
    val color: Color
)

@UnstableApi
@Composable
fun AboutScreen(navController: NavController, modifier: Modifier = Modifier, isDarkModeState: MutableState<Boolean> = mutableStateOf(false)) {
    val crops = listOf(
        CropInfo(
            name = "Banana",
            description = "A tropical fruit crop rich in potassium. Bananas are widely cultivated in tropical and subtropical regions. They require consistent moisture and warm temperatures. The plant grows tall and produces heavy bunches of fruit. Ideal for regions with high humidity and rainfall.",
            imageUrl = "https://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExbXRoZTBuZjF5YWJtMnRhdmpjMHFtc2RzM213bG54aTFjZmtyaXB4NSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/oUF3xpfAsjXPnKSLMS/giphy.gif",
            nitrogen = "60-100 kg/ha",
            phosphorus = "40-60 kg/ha",
            potassium = "150-200 kg/ha",
            phLevel = "5.5-7.5",
            temperature = "24-30°C",
            moisture = "High (1500-2250mm annual)",
            plantingDistance = "2.4m × 2.4m or 2m × 2m",
            color = Color(0xFFFFD700)
        ),
        CropInfo(
            name = "Cassava",
            description = "An important root crop and staple food in many tropical regions. Cassava is drought-tolerant and adapts well to poor soils. The tuberous roots are harvested for food and industrial uses. It requires minimal fertilizer and is one of the most resilient crops. Also used for starch, flour, and animal feed production.",
            drawableId = R.drawable.fcassava,
            nitrogen = "40-60 kg/ha",
            phosphorus = "20-40 kg/ha",
            potassium = "60-100 kg/ha",
            phLevel = "5.5-7.0",
            temperature = "20-30°C",
            moisture = "Moderate (750-1500mm annual)",
            plantingDistance = "1.0m × 1.0m or 1.2m × 0.8m",
            color = Color(0xFF8B6F47)
        ),
        CropInfo(
            name = "Sweet Potato",
            description = "A nutrient-dense root vegetable rich in vitamins and minerals. Sweet potatoes thrive in warm climates and sandy soils. The vines spread across the ground while the edible tubers develop underground. Very productive and can be harvested in 3-4 months. Excellent source of vitamin A and dietary fiber.",
            drawableId = R.drawable.fspotato,
            nitrogen = "80-120 kg/ha",
            phosphorus = "60-90 kg/ha",
            potassium = "120-180 kg/ha",
            phLevel = "5.8-6.5",
            temperature = "22-28°C",
            moisture = "Moderate (750-1000mm annual)",
            plantingDistance = "0.9m × 0.3m or 1.0m × 0.25m",
            color = Color(0xFFFF8C00)
        ),
        CropInfo(
            name = "Corn",
            description = "A versatile grain crop used for food, animal feed, and industrial products. Corn requires good drainage and fertile soil. It grows tall with tassels at the top and ears developing along the stalk. Fast-growing with high yield potential. One of the most important cereal crops worldwide.",
            imageUrl = "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExZWhibW11ejk2MXR6eTh2Zm94emRmcjg1eXpzMXJlYWFuc3Rybm13bSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/xT9IgMfVVT58g6LWI8/giphy.gif\n",
            nitrogen = "120-150 kg/ha",
            phosphorus = "60-80 kg/ha",
            potassium = "80-100 kg/ha",
            phLevel = "6.0-7.5",
            temperature = "18-27°C",
            moisture = "Moderate (400-600mm during season)",
            plantingDistance = "0.75m × 0.25m or 0.9m × 0.2m",
            color = Color(0xFFFFA500)
        ),
        CropInfo(
            name = "Coconut",
            description = "A tropical tree crop valued for its coconuts and coconut products. Coconuts are highly adaptable to sandy and well-drained soils. The tree produces coconuts year-round and can live for 60-80 years. Used for food, oil, fiber, and various industrial applications. Rich in minerals and provides multiple products from a single tree.",
            drawableId = R.drawable.coconut,
            nitrogen = "70-100 kg/ha",
            phosphorus = "40-70 kg/ha",
            potassium = "100-150 kg/ha",
            phLevel = "5.0-8.0",
            temperature = "24-32°C",
            moisture = "Moderate to High (1500-2250mm annual)",
            plantingDistance = "10m × 10m or 9m × 9m (for spacing mature trees)",
            color = Color(0xFF8B4513)
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        VideoBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
        ) {
            // Top bar with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "CROP INFORMATION",
                    fontSize = 24.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 30.dp)
                )
            }

            // Crops List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(crops) { crop ->
                    CropCard(crop)
                }
            }
        }
    }
}

@Composable
fun CropCard(crop: CropInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Crop name with color indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(crop.color, shape = RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = crop.name,
                    fontSize = 24.sp,
                    color = Color.Black
                )
            }

            // Large GIF Image
            if (crop.drawableId != null) {
                Image(
                    painter = painterResource(id = crop.drawableId),
                    contentDescription = crop.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = crop.imageUrl,
                    contentDescription = crop.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = crop.description,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Parameters Section
            Text(
                text = "Growing Requirements",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier
                    .padding(bottom = 12.dp)
            )

            // Nutrients Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParameterChip(label = "N", value = crop.nitrogen, modifier = Modifier.weight(1f))
                ParameterChip(label = "P", value = crop.phosphorus, modifier = Modifier.weight(1f))
                ParameterChip(label = "K", value = crop.potassium, modifier = Modifier.weight(1f))
            }

            // pH and Temperature Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParameterChip(label = "pH Level", value = crop.phLevel, modifier = Modifier.weight(1f))
                ParameterChip(label = "Temperature", value = crop.temperature, modifier = Modifier.weight(1f))
            }

            // Moisture and Planting Distance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParameterChip(label = "Moisture", value = crop.moisture, modifier = Modifier.weight(1f))
                ParameterChip(label = "Plant Distance", value = crop.plantingDistance, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ParameterChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}



