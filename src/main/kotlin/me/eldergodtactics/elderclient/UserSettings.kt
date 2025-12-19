package me.eldergodtactics.elderclient

import java.awt.Robot
import java.awt.event.KeyEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object UserSettings {
    var showHitboxes: Boolean = false
    var showChunkBorders: Boolean = false
    var showPathfinding: Boolean = false

    private var appliedHitboxes: Boolean = false
    private var appliedChunkBorders: Boolean = false
    private var appliedPathfinding: Boolean = false
    var pendingHitboxToggle: Boolean = false

    // UI-only toggles for a normal client (no cheats)
    var fpsCounter: Boolean = false
    var showPing: Boolean = false
    var watermark: Boolean = true
    var darkTheme: Boolean = false
    var translucentGui: Boolean = false
    var potionHUD: Boolean = false
    var armorHud: Boolean = false
    var coordsHud: Boolean = false
    var directionHud: Boolean = false
    var keyHints: Boolean = false
    var particleReduction: Boolean = false
    var crosshairCustomizer: Boolean = false

    private var fullbrightEnabled: Boolean = false
    private var previousGamma: Double? = null


    fun apply(mc: Minecraft? = null) {
        try {
            val minecraft = mc ?: Minecraft.getInstance()
            val mcClass = minecraft::class.java
            val debugField = try {
                mcClass.getDeclaredField("debugRenderer")
            } catch (_: NoSuchFieldException) {
                try {
                    mcClass.getDeclaredField("debug\$debugRenderer")
                } catch (_: Throwable) {
                    null
                }
            }
            if (debugField != null) {
                debugField.isAccessible = true
                val debugInstance = debugField.get(minecraft)
                if (debugInstance != null) {
                    fun setBooleanByCandidates(candidates: Array<String>, value: Boolean): Boolean {
                        for (name in candidates) {
                            try {
                                val f = debugInstance::class.java.getDeclaredField(name)
                                f.isAccessible = true
                                if (f.type == Boolean::class.javaPrimitiveType) {
                                    f.setBoolean(debugInstance, value)
                                    return true
                                }
                                if (f.type == java.lang.Boolean::class.java) {
                                    f.set(debugInstance, java.lang.Boolean.valueOf(value))
                                    return true
                                }
                            } catch (_: Throwable) {
                            }
                        }
                        for (m in debugInstance::class.java.declaredMethods) {
                            try {
                                val nm = m.name.lowercase()
                                for (c in candidates) {
                                    if (nm.contains(c.lowercase())) {
                                        val params = m.parameterTypes
                                        if (params.size == 1 && (params[0] == Boolean::class.javaPrimitiveType || params[0] == java.lang.Boolean::class.java)) {
                                            m.isAccessible = true
                                            m.invoke(debugInstance, value)
                                            return true
                                        }
                                    }
                                }
                            } catch (_: Throwable) {
                            }
                        }
                        return false
                    }

                    val hitCandidates =
                        arrayOf("showHitboxes", "showBoundingBox", "showBoundingBoxes", "boundingBox", "hitbox")
                    val chunkCandidates =
                        arrayOf("showChunkBorders", "showChunkBorder", "chunkBorder", "renderChunkBorders")
                    val pathCandidates = arrayOf("showPathfinding", "pathfinding", "renderPathfinding")

                    try {
                        setBooleanByCandidates(hitCandidates, showHitboxes)
                    } catch (_: Throwable) {
                    }
                    if (showHitboxes != appliedHitboxes) {
                        pendingHitboxToggle = true
                        appliedHitboxes = showHitboxes
                    }
                    // nothing else: other toggles update debug renderer fields when possible
                    try {
                        setBooleanByCandidates(chunkCandidates, showChunkBorders)
                    } catch (_: Throwable) {
                    }
                    try {
                        setBooleanByCandidates(pathCandidates, showPathfinding)
                    } catch (_: Throwable) {
                    }

                    if (showChunkBorders != appliedChunkBorders) {
                        appliedChunkBorders = showChunkBorders
                    }
                    if (showPathfinding != appliedPathfinding) {
                        appliedPathfinding = showPathfinding
                    }
                }
            }
        } catch (t: Throwable) {
            println("UserSettings.apply failed: ${t.message}")
        }
    }

    private fun triggerKeyMappingKeywords(minecraft: Minecraft, keywords: Array<String>) {
        try {
            val options = try {
                val f = minecraft::class.java.getDeclaredField("options")
                f.isAccessible = true
                f.get(minecraft)
            } catch (_: Throwable) {
                null
            }
            if (options == null) return
            val optClass = options::class.java
            for (field in optClass.declaredFields) {
                try {
                    val value = field.get(options) ?: continue
                    val cls = value::class.java
                    if (cls.name.contains("KeyMapping") || cls.simpleName == "KeyMapping") {
                        try {
                            var desc: String? = null
                            for (m in cls.declaredMethods) {
                                try {
                                    if (m.parameterCount == 0) {
                                        val n = m.name.lowercase()
                                        if (n.contains("name") || n.contains("translation") || n.contains("get")) {
                                            m.isAccessible = true
                                            val r = m.invoke(value)
                                            if (r is String) desc = r
                                        }
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                            val fieldName = field.name.lowercase()
                            val descLower = desc?.lowercase() ?: ""
                            var matches = false
                            for (k in keywords) {
                                if (fieldName.contains(k) || descLower.contains(k)) {
                                    matches = true; break
                                }
                            }
                            if (matches) {
                                try {
                                    for (m in cls.declaredMethods) {
                                        val mn = m.name.lowercase()
                                        if (mn.contains("click") || mn.contains("setdown") || mn.contains("press") || mn.contains(
                                                "toggle"
                                            )
                                        ) {
                                            m.isAccessible = true
                                            if (m.parameterCount == 0) {
                                                m.invoke(value)
                                            } else if (m.parameterCount == 1 && m.parameterTypes[0] == Boolean::class.javaPrimitiveType) {
                                                m.invoke(value, true)
                                            }
                                        }
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }


    fun toggleFullbright(enabled: Boolean, mc: Minecraft? = null) {
        try {
            val minecraft = mc ?: Minecraft.getInstance()
            val optionsField = try {
                minecraft::class.java.getDeclaredField("options")
            } catch (_: Throwable) {
                null
            }
            val options = optionsField?.let { it.isAccessible = true; it.get(minecraft) }
            if (options != null) {
                val optClass = options::class.java
                var gammaField = try {
                    optClass.getDeclaredField("gamma")
                } catch (_: Throwable) {
                    null
                }
                if (gammaField == null) {
                    for (f in optClass.declaredFields) {
                        val n = f.name.lowercase()
                        if (n.contains("gamma") || n.contains("brightness")) {
                            gammaField = f; break
                        }
                    }
                }
                if (gammaField != null) {
                    gammaField.isAccessible = true
                    try {
                        val cur = gammaField.get(options)
                        if (cur is Number && previousGamma == null) previousGamma = cur.toDouble()
                        if (cur is Float) gammaField.setFloat(
                            options,
                            if (enabled) Float.MAX_VALUE else (previousGamma?.toFloat() ?: 1.0f)
                        )
                        else if (cur is Double) gammaField.setDouble(
                            options,
                            if (enabled) 10000.0 else (previousGamma ?: 1.0)
                        )
                    } catch (_: Throwable) {
                    }
                    fullbrightEnabled = enabled
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun isFullbrightEnabled(): Boolean = fullbrightEnabled

    class SettingsScreen(private val parent: Screen?) : Screen(Component.literal("ElderClient Settings")) {
        private var hitboxButton: Button? = null
        private var chunkButton: Button? = null
        private var pathButton: Button? = null
        private var fullbrightButton: Button? = null
        private var fpsButton: Button? = null
        private var pingButton: Button? = null
        private var watermarkButton: Button? = null
        private var darkThemeButton: Button? = null
        private var translucentGuiButton: Button? = null
        private var potionHudButton: Button? = null
        private var armorHudButton: Button? = null
        private var coordsButton: Button? = null
        private var directionButton: Button? = null
        private var keyHintsButton: Button? = null
        private var particleReductionButton: Button? = null
        private var crosshairButton: Button? = null

        override fun init() {
            super.init()
            val bw = 140
            val bh = 20
            val x = 10
            var y = 30

            hitboxButton = addRenderableWidget(
                Button.builder(Component.literal("Hitboxes: ${if (UserSettings.showHitboxes) "ON" else "OFF"}")) { _ ->
                    UserSettings.showHitboxes = !UserSettings.showHitboxes
                    UserSettings.apply(minecraft)
                    hitboxButton?.message =
                        Component.literal("Hitboxes: ${if (UserSettings.showHitboxes) "ON" else "OFF"}")
                }.bounds(x, y, bw, bh).build()
            )
            y += bh + 6

            chunkButton = addRenderableWidget(
                Button.builder(Component.literal("Chunk Borders: ${if (UserSettings.showChunkBorders) "ON" else "OFF"}")) { _ ->
                    UserSettings.showChunkBorders = !UserSettings.showChunkBorders
                    UserSettings.apply(minecraft)
                    chunkButton?.message =
                        Component.literal("Chunk Borders: ${if (UserSettings.showChunkBorders) "ON" else "OFF"}")
                }.bounds(x, y, bw, bh).build()
            )
            y += bh + 6

            pathButton = addRenderableWidget(
                Button.builder(Component.literal("Pathfinding: ${if (UserSettings.showPathfinding) "ON" else "OFF"}")) { _ ->
                    UserSettings.showPathfinding = !UserSettings.showPathfinding
                    UserSettings.apply(minecraft)
                    pathButton?.message =
                        Component.literal("Pathfinding: ${if (UserSettings.showPathfinding) "ON" else "OFF"}")
                }.bounds(x, y, bw, bh).build()
            )
            y += bh + 10

            fullbrightButton = addRenderableWidget(
                Button.builder(Component.literal("Fullbright: ${if (UserSettings.isFullbrightEnabled()) "ON" else "OFF"}")) { _ ->
                    val new = !UserSettings.isFullbrightEnabled()
                    UserSettings.toggleFullbright(new, minecraft)
                    fullbrightButton?.message =
                        Component.literal("Fullbright: ${if (UserSettings.isFullbrightEnabled()) "ON" else "OFF"}")
                }.bounds(x, y, bw, bh).build()
            )
            y += bh + 10
            var cx = x
            var cy = y
            val colW = bw
            val gap = 6

            fun nextColumn() {
                if (cx == x) cx = x + colW + 10 else cx = x
                cy = y
            }

            fpsButton = addRenderableWidget(
                Button.builder(Component.literal("FPS Counter: ${if (UserSettings.fpsCounter) "ON" else "OFF"}")) { _ ->
                    UserSettings.fpsCounter = !UserSettings.fpsCounter
                    fpsButton?.message =
                        Component.literal("FPS Counter: ${if (UserSettings.fpsCounter) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            pingButton = addRenderableWidget(
                Button.builder(Component.literal("Ping: ${if (UserSettings.showPing) "ON" else "OFF"}")) { _ ->
                    UserSettings.showPing = !UserSettings.showPing
                    pingButton?.message = Component.literal("Ping: ${if (UserSettings.showPing) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            watermarkButton = addRenderableWidget(
                Button.builder(Component.literal("Watermark: ${if (UserSettings.watermark) "ON" else "OFF"}")) { _ ->
                    UserSettings.watermark = !UserSettings.watermark
                    watermarkButton?.message =
                        Component.literal("Watermark: ${if (UserSettings.watermark) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            darkThemeButton = addRenderableWidget(
                Button.builder(Component.literal("Dark Theme: ${if (UserSettings.darkTheme) "ON" else "OFF"}")) { _ ->
                    UserSettings.darkTheme = !UserSettings.darkTheme
                    darkThemeButton?.message =
                        Component.literal("Dark Theme: ${if (UserSettings.darkTheme) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            translucentGuiButton = addRenderableWidget(
                Button.builder(Component.literal("Translucent GUI: ${if (UserSettings.translucentGui) "ON" else "OFF"}")) { _ ->
                    UserSettings.translucentGui = !UserSettings.translucentGui
                    translucentGuiButton?.message =
                        Component.literal("Translucent GUI: ${if (UserSettings.translucentGui) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            potionHudButton = addRenderableWidget(
                Button.builder(Component.literal("Potion HUD: ${if (UserSettings.potionHUD) "ON" else "OFF"}")) { _ ->
                    UserSettings.potionHUD = !UserSettings.potionHUD
                    potionHudButton?.message =
                        Component.literal("Potion HUD: ${if (UserSettings.potionHUD) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            armorHudButton = addRenderableWidget(
                Button.builder(Component.literal("Armor HUD: ${if (UserSettings.armorHud) "ON" else "OFF"}")) { _ ->
                    UserSettings.armorHud = !UserSettings.armorHud
                    armorHudButton?.message =
                        Component.literal("Armor HUD: ${if (UserSettings.armorHud) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            coordsButton = addRenderableWidget(
                Button.builder(Component.literal("Coordinates: ${if (UserSettings.coordsHud) "ON" else "OFF"}")) { _ ->
                    UserSettings.coordsHud = !UserSettings.coordsHud
                    coordsButton?.message =
                        Component.literal("Coordinates: ${if (UserSettings.coordsHud) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            directionButton = addRenderableWidget(
                Button.builder(Component.literal("Direction HUD: ${if (UserSettings.directionHud) "ON" else "OFF"}")) { _ ->
                    UserSettings.directionHud = !UserSettings.directionHud
                    directionButton?.message =
                        Component.literal("Direction HUD: ${if (UserSettings.directionHud) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            keyHintsButton = addRenderableWidget(
                Button.builder(Component.literal("Key Hints: ${if (UserSettings.keyHints) "ON" else "OFF"}")) { _ ->
                    UserSettings.keyHints = !UserSettings.keyHints
                    keyHintsButton?.message =
                        Component.literal("Key Hints: ${if (UserSettings.keyHints) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            particleReductionButton = addRenderableWidget(
                Button.builder(Component.literal("Particle Reduction: ${if (UserSettings.particleReduction) "ON" else "OFF"}")) { _ ->
                    UserSettings.particleReduction = !UserSettings.particleReduction
                    particleReductionButton?.message =
                        Component.literal("Particle Reduction: ${if (UserSettings.particleReduction) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            crosshairButton = addRenderableWidget(
                Button.builder(Component.literal("Crosshair Customizer: ${if (UserSettings.crosshairCustomizer) "ON" else "OFF"}")) { _ ->
                    UserSettings.crosshairCustomizer = !UserSettings.crosshairCustomizer
                    crosshairButton?.message =
                        Component.literal("Crosshair Customizer: ${if (UserSettings.crosshairCustomizer) "ON" else "OFF"}")
                }.bounds(cx, cy, bw, bh).build()
            )
            cy += bh + gap

            val doneY = maxOf(cy + 10, y + 220)
            addRenderableWidget(
                Button.builder(Component.literal("Done")) { _ ->
                    minecraft?.setScreen(parent)
                }.bounds(x, doneY, bw, bh).build()
            )
        }

        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            guiGraphics.fill(0, 0, width, height, 0xBB000000.toInt())
            guiGraphics.drawString(font, Component.literal("ElderClient Settings"), 10, 10, 0xFFFFFF)
            super.render(guiGraphics, mouseX, mouseY, partialTicks)
        }
    }
}
