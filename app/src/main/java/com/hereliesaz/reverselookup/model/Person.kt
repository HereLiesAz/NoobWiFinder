package com.hereliesaz.reverselookup.model

data class Person(
    val name: String,
    val age: Int,
    val addresses: List<Address>,
    val phones: List<Phone>,
    val emails: List<String>,
    val relatives: List<String>
)
