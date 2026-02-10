package net.lucas.narrowzone.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public class LightCrossbowItem extends CrossbowItem {

    public static final double AIM_SPEED_AMOUNT = 3.5D;
    public static final UUID AIM_SPEED_UUID =
            UUID.fromString("8d8c3d3b-8af1-4b35-a6a1-4c6d6b5a8c22");

    public LightCrossbowItem(Properties pProperties) {super(pProperties);}

    private static AttributeModifier aimSlowModifier() {
        return new AttributeModifier(
                AIM_SPEED_UUID,
                "light_crossbow_aim_speed",
                AIM_SPEED_AMOUNT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    private static void setAimingSpeed(Player player, boolean enabled) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        if (enabled) {
            if (attr.getModifier(AIM_SPEED_UUID) == null) {
                attr.removeModifier(AIM_SPEED_UUID);
                attr.addTransientModifier(aimSlowModifier());
            }
        } else {
            if (attr.getModifier(AIM_SPEED_UUID) != null) {
                attr.removeModifier(AIM_SPEED_UUID);
            }
        }
    }

    public static void forceDisable(Player player) {
        setAimingSpeed(player, false);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (!(entity instanceof Player player)) return;

        boolean usingThis = player.isUsingItem() && player.getUseItem() == stack;
        setAimingSpeed(player, usingThis);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        try {
            //sem super.releaseUsing, porque ele usa o chargeDuration vanilla
            if (!(entity instanceof Player player)) return;

            int usedTicks = this.getUseDuration(stack) - timeLeft;
            int charge = getMyChargeDuration(stack);

            if (usedTicks < charge) return;

            if (!CrossbowItem.isCharged(stack)) {
                boolean loaded = tryLoadProjectiles(player, stack);
                if (loaded) {
                    CrossbowItem.setCharged(stack, true);

                    // som de "carregado"
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        } finally {
            if (entity instanceof Player player) {
                setAimingSpeed(player, false);
            }
        }
    }

    private int getMyChargeDuration(ItemStack stack) {
        int qc = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        int base = 20;
        return Math.max(5, base - 5 * qc);
    }

    // Carrega 1 projétil (ou 3 se Multishot) na tag ChargedProjectiles
    private boolean tryLoadProjectiles(Player player, ItemStack crossbow) {
        boolean creative = player.getAbilities().instabuild;

        ItemStack ammo = player.getProjectile(crossbow);
        if (ammo.isEmpty() && !creative) return false;

        ItemStack ammoType = ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo;

        int multishot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, crossbow);
        int count = multishot > 0 ? 3 : 1;

        if (!creative && ammo.getCount() < count) return false;

        CompoundTag tag = crossbow.getOrCreateTag();
        ListTag list = new ListTag();

        for (int i = 0; i < count; i++) {
            ItemStack one = ammoType.copy();
            one.setCount(1);

            CompoundTag oneTag = new CompoundTag();
            one.save(oneTag);
            list.add(oneTag);

            if (!creative && !ammo.isEmpty()) {
                ammo.shrink(1);
                if (ammo.isEmpty()) player.getInventory().removeItem(ammo);
            }
        }

        tag.put("ChargedProjectiles", list);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;

        boolean usingThis = player.isUsingItem() && player.getUseItem().getItem() instanceof LightCrossbowItem;
        if (!usingThis) setAimingSpeed(player, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (CrossbowItem.isCharged(stack)) {
            if (!level.isClientSide) {
                float velocity = CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET)
                        ? 1.6F
                        : 2.0F;

                CrossbowItem.performShooting(level, player, hand, stack, velocity, 1.0F);
            }

            CrossbowItem.setCharged(stack, false);
            stack.removeTagKey("ChargedProjectiles");

            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.consume(stack);
        }

        boolean creative = player.getAbilities().instabuild;
        ItemStack ammo = player.getProjectile(stack);

        if (!creative && ammo.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

}
