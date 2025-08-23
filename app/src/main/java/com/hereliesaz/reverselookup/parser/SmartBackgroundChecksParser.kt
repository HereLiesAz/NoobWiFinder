package com.hereliesaz.reverselookup.parser

import com.hereliesaz.reverselookup.model.Address
import com.hereliesaz.reverselookup.model.Person
import com.hereliesaz.reverselookup.model.Phone
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class SmartBackgroundChecksParser {

    fun parse(html: String): Person {
        val doc: Document = Jsoup.parse(html)

        val name = doc.select("h1.h1Title").text()
        val ageString = doc.select("h2.h2Title").text().substringAfterLast("years old")
        val age = ageString.filter { it.isDigit() }.toIntOrNull() ?: 0

        val addresses = mutableListOf<Address>()
        doc.select(".propBox#target_address .card-block").forEach {
            val addressText = it.text()
            val addressParts = addressText.split(", ")
            if (addressParts.size == 3) {
                val street = addressParts[0]
                val city = addressParts[1]
                val stateAndZip = addressParts[2].split(" ")
                val state = stateAndZip[0]
                val zip = stateAndZip[1]
                addresses.add(Address(street, city, state, zip))
            }
        }

        val phones = mutableListOf<Phone>()
        doc.select(".propBox#target_phone .card-block").forEach {
            val phoneText = it.select("a.link-underline").first()?.text()
            val phoneType = it.select("small").last()?.text()
            if (phoneText != null && phoneType != null) {
                phones.add(Phone(phoneText, phoneType))
            }
        }

        val emails = mutableListOf<String>()
        doc.select(".propBox#target_email .card-block h3").forEach {
            emails.add(it.text())
        }

        val relatives = mutableListOf<String>()
        doc.select(".propBox#target_relatives .card-block a.link-underline").forEach {
            relatives.add(it.text())
        }

        return Person(
            name = name,
            age = age,
            addresses = addresses,
            phones = phones,
            emails = emails,
            relatives = relatives
        )
    }
}
