package me.eldergodtactics.elderclient

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW
import java.io.File

class CapeEditorScreen : Screen(Component.literal("Cape Editor")) {
    private lateinit var pathBox: EditBox
    private var capeFiles: List<File> = emptyList()
    private var scrollOffset = 0
    private var selectedIndex = -1
    override fun init() {
        super.init()
        val bw = 120
        val bh = 20
        val totalW = 300
        val x = (width - totalW) / 2
        val y = (height / 2) - 20
        pathBox = EditBox(font, x, y, totalW, bh, Component.literal("Path or ID (e.g. C:/Capes/1.png or 127487)"))
        addRenderableWidget(pathBox)
        addRenderableWidget(
            Button.builder(Component.literal("Load Cape")) { _ ->
                try {
                    val spec = pathBox.value.trim()
                    if (spec.isNotEmpty()) {
                        CapeManager.loadCapeFromSpec(spec)
                        refreshList()
                        println("Selected cape spec: $spec")
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }.bounds(x + (totalW - bw) / 2, y + bh + 8, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Refresh")) { _ ->
                refreshList()
            }.bounds(x + totalW + 6, y, 80, bh).build()
        )
        refreshList()
    }

    private fun refreshList() {
        capeFiles = CapeManager.listCapes()
        if (selectedIndex >= capeFiles.size) selectedIndex = -1
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.fill(0, 0, width, height, 0xBB000000.toInt())
        guiGraphics.drawCenteredString(font, "Cape Editor - customize your cape", width / 2, 10, 0xFFFFFF)
        val listX = 20
        val listY = 40
        val listW = 260
        val listH = height - listY - 20
        val itemH = 24
        guiGraphics.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2, 0xFF222222.toInt())
        val visibleCount = listH / itemH
        val start = scrollOffset
        val end = (start + visibleCount).coerceAtMost(capeFiles.size)

        for (i in start until end) {
            val itemY = listY + (i - start) * itemH
            val file = capeFiles[i]
            if (i == selectedIndex) {
                guiGraphics.fill(listX, itemY, listX + listW, itemY + itemH - 1, 0xFF4444AA.toInt())
            } else if (mouseX in listX..(listX + listW) && mouseY in itemY..(itemY + itemH - 1)) {
                guiGraphics.fill(listX, itemY, listX + listW, itemY + itemH - 1, 0xFF333333.toInt())
            }
            try {
                val preview = CapeManager.getPreviewLocationForPath(file.absolutePath)
                if (preview != null) {
                    val thumbW = 48
                    val thumbH = 24
                    val rtFunc = java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> { _ -> net.minecraft.client.renderer.RenderType.gui() }
                    guiGraphics.blit(rtFunc, preview, listX + 4, itemY + 1, 0f, 0f, thumbW, thumbH, thumbW, thumbH)
                    guiGraphics.drawString(font, file.name, listX + 8 + thumbW, itemY + 6, 0xFFFFFF)
                } else {
                    guiGraphics.drawString(font, file.name, listX + 8, itemY + 6, 0xFFFFFF)
                }
            } catch (_: Throwable) {
                guiGraphics.drawString(font, file.name, listX + 8, itemY + 6, 0xFFFFFF)
            }
        }
        val previewX = listX + listW + 20
        val previewY = listY
        val previewW = 240
        val previewH = 240
        guiGraphics.fill(previewX - 2, previewY - 2, previewX + previewW + 2, previewY + previewH + 2, 0xFF222222.toInt())

        if (selectedIndex in capeFiles.indices) {
            val selectedFile = capeFiles[selectedIndex]
            val rl = CapeManager.getPreviewLocationForPath(selectedFile.absolutePath)
            if (rl != null) {
                try {
                    val rtFunc = java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> { _ -> net.minecraft.client.renderer.RenderType.gui() }
                    guiGraphics.blit(rtFunc, rl, previewX, previewY, 0f, 0f, previewW, previewH, previewW, previewH)
                } catch (_: Throwable) {
                    guiGraphics.drawCenteredString(font, "Preview unavailable", previewX + previewW / 2, previewY + previewH / 2, 0xFFFFFF)
                }
            } else {
                guiGraphics.drawCenteredString(font, "No preview", previewX + previewW / 2, previewY + previewH / 2, 0xFFFFFF)
            }

            guiGraphics.drawString(font, "Selected: ${capeFiles[selectedIndex].name}", previewX, previewY + previewH + 6, 0xFFFFFF)
        } else {
            guiGraphics.drawCenteredString(font, "No cape selected", previewX + previewW / 2, previewY + previewH / 2, 0xFFFFFF)
        }
        if (CapeManager.capeLoaded) {
            guiGraphics.drawString(font, "Loaded: ${File(CapeManager.capeFilePath).name}", previewX, previewY + previewH + 22, 0xFFFFFF)
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val listX = 20
        val listY = 40
        val listW = 260
        val itemH = 24
        val visibleCount = (height - listY - 20) / itemH
        val start = scrollOffset
        val end = (start + visibleCount).coerceAtMost(capeFiles.size)
        if (mx in listX..(listX + listW)) {
            for (i in start until end) {
                val itemY = listY + (i - start) * itemH
                if (my in itemY until (itemY + itemH - 1)) {
                    selectedIndex = i
                    try {
                        val file = capeFiles[i]
                        CapeManager.loadCapeFromFile(file.absolutePath)
                        println("Loaded cape from list: ${file.absolutePath}")
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    return true
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(d0: Double, d1: Double, deltaX: Double, deltaY: Double): Boolean {
        val amount = deltaY.toInt()
        scrollOffset = (scrollOffset - amount).coerceAtLeast(0)
        val maxStart = (capeFiles.size - ((height - 40 - 20) / 24)).coerceAtLeast(0)
        if (scrollOffset > maxStart) scrollOffset = maxStart
        return true
    }

    override fun keyPressed(i: Int, j: Int, k: Int): Boolean {
        if (i == GLFW.GLFW_KEY_ENTER || i == GLFW.GLFW_KEY_KP_ENTER) {
            if (::pathBox.isInitialized && pathBox.isFocused) {
                val path = pathBox.value.trim()
                if (path.isNotEmpty()) {
                    try {
                        CapeManager.loadCapeFromSpec(path)
                        refreshList()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
                return true
            }
        }

        if (i == GLFW.GLFW_KEY_R) {
            minecraft?.setScreen(OverlayScreen())
            return true
        }
        return super.keyPressed(i, j, k)
    }
}
