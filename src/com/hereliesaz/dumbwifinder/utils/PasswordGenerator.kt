package com.hereliesaz.dumbwifinder.utils

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

    fun generatePhoneNumberVariations(phoneNumber: String): List<String> {
        val variations = mutableListOf<String>()
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        if (digitsOnly.isNotEmpty()) {
            variations.add(digitsOnly)
            if (digitsOnly.length > 10) {
                variations.add(digitsOnly.substring(digitsOnly.length - 10))
            }
            if (digitsOnly.length == 10) {
                 variations.add(digitsOnly.substring(3))
            }
        }
        return variations.distinct()
    }

    fun generatePasswords(address: String, phoneNumber: String?): List<String> {
        val passwords = mutableListOf<String>()
        passwords.addAll(generateAddressVariations(address))
        phoneNumber?.let {
            passwords.addAll(generatePhoneNumberVariations(it))
        }
        return passwords.distinct()
    }
}
