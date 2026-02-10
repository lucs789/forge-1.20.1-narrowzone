package net.lucas.narrowzone.event;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.custom.LightCrossbowItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UseStopHandler {

    @SubscribeEvent
    public static void onStop(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack stack = event.getItem();
        if (stack.getItem() instanceof LightCrossbowItem) {
            LightCrossbowItem.forceDisable(player);
        }
    }
}

