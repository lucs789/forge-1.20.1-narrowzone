package net.lucas.narrowzone.item.custom;

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
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class LongBowItem extends BowItem {

    public static final int CHARGE_TICKS = 30;
    public static final double AIM_SLOW_AMOUNT = -0.7D;
    private static final float MIN_POWER_TO_SHOOT = 0.1F;

    public static final UUID AIM_SLOW_UUID =
            UUID.fromString("8d8c3d3b-8af1-4b35-a6a1-4c6d6b5a8c21");

    public LongBowItem(Properties properties) {
        super(properties);
    }

    private static AttributeModifier aimSlowModifier() {
        return new AttributeModifier(
                AIM_SLOW_UUID,
                "longbow_aim_slow",
                AIM_SLOW_AMOUNT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    private static void setAimingSlow(Player player, boolean enabled) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        if (enabled) {
            if (attr.getModifier(AIM_SLOW_UUID) == null) {
                attr.removeModifier(AIM_SLOW_UUID);
                attr.addTransientModifier(aimSlowModifier());
            }
        } else {
            if (attr.getModifier(AIM_SLOW_UUID) != null) {
                attr.removeModifier(AIM_SLOW_UUID);
            }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (!level.isClientSide && entity instanceof Player player) {
            boolean aimingThis = player.isUsingItem() && player.getUseItem() == stack;
            setAimingSlow(player, aimingThis);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player) {
            boolean aimingThis = player.isUsingItem() && player.getUseItem().getItem() == this;
            if (!aimingThis) setAimingSlow(player, false);
        }
    }

    private static float getBowPower(int useTicks) {
        float t = (float) useTicks / (float) CHARGE_TICKS;
        t = Mth.clamp(t, 0.0F, 1.0F);
        // curva vanilla: (t^2 + 2t) / 3
        float power = (t * t + 2.0F * t) / 3.0F;
        return Mth.clamp(power, 0.0F, 1.0F);
    }

    @Override
    public void releaseUsing(ItemStack bowStack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player)) return;

        try {
            int useTicks = this.getUseDuration(bowStack) - timeLeft;

            float power = getBowPower(useTicks);
            if (power < MIN_POWER_TO_SHOOT) return;

            ItemStack ammo = player.getProjectile(bowStack);

            boolean creative = player.getAbilities().instabuild;
            int infinityLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bowStack);

            if (ammo.isEmpty() && !creative && infinityLevel <= 0) return;

            ItemStack arrowStack = ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo;

            if (!level.isClientSide) {
                ArrowItem arrowItem = (arrowStack.getItem() instanceof ArrowItem ai)
                        ? ai
                        : (ArrowItem) Items.ARROW;

                AbstractArrow arrow = arrowItem.createArrow(level, arrowStack, player);
                arrow = this.customArrow(arrow);



                // Encantamentos vanilla
                int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bowStack);
                if (powerLevel > 0) {
                    arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D);
                }

                int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bowStack);
                if (punchLevel > 0) arrow.setKnockback(punchLevel);

                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bowStack) > 0) {
                    arrow.setSecondsOnFire(100);
                }

                // Crítico vanilla: só no full pull
                boolean critico = power >= 1.0F;

                float velocityMultiplier = 4F;
                arrow.setCritArrow(critico);

                if ((critico)) {
                    arrow.setBaseDamage(arrow.getBaseDamage());
                    //arrow.setNoGravity(true); int noGravTicks = 16; arrow.getPersistentData().putInt("tm_no_grav_ticks", noGravTicks);
                } else {
                    arrow.setBaseDamage(arrow.getBaseDamage() - 1.0D);
                }

                // Velocidade vanilla, mas com inaccuracy menor
                arrow.shootFromRotation(
                        player,
                        player.getXRot(),
                        player.getYRot(),
                        0.0F,
                        power * velocityMultiplier,
                        0.1F
                );

                var v = arrow.getDeltaMovement();
                float yaw = (float)(Mth.atan2(v.x, v.z) * (180F / Math.PI));
                float pitch = (float)(Mth.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * (180F / Math.PI));

                arrow.setYRot(yaw);
                arrow.setXRot(pitch);

                arrow.setOldPosAndRot();
                arrow.hasImpulse = true;

                if (creative || infinityLevel > 0) {
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                level.addFreshEntity(arrow);
            }

            level.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);

            bowStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(p.getUsedItemHand()));

            if (!creative && infinityLevel <= 0 && !ammo.isEmpty()) {
                ammo.shrink(1);
                if (ammo.isEmpty()) player.getInventory().removeItem(ammo);
            }

            player.awardStat(Stats.ITEM_USED.get(this));

        } finally {
            if (!level.isClientSide) setAimingSlow(player, false);
        }
    }
}