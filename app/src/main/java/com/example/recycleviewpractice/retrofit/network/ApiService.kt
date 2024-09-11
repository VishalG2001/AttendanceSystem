package com.example.recycleviewpractice.retrofit.network

import com.example.recycleviewpractice.retrofit.model.ChargingStationModel
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("static/charging-stations")
    suspend fun getChargingStationData(): Response<ChargingStationModel>
}