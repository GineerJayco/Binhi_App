package com.example.binhi

import com.example.binhi.R

data class CropPlanting(
    val name: String,
    val areaPerPlant: Double,
    val plantingDistance: Double, // in meters (legacy, kept for compatibility)
    val rowSpacing: Double, // in meters (distance between rows)
    val columnSpacing: Double, // in meters (distance between plants in a row)
    val iconResource: Int // R.drawable resource ID
) {
    // Constructor for backward compatibility
    constructor(
        name: String,
        areaPerPlant: Double,
        plantingDistance: Double,
        iconResource: Int
    ) : this(name, areaPerPlant, plantingDistance, plantingDistance, plantingDistance, iconResource)
}

object CropData {
    val crops = mapOf(
        // Corn: 0.25m x 0.75m (rowSpacing x columnSpacing)
        "Corn" to CropPlanting("Corn", 0.1875, 0.75, 0.25, 0.75, R.drawable.ic_corn),
        // Cassava: 1m x 0.5m (rowSpacing x columnSpacing)
        "Cassava" to CropPlanting("Cassava", 0.5, 1.0, 1.0, 0.5, R.drawable.ic_cassava),
        // Sweet Potato: 0.5m x 1m (rowSpacing x columnSpacing)
        "Sweet Potato" to CropPlanting("Sweet Potato", 0.5, 1.0, 0.5, 1.0, R.drawable.ic_sweet_potato),
        // Banana: 5m x 5m (rowSpacing x columnSpacing)
        "Banana" to CropPlanting("Banana", 25.0, 5.0, 5.0, 5.0, R.drawable.ic_banana),
        // Coconut: 10m x 10m (rowSpacing x columnSpacing)
        "Coconut" to CropPlanting("Coconut", 100.0, 10.0, 10.0, 10.0, R.drawable.ic_coconut)
    )
}
