package me.eldergodtactics.elderclient

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Desktop
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.concurrent.thread

class ModsBrowserScreen : Screen(Component.literal("Mods Browser")) {
    private var statusMessage: String? = null
    private val results = mutableListOf<SearchResult>()
    private var isLoading = false
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    data class SearchResult(val title: String, val slug: String, val url: String)

    override fun init() {
        super.init()
        addRenderableWidget(
            Button.builder(Component.literal("Available Mods")) { _ -> }
                .bounds(10, 10, 140, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("After downloading please restart")) { _ -> }
                .bounds(160, 10, 240, 20).build()
        )
        if (results.isEmpty()) {
            val remoteUrl = "https://etme-tech.me/Elderclient/mods.json"
            val loaded = try { loadRemoteModsList(remoteUrl) } catch (_: Throwable) { false }
            if (loaded) {
                statusMessage = "Loaded ${results.size} mods from remote list"
            } else {
                val localLoaded = try { loadLocalModsList() } catch (_: Throwable) { false }
                if (localLoaded) {
                    statusMessage = "Loaded ${results.size} mods from local list"
                } else {
                    if (!isLoading) {
                        isLoading = true
                        statusMessage = "Loading search results..."
                        thread {
                            try {
                                fetchSearchResults()
                                statusMessage = "Loaded ${results.size} results"
                            } catch (e: Exception) {
                                e.printStackTrace()
                                statusMessage = "Failed to load results: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                            try {
                                minecraft?.execute { this@ModsBrowserScreen.init() }
                            } catch (_: Throwable) { }
                        }
                    }
                }
            }
        }

        val blockWidth = (280 * 0.3).toInt()
        val blockHeight = (200 * 0.3).toInt()
        val spacing = 10
        var x = 10
        var y = 40

        for (res in results) {
            val nameHeight = (blockHeight * 0.75).toInt()
            val downloadHeight = blockHeight - nameHeight
            addRenderableWidget(
                Button.builder(Component.literal(res.title)) { _ -> }
                    .bounds(x, y, blockWidth, nameHeight)
                    .build()
            )
            addRenderableWidget(
                Button.builder(Component.literal("Download")) { _ ->
                    statusMessage = "Starting download: ${res.title}"
                    thread {
                        try {
                            val ok = downloadProjectLatest(res.url)
                            if (ok) {
                                minecraft?.execute {
                                    minecraft?.setScreen(
                                        ConfirmScreen(
                                            { yes ->
                                                if (yes) {
                                                    minecraft?.stop()
                                                } else {
                                                    statusMessage = "Restart cancelled"
                                                    minecraft?.setScreen(this@ModsBrowserScreen)
                                                }
                                            },
                                            Component.literal("Restart required"),
                                            Component.literal("Do you want to restart the game now?")
                                        )
                                    )
                                }
                            } else {
                                statusMessage = "Download failed for ${res.title}"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            statusMessage = "Error: ${e.message}"
                        }
                    }
                }.bounds(x, y + nameHeight, blockWidth, downloadHeight).build()
            )
            x += blockWidth + spacing
            if (x + blockWidth > this.width) {
                x = 10
                y += blockHeight + spacing
            }
        }

        addRenderableWidget(
            Button.builder(Component.literal("Back (R)")) { _ ->
                minecraft?.setScreen(OverlayScreen())
            }.bounds(10, height - 30, 80, 20).build()
        )
    }



    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0, width, height, 0xBB000000.toInt())
        guiGraphics.drawCenteredString(font, "Mods Browser (CurseForge search)", width / 2, 6, 0xFFFFFF)
        statusMessage?.let {
            guiGraphics.drawCenteredString(font, it, width / 2, height - 18, 0xFFDD55)
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == GLFW.GLFW_KEY_R) {
            minecraft?.setScreen(OverlayScreen())
            return true
        }
        return super.keyPressed(i, j, k)
    }

    private fun fetchSearchResults() {
        val url = "https://www.curseforge.com/minecraft/search?page=1&pageSize=20&sortBy=relevancy&version=1.21.4&gameVersionTypeId=4"
        try {
            val req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.curseforge.com/")
                .GET()
                .build()

            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() == 403) {
                val key = readApiKey()
                if (key != null) {
                    fetchViaApi(key)
                    return
                }
                throw java.io.IOException("Server returned HTTP 403. CurseForge blocks direct scraping; provide an API key via CURSEFORGE_API_KEY env or run/curseforge_key.txt to use the CurseForge API as a fallback.")
            }
            parseSearchHtml(resp.body())
        } catch (e: java.io.IOException) {
            val key = readApiKey()
            if (key != null) {
                try {
                    fetchViaApi(key)
                    return
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            throw e
        }
    }

    private fun readApiKey(): String? {
        try {
            val env = System.getenv("CURSEFORGE_API_KEY")
            if (!env.isNullOrBlank()) return env
        } catch (_: Throwable) { }
        try {
            val runDir = File(System.getProperty("user.dir"), "run")
            val f1 = File(runDir, "curseforge_key.txt")
            if (f1.exists()) return f1.readText().trim()
            val f2 = File(runDir, "curseforge_api_key.txt")
            if (f2.exists()) return f2.readText().trim()
        } catch (_: Throwable) { }
        return null
    }
    private fun fetchViaApi(apiKey: String) {
        val q = "https://api.curseforge.com/v1/mods/search?gameId=432&gameVersion=1.21.4&pageSize=20"
        val req = HttpRequest.newBuilder()
            .uri(URI.create(q))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "ElderClient/1.0")
            .header("Accept", "application/json")
            .header("x-api-key", apiKey)
            .GET()
            .build()

        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() != 200) throw java.io.IOException("CurseForge API returned HTTP ${resp.statusCode()}")
        parseApiJson(resp.body())
    }

    private fun parseApiJson(json: String) {
        results.clear()
        // very small parser: pick up slug and name values in order
        val slugRegex = Regex("\"slug\"\\s*:\\s*\"([^\\\"]+)\"")
        val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\\\"]+)\"")
        val slugs = slugRegex.findAll(json).map { it.groupValues[1] }.toList()
        val names = nameRegex.findAll(json).map { it.groupValues[1] }.toList()
        val count = minOf(slugs.size, names.size)
        for (i in 0 until count) {
            val slug = slugs[i]
            val name = names[i]
            val full = "https://www.curseforge.com/minecraft/mc-mods/$slug"
            if (results.none { it.slug == slug }) results.add(SearchResult(name, slug, full))
        }
    }

    private fun parseSearchHtml(html: String) {
        results.clear()
        val regex = Regex("<a\\s+href=\"(/minecraft/mc-mods/([\\w-]+))\"[^>]*>([^<]+)</a>", RegexOption.IGNORE_CASE)
        for (m in regex.findAll(html)) {
            try {
                val path = m.groupValues[1]
                val slug = m.groupValues[2]
                val title = m.groupValues[3].trim()
                val full = "https://www.curseforge.com$path"
                if (results.none { it.slug == slug }) {
                    results.add(SearchResult(title, slug, full))
                }
            } catch (_: Throwable) {}
        }
    }

    private fun downloadProjectLatest(urlOrSlug: String): Boolean {
        val client = minecraft ?: return false
        try {
            val directUrl = if (urlOrSlug.startsWith("http://") || urlOrSlug.startsWith("https://")) {
                urlOrSlug
            } else {
                "https://www.curseforge.com/minecraft/mc-mods/$urlOrSlug/download"
            }

            val req = HttpRequest.newBuilder()
                .uri(URI.create(directUrl))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build()

            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream())
            if (resp.statusCode() !in 200..299) throw java.io.IOException("Download returned HTTP ${resp.statusCode()}")

            val finalUri = resp.uri()
            val cd = resp.headers().firstValue("content-disposition").orElse(null)
            val fileName = when {
                cd != null && cd.contains("filename=") -> cd.substringAfter("filename=").trim('"')
                else -> {
                    val path = finalUri.path
                    path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "downloaded-mod.jar"
                }
            }

            BufferedInputStream(resp.body()).use { input ->
                val modsDir = ModsManager.modsDir(client)
                if (!modsDir.exists()) modsDir.mkdirs()
                val outFile = File(modsDir, fileName)
                FileOutputStream(outFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (true) {
                        read = input.read(buffer)
                        if (read <= 0) break
                        fos.write(buffer, 0, read)
                    }
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun tryFetchHtml(urlStr: String): String {
        val req = HttpRequest.newBuilder()
            .uri(URI.create(urlStr))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
            .header("Accept", "text/html")
            .header("Referer", "https://www.curseforge.com/")
            .GET()
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() == 403) throw java.io.IOException("HTTP 403: access denied by server (embedding/scraping blocked)")
        return resp.body()
    }

    private fun loadRemoteModsList(remoteUrl: String): Boolean {
        try {
            try {
                val req = HttpRequest.newBuilder()
                    .uri(URI.create(remoteUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build()
                val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
                if (resp.statusCode() == 200) {
                    return parseModsJsonText(resp.body())
                }
            } catch (_: Exception) {
            }

            return loadLocalModsList()
        } catch (t: Throwable) {
            t.printStackTrace()
            return false
        }
    }

    private fun loadLocalModsList(): Boolean {
        try {
            val stream = this::class.java.classLoader.getResourceAsStream("mods_list.json") ?: return false
            val text = stream.bufferedReader().use { it.readText() }
            return parseModsJsonText(text)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun parseModsJsonText(text: String): Boolean {
        try {
            results.clear()
            val objRegex = Regex("\\{[^}]*\\}")
            val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\\\"]+)\"")
            val urlRegex = Regex("\"url\"\\s*:\\s*\"([^\\\"]+)\"")
            for (m in objRegex.findAll(text)) {
                val obj = m.value
                val name = nameRegex.find(obj)?.groupValues?.get(1)?.trim() ?: continue
                val url = urlRegex.find(obj)?.groupValues?.get(1)?.trim() ?: continue
                val slug = if (url.startsWith("http://") || url.startsWith("https://")) url.substringAfterLast('/').substringBefore('?') else url
                results.add(SearchResult(name, slug, url))
            }
            return results.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
