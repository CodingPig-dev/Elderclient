package me.eldergodtactics.elderclient

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import com.mojang.blaze3d.platform.NativeImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

object CapeManager {
    @Volatile
    var capeFilePath: String = ""

    @Volatile
    var capeLoaded: Boolean = false

    @Volatile
    @JvmField
    var capeTextureLocation: ResourceLocation? = null

    @Volatile
    var dynamicTexture: DynamicTexture? = null
    private val capesDir: File = File(System.getProperty("user.home"), "Eldercapes")
    private val currentFile: File = File(capesDir, "current.txt")
    private val previewCache: MutableMap<String, PreviewInfo> = ConcurrentHashMap()

    private val listeners: CopyOnWriteArrayList<(String?) -> Unit> = CopyOnWriteArrayList()

    data class PreviewInfo(val location: ResourceLocation, val width: Int, val height: Int, val texture: DynamicTexture)

    fun addOnCapeChangedListener(listener: (String?) -> Unit) {
        listeners.add(listener)
    }

    fun removeOnCapeChangedListener(listener: (String?) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyCapeChanged(path: String?) {
        for (l in listeners) {
            try {
                l(path)
            } catch (_: Throwable) {
            }
        }
    }

    init {
        if (!capesDir.exists()) {
            try {
                capesDir.mkdirs()
            } catch (_: Throwable) {
            }
        }
        try {
            val saved = readCurrentFile()
            if (!saved.isNullOrEmpty()) {
                try {
                    loadCapeFromFile(saved)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun readCurrentFile(): String? {
        try {
            if (!currentFile.exists()) return null
            return currentFile.readText().trim().ifEmpty { null }
        } catch (_: Throwable) {
            return null
        }
    }

    private fun writeCurrentFile(path: String?) {
        try {
            if (path == null) {
                if (currentFile.exists()) currentFile.delete()
                return
            }
            currentFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            FileOutputStream(currentFile).use { fos ->
                fos.write(path.toByteArray())
            }
        } catch (_: Throwable) {
        }
    }

    fun listCapes(): List<File> {
        return try {
            capesDir.listFiles { f -> f.isFile && (f.name.endsWith(".png", true) || f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true)) }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun getPreviewInfo(path: String): PreviewInfo? {
        try {
            val file = File(path)
            if (!file.exists()) return null

            previewCache[path]?.let { return it }

            FileInputStream(file).use { fis ->
                val img = NativeImage.read(fis) ?: return null
                val w = img.width
                val h = img.height
                val dyn = DynamicTexture(img)
                val safeName = "preview_${path.hashCode().toString().replace('-', 'n')}"
                val id = ResourceLocation.tryParse("elderclient:$safeName") ?: return null
                Minecraft.getInstance().textureManager.register(id, dyn)
                val info = PreviewInfo(id, w, h, dyn)
                previewCache[path] = info
                return info
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    // backward-compatible helper
    fun getPreviewLocationForPath(path: String): ResourceLocation? {
        return try {
            getPreviewInfo(path)?.location
        } catch (_: Throwable) {
            null
        }
    }

    fun loadCapeFromFile(pathOriginal: String) {
        try {
            val path = pathOriginal
            val file = File(path)
            val resolvedFile = if (file.exists()) {
                file
            } else {
                val png = File(path + ".png")
                val jpg = File(path + ".jpg")
                val jpeg = File(path + ".jpeg")
                when {
                    png.exists() -> png
                    jpg.exists() -> jpg
                    jpeg.exists() -> jpeg
                    else -> file
                }
            }

            if (!resolvedFile.exists()) return

            FileInputStream(resolvedFile).use { fis ->
                val img = NativeImage.read(fis) ?: return
                val dyn = DynamicTexture(img)
                val safeName = "cape_${resolvedFile.name.hashCode().toString().replace('-', 'n')}"
                val id = ResourceLocation.tryParse("elderclient:$safeName")
                if (id != null) {
                    Minecraft.getInstance().textureManager.register(id, dyn)
                }
                capeFilePath = resolvedFile.absolutePath
                capeLoaded = true
                capeTextureLocation = id
                dynamicTexture = dyn
            }

            try {
                val player = Minecraft.getInstance().player
                if (player != null) {
                    val rlClass = ResourceLocation::class.java
                    for (field in player::class.java.declaredFields) {
                        try {
                            if (field.type == rlClass) {
                                field.isAccessible = true
                                val name = field.name.lowercase()
                                if (name.contains("cape") || name.contains("cloak") || name.contains("cloaktexture") || name.contains("cloak_texture")) {
                                    field.set(player, capeTextureLocation)
                                }
                            }
                            if (field.type == java.lang.Boolean.TYPE) {
                                field.isAccessible = true
                                val name = field.name.lowercase()
                                if (name.contains("cape") || name.contains("cloak")) {
                                    field.setBoolean(player, true)
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            println("Registered cape texture: ${capeFilePath} -> ${capeTextureLocation}")

            notifyCapeChanged(capeFilePath)

            try {
                writeCurrentFile(capeFilePath)
            } catch (_: Throwable) {
            }

            try {
                CapeFeatureRenderer.applyNow()
            } catch (_: Throwable) {
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun loadCapeFromSpec(spec: String) {
        try {
            val trimmed = spec.trim()
            if (trimmed.isEmpty()) return
            val isNumeric = trimmed.all { it.isDigit() }
            if (isNumeric) {
                val url = "https://skinmc.net/capes/$trimmed/download"
                val dest = File(capesDir, "$trimmed.png")
                if (downloadToFile(url, dest)) {
                    loadCapeFromFile(dest.absolutePath)
                }
                return
            }
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val urlObj = URI(trimmed).toURL()
                val pathSeg = urlObj.path.substringAfterLast('/').ifEmpty { "${System.currentTimeMillis()}.png" }
                val dest = File(capesDir, pathSeg)
                if (downloadToFile(trimmed, dest)) {
                    loadCapeFromFile(dest.absolutePath)
                }
                return
            }
            val local = File(trimmed)
            if (local.exists()) {
                loadCapeFromFile(local.absolutePath)
            } else {
                val rel = File(capesDir, trimmed)
                if (rel.exists()) loadCapeFromFile(rel.absolutePath)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun downloadToFile(urlStr: String, dest: File): Boolean {
        try {
            val url = URI(urlStr).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            conn.connect()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.inputStream.close()
                return false
            }
            dest.parentFile?.let { if (!it.exists()) it.mkdirs() }

            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            return true
        } catch (e: Throwable) {
            e.printStackTrace()
            return false
        }
    }
}
