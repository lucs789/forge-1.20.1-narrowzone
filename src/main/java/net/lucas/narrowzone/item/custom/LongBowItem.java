package net.lucas.narrowzone.item.custom;

import net.lucas.narrowzone.enchantment.ModRangedEnchantmentEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class LongBowItem extends BowItem {
    public static final int CHARGE_TICKS = 30;
    public static final double AIM_SLOW_AMOUNT = -0.7D;
    private static final float MIN_POWER_TO_SHOOT = 0.1F;
    public static final UUID AIM_SLOW_UUID = UUID.fromString("8d8c3d3b-8af1-4b35-a6a1-4c6d6b5a8c21");

    public LongBowItem(Properties properties) {
        super(properties);
    }

    private static AttributeModifier aimSlowModifier() {
        return new AttributeModifier(AIM_SLOW_UUID, "longbow_aim_slow", AIM_SLOW_AMOUNT, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void setAimingSlow(Player player, boolean enabled) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        if (enabled) {
            if (attribute.getModifier(AIM_SLOW_UUID) == null) {
                attribute.addTransientModifier(aimSlowModifier());
            }
        } else if (attribute.getModifier(AIM_SLOW_UUID) != null) {
            attribute.removeModifier(AIM_SLOW_UUID);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (!level.isClientSide && entity instanceof Player player) {
            setAimingSlow(player, player.isUsingItem() && player.getUseItem() == stack);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player) {
            boolean active = player.isUsingItem() && player.getUseItem().getItem() == this;
            if (!active) {
                setAimingSlow(player, false);
            }
        }
    }

    private static float getBowPower(int charge) {
        float power = (float) charge / (float) CHARGE_TICKS;
        power = Mth.clamp(power, 0.0F, 1.0F);
        power = (power * power + power * 2.0F) / 3.0F;
        return Mth.clamp(power, 0.0F, 1.0F);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        try {
            int charge = ModRangedEnchantmentEffects.applyQuickstring(stack, this.getUseDuration(stack) - timeLeft);
            float power = getBowPower(charge);
            if (power < MIN_POWER_TO_SHOOT) {
                if (!level.isClientSide) {
                    setAimingSlow(player, false);
                }
                return;
            }

            ItemStack projectileStack = player.getProjectile(stack);
            boolean creative = player.getAbilities().instabuild;
            int infinity = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack);

            if (projectileStack.isEmpty() && !creative && infinity <= 0) {
                if (!level.isClientSide) {
                    setAimingSlow(player, false);
                }
                return;
            }

            ItemStack ammo = projectileStack.isEmpty() ? new ItemStack(Items.ARROW) : projectileStack;

            if (!level.isClientSide) {
                ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem) Items.ARROW;
                AbstractArrow arrow = arrowItem.createArrow(level, ammo, player);
                arrow = customArrow(arrow);

                int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                if (powerLevel > 0) {
                    arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D);
                }

                int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
                if (punch > 0) {
                    arrow.setKnockback(punch);
                }

                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
                    arrow.setSecondsOnFire(100);
                }

                boolean critical = power >= 1.0F;
                arrow.setCritArrow(critical);
                if (!critical) {
                    arrow.setBaseDamage(arrow.getBaseDamage() - 1.0D);
                }

                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 4.0F, 0.1F);

                if (creative || infinity > 0) {
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                level.addFreshEntity(arrow);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                    1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);

            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));

            if (!creative && infinity <= 0 && !projectileStack.isEmpty()) {
                projectileStack.shrink(1);
                if (projectileStack.isEmpty()) {
                    player.getInventory().removeItem(projectileStack);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(this));
        } finally {
            if (!level.isClientSide) {
                setAimingSlow(player, false);
            }
        }
    }
}
