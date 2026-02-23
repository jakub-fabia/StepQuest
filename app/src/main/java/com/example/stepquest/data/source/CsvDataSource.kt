package com.example.stepquest.data.source

import android.content.ContentResolver
import android.net.Uri
import com.example.stepquest.data.local.DailyStepsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

class CsvDataSource(private val contentResolver: ContentResolver) {

    suspend fun parse(uri: Uri): List<DailyStepsEntity> = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: return@withContext emptyList()
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.lineSequence()
                .mapNotNull { parseLine(it) }
                .toList()
        }
    }

    suspend fun write(uri: Uri, entities: List<DailyStepsEntity>): Unit = withContext(Dispatchers.IO) {
        val outputStream = contentResolver.openOutputStream(uri)
            ?: return@withContext
        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
            writer.write("date,steps")
            writer.newLine()
            for (entity in entities) {
                writer.write("${entity.date},${entity.steps}")
                writer.newLine()
            }
        }
    }

    private fun parseLine(line: String): DailyStepsEntity? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null

        val parts = trimmed.split(",")
        if (parts.size < 2) return null

        val dateStr = parts[0].trim()
        val stepsStr = parts[1].trim()

        if (dateStr.equals("date", ignoreCase = true)) return null

        val date = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            return null
        }

        val steps = stepsStr.toLongOrNull() ?: return null
        if (steps < 0) return null

        return DailyStepsEntity(date = date.toString(), steps = steps)
    }
}
