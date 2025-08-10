package com.hereliesaz.noobwifinder.utils

object PasswordGenerator {

    fun generateAddressVariations(address: String): List<String> {
        val variations = mutableListOf<String>()
        val lower = address.toLowerCase()
        variations.add(lower)
        variations.add(address.toUpperCase())
        variations.add(address.capitalize())

        // Remove street, avenue, etc.
        val streetPattern = "\\s+(street|st|avenue|ave|boulevard|blvd|road|rd|drive|dr)$".toRegex(RegexOption.IGNORE_CASE)
        val withoutDesignation = lower.replace(streetPattern, "")
        if (withoutDesignation != lower) {
            variations.add(withoutDesignation)
            variations.add(withoutDesignation.toUpperCase())
            variations.add(withoutDesignation.capitalize())
        }

        // Just street name without number
        val streetName = lower.replaceFirst(Regex("^\\d+\\s+"), "")
        if(streetName != lower) {
            variations.add(streetName)
            variations.add(streetName.toUpperCase())
            variations.add(streetName.capitalize())
            val streetNameWithoutDesignation = streetName.replace(streetPattern, "")
            if (streetNameWithoutDesignation != streetName) {
                variations.add(streetNameWithoutDesignation)
                variations.add(streetNameWithoutDesignation.toUpperCase())
                variations.add(streetNameWithoutDesignation.capitalize())
            }
        }


        return variations.distinct()
    }

    fun generatePasswords(address: String): List<String> {
        return generateAddressVariations(address)
    }
}