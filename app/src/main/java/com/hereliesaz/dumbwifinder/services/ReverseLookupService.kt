package com.hereliesaz.dumbwifinder.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReverseLookupService {

    suspend fun lookupPhoneNumber(address: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // The URL and selector are based on the original code.
                // This might need to be updated if the website structure changes.
                val url =
                    "https://smartbackgroundchecks.com/people?name=${address.replace(" ", "+")}"
                val doc = Jsoup.connect(url).get()
                val phoneElement = doc.select(".phone-number").first()
                phoneElement?.text()
            } catch (e: Exception) {
                // In a real app, you would log this exception.
                e.printStackTrace()
                null
            }
        }
    }
}