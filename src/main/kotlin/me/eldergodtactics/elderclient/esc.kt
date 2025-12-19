package me.eldergodtactics.elderclient

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class EscScreen : PauseScreen(true) {
    override fun init() {
        super.init()
        val bw = 90
        val bh = 20
        val spacing = 6
        val capesX = 5
        val topY = 5
        addRenderableWidget(
            Button.builder(Component.literal("View Mods")) { _ ->
                minecraft?.setScreen(ViewModsScreen())
            }.bounds(capesX, topY, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Mods Browser")) { _ ->
                minecraft?.setScreen(ModsBrowserScreen())
            }.bounds(capesX + bw + spacing, topY, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Home")) { _ ->
                ClientMain.menuOpen = false
                minecraft?.setScreen(null)
            }.bounds(capesX + (bw + spacing) * 2, topY, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Capes")) { _ ->
                minecraft?.setScreen(CapeEditorScreen())
            }.bounds(width - bw - 10, topY, bw, bh).build()
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
        val overlayHeight = 5 + 20 + 5
        guiGraphics.fill(0, 0, width, overlayHeight, 0xBB000000.toInt())
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_R) {
            ClientMain.escOverlayDisabled = true
            minecraft?.setScreen(PauseScreen(true))
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}