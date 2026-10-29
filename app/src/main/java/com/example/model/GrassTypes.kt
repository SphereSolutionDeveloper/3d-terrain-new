package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Botanical Species of Grass with botanical parameters:
 * Width: 2mm - 5mm (0.002m - 0.005m)
 * Height: 6cm - 11cm max (0.06m - 0.11m)
 */
enum class GrassSpecies(
    val speciesName: String,
    val commonName: String,
    val minHeightMeters: Float,
    val maxHeightMeters: Float,
    val minWidthMeters: Float,
    val maxWidthMeters: Float,
    val stiffness: Float, // resistance to wind bend
    val meshFolderKey: String // "grass" (broad tuft) or "grass_fine" (fine arched)
) {
    KENTUCKY_BLUEGRASS(
        speciesName = "Poa pratensis",
        commonName = "Kentucky Bluegrass",
        minHeightMeters = 0.075f,
        maxHeightMeters = 0.105f,
        minWidthMeters = 0.0025f,
        maxWidthMeters = 0.0040f,
        stiffness = 0.72f,
        meshFolderKey = "grass"
    ),
    PERENNIAL_RYEGRASS(
        speciesName = "Lolium perenne",
        commonName = "Perennial Ryegrass",
        minHeightMeters = 0.080f,
        maxHeightMeters = 0.110f,
        minWidthMeters = 0.0020f,
        maxWidthMeters = 0.0035f,
        stiffness = 0.65f,
        meshFolderKey = "grass_fine"
    ),
    FINE_FESCUE(
        speciesName = "Festuca rubra",
        commonName = "Creeping Red Fescue",
        minHeightMeters = 0.070f,
        maxHeightMeters = 0.095f,
        minWidthMeters = 0.0012f,
        maxWidthMeters = 0.0022f,
        stiffness = 0.50f,
        meshFolderKey = "grass_fine"
    ),
    TALL_FESCUE(
        speciesName = "Schedonorus arundinaceus",
        commonName = "Tall Fescue",
        minHeightMeters = 0.085f,
        maxHeightMeters = 0.110f,
        minWidthMeters = 0.0040f,
        maxWidthMeters = 0.0055f,
        stiffness = 0.88f,
        meshFolderKey = "grass"
    ),
    BERMUDA_GRASS(
        speciesName = "Cynodon dactylon",
        commonName = "Bermudagrass Hybrid",
        minHeightMeters = 0.065f,
        maxHeightMeters = 0.090f,
        minWidthMeters = 0.0020f,
        maxWidthMeters = 0.0032f,
        stiffness = 0.78f,
        meshFolderKey = "grass"
    )
}

/**
 * 6 Realistic Botanical Blend Color & Weathering Types
 * Including healthy lush chlorophyll, spring emerald, warm lime, olive forest,
 * golden senescence, and dry brown weathered thatch.
 */
enum class GrassBlendType(
    val blendName: String,
    val description: String,
    val baseColor: Color,
    val tipColor: Color,
    val isWeatheredBrown: Boolean = false
) {
    LUSH_CHLOROPHYLL(
        blendName = "Lush Chlorophyll",
        description = "Deep vibrant botanical lawn green with high nitrogen",
        baseColor = Color(0xFF1E5E2A),
        tipColor = Color(0xFF35A848)
    ),
    EMERALD_SPRING(
        blendName = "Emerald Spring",
        description = "Tender young spring vegetative growth with bright hue",
        baseColor = Color(0xFF287834),
        tipColor = Color(0xFF4EE060)
    ),
    OLIVE_SHADOW(
        blendName = "Olive Understory",
        description = "Shaded moist forest floor and understory blades",
        baseColor = Color(0xFF1C4222),
        tipColor = Color(0xFF2E6B37)
    ),
    WARM_LIME(
        blendName = "Golden Meadow Lime",
        description = "Sun-drenched field blades with warm amber-yellow chlorophyll",
        baseColor = Color(0xFF2C5C21),
        tipColor = Color(0xFF7CB832)
    ),
    SENESCENT_GOLD(
        blendName = "Senescent Ochre",
        description = "Naturally aging autumn blade tips and straw highlights",
        baseColor = Color(0xFF525022),
        tipColor = Color(0xFFB5A642),
        isWeatheredBrown = true
    ),
    WEATHERED_BROWN_THATCH(
        blendName = "Weathered Brown Thatch",
        description = "Dry desiccated basal thatch and dormant weathered dead blades",
        baseColor = Color(0xFF4A3820),
        tipColor = Color(0xFF8A6A40),
        isWeatheredBrown = true
    )
}

/**
 * Grass Presentation Mode:
 * - SINGLE: Inspection of a single botanical grass clump/specimen with individual blade details.
 * - VARIETY: The full 10x10m scene grid populated with millions of uncountable grass blades, 5 species & 6 blends.
 */
enum class GrassViewMode {
    SINGLE,
    VARIETY
}
