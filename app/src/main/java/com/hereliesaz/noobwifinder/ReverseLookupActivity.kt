package com.hereliesaz.noobwifinder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.hereliesaz.noobwifinder.data.Person
import com.hereliesaz.noobwifinder.parser.SmartBackgroundChecksParser
import java.io.InputStream

class ReverseLookupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reverse_lookup)
        parseHtmlFiles()
    }

    private fun parseHtmlFiles() {
        val assetManager = assets
        try {
            val files = assetManager.list("")
            val htmlFiles = files?.filter { it.endsWith(".html") }

            if (htmlFiles != null) {
                for (fileName in htmlFiles) {
                    try {
                        val inputStream: InputStream = assetManager.open(fileName)
                        val html = inputStream.bufferedReader().use { it.readText() }
                        val parser = SmartBackgroundChecksParser()
                        val person = parser.parse(html)
                        Log.d("ReverseLookupActivity", "Parsed person: $person")
                    } catch (e: Exception) {
                        Log.e("ReverseLookupActivity", "Error parsing file: $fileName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReverseLookupActivity", "Error listing assets", e)
        }
    }
}
