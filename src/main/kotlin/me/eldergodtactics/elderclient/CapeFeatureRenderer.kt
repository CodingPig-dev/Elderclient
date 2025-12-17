package me.eldergodtactics.elderclient

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

object CapeFeatureRenderer {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        try {
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                try {
                    if (!CapeManager.capeLoaded) return@register

                    val world = client.level ?: return@register

                    val players = try { world.players() } catch (_: Throwable) { emptyList() }

                    for (player in players) {
                        try {
                            val applied = applyToPlayer(player)
                            if (applied > 0) println("Applied cape to player ${getPlayerName(player)}: $applied fields/methods updated")
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun applyNow() {
        try {
            if (!CapeManager.capeLoaded) return
            val mc = Minecraft.getInstance()
            val player = mc.player
            if (player != null) {
                val applied = applyToPlayer(player)
                if (applied > 0) println("Applied cape to local player ${getPlayerName(player)}: $applied fields/methods updated")
            }
            val world = mc.level
            if (world != null) {
                for (p in try { world.players() } catch (_: Throwable) { emptyList() }) {
                    try {
                        val applied = applyToPlayer(p)
                        if (applied > 0) println("Applied cape to player ${getPlayerName(p)}: $applied fields/methods updated")
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun applyToPlayer(player: Any): Int {
        var count = 0
        try {
            val texture = CapeManager.capeTextureLocation ?: return 0
            val rlClass = ResourceLocation::class.java

            var clazz: Class<*>? = player::class.java
            while (clazz != null && clazz != Any::class.java) {
                try {
                    for (field in clazz.declaredFields) {
                        try {
                            field.isAccessible = true
                            val name = field.name.lowercase()
                            val t = field.type
                            if (rlClass.isAssignableFrom(t)) {
                                if (name.contains("cape") || name.contains("cloak") || name.contains("cloaktexture") || name.contains("cloak_texture")) {
                                    field.set(player, texture)
                                    println("Set resource field '${clazz.name}.${field.name}' for player ${getPlayerName(player)} -> $texture")
                                    count++
                                }
                            } else if (t == java.lang.Boolean.TYPE) {
                                if (name.contains("cape") || name.contains("cloak") || name.contains("hascloak")) {
                                    field.setBoolean(player, true)
                                    println("Set boolean field '${clazz.name}.${field.name}'=true for player ${getPlayerName(player)}")
                                    count++
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }

                    for (method in clazz.declaredMethods) {
                        try {
                            method.isAccessible = true
                            val mname = method.name.lowercase()
                            val params = method.parameterTypes
                            if (params.size == 1) {
                                val p = params[0]
                                if (rlClass.isAssignableFrom(p) && (mname.contains("cape") || mname.contains("cloak"))) {
                                    method.invoke(player, texture)
                                    println("Invoked method '${clazz.name}.${method.name}' with ResourceLocation for player ${getPlayerName(player)}")
                                    count++
                                } else if (p == java.lang.String::class.java && (mname.contains("cape") || mname.contains("cloak"))) {
                                    method.invoke(player, texture.toString())
                                    println("Invoked method '${clazz.name}.${method.name}' with String for player ${getPlayerName(player)}")
                                    count++
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
                clazz = clazz.superclass
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return count
    }

    private fun getPlayerName(player: Any): String {
        return try {
            val m = player::class.java.getMethod("getName")
            val comp = m.invoke(player)
            try {
                val s = comp::class.java.getMethod("getString").invoke(comp) as? String
                s ?: comp.toString()
            } catch (_: Throwable) {
                comp.toString()
            }
        } catch (_: Throwable) {
            try {
                val f = player::class.java.getField("name")
                f.get(player)?.toString() ?: "<unknown>"
            } catch (_: Throwable) {
                "<unknown>"
            }
        }
    }
}
