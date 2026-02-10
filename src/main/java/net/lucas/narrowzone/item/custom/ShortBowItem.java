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
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ShortBowItem extends BowItem {

    public static final int CHARGE_TICKS = 15;              // pull
    private static final float MIN_POWER_TO_SHOOT = 0.1F;   // precisão
    public static final double AIM_FAST_AMOUNT = 0.7D;      // lentidão

    public static final UUID AIM_FAST_UUID =
            UUID.fromString("8d8c9d3b-8af1-4b35-a6a1-4c6d6b9a8c23");

    public ShortBowItem(Properties pProperties) {
        super(pProperties);
    }

    private static AttributeModifier aimSlowModifier() {
        return new AttributeModifier(
                AIM_FAST_UUID,
                "longbow_aim_slow",
                AIM_FAST_AMOUNT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    private static void setAimingFast(Player player, boolean enabled) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        if (enabled) {
            if (attr.getModifier(AIM_FAST_UUID) == null) {
                attr.removeModifier(AIM_FAST_UUID);
                attr.addTransientModifier(aimSlowModifier());
            }
        } else {
            if (attr.getModifier(AIM_FAST_UUID) != null) {
                attr.removeModifier(AIM_FAST_UUID);
            }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (!level.isClientSide && entity instanceof Player player) {
            boolean aimingThis = player.isUsingItem() && player.getUseItem() == stack;
            setAimingFast(player, aimingThis);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player) {
            boolean aimingThis = player.isUsingItem() && player.getUseItem().getItem() == this;
            if (!aimingThis) setAimingFast(player, false);
        }
    }

    private static float getBowPower(int useTicks) {
        float t = (float) useTicks / (float) CHARGE_TICKS;
        t = Mth.clamp(t, 0.0F, 1.0F);
        // curva vanilla: (t^2 + 2t) / 3
        float power = (t * t + 2.0F * t) / 3.0F;
        return Mth.clamp(power, 0.0F, 1.0F);
    }

    public static int getArrows(float power) {
        if (power <= 0.80f) return 1;
        if (power <= 0.99f) return 2;
        return 3;
    }

    @Override
    public void releaseUsing(ItemStack bowStack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player)) return;

        try {
            int useTicks = this.getUseDuration(bowStack) - timeLeft;
            float power = getBowPower(useTicks);
            if (power < MIN_POWER_TO_SHOOT) return;

            int arrows = getArrows(power);

            boolean creative = player.getAbilities().instabuild;
            int infinityLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bowStack);

            if (!level.isClientSide) {
                for (int i = 0; i < arrows; i++) {

                    ItemStack ammo = player.getProjectile(bowStack);
                    if (ammo.isEmpty() && !creative && infinityLevel <= 0) break;

                    ItemStack arrowStack = ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo;
                    ArrowItem arrowItem = (arrowStack.getItem() instanceof ArrowItem ai) ? ai : (ArrowItem) Items.ARROW;

                    AbstractArrow arrow = arrowItem.createArrow(level, arrowStack, player);
                    arrow = this.customArrow(arrow);

                    // Encantamentos
                    int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bowStack);
                    if (powerLevel > 0) arrow.setBaseDamage(arrow.getBaseDamage() + powerLevel * 0.5D + 0.5D);

                    int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bowStack);
                    if (punchLevel > 0) arrow.setKnockback(punchLevel);

                    if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bowStack) > 0) {
                        arrow.setSecondsOnFire(100);
                    }

                    arrow.setCritArrow(power >= 1.0F);

                    // Velocidade
                    float VELOCITY_MULTIPLIER = 2F;

                    arrow.shootFromRotation(
                            player,
                            player.getXRot(),
                            player.getYRot(),
                            0.0F,
                            power * VELOCITY_MULTIPLIER,
                            10F
                    );

                    // alinhamento opcional
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

                    // som por flecha
                    level.playSound(null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARROW_SHOOT,
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F
                    );

                    // Consumo por flecha
                    if (!creative && infinityLevel <= 0 && !ammo.isEmpty()) {
                        ammo.shrink(1);
                        if (ammo.isEmpty()) player.getInventory().removeItem(ammo);
                    }
                }
                // Durabilidade por flecha
                bowStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(p.getUsedItemHand()));
            }

            player.awardStat(Stats.ITEM_USED.get(this));

        } finally {
            if (!level.isClientSide) setAimingFast(player, false);
        }
    }




}
