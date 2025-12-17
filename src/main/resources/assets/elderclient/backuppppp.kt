package assets.elderclient

import me.eldergodtactics.elderclient.ClientMain.menuOpen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class OverlayScreen : net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("ElderClient")) {
    override fun init() {
        super.init()
        val bw = 150
        val bh = 20
        val spacing = 6
        val cx = net.minecraft.client.gui.screens.Screen.width / 2
        val centerY = net.minecraft.client.gui.screens.Screen.height / 2
        net.minecraft.client.gui.screens.Screen.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Mods Browser")) { _ ->
                net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(ModsBrowserScreen())
            }.bounds(cx - bw / 2, centerY - (bh * 2 + spacing), bw, bh).build()
        )
        net.minecraft.client.gui.screens.Screen.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("View Mods")) { _ ->
                net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(ViewModsScreen())
            }.bounds(cx - bw / 2, centerY - bh, bw, bh).build()
        )
        net.minecraft.client.gui.screens.Screen.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Capes")) { _ ->
                net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(CapeEditorScreen())
            }.bounds(cx - bw / 2, centerY + (spacing), bw, bh).build()
        )
        net.minecraft.client.gui.screens.Screen.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Close")) { _ ->
                me.eldergodtactics.elderclient.ClientMain.menuOpen = false
                net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(null)
            }.bounds(cx - bw / 2, centerY + (bh * 2 + spacing), bw, bh).build()
        )
    }

    override fun render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0,
            net.minecraft.client.gui.screens.Screen.width,
            net.minecraft.client.gui.screens.Screen.height,
            Long.toInt()
        )
        val panelW = 300
        val panelH = 220
        val px = net.minecraft.client.gui.screens.Screen.width / 2 - panelW / 2
        val py = net.minecraft.client.gui.screens.Screen.height / 2 - panelH / 2
        guiGraphics.fill(px, py, px + panelW, py + panelH, Long.toInt())
        guiGraphics.fill(px, py, px + panelW, py + 2, Long.toInt())
        guiGraphics.fill(px, py + panelH - 2, px + panelW, py + panelH, Long.toInt())
        guiGraphics.fill(px, py, px + 2, py + panelH, Long.toInt())
        guiGraphics.fill(px + panelW - 2, py, px + panelW, py + panelH, Long.toInt())
        guiGraphics.drawCenteredString(net.minecraft.client.gui.screens.Screen.font, net.minecraft.network.chat.Component.literal("Press R to close"), net.minecraft.client.gui.screens.Screen.width / 2, 10, 0xFFFFFF)

        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            me.eldergodtactics.elderclient.ClientMain.menuOpen = false
            net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(null)
            return true
        }
        return super.keyPressed(i, j, k)
    }
}

class ModsBrowserScreen : net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Mods Browser")) {
    override fun render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0,
            net.minecraft.client.gui.screens.Screen.width,
            net.minecraft.client.gui.screens.Screen.height,
            Long.toInt()
        )
        guiGraphics.drawCenteredString(net.minecraft.client.gui.screens.Screen.font, "Mods Browser - work in progress", net.minecraft.client.gui.screens.Screen.width / 2, net.minecraft.client.gui.screens.Screen.height / 2 - 10, 0xFFFFFF)
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(OverlayScreen())
            return true
        }
        return super.keyPressed(i, j, k)
    }
}

class ViewModsScreen : net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("View Mods")) {
    override fun render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0,
            net.minecraft.client.gui.screens.Screen.width,
            net.minecraft.client.gui.screens.Screen.height,
            Long.toInt()
        )
        guiGraphics.drawCenteredString(net.minecraft.client.gui.screens.Screen.font, "Installed mods will be listed here", net.minecraft.client.gui.screens.Screen.width / 2, net.minecraft.client.gui.screens.Screen.height / 2 - 10, 0xFFFFFF)
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(OverlayScreen())
            return true
        }
        return super.keyPressed(i, j, k)
    }
}

class CapeEditorScreen : net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Cape Editor")) {
    override fun render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0,
            net.minecraft.client.gui.screens.Screen.width,
            net.minecraft.client.gui.screens.Screen.height,
            Long.toInt()
        )
        guiGraphics.drawCenteredString(net.minecraft.client.gui.screens.Screen.font, "Cape Editor - customize your cape", net.minecraft.client.gui.screens.Screen.width / 2, net.minecraft.client.gui.screens.Screen.height / 2 - 10, 0xFFFFFF)
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(OverlayScreen())
            return true
        }
        return super.keyPressed(i, j, k)
    }
}
