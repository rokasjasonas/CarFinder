package lt.carfinder.util

import lt.carfinder.model.BodyType
import lt.carfinder.model.Drive
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Usage

fun Usage.usageLabel(): String = when (this) {
    Usage.COMMUTE -> "commuting"
    Usage.FAMILY -> "family life"
    Usage.SPORT -> "spirited driving"
    Usage.ADVENTURE -> "adventure trips"
    Usage.CITY -> "city life"
}

fun Usage.shortLabel(): String = when (this) {
    Usage.COMMUTE -> "🛣️ Commute"
    Usage.FAMILY -> "👨‍👩‍👧‍👦 Family"
    Usage.SPORT -> "🏁 Sport"
    Usage.ADVENTURE -> "⛰️ Adventure"
    Usage.CITY -> "🏙️ City"
}

fun BodyType.label(): String = when (this) {
    BodyType.HATCHBACK -> "Hatchback"
    BodyType.SEDAN -> "Sedan"
    BodyType.WAGON -> "Wagon"
    BodyType.SUV -> "SUV"
    BodyType.COUPE -> "Coupé"
    BodyType.CONVERTIBLE -> "Convertible"
    BodyType.PICKUP -> "Pickup"
    BodyType.VAN -> "MPV / Van"
}

fun FuelType.label(): String = when (this) {
    FuelType.PETROL -> "Petrol"
    FuelType.DIESEL -> "Diesel"
    FuelType.HYBRID -> "Hybrid"
    FuelType.EV -> "Electric"
}

fun Gearbox.label(): String = when (this) {
    Gearbox.MANUAL -> "Manual"
    Gearbox.AUTOMATIC -> "Automatic"
}

fun Drive.label(): String = when (this) {
    Drive.FWD -> "FWD"
    Drive.RWD -> "RWD"
    Drive.AWD -> "AWD"
}
