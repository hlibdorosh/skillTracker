package com.example.startwatch

data class Task(
    val id: Int,
    val text: String,
    var isDone: Boolean = false
)
