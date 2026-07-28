package net.lucas.narrowzone.event;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.enchantment.ModEnchantments;
import net.lucas.narrowzone.enchantment.ModRangedEnchantmentEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ModClientGameplayEvents {
    private ModClientGameplayEvents() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !player.isUsingItem()) {
            return;
        }

        ItemStack using = player.getUseItem();
        if (!ModRangedEnchantmentEffects.isBowOrCrossbow(using)) {
            return;
        }

        int steadyDrawLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.STEADY_DRAW.get(), using);
        if (steadyDrawLevel <= 0) {
            return;
        }

        float multiplier = ModRangedEnchantmentEffects.getSteadyDrawInputMultiplier(steadyDrawLevel);
        event.getInput().leftImpulse *= multiplier;
        event.getInput().forwardImpulse *= multiplier;
    }
}
