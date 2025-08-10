package com.hereliesaz.noobwifinder.utils

import java.util.Locale
import java.util.Locale.getDefault

object PasswordGenerator {

    fun generateAddressVariations(address: String): List<String> {
        val variations = mutableListOf<String>()
        val lower = address.lowercase(getDefault())
        variations.add(lower)
        variations.add(address.uppercase(getDefault()))
        variations.add(address.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })

        // Remove street, avenue, etc.
        val streetPattern = "\\s+(street|st|avenue|ave|boulevard|blvd|road|rd|drive|dr)$".toRegex(RegexOption.IGNORE_CASE)
        val withoutDesignation = lower.replace(streetPattern, "")
        if (withoutDesignation != lower) {
            variations.add(withoutDesignation)
            variations.add(withoutDesignation.uppercase(getDefault()))
            variations.add(withoutDesignation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                Locale.ROOT
            ) else it.toString() })
        }

        // Just street name without number
        val streetName = lower.replaceFirst(Regex("^\\d+\\s+"), "")
        if(streetName != lower) {
            variations.add(streetName)
            variations.add(streetName.uppercase(getDefault()))
            variations.add(streetName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
            val streetNameWithoutDesignation = streetName.replace(streetPattern, "")
            if (streetNameWithoutDesignation != streetName) {
                variations.add(streetNameWithoutDesignation)
                variations.add(streetNameWithoutDesignation.uppercase(getDefault()))
                variations.add(streetNameWithoutDesignation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT
                ) else it.toString() })
            }
        }


        return variations.distinct()
    }

    fun generatePasswords(address: String): List<String> {
        return generateAddressVariations(address)
    }
}