package me.eldergodtactics.elderclient

import net.minecraft.client.Minecraft
import java.io.File

object ModsManager {
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

    fun modsDir(client: Minecraft): File {
        val runDir = resolveRunDir(client)
        return File(runDir, "mods")
    }

    fun disabledDir(client: Minecraft): File {
        val runDir = resolveRunDir(client)
        val d = File(runDir, "mods_disabled")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun listMods(client: Minecraft): List<File> {
        val mods = modsDir(client)
        val disabled = disabledDir(client)
        val list = mutableListOf<File>()
        if (mods.exists() && mods.isDirectory) {
            mods.listFiles()?.let { list.addAll(it) }
        }
        if (disabled.exists() && disabled.isDirectory) {
            disabled.listFiles()?.let { list.addAll(it) }
        }
        return list.filter { f ->
            try {
                val name = f.name.lowercase()
                (f.isFile && (name.endsWith(".jar") || name.endsWith(".zip"))) || f.isDirectory
            } catch (_: Throwable) { false }
        }.sortedBy { it.name.lowercase() }
    }

    fun isEnabled(client: Minecraft, file: File): Boolean {
        val mods = modsDir(client).canonicalFile
        return try {
            val fp = file.canonicalFile
            // enabled if the file is located inside the mods folder but not inside the disabled folder
            fp.canonicalPath.startsWith(mods.canonicalPath) && !fp.canonicalPath.contains("mods_disabled")
        } catch (_: Throwable) {
            false
        }
    }

    fun toggleMod(client: Minecraft, file: File): Boolean {
        try {
            val mods = modsDir(client)
            val disabled = disabledDir(client)
            val src = file
            if (!src.exists()) return false

            val success = if (isEnabled(client, src)) {
                // move to disabled folder
                val dest = File(disabled, src.name)
                if (src.renameTo(dest)) {
                    true
                } else {
                    try {
                        src.copyRecursively(dest, overwrite = true)
                        src.deleteRecursively()
                    } catch (_: Throwable) {
                        false
                    }
                }
            } else {
                // move back to mods folder
                val dest = File(mods, src.name)
                if (src.renameTo(dest)) {
                    true
                } else {
                    try {
                        src.copyRecursively(dest, overwrite = true)
                        src.deleteRecursively()
                    } catch (_: Throwable) {
                        false
                    }
                }
            }

            return success
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
