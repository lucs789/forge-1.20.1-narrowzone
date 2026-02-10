package net.lucas.narrowzone.event;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.ModItems;
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

import static net.lucas.narrowzone.item.custom.LightCrossbowItem.AIM_SPEED_UUID;
import static net.lucas.narrowzone.item.custom.LongBowItem.AIM_SLOW_UUID;
import static net.lucas.narrowzone.item.custom.ShortBowItem.AIM_FAST_UUID;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFovModifierEvent(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeModifier mod = speedAttr != null ? speedAttr.getModifier(AIM_SPEED_UUID) : null;

        boolean usingShort = player.isUsingItem() && player.getUseItem().is(ModItems.SHORT_BOW.get());
        boolean usingLong  = player.isUsingItem() && player.getUseItem().is(ModItems.LONG_BOW.get());
        boolean usingLight = player.isUsingItem()
                && player.getUseItem().is(ModItems.LIGHT_CROSSBOW.get())
                && !CrossbowItem.isCharged(player.getUseItem());
        boolean lightActive = usingLight || mod != null;

        if (!usingShort && !usingLong && !lightActive) return;

        float fov = event.getFovModifier();

        if (usingShort) {
            fov = removeOnlyMySpeedFov(player, fov, AIM_FAST_UUID);

            int ticks = player.getTicksUsingItem();
            float chargeTicks = 15f;
            float zoomStrength = 0.075f;

            float t = Mth.clamp(ticks / chargeTicks, 0f, 1f);
            float power = (t * t + 2f * t) / 3f;

            event.setNewFovModifier(fov * (1f - power * zoomStrength));
            return;
        }

        if (usingLong) {
            fov = removeOnlyMySpeedFov(player, fov, AIM_SLOW_UUID);

            int ticks = player.getTicksUsingItem();
            float chargeTicks = 30f;
            float zoomStrength = 0.3f;

            float t = Mth.clamp(ticks / chargeTicks, 0f, 1f);
            float power = (t * t + 2f * t) / 3f;

            event.setNewFovModifier(fov * (1f - power * zoomStrength));
            return;
        }

        if (lightActive) {
            fov = removeOnlyMySpeedFov(player, fov, AIM_SPEED_UUID);
            event.setNewFovModifier(fov);
            return;
        }
    }

    /*
    Remove do FOV apenas a parcela causada por UM AttributeModifier (MULTIPLY_TOTAL)
    no Attributes.MOVEMENT_SPEED, mantendo sprint/poções/flying/etc.
    */

    private static float removeOnlyMySpeedFov(Player player, float fov, UUID myUuid) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return fov;

        AttributeModifier mod = speedAttr.getModifier(myUuid);
        if (mod == null) return fov;
        if (mod.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) return fov;

        float walkingSpeed = player.getAbilities().getWalkingSpeed();
        if (walkingSpeed <= 0f) return fov;

        // velocidade "com" o mod (valor atual do atributo)
        double speedWith = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

        // remove só o seu MULTIPLY_TOTAL: speedWithout = speedWith / (1 + amount)
        double factor = 1.0 + mod.getAmount(); // ex: +2.1 => 3.1 | -0.35 => 0.65
        if (factor == 0.0) return fov;

        double speedWithout = speedWith / factor;

        // fator de FOV vanilla baseado em velocidade:
        // g = ((speed / walkingSpeed) + 1) / 2
        double gWith = ((speedWith / walkingSpeed) + 1.0) / 2.0;
        double gWithout = ((speedWithout / walkingSpeed) + 1.0) / 2.0;

        if (gWith == 0.0) return fov;

        // Corrige o fov removendo só a parcela da velocidade
        return (float) (fov * (gWithout / gWith));
    }
}
