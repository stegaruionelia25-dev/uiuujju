package ro.profitride.core

/** Mărci și modele frecvente pe piața europeană, doar ca listă de alegere. Pentru orice altceva: „Altul”. */
object CarCatalog {
    val models: Map<String, List<String>> = sortedMapOf(
        "Abarth" to listOf("500", "595", "695"),
        "Alfa Romeo" to listOf("147", "156", "159", "Giulia", "Giulietta", "MiTo", "Stelvio", "Tonale"),
        "Audi" to listOf("A1", "A3", "A4", "A5", "A6", "A7", "A8", "Q2", "Q3", "Q5", "Q7", "Q8", "e-tron", "TT"),
        "BMW" to listOf("Seria 1", "Seria 2", "Seria 3", "Seria 4", "Seria 5", "Seria 7", "X1", "X2", "X3", "X4", "X5", "X6", "i3", "i4", "iX3"),
        "BYD" to listOf("Atto 3", "Dolphin", "Han", "Seal", "Tang"),
        "Chevrolet" to listOf("Aveo", "Captiva", "Cruze", "Orlando", "Spark"),
        "Chrysler" to listOf("300C", "Voyager"),
        "Citroën" to listOf("Berlingo", "C-Elysée", "C1", "C3", "C3 Aircross", "C4", "C4 Picasso", "C5", "C5 Aircross", "DS3", "e-C4"),
        "Cupra" to listOf("Born", "Formentor", "Leon"),
        "Dacia" to listOf("Dokker", "Duster", "Jogger", "Lodgy", "Logan", "Sandero", "Spring"),
        "Daewoo" to listOf("Matiz", "Nubira"),
        "DS" to listOf("DS 3", "DS 4", "DS 7"),
        "Fiat" to listOf("500", "500X", "Bravo", "Doblo", "Grande Punto", "Panda", "Punto", "Tipo", "Qubo"),
        "Ford" to listOf("C-Max", "EcoSport", "Fiesta", "Focus", "Fusion", "Galaxy", "Kuga", "Mondeo", "Mustang Mach-E", "Puma", "S-Max", "Transit Connect"),
        "Honda" to listOf("Accord", "Civic", "CR-V", "HR-V", "Jazz"),
        "Hyundai" to listOf("Elantra", "i10", "i20", "i30", "i40", "Ioniq", "Ioniq 5", "ix35", "Kona", "Santa Fe", "Tucson"),
        "Infiniti" to listOf("Q30", "Q50"),
        "Jaguar" to listOf("E-Pace", "F-Pace", "XE", "XF"),
        "Jeep" to listOf("Avenger", "Cherokee", "Compass", "Renegade", "Wrangler"),
        "Kia" to listOf("Ceed", "EV6", "Niro", "Optima", "Picanto", "Rio", "Sorento", "Sportage", "Stonic", "Soul", "Venga", "XCeed"),
        "Lancia" to listOf("Delta", "Ypsilon"),
        "Land Rover" to listOf("Discovery", "Discovery Sport", "Freelander", "Range Rover", "Range Rover Evoque", "Range Rover Sport"),
        "Lexus" to listOf("CT", "ES", "IS", "NX", "RX", "UX"),
        "Mazda" to listOf("2", "3", "5", "6", "CX-3", "CX-30", "CX-5", "MX-30"),
        "Mercedes-Benz" to listOf("A-Class", "B-Class", "C-Class", "CLA", "CLS", "E-Class", "EQA", "EQC", "GLA", "GLB", "GLC", "GLE", "S-Class", "Vito", "V-Class"),
        "MG" to listOf("3", "4", "5", "HS", "ZS"),
        "Mini" to listOf("Clubman", "Cooper", "Countryman"),
        "Mitsubishi" to listOf("ASX", "Colt", "Eclipse Cross", "Lancer", "Outlander", "Space Star"),
        "Nissan" to listOf("Juke", "Leaf", "Micra", "Note", "Pulsar", "Qashqai", "X-Trail"),
        "Opel" to listOf("Adam", "Astra", "Combo", "Corsa", "Crossland", "Grandland", "Insignia", "Meriva", "Mokka", "Vectra", "Zafira"),
        "Peugeot" to listOf("107", "108", "207", "208", "301", "307", "308", "2008", "3008", "407", "508", "5008", "Partner", "Rifter", "e-208"),
        "Polestar" to listOf("2"),
        "Porsche" to listOf("Cayenne", "Macan", "Panamera", "Taycan"),
        "Renault" to listOf("Austral", "Captur", "Clio", "Espace", "Fluence", "Kadjar", "Kangoo", "Laguna", "Megane", "Scenic", "Symbol", "Talisman", "Twingo", "Zoe"),
        "Seat" to listOf("Alhambra", "Arona", "Ateca", "Ibiza", "Leon", "Mii", "Tarraco", "Toledo"),
        "Škoda" to listOf("Citigo", "Enyaq", "Fabia", "Kamiq", "Karoq", "Kodiaq", "Octavia", "Rapid", "Roomster", "Scala", "Superb", "Yeti"),
        "Smart" to listOf("Forfour", "Fortwo"),
        "SsangYong" to listOf("Korando", "Tivoli"),
        "Subaru" to listOf("Forester", "Impreza", "Outback", "XV"),
        "Suzuki" to listOf("Baleno", "Ignis", "SX4", "SX4 S-Cross", "Swift", "Vitara"),
        "Tesla" to listOf("Model 3", "Model S", "Model X", "Model Y"),
        "Toyota" to listOf("Auris", "Avensis", "Aygo", "C-HR", "Camry", "Corolla", "Prius", "Proace", "RAV4", "Yaris", "Yaris Cross"),
        "Volkswagen" to listOf("Arteon", "Caddy", "Golf", "ID.3", "ID.4", "Jetta", "Passat", "Polo", "Sharan", "T-Cross", "T-Roc", "Tiguan", "Touareg", "Touran", "Up!"),
        "Volvo" to listOf("S40", "S60", "S90", "V40", "V60", "V90", "XC40", "XC60", "XC90"),
    )

    val makes: List<String> get() = models.keys.toList()

    fun modelsOf(make: String): List<String> = models[make].orEmpty()

    /** Anii oferiți la alegere: de la anul curent până în 2000. */
    fun years(currentYear: Int): List<Int> = (currentYear downTo 2000).toList()
}
