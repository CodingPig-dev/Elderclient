package me.eldergodtactics.elderclient

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

@Suppress("unused")
object TitleScreenManager {
    private var lastInGame = false

    @Suppress("unused")
    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            try {
                if (client.screen == null) {
                    if (!lastInGame) {
                        try {
                            client.window.setTitle("Elderclient 1.21.4 (Modded)")
                        } catch (_: Throwable) {
                        }
                    }
                    lastInGame = true
                } else {
                    lastInGame = false
                }
            } catch (_: Throwable) {
            }
        }

        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            try {
                client.window.setTitle("Elderclient 1.21.4 (Modded)")
            } catch (_: Throwable) {
            }

            if (screen::class.java.name == "net.minecraft.client.gui.screens.TitleScreen") {
                try {
                    val bw = 90
                    val bh = 20
                    val spacing = 6
                    val startX = 5
                    val topY = 5

                    val viewBtn = Button.builder(Component.literal("View Mods")) { _ ->
                        client.setScreen(ViewModsScreen())
                    }.bounds(startX, topY, bw, bh).build()

                    val modsBtn = Button.builder(Component.literal("Mods Browser")) { _ ->
                        client.setScreen(ModsBrowserScreen())
                    }.bounds(startX + bw + spacing, topY, bw, bh).build()

                    val homeBtn = Button.builder(Component.literal("Home")) { _ ->
                        ClientMain.menuOpen = false
                        client.setScreen(null)
                    }.bounds(startX + (bw + spacing) * 2, topY, bw, bh).build()

                    val capesBtn = Button.builder(Component.literal("Capes")) { _ ->
                        client.setScreen(CapeEditorScreen())
                    }.bounds(screen.width - bw - 10, topY, bw, bh).build()

                    val method = net.minecraft.client.gui.screens.Screen::class.java.declaredMethods.first { it.name == "addRenderableWidget" }
                    method.isAccessible = true
                    method.invoke(screen, viewBtn)
                    method.invoke(screen, modsBtn)
                    method.invoke(screen, homeBtn)
                    method.invoke(screen, capesBtn)


                    // Replace any Component fields containing "Minecraft" with "ElderClient" (search in class hierarchy)
                    val compClass = Component::class.java
                    var cls: Class<*>? = screen::class.java
                    while (cls != null && cls != Any::class.java) {
                        for (field in cls.declaredFields) {
                            if (compClass.isAssignableFrom(field.type)) {
                                field.isAccessible = true
                                try {
                                    val value = field.get(screen) as? Component
                                    if (value != null) {
                                        val text = try { value.string } catch (_: Throwable) { value.toString() }
                                        if (text.contains("Minecraft", ignoreCase = true)) {
                                            field.set(screen, Component.literal("ElderClient"))
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                        cls = cls.superclass
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}