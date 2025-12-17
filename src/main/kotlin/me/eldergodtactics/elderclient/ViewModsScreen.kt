package me.eldergodtactics.elderclient

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class ViewModsScreen : Screen(Component.literal("View Mods")) {
    private var statusMessage: String? = null

    override fun init() {
        super.init()
        // layout
        val bw = 90
        val bh = 20
        val spacing = 6
        var y = 10

        val client = this.minecraft
        val mods = client?.let { ModsManager.listMods(it) } ?: emptyList()

        for (file in mods) {
            val name = file.name
            val enabled = client?.let { ModsManager.isEnabled(it, file) } ?: true
            // name label (as a button for easier visual)
            addRenderableWidget(
                Button.builder(Component.literal(name)) { _ -> }
                    .bounds(10, y, width - (bw + 30), bh)
                    .build()
            )

            val label = if (enabled) "Disable" else "Enable"
            addRenderableWidget(
                Button.builder(Component.literal(label)) { _ ->
                    try {
                        val c = this.minecraft ?: return@builder
                        val ok = ModsManager.toggleMod(c, file)
                        statusMessage = if (ok) "${file.name} toggled. Restarting to apply changes..." else "Failed to toggle ${file.name}"
                        // schedule exit so the launcher can be used to restart (delayed to let UI update)
                        Thread {
                            try { Thread.sleep(600) } catch (_: InterruptedException) {}
                            // best-effort terminate the game so the launcher becomes visible
                            try { System.exit(if (ok) 0 else 1) } catch (_: Throwable) {}
                        }.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        statusMessage = "Error: ${e.message}"
                    }
                }
                    .bounds(width - (bw + 10), y, bw, bh)
                    .build()
            )

            y += bh + spacing
            // if we run out of vertical space, stop adding more widgets
            if (y > height - 50) break
        }

        // close button
        addRenderableWidget(
            Button.builder(Component.literal("Close (R)")) { _ ->
                minecraft?.setScreen(OverlayScreen())
            }.bounds(10, height - 30, 80, 20).build()
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0, width, height, 0xBB000000.toInt())
        guiGraphics.drawCenteredString(font, "Installed mods:", width / 2, 6, 0xFFFFFF)
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
}
