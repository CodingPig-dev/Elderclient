package me.eldergodtactics.elderclient

import net.minecraft.client.Minecraft
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

    private val capeChangedListener: (String?) -> Unit = {
        Minecraft.getInstance().execute {
            refreshList()
        }
    }

    override fun init() {
        super.init()
        val bw = 120
        val bh = 20
        val topY = 20
        val totalW = 480
        val x = (width - totalW) / 2
        val y = topY
        pathBox = EditBox(font, x, y, totalW - 220, bh, Component.literal("Path or ID (e.g. C:/Capes/1.png or 127487)"))
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
            }.bounds(x + (totalW - 220) + 8, y, bw, bh).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Refresh")) { _ ->
                refreshList()
            }.bounds(x + (totalW - 220) + 8 + bw + 8, y, 80, bh).build()
        )
        refreshList()
        CapeManager.addOnCapeChangedListener(capeChangedListener)
    }

    private fun refreshList() {
        capeFiles = CapeManager.listCapes()
        if (selectedIndex >= capeFiles.size) selectedIndex = -1
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.drawCenteredString(font, "Cape Editor - customize your cape", width / 2, 8, 0xFFFFFF)
        val topY = 20
        val bh = 20
        val listX = 20
        val listY = topY + bh + 12
        val listW = 320
        val listH = height - listY - 20
        val itemH = 40
        guiGraphics.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2, 0xFF1F1F1F.toInt())
        val visibleCount = (listH / itemH).coerceAtLeast(1)
        val start = scrollOffset.coerceAtLeast(0)
        val end = (start + visibleCount).coerceAtMost(capeFiles.size)
        for (i in start until end) {
            val itemY = listY + (i - start) * itemH
            val file = capeFiles[i]
            if (i == selectedIndex) {
                guiGraphics.fill(listX, itemY, listX + listW, itemY + itemH - 1, 0xFF33447A.toInt())
            } else if (mouseX in listX..(listX + listW) && mouseY in itemY..(itemY + itemH - 1)) {
                guiGraphics.fill(listX, itemY, listX + listW, itemY + itemH - 1, 0xFF2A2A2A.toInt())
            }
            try {
                val info = CapeManager.getPreviewInfo(file.absolutePath)
                val thumbW = 56
                val thumbH = 32
                val thumbX = listX + 6
                val thumbY = itemY + (itemH - thumbH) / 2
                guiGraphics.fill(thumbX - 1, thumbY - 1, thumbX + thumbW + 1, thumbY + thumbH + 1, 0xFF101010.toInt())
                if (info != null) {
                    val rtFunc = java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> { _ -> net.minecraft.client.renderer.RenderType.gui() }
                    val scale = kotlin.math.min(thumbW.toFloat() / info.width.toFloat(), thumbH.toFloat() / info.height.toFloat())
                    val drawW = (info.width * scale).toInt().coerceAtLeast(1)
                    val drawH = (info.height * scale).toInt().coerceAtLeast(1)
                    val drawX = thumbX + (thumbW - drawW) / 2
                    val drawY = thumbY + (thumbH - drawH) / 2
                    guiGraphics.blit(rtFunc, info.location, drawX, drawY, 0f, 0f, drawW, drawH, info.width, info.height)
                    val textX = listX + 8 + thumbW
                    val textY = itemY + (itemH - font.lineHeight) / 2
                    guiGraphics.drawString(font, file.name, textX, textY, 0xFFFFFF)
                } else {
                    val textY = itemY + (itemH - font.lineHeight) / 2
                    guiGraphics.drawString(font, file.name, listX + 8, textY, 0xFFFFFF)
                }
            } catch (_: Throwable) {
                val textY = itemY + (itemH - font.lineHeight) / 2
                guiGraphics.drawString(font, file.name, listX + 8, textY, 0xFFFFFF)
            }
        }
        val previewX = listX + listW + 20
        val previewY = listY
        val previewW = (width - previewX - 20).coerceAtLeast(200)
        val previewH = previewW.coerceAtMost(360)
        guiGraphics.fill(previewX - 2, previewY - 2, previewX + previewW + 2, previewY + previewH + 2, 0xFF1F1F1F.toInt())

        if (selectedIndex in capeFiles.indices) {
            val selectedFile = capeFiles[selectedIndex]
            val info = CapeManager.getPreviewInfo(selectedFile.absolutePath)
            guiGraphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF0F0F0F.toInt())
            if (info != null) {
                try {
                    val rtFunc = java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> { _ -> net.minecraft.client.renderer.RenderType.gui() }
                    val scale = kotlin.math.min(previewW.toFloat() / info.width.toFloat(), previewH.toFloat() / info.height.toFloat())
                    val drawW = (info.width * scale).toInt().coerceAtLeast(1)
                    val drawH = (info.height * scale).toInt().coerceAtLeast(1)
                    val drawX = previewX + (previewW - drawW) / 2
                    val drawY = previewY + (previewH - drawH) / 2
                    guiGraphics.blit(rtFunc, info.location, drawX, drawY, 0f, 0f, drawW, drawH, info.width, info.height)
                } catch (_: Throwable) {
                    guiGraphics.drawCenteredString(font, "Preview unavailable", previewX + previewW / 2, previewY + previewH / 2 - font.lineHeight / 2, 0xFFFFFF)
                }
            } else {
                guiGraphics.drawCenteredString(font, "No preview", previewX + previewW / 2, previewY + previewH / 2 - font.lineHeight / 2, 0xFFFFFF)
            }

            guiGraphics.drawString(font, "Selected: ${capeFiles[selectedIndex].name}", previewX, previewY + previewH + 6, 0xFFFFFF)
        } else {
            guiGraphics.drawCenteredString(font, "No cape selected", previewX + previewW / 2, previewY + previewH / 2 - font.lineHeight / 2, 0xFFFFFF)
        }
        if (CapeManager.capeLoaded) {
            guiGraphics.drawString(font, "Loaded: ${File(CapeManager.capeFilePath).name}", previewX, previewY + previewH + 22, 0xFFFFFF)
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val topY = 20
        val bh = 20
        val listX = 20
        val listY = topY + bh + 12
        val listW = 320
        val itemH = 40
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
        val topY = 20
        val bh = 20
        val listY = topY + bh + 12
        val listH = height - listY - 20
        val itemH = 40
        val maxStart = (capeFiles.size - (listH / itemH)).coerceAtLeast(0)
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

    override fun onClose() {
        try {
            CapeManager.removeOnCapeChangedListener(capeChangedListener)
        } catch (_: Throwable) {
        }
        super.onClose()
    }
}
