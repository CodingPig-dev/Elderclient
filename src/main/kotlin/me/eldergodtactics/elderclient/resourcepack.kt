package me.eldergodtactics.elderclient

import net.minecraft.client.Minecraft
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object ResourcePackUtil {
    private fun resolveRunDir(client: Minecraft): File {
        try {
            val fieldNames = listOf("runDirectory", "gameDirectory", "gameDir", "minecraftDir")
            for (name in fieldNames) {
                try {
                    val f = client::class.java.getDeclaredField(name)
                    f.isAccessible = true
                    val v = f.get(client)
                    if (v is File) return v
                } catch (_: Throwable) { }
            }
        } catch (_: Throwable) { }
        return File(System.getProperty("user.dir"))
    }
    fun applyDarkModePack(client: Minecraft): Boolean {
        try {
            val runDir = resolveRunDir(client)
            val candidatePaths = listOf(
                File(runDir, "resources/assets/elderclient/DarkMode.zip"),
                File("resources/assets/elderclient/DarkMode.zip"),
                File("resourcepacks/DarkMode.zip"),
                File("resources/DarkMode.zip")
            )

            val src: File? = candidatePaths.firstOrNull { it.exists() && it.isFile }

            // If not found on disk, try to load from classpath inside the jar (as resource)
            var tempFromStream: File? = null
            if (src == null) {
                val stream = ResourcePackUtil::class.java.classLoader.getResourceAsStream("assets/elderclient/DarkMode.zip")
                if (stream != null) {
                    val rpDir = File(runDir, "resourcepacks")
                    if (!rpDir.exists()) rpDir.mkdirs()
                    val out = File(rpDir, "DarkMode.zip")
                    FileOutputStream(out).use { fos ->
                        stream.copyTo(fos)
                    }
                    tempFromStream = out
                }
            }

            if (src == null && tempFromStream == null) return false

            val rpDir = File(runDir, "resourcepacks")
            if (!rpDir.exists()) rpDir.mkdirs()
            val dest = File(rpDir, "DarkMode.zip")
            try {
                if (src != null) src.copyTo(dest, overwrite = true)
            } catch (_: Throwable) { }

            try {
                addPackToOptions(runDir)
            } catch (_: Throwable) { }
            return applyFirstFromGameDir(client)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun applyFirstFromGameDir(client: Minecraft): Boolean {
        try {
            val runDir = try {
                val fieldNames = listOf("runDirectory", "gameDirectory", "gameDir", "minecraftDir")
                var dir: File? = null
                for (name in fieldNames) {
                    try {
                        val f = client::class.java.getDeclaredField(name)
                        f.isAccessible = true
                        val v = f.get(client)
                        if (v is File) { dir = v; break }
                    } catch (_: Throwable) {
                    }
                }
                dir ?: File(System.getProperty("user.dir"))
            } catch (_: Throwable) {
                File(System.getProperty("user.dir"))
            }
            val rpDir = File(runDir, "resourcepacks")
            if (!rpDir.exists() || !rpDir.isDirectory) return false
            val candidates = rpDir.listFiles { f ->
                try {
                    if (f.isDirectory) {
                        File(f, "pack.mcmeta").exists()
                    } else if (f.isFile && f.name.endsWith(".zip", ignoreCase = true)) {
                        try {
                            ZipFile(f).use { zip -> zip.getEntry("pack.mcmeta") != null }
                        } catch (_: Throwable) { false }
                    } else false
                } catch (_: Throwable) { false }
            }?.sortedBy { it.name.lowercase() } ?: emptyList()
            if (candidates.isEmpty()) return false
            val clientClass = client::class.java
            val reloadMethodNames = listOf("reloadResources", "reload", "refreshResources", "reloadResourcePacks")
            for (name in reloadMethodNames) {
                try {
                    val method = clientClass.methods.firstOrNull { it.name == name }
                    if (method != null) {
                        try {
                            method.isAccessible = true
                            when (method.parameterCount) {
                                0 -> method.invoke(client)
                                1 -> {
                                    val ptype = method.parameterTypes[0]
                                    when {
                                        Runnable::class.java.isAssignableFrom(ptype) -> method.invoke(client, Runnable { })
                                        java.util.concurrent.Executor::class.java.isAssignableFrom(ptype) -> method.invoke(client, java.util.concurrent.Executor { r -> r.run() })
                                        else -> method.invoke(client, null)
                                    }
                                }
                                else -> method.invoke(client)
                            }
                            return true
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            val repoField = clientClass.declaredFields.firstOrNull { it.type.name.contains("Pack", ignoreCase = true) && it.type.name.contains("Repository", ignoreCase = true) }
            if (repoField != null) {
                repoField.isAccessible = true
                val repo = repoField.get(client) ?: return false
                val repoClass = repo::class.java
                val repoMethodNames = listOf("reload", "scanPacks", "reloadPacks", "applyChanges", "scan")
                for (name in repoMethodNames) {
                    try {
                        val m = repoClass.methods.firstOrNull { it.name == name }
                        if (m != null) {
                            m.isAccessible = true
                            try {
                                if (m.parameterCount == 0) {
                                    m.invoke(repo)
                                } else {
                                    val params = arrayOfNulls<Any>(m.parameterCount)
                                    m.invoke(repo, *params)
                                }
                                return true
                            } catch (_: Throwable) {
                                // try next method
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun addPackToOptions(runDir: File) {
        try {
            val optionsFile = File(runDir, "options.txt")
            if (!optionsFile.exists()) return
            val text = optionsFile.readText()
            val regex = Regex("resourcePacks:\\s*(\\[.*?])")
            val match = regex.find(text)
            if (match != null) {
                val arrText = match.groupValues[1]
                // extract quoted items using a regex to avoid JSON dependency
                val items = Regex("\"([^\"]*)\"").findAll(arrText).map { it.groupValues[1] }.toMutableList()
                val packId = "DarkMode"
                if (!items.contains(packId)) {
                    items.add(packId)
                    val newArr = items.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"")
                    val newText = text.replaceRange(match.range, "resourcePacks:$newArr")
                    optionsFile.writeText(newText)
                }
            }
         } catch (_: Throwable) {
         }
     }
}

