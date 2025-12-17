package me.eldergodtactics.elderclient

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraft.client.Minecraft

@Suppress("unused")
object ClientMain : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("elderclient")
    var menuOpen: Boolean = false
    var flyEnabled: Boolean = false
    private data class QueuedBlockAction(val pos: BlockPos, val isPlace: Boolean, val item: ItemStack?)
    private val queuedBlockActions: MutableList<QueuedBlockAction> = ArrayList()
    private val queuedMovements: MutableList<Vec3> = ArrayList()
    private var lastRightPressed: Boolean = false
    private lateinit var keyBinding: KeyMapping
    private const val CATEGORY = "ElderClient"
	override fun onInitializeClient() {
        keyBinding = KeyMapping(
            "CatVision",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
        )
        KeyBindingHelper.registerKeyBinding(keyBinding)

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (keyBinding.consumeClick()) {
                menuOpen = !menuOpen
                if (menuOpen) {
                    client.setScreen(OverlayScreen())
                } else {
                    client.setScreen(null)
                }
            }
        }

        TitleScreenManager.register()
        try {
            CapeFeatureRenderer.register()
        } catch (_: Throwable) {
        }

        try {
            ResourcePackUtil.applyDarkModePack(Minecraft.getInstance())
        } catch (_: Throwable) {
        }

        println("CatClient Lite loaded!")
    }
}
