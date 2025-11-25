package com.example.binhi

import com.example.binhi.R

data class CropPlanting(
    val name: String,
    val areaPerPlant: Double,
    val plantingDistance: Double, // in meters
    val iconResource: Int // R.drawable resource ID
)

object CropData {
    val crops = mapOf(
        "Banana" to CropPlanting("Banana", 3.24, 2.0, R.drawable.ic_banana),
        "Cassava" to CropPlanting("Cassava", 1.0, 1.0, R.drawable.ic_cassava),
        "Sweet Potato" to CropPlanting("Sweet Potato", 0.23, 0.5, R.drawable.ic_sweet_potato),
        "Mango" to CropPlanting("Mango", 400.0, 20.0, R.drawable.ic_mango),
        "Corn" to CropPlanting("Corn", 0.38, 0.75, R.drawable.ic_corn)
    )
}
