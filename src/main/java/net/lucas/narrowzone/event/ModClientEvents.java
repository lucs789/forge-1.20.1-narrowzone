package net.lucas.narrowzone.event;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.ModItems;
import net.lucas.narrowzone.item.custom.LightCrossbowItem;
import net.lucas.narrowzone.item.custom.LongBowItem;
import net.lucas.narrowzone.item.custom.ShortBowItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModClientEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFovModifierEvent(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeModifier lightModifier = movement != null ? movement.getModifier(LightCrossbowItem.AIM_SPEED_UUID) : null;

        boolean shortActive = player.isUsingItem() && player.getUseItem().is(ModItems.SHORT_BOW.get());
        boolean longActive = player.isUsingItem() && player.getUseItem().is(ModItems.LONG_BOW.get());
        boolean lightAiming = player.isUsingItem()
                && player.getUseItem().is(ModItems.LIGHT_CROSSBOW.get())
                && !CrossbowItem.isCharged(player.getUseItem());
        boolean lightActive = lightAiming || lightModifier != null;

        if (!shortActive && !longActive && !lightActive) {
            return;
        }

        float fov = event.getFovModifier();

        if (shortActive) {
            fov = removeOnlyMySpeedFov(player, fov, ShortBowItem.AIM_FAST_UUID);
            float pull = Mth.clamp((float) player.getTicksUsingItem() / 15.0F, 0.0F, 1.0F);
            float eased = (pull * pull + pull * 2.0F) / 3.0F;
            event.setNewFovModifier(fov * (1.0F - eased * 0.075F));
            return;
        }

        if (longActive) {
            fov = removeOnlyMySpeedFov(player, fov, LongBowItem.AIM_SLOW_UUID);
            float pull = Mth.clamp((float) player.getTicksUsingItem() / 30.0F, 0.0F, 1.0F);
            float eased = (pull * pull + pull * 2.0F) / 3.0F;
            event.setNewFovModifier(fov * (1.0F - eased * 0.3F));
            return;
        }

        if (lightActive) {
            fov = removeOnlyMySpeedFov(player, fov, LightCrossbowItem.AIM_SPEED_UUID);
            event.setNewFovModifier(fov);
        }
    }

    private static float removeOnlyMySpeedFov(Player player, float fov, UUID modifierId) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return fov;
        }

        AttributeModifier modifier = movement.getModifier(modifierId);
        if (modifier == null || modifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
            return fov;
        }

        float walkSpeed = player.getAbilities().getWalkingSpeed();
        if (walkSpeed <= 0.0F) {
            return fov;
        }

        double currentSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double multiplier = 1.0D + modifier.getAmount();
        if (multiplier == 0.0D) {
            return fov;
        }

        double speedWithoutModifier = currentSpeed / multiplier;
        double currentFovFactor = (currentSpeed / walkSpeed + 1.0D) / 2.0D;
        double correctedFovFactor = (speedWithoutModifier / walkSpeed + 1.0D) / 2.0D;

        if (currentFovFactor == 0.0D) {
            return fov;
        }

        return (float) (fov * correctedFovFactor / currentFovFactor);
    }
}
