package net.lucas.narrowzone.item.custom;

import net.lucas.narrowzone.enchantment.ModRangedEnchantmentEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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

import java.util.UUID;

public class LightCrossbowItem extends CrossbowItem {
    public static final double AIM_SPEED_AMOUNT = 3.5D;
    public static final UUID AIM_SPEED_UUID = UUID.fromString("8d8c3d3b-8af1-4b35-a6a1-4c6d6b5a8c22");

    public LightCrossbowItem(Properties properties) {
        super(properties);
    }

    private static AttributeModifier aimSpeedModifier() {
        return new AttributeModifier(AIM_SPEED_UUID, "light_crossbow_aim_speed", AIM_SPEED_AMOUNT, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void setAimingSpeed(Player player, boolean enabled) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        if (enabled) {
            if (attribute.getModifier(AIM_SPEED_UUID) == null) {
                attribute.addTransientModifier(aimSpeedModifier());
            }
        } else if (attribute.getModifier(AIM_SPEED_UUID) != null) {
            attribute.removeModifier(AIM_SPEED_UUID);
        }
    }

    public static void forceDisable(Player player) {
        setAimingSpeed(player, false);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (entity instanceof Player player) {
            setAimingSpeed(player, player.isUsingItem() && player.getUseItem() == stack && !ModRangedEnchantmentEffects.hasSteadyDraw(stack));
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        try {
            int charge = this.getUseDuration(stack) - timeLeft;
            int chargeDuration = getMyChargeDuration(stack);
            if (charge < chargeDuration) {
                setAimingSpeed(player, false);
                return;
            }

            if (!CrossbowItem.isCharged(stack) && tryLoadProjectiles(player, stack)) {
                CrossbowItem.setCharged(stack, true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        } finally {
            setAimingSpeed(player, false);
        }
    }

    private int getMyChargeDuration(ItemStack stack) {
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return Math.max(5, 20 - 5 * quickCharge);
    }

    private boolean tryLoadProjectiles(Player player, ItemStack crossbow) {
        boolean creative = player.getAbilities().instabuild;
        ItemStack projectile = player.getProjectile(crossbow);
        if (projectile.isEmpty() && !creative) {
            return false;
        }

        ItemStack ammo = projectile.isEmpty() ? new ItemStack(Items.ARROW) : projectile;
        int multishot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, crossbow);
        int count = multishot > 0 ? 3 : 1;

        if (!creative && projectile.getCount() < count) {
            return false;
        }

        CompoundTag tag = crossbow.getOrCreateTag();
        ListTag list = new ListTag();

        for (int i = 0; i < count; i++) {
            ItemStack copy = ammo.copy();
            copy.setCount(1);
            CompoundTag projectileTag = new CompoundTag();
            copy.save(projectileTag);
            list.add(projectileTag);

            if (!creative && !projectile.isEmpty()) {
                projectile.shrink(1);
                if (projectile.isEmpty()) {
                    player.getInventory().removeItem(projectile);
                }
            }
        }

        tag.put("ChargedProjectiles", list);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player) {
            boolean active = player.isUsingItem() && player.getUseItem().getItem() instanceof LightCrossbowItem && !ModRangedEnchantmentEffects.hasSteadyDraw(stack);
            if (!active) {
                setAimingSpeed(player, false);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (CrossbowItem.isCharged(stack)) {
            if (!level.isClientSide) {
                float velocity = CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.6F : 2.0F;
                CrossbowItem.performShooting(level, player, hand, stack, velocity, 1.0F);
            }

            CrossbowItem.setCharged(stack, false);
            stack.removeTagKey("ChargedProjectiles");
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.consume(stack);
        }

        boolean creative = player.getAbilities().instabuild;
        ItemStack projectile = player.getProjectile(stack);
        if (!creative && projectile.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
