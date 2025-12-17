package me.eldergodtactics.elderclient

import me.eldergodtactics.elderclient.ClientMain.menuOpen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class OverlayScreen : Screen(net.minecraft.network.chat.Component.literal("ElderClient")) {
    override fun init() {
        super.init()
        val bw = 90
        val bh = 20
        val spacing = 6
        val capesX = 5
        val topY = 5
        addRenderableWidget(
            Button.builder(net.minecraft.network.chat.Component.literal("View Mods")) { _ ->
                minecraft?.setScreen(ViewModsScreen())
            }.bounds(capesX, topY, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(net.minecraft.network.chat.Component.literal("Mods Browser")) { _ ->
                minecraft?.setScreen(ModsBrowserScreen())
            }.bounds(capesX + bw + spacing, topY, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(net.minecraft.network.chat.Component.literal("Home")) { _ ->
                menuOpen = false
                minecraft?.setScreen(null)
            }.bounds(capesX + (bw + spacing) * 2, topY, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(net.minecraft.network.chat.Component.literal("Capes")) { _ ->
                minecraft?.setScreen(CapeEditorScreen())
            }.bounds(width - bw - 10, topY, bw, bh).build()
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0, width, height, 0xBB000000.toInt())
        guiGraphics.drawCenteredString(font, Component.literal("Press R to close"), width / 2, 10, 0xFFFFFF)

        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == GLFW.GLFW_KEY_R) {
            menuOpen = false
            minecraft?.setScreen(null)
            return true
        }
        return super.keyPressed(i, j, k)
    }
}


