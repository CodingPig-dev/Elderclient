package me.eldergodtactics.elderclient

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Desktop
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Timer
import java.util.TimerTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.math.max
import kotlin.math.min
import kotlin.concurrent.thread

class ModsBrowserScreen : Screen(Component.literal("Mods Browser")) {
    private var statusMessage: String? = null
    private val results = mutableListOf<SearchResult>()
    private var isLoading = false
    private var scrollOffset = 0f
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private var searchText: String = ""
    private var searchBox: EditBox? = null
    private var searchTimer: Timer? = null
    private var searchTimerTask: TimerTask? = null
    private var searchHasFocus: Boolean = false
    private val scrollStep = 20f

    data class SearchResult(val title: String, val slug: String, val url: String)

    override fun init() {
        try { searchHasFocus = searchBox?.isFocused == true } catch (_: Throwable) { }
        try { this.clearWidgets() } catch (_: Throwable) { }
        super.init()

        addRenderableWidget(
            Button.builder(Component.literal("Available Mods")) { _ ->
            }.bounds(10, 10, 140, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Mods are from Modrinth")) { _ ->
                try {
                    val url = "https://modrinth.com/"
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI.create(url))
                    } else {
                        try { ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start() } catch (_: Throwable) { }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.bounds(160, 10, 150, 20).build()
        )

        searchBox = EditBox(font, 310, 10, 150, 20, Component.literal("Search"))
        searchBox?.setValue(searchText)
        searchBox?.setResponder { newValue ->
            searchText = newValue
            try { searchHasFocus = searchBox?.isFocused == true } catch (_: Throwable) { }
            searchTimerTask?.cancel()
            if (searchTimer == null) searchTimer = Timer(true)
            searchTimerTask = object : TimerTask() {
                override fun run() {
                    try { minecraft?.execute { this@ModsBrowserScreen.init() } } catch (_: Throwable) { }
                }
            }
            searchTimer?.schedule(searchTimerTask, 250)
        }
        try { searchBox?.setFocused(searchHasFocus) } catch (_: Throwable) { }
        searchBox?.let { addRenderableWidget(it) }

        if (results.isEmpty()) {
            val loaded = try { loadRemoteModsList() } catch (_: Throwable) { false }
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
        val contentTop = 70
        val filtered = if (searchText.isBlank()) results else results.filter {
            it.title.contains(searchText, ignoreCase = true) || it.slug.contains(searchText, ignoreCase = true)
        }
        val columns = max(1, (this.width - 20) / (blockWidth + spacing))
        val rows = (filtered.size + columns - 1) / columns
        val contentHeight = rows * (blockHeight + spacing)
        val visibleHeight = this.height - contentTop - 40
        val maxScroll = max(0, contentHeight - visibleHeight).toFloat()
        scrollOffset = min(maxScroll, max(0f, scrollOffset))
        var x = 10
        var y = contentTop - scrollOffset.toInt()

        for ((index, res) in filtered.withIndex()) {
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
            if ((index + 1) % columns == 0) {
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

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val handled = super.mouseClicked(mouseX, mouseY, button)
        try { searchHasFocus = searchBox?.isFocused == true } catch (_: Throwable) { }
        return handled
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double): Boolean {
        val oldOffset = scrollOffset
        scrollOffset -= (deltaY * scrollStep).toFloat()
        if (scrollOffset != oldOffset) {
            val contentTop = 70
            val blockW = (280 * 0.3).toInt()
            val blockH = (200 * 0.3).toInt()
            val spacing = 10
            val columns = max(1, (this.width - 20) / (blockW + spacing))
            val rows = (results.size + columns - 1) / columns
            val contentHeight = rows * (blockH + spacing)
            val visibleHeight = this.height - contentTop - 40
            val maxScroll = max(0, contentHeight - visibleHeight).toFloat()
            scrollOffset = min(maxScroll, max(0f, scrollOffset))
            try { minecraft?.execute { this@ModsBrowserScreen.init() } } catch (_: Throwable) { }
        }
        return true
    }

    private fun fetchSearchResults() {
    }

    private fun downloadProjectLatest(urlOrSlug: String): Boolean {
        return false
    }

    private fun loadRemoteModsList(): Boolean {
        val remoteUrl = "https://etme-tech.me/Elderclient/mods.json"
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
                val nameMatch = nameRegex.find(obj)
                val urlMatch = urlRegex.find(obj)
                val name = if (nameMatch != null && nameMatch.groupValues.size > 1) nameMatch.groupValues[1].trim() else continue
                val url = if (urlMatch != null && urlMatch.groupValues.size > 1) urlMatch.groupValues[1].trim() else continue
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
