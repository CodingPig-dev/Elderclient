package assets.elderclient

import me.eldergodtactics.elderclient.ClientMain.menuOpen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.event.KeyEvent

class OverlayScreen : net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Mod Menu")) {
    override fun init() {
        super.init()
        val bw = 150
        val bh = 20
        val cx = net.minecraft.client.gui.screens.Screen.width / 2

        net.minecraft.client.gui.screens.Screen.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Close")) { _ ->
                me.eldergodtactics.elderclient.ClientMain.menuOpen = false
                net.minecraft.client.gui.screens.Screen.minecraft?.setScreen(null)
            }.bounds(cx - bw / 2, net.minecraft.client.gui.screens.Screen.height / 2 + 18, bw, bh).build()
        )
    }

    override fun render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0,
            net.minecraft.client.gui.screens.Screen.width,
            net.minecraft.client.gui.screens.Screen.height,
            Long.toInt()
        )
        guiGraphics.drawCenteredString(net.minecraft.client.gui.screens.Screen.font, net.minecraft.network.chat.Component.literal("ElderClient press R to close"), net.minecraft.client.gui.screens.Screen.width / 2, 10, 0xFFFFFF)

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
