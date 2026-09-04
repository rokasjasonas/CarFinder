package lt.carfinder.data

import lt.carfinder.model.BodyType
import lt.carfinder.model.Car
import lt.carfinder.model.Drive
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox

object Catalog {

    val cars: List<Car> = listOf(
        Car(
            id = "dacia-sandero", brand = "Dacia", model = "Sandero TCe 90", year = 2022,
            priceEur = 12500, bodyType = BodyType.HATCHBACK, fuelType = FuelType.PETROL, gearbox = Gearbox.MANUAL,
            seats = 5, powerHp = 90, trunkL = 328, consumption = 5.3, drive = Drive.FWD,
            blurb = "The champion of euros per kilometre. Nothing more, nothing less.",
            emoji = "🚗", accent = 0xFF4A6FA5,
        ),
        Car(
            id = "renault-clio", brand = "Renault", model = "Clio TCe 100", year = 2021,
            priceEur = 13900, bodyType = BodyType.HATCHBACK, fuelType = FuelType.PETROL, gearbox = Gearbox.MANUAL,
            seats = 5, powerHp = 100, trunkL = 391, consumption = 5.2, drive = Drive.FWD,
            blurb = "French hatch with a premium-feeling cabin for small money.",
            emoji = "🚗", accent = 0xFF2E5E8C,
        ),
        Car(
            id = "fiat-500-hybrid", brand = "Fiat", model = "500 Hybrid", year = 2022,
            priceEur = 14500, bodyType = BodyType.HATCHBACK, fuelType = FuelType.HYBRID, gearbox = Gearbox.MANUAL,
            seats = 4, powerHp = 70, trunkL = 185, consumption = 4.8, drive = Drive.FWD,
            blurb = "City fashion statement. Boot optional, style mandatory.",
            emoji = "🚗", accent = 0xFF6FA8DC,
        ),
        Car(
            id = "peugeot-208", brand = "Peugeot", model = "208 BlueHDi 100", year = 2021,
            priceEur = 15200, bodyType = BodyType.HATCHBACK, fuelType = FuelType.DIESEL, gearbox = Gearbox.MANUAL,
            seats = 5, powerHp = 100, trunkL = 285, consumption = 4.2, drive = Drive.FWD,
            blurb = "Tiny 3D-dashboard, tiny fuel bills.",
            emoji = "🚗", accent = 0xFF3D5A80,
        ),
        Car(
            id = "fiat-500e", brand = "Fiat", model = "500e", year = 2022,
            priceEur = 19900, bodyType = BodyType.HATCHBACK, fuelType = FuelType.EV, gearbox = Gearbox.AUTOMATIC,
            seats = 4, powerHp = 118, trunkL = 185, consumption = 13.0, drive = Drive.FWD, rangeKm = 190,
            blurb = "Electric city icon. 190 km of range, 100% of the charm.",
            emoji = "🚗", accent = 0xFF76B5C5,
        ),
        Car(
            id = "vw-golf", brand = "Volkswagen", model = "Golf 1.5 TSI", year = 2021,
            priceEur = 21900, bodyType = BodyType.HATCHBACK, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 130, trunkL = 380, consumption = 5.6, drive = Drive.FWD,
            blurb = "The default answer. Still a great one.",
            emoji = "🚗", accent = 0xFF1F4E79,
        ),
        Car(
            id = "toyota-corolla", brand = "Toyota", model = "Corolla 1.8 Hybrid", year = 2021,
            priceEur = 22500, bodyType = BodyType.HATCHBACK, fuelType = FuelType.HYBRID, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 122, trunkL = 313, consumption = 4.5, drive = Drive.FWD,
            blurb = "Boring reliability with hybrid sipping. Boring is good.",
            emoji = "🚗", accent = 0xFF97CAEB,
        ),
        Car(
            id = "skoda-octavia", brand = "Škoda", model = "Octavia Combi TDI", year = 2021,
            priceEur = 24500, bodyType = BodyType.WAGON, fuelType = FuelType.DIESEL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 150, trunkL = 640, consumption = 4.8, drive = Drive.FWD,
            blurb = "640 litres of boot. You could live in it. Some do.",
            emoji = "🚘", accent = 0xFF2C3E50,
        ),
        Car(
            id = "dacia-duster", brand = "Dacia", model = "Duster TCe 130 4x4", year = 2022,
            priceEur = 21500, bodyType = BodyType.SUV, fuelType = FuelType.PETROL, gearbox = Gearbox.MANUAL,
            seats = 5, powerHp = 130, trunkL = 445, consumption = 6.4, drive = Drive.AWD,
            blurb = "Honest box on wheels with real 4x4. Backpacker approved.",
            emoji = "🚙", accent = 0xFFB08968,
        ),
        Car(
            id = "citroen-berlingo", brand = "Citroën", model = "Berlingo XL", year = 2021,
            priceEur = 22900, bodyType = BodyType.VAN, fuelType = FuelType.DIESEL, gearbox = Gearbox.MANUAL,
            seats = 7, powerHp = 130, trunkL = 1050, consumption = 5.5, drive = Drive.FWD,
            blurb = "Sliding doors, three seats in row two, epic practicality.",
            emoji = "🚐", accent = 0xFF8D99AE,
        ),
        Car(
            id = "vw-tiguan", brand = "Volkswagen", model = "Tiguan 1.5 TSI", year = 2021,
            priceEur = 29900, bodyType = BodyType.SUV, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 150, trunkL = 615, consumption = 7.3, drive = Drive.FWD,
            blurb = "The family SUV everyone cross-shops. Solid, if unexciting.",
            emoji = "🚙", accent = 0xFF264653,
        ),
        Car(
            id = "mazda-mx5", brand = "Mazda", model = "MX-5 2.0", year = 2022,
            priceEur = 29900, bodyType = BodyType.CONVERTIBLE, fuelType = FuelType.PETROL, gearbox = Gearbox.MANUAL,
            seats = 2, powerHp = 184, trunkL = 130, consumption = 6.9, drive = Drive.RWD,
            blurb = "990 kg of open-top joy. Luggage goes in a friend's car.",
            emoji = "🏎️", accent = 0xFFC1121F,
        ),
        Car(
            id = "toyota-rav4", brand = "Toyota", model = "RAV4 Hybrid AWD", year = 2021,
            priceEur = 31900, bodyType = BodyType.SUV, fuelType = FuelType.HYBRID, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 218, trunkL = 580, consumption = 5.0, drive = Drive.AWD,
            blurb = "Hybrid SUV with actual torque and Toyota's reputation.",
            emoji = "🚙", accent = 0xFF40634A,
        ),
        Car(
            id = "skoda-kodiaq", brand = "Škoda", model = "Kodiaq TDI", year = 2021,
            priceEur = 32900, bodyType = BodyType.SUV, fuelType = FuelType.DIESEL, gearbox = Gearbox.AUTOMATIC,
            seats = 7, powerHp = 150, trunkL = 720, consumption = 5.7, drive = Drive.FWD,
            blurb = "Seven seats and a boot the size of a small flat.",
            emoji = "🚙", accent = 0xFF355070,
        ),
        Car(
            id = "tesla-model-3", brand = "Tesla", model = "Model 3 RWD", year = 2022,
            priceEur = 32900, bodyType = BodyType.SEDAN, fuelType = FuelType.EV, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 283, trunkL = 594, consumption = 14.9, drive = Drive.RWD, rangeKm = 510,
            blurb = "The EV benchmark: fast, quiet, and the trunk has a trunk.",
            emoji = "🚗", accent = 0xFFB02E2E,
        ),
        Car(
            id = "vw-id4", brand = "Volkswagen", model = "ID.4 Pro", year = 2022,
            priceEur = 33900, bodyType = BodyType.SUV, fuelType = FuelType.EV, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 204, trunkL = 543, consumption = 16.8, drive = Drive.RWD, rangeKm = 520,
            blurb = "Electric family SUV with a huge boot and calm manners.",
            emoji = "🚙", accent = 0xFF2A6041,
        ),
        Car(
            id = "hyundai-ioniq5", brand = "Hyundai", model = "Ioniq 5 58 kWh", year = 2022,
            priceEur = 34500, bodyType = BodyType.SUV, fuelType = FuelType.EV, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 218, trunkL = 520, consumption = 16.5, drive = Drive.RWD, rangeKm = 385,
            blurb = "Retro-future looks and 800V charging that shames petrol stops.",
            emoji = "🚙", accent = 0xFF4E6E8E,
        ),
        Car(
            id = "polestar-2", brand = "Polestar", model = "2 Single Motor", year = 2022,
            priceEur = 35900, bodyType = BodyType.HATCHBACK, fuelType = FuelType.EV, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 231, trunkL = 405, consumption = 16.2, drive = Drive.FWD, rangeKm = 470,
            blurb = "Scandinavian minimalism with a Google-powered cabin.",
            emoji = "🚗", accent = 0xFF35495E,
        ),
        Car(
            id = "audi-a4", brand = "Audi", model = "A4 40 TFSI", year = 2021,
            priceEur = 34500, bodyType = BodyType.SEDAN, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 204, trunkL = 424, consumption = 6.5, drive = Drive.FWD,
            blurb = "Business-class cabin, subtle speed.",
            emoji = "🚗", accent = 0xFF37474F,
        ),
        Car(
            id = "volvo-v60", brand = "Volvo", model = "V60 B4", year = 2021,
            priceEur = 33500, bodyType = BodyType.WAGON, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 197, trunkL = 529, consumption = 6.6, drive = Drive.FWD,
            blurb = "Scandi wagon: kids safe, dogs comfy, coffee hot.",
            emoji = "🚘", accent = 0xFF3C5A6E,
        ),
        Car(
            id = "hyundai-tucson", brand = "Hyundai", model = "Tucson HEV AWD", year = 2022,
            priceEur = 30500, bodyType = BodyType.SUV, fuelType = FuelType.HYBRID, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 230, trunkL = 616, consumption = 5.4, drive = Drive.AWD,
            blurb = "Bold angular styling, sensible hybrid drivetrain.",
            emoji = "🚙", accent = 0xFF52796F,
        ),
        Car(
            id = "bmw-320d", brand = "BMW", model = "320d Touring", year = 2022,
            priceEur = 38900, bodyType = BodyType.WAGON, fuelType = FuelType.DIESEL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 190, trunkL = 500, consumption = 5.1, drive = Drive.RWD,
            blurb = "Long-distance missile. 5.1 l/100 km at 160 km/h.",
            emoji = "🚘", accent = 0xFF1B4965,
        ),
        Car(
            id = "mercedes-c200", brand = "Mercedes-Benz", model = "C200", year = 2022,
            priceEur = 42900, bodyType = BodyType.SEDAN, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 204, trunkL = 455, consumption = 6.9, drive = Drive.RWD,
            blurb = "The office feeling, but the commute is the destination.",
            emoji = "🚗", accent = 0xFF2F3E46,
        ),
        Car(
            id = "ford-ranger", brand = "Ford", model = "Ranger EcoBlue", year = 2022,
            priceEur = 38500, bodyType = BodyType.PICKUP, fuelType = FuelType.DIESEL, gearbox = Gearbox.AUTOMATIC,
            seats = 5, powerHp = 213, trunkL = 1180, consumption = 8.6, drive = Drive.AWD,
            blurb = "Tow the boat, cross the mud, never apologise.",
            emoji = "🛻", accent = 0xFF6B4F3A,
        ),
        Car(
            id = "ford-mustang", brand = "Ford", model = "Mustang GT", year = 2022,
            priceEur = 54900, bodyType = BodyType.COUPE, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 4, powerHp = 449, trunkL = 334, consumption = 12.0, drive = Drive.RWD,
            blurb = "V8 thunder. Fuel stations will become your friends.",
            emoji = "🏎️", accent = 0xFF8B1E3F,
        ),
        Car(
            id = "bmw-m2", brand = "BMW", model = "M2", year = 2023,
            priceEur = 72900, bodyType = BodyType.COUPE, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 4, powerHp = 460, trunkL = 390, consumption = 9.8, drive = Drive.RWD,
            blurb = "The last analogue M car. Straight-six fireworks.",
            emoji = "🏎️", accent = 0xFF31572C,
        ),
        Car(
            id = "porsche-911", brand = "Porsche", model = "911 Carrera", year = 2022,
            priceEur = 128000, bodyType = BodyType.COUPE, fuelType = FuelType.PETROL, gearbox = Gearbox.AUTOMATIC,
            seats = 4, powerHp = 385, trunkL = 132, consumption = 10.1, drive = Drive.RWD,
            blurb = "The answer, whatever the question was.",
            emoji = "🏎️", accent = 0xFF9D0208,
        ),
    )

    fun car(id: String): Car? = cars.firstOrNull { it.id == id }
}
