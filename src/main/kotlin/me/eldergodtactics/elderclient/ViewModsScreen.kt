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
        val blockWidth = (280 * 0.3).toInt()
        val blockHeight = (200 * 0.3).toInt()
        val spacing = 10
        var x = 10
        var y = 40

        val client = this.minecraft
        val mods = client?.let { ModsManager.listMods(it) } ?: emptyList()

        for (file in mods) {
            val enabled = client?.let { ModsManager.isEnabled(it, file) } ?: true
            val nameHeight = (blockHeight * 0.75).toInt()
            val toggleHeight = blockHeight - nameHeight

            addRenderableWidget(
                Button.builder(Component.literal(file.name)) { _ -> }
                    .bounds(x, y, blockWidth, nameHeight)
                    .build()
            )

            val label = if (enabled) "Disable" else "Enable"
            addRenderableWidget(
                Button.builder(Component.literal(label)) { _ ->
                    try {
                        val c = this.minecraft ?: return@builder
                        val ok = ModsManager.toggleMod(c, file)
                        statusMessage = if (ok) "${file.name} toggled. Restarting to apply changes..." else "Failed to toggle ${file.name}"
                        Thread {
                            try { Thread.sleep(600) } catch (_: InterruptedException) {}
                            try { System.exit(if (ok) 0 else 1) } catch (_: Throwable) {}
                        }.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        statusMessage = "Error: ${e.message}"
                    }
                }.bounds(x, y + nameHeight, blockWidth, toggleHeight).build()
            )

            x += blockWidth + spacing
            if (x + blockWidth > this.width) {
                x = 10
                y += blockHeight + spacing
            }
        }

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
