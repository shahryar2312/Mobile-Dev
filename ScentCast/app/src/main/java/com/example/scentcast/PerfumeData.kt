package com.example.scentcast

/**
 * Built-in details for the starter perfumes: notes, accords, how to wear them, and a cheaper
 * alternative. Notes are typical community descriptions. The budget picks are "similar style"
 * suggestions, not exact copies, and the prices are rough euro guesses, so check them before buying.
 */
val starterPerfumes = listOf(
    Perfume(
        id = 1, name = "Sauvage", brand = "Dior",
        imageRes = R.drawable.photo_sauvage,
        scentType = ScentType.SPICY, weather = Weather.COLD,
        seasons = setOf(Season.AUTUMN, Season.WINTER, Season.SPRING),
        topNotes = "Bergamot, Pepper",
        heartNotes = "Lavender, Pink pepper, Geranium",
        baseNotes = "Ambroxan, Cedar, Labdanum",
        accords = listOf("Fresh spicy" to 100, "Amber" to 85, "Citrus" to 75, "Aromatic" to 65, "Woody" to 55),
        howToWear = "Great for cool days, evenings out and the office. 3 to 5 sprays on chest and neck.",
        avoidWhen = "Skip it in very hot weather or small rooms, it projects strongly and can feel heavy.",
        priceEur = 100,
        dupe = Dupe("Asad", "Lattafa", 25)
    ),
    Perfume(
        id = 2, name = "Spicebomb", brand = "Viktor & Rolf",
        imageRes = R.drawable.photo_spicebomb,
        scentType = ScentType.SPICY, weather = Weather.COLD,
        seasons = setOf(Season.AUTUMN, Season.WINTER),
        topNotes = "Bergamot, Grapefruit, Pink pepper",
        heartNotes = "Cinnamon, Saffron, Paprika",
        baseNotes = "Tobacco, Leather, Vetiver",
        accords = listOf("Warm spicy" to 100, "Tobacco" to 80, "Leather" to 65, "Amber" to 60, "Citrus" to 40),
        howToWear = "A cold-weather and evening scent. 2 to 4 sprays is plenty.",
        avoidWhen = "Avoid in warm weather or at the gym, the spice turns heavy and sweaty.",
        priceEur = 95,
        dupe = Dupe("Khamrah", "Lattafa", 30)
    ),
    Perfume(
        id = 3, name = "Bleu de Chanel", brand = "Chanel",
        imageRes = R.drawable.photo_bleu,
        scentType = ScentType.WOODY, weather = Weather.MILD,
        seasons = setOf(Season.SPRING, Season.AUTUMN),
        topNotes = "Grapefruit, Lemon, Mint",
        heartNotes = "Ginger, Nutmeg, Jasmine",
        baseNotes = "Cedar, Sandalwood, Incense",
        accords = listOf("Woody" to 100, "Citrus" to 85, "Aromatic" to 70, "Fresh spicy" to 60, "Amber" to 45),
        howToWear = "An easy all-rounder for work, dates and mild days. 3 to 4 sprays.",
        avoidWhen = "Not the best pick for freezing weather, where a warmer scent holds up better.",
        priceEur = 110,
        dupe = Dupe("Tag-Him", "Armaf", 25)
    ),
    Perfume(
        id = 4, name = "1 Million", brand = "Paco Rabanne",
        imageRes = R.drawable.photo_million,
        scentType = ScentType.SWEET, weather = Weather.MILD,
        seasons = setOf(Season.AUTUMN, Season.WINTER),
        topNotes = "Blood mandarin, Grapefruit, Mint",
        heartNotes = "Cinnamon, Rose, Spicy notes",
        baseNotes = "Amber, Leather, Patchouli",
        accords = listOf("Sweet" to 100, "Warm spicy" to 85, "Amber" to 75, "Leather" to 60, "Woody" to 45),
        howToWear = "Made for nights out and cooler evenings. 2 to 3 sprays.",
        avoidWhen = "Avoid on hot days and for daytime work, it is sweet and loud.",
        priceEur = 85,
        dupe = Dupe("9PM", "Afnan", 25)
    ),
    Perfume(
        id = 5, name = "Acqua di Gio", brand = "Armani",
        imageRes = R.drawable.photo_acqua,
        scentType = ScentType.FRESH, weather = Weather.HOT,
        seasons = setOf(Season.SUMMER, Season.SPRING),
        topNotes = "Lime, Bergamot, Neroli",
        heartNotes = "Sea notes, Jasmine, Rosemary",
        baseNotes = "White musk, Cedar, Patchouli",
        accords = listOf("Aquatic" to 100, "Citrus" to 85, "Fresh" to 75, "Aromatic" to 60, "Woody" to 40),
        howToWear = "Ideal for hot days, daytime and the gym bag. 4 to 5 sprays, it fades fast in heat.",
        avoidWhen = "Not for cold evenings, it feels thin and does not last in the cold.",
        priceEur = 90,
        dupe = Dupe("Hawas", "Rasasi", 30)
    ),
    Perfume(
        id = 6, name = "Light Blue", brand = "Dolce & Gabbana",
        imageRes = R.drawable.photo_lightblue,
        scentType = ScentType.FRESH, weather = Weather.HOT,
        seasons = setOf(Season.SUMMER),
        topNotes = "Sicilian lemon, Apple, Bellflower",
        heartNotes = "Jasmine, Bamboo, White rose",
        baseNotes = "Cedar, Musk, Amber",
        accords = listOf("Citrus" to 100, "Fresh" to 90, "Floral" to 55, "Woody" to 45, "Musk" to 35),
        howToWear = "A bright summer scent for the beach, holidays and sunny days. 3 to 5 sprays.",
        avoidWhen = "Skip it at night or in winter, it is too light and sharp.",
        priceEur = 80,
        dupe = Dupe("Odyssey Mandarin Sky", "Armaf", 30)
    )
)
