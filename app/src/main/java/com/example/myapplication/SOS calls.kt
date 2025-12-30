package com.example.myapplication

import kotlinx.serialization.Serializable

@Serializable
data class SOSCall(
    val time: String,
    val username: String,
    val x_coordinate: Double,
    val y_coordinate: Double
)

