package com.example.trzezwadroga.data.entity

data class HungerDataPoint(
    val dayDate: Long,
    val maxHunger: Int,
    val note: String = "",
    val mood: String = ""
)
