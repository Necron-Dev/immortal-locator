package moe.nec.xianding.client

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import com.mojang.serialization.JsonOps
import kotlinx.coroutines.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.math.abs
import kotlin.math.min

private const val TEXTURE_URL = "https://textures.minecraft.net/texture/d3fc0ad9fcb3e4857003e306c287d8271e40a0096547d7773f0eba0ff37020"

class XiandingClient : ClientModInitializer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mc: Minecraft by lazy { Minecraft.getInstance() }

    private fun info(key: String, vararg args: Any) {
        mc.execute {
            val prefix = Component.translatable("message.xianding.prefix")
            val message = Component.translatable(key, *args)
            mc.player?.displayClientMessage(Component.empty().append(prefix).append(message), false)
        }
    }

    override fun onInitializeClient() {
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (entity.type != EntityType.EYE_OF_ENDER) return@register

            scope.launch {
                info("message.xianding.waiting")

                val pos1 = entity.position()
                delay(3500)

                if (!entity.isAlive) {
                    info("message.xianding.failed")
                    return@launch
                }

                var p1 = pos1
                var p2 = entity.position()

                if (abs(p1.x - p2.x) < 0.1 && abs(p1.z - p2.z) < 0.1) {
                    info("message.xianding.failed")
                    return@launch
                }

                var isReverse = false
                if (abs(p1.z - p2.z) > abs(p1.x - p2.x)) {
                    p1 = Vec3(p1.z, p1.y, p1.x)
                    p2 = Vec3(p2.z, p2.y, p2.x)
                    isReverse = true
                }

                val finder = GridFinder(
                    p1.x, p1.z,
                    (p2.z - p1.z) / (p2.x - p1.x),
                    if (p2.x < p1.x) -1 else 1
                )

                var foundRing = false
                repeat(500) {
                    if (!foundRing) {
                        finder.next()
                        if (finder.isInRing) foundRing = true
                    }
                }
                if (!foundRing) return@launch

                val strongholds = mutableListOf<Stronghold>()
                while (finder.isInRing) {
                    val sh = finder.next()
                    if (sh.accuracy > 2) strongholds.add(sh)
                }

                if (strongholds.isEmpty()) return@launch

                successEffect()

                val sortedResults = strongholds.sortedByDescending { it.accuracy }

                mc.execute {
                    val player = mc.player ?: return@execute
                    player.displayClientMessage(Component.empty(), false) // 换行隔开
                    player.displayClientMessage(Component.translatable("message.xianding.header").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false)

                    val maxToShow = min(sortedResults.size, 5)
                    val bestAcc = sortedResults[0].accuracy.toDouble()
                    val worstAcc = sortedResults[min(sortedResults.size - 1, 4)].accuracy.toDouble()

                    for (i in 0 until maxToShow) {
                        val sh = sortedResults[i]
                        val ox = (if (isReverse) sh.z else sh.x) + 3
                        val oz = (if (isReverse) sh.x else sh.z) + 3

                        val ratio = if (bestAcc == worstAcc) 1.0 else (sh.accuracy - worstAcc) / (bestAcc - worstAcc)
                        val color = interpolateColor(ratio)

                        val clickText = "$ox 100 $oz"
                        val resultLine = Component.translatable("message.xianding.result", ox, oz, ox / 8, oz / 8, sh.accuracy)

                        resultLine.withStyle { style ->
                            style.withColor(color)
                                .withHoverEvent(HoverEvent.ShowText(Component.translatable("message.xianding.copy_hover")))
                                .withClickEvent(ClickEvent.CopyToClipboard(clickText))
                        }

                        player.displayClientMessage(resultLine, false)
                    }

                    player.displayClientMessage(Component.empty(), false)
                    player.displayClientMessage(Component.translatable("message.xianding.success", player.name), false)
                }
            }
        }
    }

    private fun interpolateColor(ratio: Double): Int {
        val r = (255 * (1 - ratio)).toInt().coerceIn(0, 255)
        val g = (255 * ratio).toInt().coerceIn(0, 255)
        val b = 50
        return (r shl 16) or (g shl 8) or b
    }

    val skullStack = ItemStack(Items.PLAYER_HEAD)

    init {
        val json = """{"textures":{"SKIN":{"url":"$TEXTURE_URL"}}}"""
        val base64Texture = Base64.encode(json.toByteArray())

        try {
            val map = ExtraCodecs.PROPERTY_MAP.parse(JsonOps.INSTANCE, JsonParser.parseString("[{\"name\":\"textures\",\"value\":\"$base64Texture\"}]")).getOrThrow();
            val profile: ResolvableProfile = ResolvableProfile.createResolved(GameProfile(UUID.randomUUID(), "skull", map));
            skullStack.set(DataComponents.PROFILE, profile);
        } catch (e: Exception) {
        }
    }

    private fun successEffect() {
        mc.execute {
            val player = mc.player ?: return@execute
            val world = mc.level ?: return@execute

            mc.gameRenderer.displayItemActivation(skullStack)

            player.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f)

            val pos = player.position()
            repeat(30) {
                world.addParticle(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    pos.x, pos.y + 1.0, pos.z,
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.4
                )
            }
        }
    }
}