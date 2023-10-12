package com.kproject.mangok.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

class MangaRepository {

    suspend fun getImagesFromChapter(chapter: Int): List<String> {
        return try {
            val url = "https://www.brmangas.net/ler/yotsuba-to-$chapter-online/"
            val client = HttpClient()
            val html = client.get(url).bodyAsText()
            val imageArrayJson = html.split("imageArray =\"")[1].split("}")[0] + "}"
            val jsonFixed = imageArrayJson.replace("\\", "")
            val imageData = Json.decodeFromString<Images>(jsonFixed)
            imageData.images
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun generateRandomChapter(): Int {
        return try {
            val lastChapter = getLastChapter()
            Random.nextInt(1, lastChapter + 1)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private suspend fun getLastChapter(): Int {
        val url = "https://www.brmangas.net/manga/yotsuba-to-online"
        val client = HttpClient()
        val html = client.get(url).bodyAsText()
        val chapter = html.substringAfterLast("data-cap=\"").substringBefore("\"")
        return chapter.toInt()
    }
}

@Serializable
data class Images(
    val images: List<String>
)