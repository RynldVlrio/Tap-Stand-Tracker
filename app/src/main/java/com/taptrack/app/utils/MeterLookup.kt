package com.taptrack.app.utils

import android.content.Context

object MeterLookup {
    private var customers: Map<String, String> = emptyMap()

    fun init(context: Context) {
        if (customers.isNotEmpty()) return
        try {
            val lines = context.assets
                .open("barugo_customer_meterno.csv")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }

            if (lines.isEmpty()) return

            val header = parseCsvLine(lines[0])
            val meterCol = header.indexOfFirst { it.equals("MeterNo", ignoreCase = true) }
            val nameCol = header.indexOfFirst { it.equals("CustomerName", ignoreCase = true) }
            if (meterCol < 0 || nameCol < 0) return

            customers = lines.drop(1).mapNotNull { line ->
                val cols = parseCsvLine(line)
                val meter = cols.getOrNull(meterCol)?.trim() ?: return@mapNotNull null
                val name = cols.getOrNull(nameCol)?.trim() ?: return@mapNotNull null
                if (meter.isBlank()) null else meter to name
            }.toMap()
        } catch (_: Exception) {
            // CSV missing or unreadable — lookup returns null for all queries
        }
    }

    fun lookup(meterNumber: String): String? = customers[meterNumber.trim()]

    val count: Int get() = customers.size

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}
