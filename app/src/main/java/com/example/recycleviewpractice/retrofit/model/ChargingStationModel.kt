package com.example.recycleviewpractice.retrofit.model

data class ChargingStationModel(
    val message: String,
    val result: Result,
    val status: Int
)
data class Result(
    val stations: List<Station>,
    val total_no_of_charging_points: Int,
    val total_no_of_stations: Int
)
data class Station(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val station_status: Int,
    val type: String
)