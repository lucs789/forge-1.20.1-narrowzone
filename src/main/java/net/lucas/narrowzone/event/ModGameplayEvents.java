package net.lucas.narrowzone.event;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModGameplayEvents {
    private static final String CHECKED_SKELETON_WEAPON_TAG = NarrowZone.MOD_ID + ":checked_skeleton_weapon";

    private static final float SKELETON_MOD_BOW_CHANCE = 0.45F;

    // Above the vanilla skeleton bow/melee goals, so these never fall back to melee.
    private static final int MOD_BOW_GOAL_PRIORITY = 2;

    private ModGameplayEvents() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.FLETCHER) {
            return;
        }

        List<VillagerTrades.ItemListing> apprenticeTrades = getTradesForLevel(event, 2);
        apprenticeTrades.add(new WeaponForEmeralds(ModItems.SHORT_BOW, 2, 8, 5));
        apprenticeTrades.add(new WeaponForEmeralds(ModItems.LONG_BOW, 2, 6, 10));

        List<VillagerTrades.ItemListing> journeymanTrades = getTradesForLevel(event, 3);
        journeymanTrades.add(new WeaponForEmeralds(ModItems.LIGHT_CROSSBOW, 3, 6, 10));
        journeymanTrades.add(new WeaponForEmeralds(ModItems.REPEATING_CROSSBOW, 3, 4, 15));
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof AbstractSkeleton skeleton)) {
            return;
        }

        ItemStack mainHand = skeleton.getMainHandItem();
        if (isModBow(mainHand)) {
            addModBowGoal(skeleton);
            return;
        }

        CompoundTag persistentData = skeleton.getPersistentData();
        if (persistentData.getBoolean(CHECKED_SKELETON_WEAPON_TAG)) {
            return;
        }
        persistentData.putBoolean(CHECKED_SKELETON_WEAPON_TAG, true);

        if (!mainHand.is(Items.BOW)) {
            return;
        }

        RandomSource random = skeleton.getRandom();
        if (random.nextFloat() >= SKELETON_MOD_BOW_CHANCE) {
            return;
        }

        Item replacementItem = random.nextBoolean() ? ModItems.SHORT_BOW.get() : ModItems.LONG_BOW.get();
        ItemStack replacement = copyWeaponData(mainHand, replacementItem);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, replacement);
        addModBowGoal(skeleton);
    }

    private static void addModBowGoal(AbstractSkeleton skeleton) {
        skeleton.goalSelector.addGoal(MOD_BOW_GOAL_PRIORITY, new ModBowSkeletonAttackGoal(skeleton, 1.0D, 20, 22.0F));
    }

    private static boolean isModBow(ItemStack stack) {
        return stack.is(ModItems.SHORT_BOW.get()) || stack.is(ModItems.LONG_BOW.get());
    }

    private static List<VillagerTrades.ItemListing> getTradesForLevel(VillagerTradesEvent event, int level) {
        List<VillagerTrades.ItemListing> trades = event.getTrades().get(level);
        if (trades == null) {
            trades = new ArrayList<>();
            event.getTrades().put(level, trades);
        }
        return trades;
    }

    private static ItemStack copyWeaponData(ItemStack original, Item replacementItem) {
        ItemStack replacement = new ItemStack(replacementItem, Math.max(1, original.getCount()));

        if (original.hasTag() && original.getTag() != null) {
            replacement.setTag(original.getTag().copy());
        }

        if (replacement.isDamageableItem()) {
            replacement.setDamageValue(Math.min(original.getDamageValue(), replacement.getMaxDamage() - 1));
        }

        return replacement;
    }

    private static final class ModBowSkeletonAttackGoal extends Goal {
        private static final double MIN_COMBAT_DISTANCE_SQR = 7.0D * 7.0D;
        private static final double PREFERRED_COMBAT_DISTANCE_SQR = 14.0D * 14.0D;

        private final AbstractSkeleton skeleton;
        private final double speedModifier;
        private final int attackInterval;
        private final float attackRadiusSqr;
        private int attackTime = -1;
        private int seeTime;
        private int strafingTime = -1;
        private boolean strafingClockwise;
        private boolean strafingBackwards;

        private ModBowSkeletonAttackGoal(AbstractSkeleton skeleton, double speedModifier, int attackInterval, float attackRadius) {
            this.skeleton = skeleton;
            this.speedModifier = speedModifier;
            this.attackInterval = attackInterval;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.skeleton.getTarget();
            return target != null && target.isAlive() && isModBow(this.skeleton.getMainHandItem());
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.skeleton.getTarget();
            return target != null && target.isAlive() && isModBow(this.skeleton.getMainHandItem());
        }

        @Override
        public void start() {
            super.start();
            this.skeleton.setAggressive(true);
            this.attackTime = 0;
        }

        @Override
        public void stop() {
            super.stop();
            this.skeleton.setAggressive(false);
            this.seeTime = 0;
            this.attackTime = -1;
            this.strafingTime = -1;
            this.skeleton.stopUsingItem();
            this.skeleton.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.skeleton.getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }

            double distanceSqr = this.skeleton.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSee = this.skeleton.getSensing().hasLineOfSight(target);
            boolean hadSight = this.seeTime > 0;

            if (canSee != hadSight) {
                this.seeTime = 0;
            }

            if (canSee) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            updateMovement(target, distanceSqr, canSee);
            updateAttack(target, canSee);
        }

        private void updateMovement(LivingEntity target, double distanceSqr, boolean canSee) {
            if (canSee && distanceSqr <= this.attackRadiusSqr) {
                this.skeleton.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.skeleton.getNavigation().moveTo(target, this.speedModifier);
                this.strafingTime = -1;
                this.skeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
                return;
            }

            if (this.strafingTime >= 20) {
                if (this.skeleton.getRandom().nextFloat() < 0.3F) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if (this.skeleton.getRandom().nextFloat() < 0.3F) {
                    this.strafingBackwards = !this.strafingBackwards;
                }

                this.strafingTime = 0;
            }

            float forward;
            if (distanceSqr < MIN_COMBAT_DISTANCE_SQR) {
                forward = -0.8F;
                this.strafingBackwards = true;
            } else if (distanceSqr > PREFERRED_COMBAT_DISTANCE_SQR) {
                forward = 0.35F;
                this.strafingBackwards = false;
            } else {
                forward = this.strafingBackwards ? -0.45F : 0.45F;
            }

            float sideways = this.strafingClockwise ? 0.65F : -0.65F;
            this.skeleton.getMoveControl().strafe(forward, sideways);
            this.skeleton.lookAt(target, 30.0F, 30.0F);
        }

        private void updateAttack(LivingEntity target, boolean canSee) {
            if (this.skeleton.isUsingItem()) {
                if (!canSee && this.seeTime < -60) {
                    this.skeleton.stopUsingItem();
                    return;
                }

                int useTicks = this.skeleton.getTicksUsingItem();
                ItemStack bow = this.skeleton.getMainHandItem();
                int chargeTicks = bow.is(ModItems.SHORT_BOW.get()) ? 15 : 30;

                if (useTicks >= chargeTicks) {
                    this.skeleton.stopUsingItem();
                    shootModBow(target, bow);
                    this.attackTime = this.attackInterval;
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.skeleton.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }

        private void shootModBow(LivingEntity target, ItemStack bow) {
            if (bow.is(ModItems.SHORT_BOW.get())) {
                shootArrow(target, bow, 1.6F, 11.0F, 0.0D, 0.16D, 0.55D);
                shootArrow(target, bow, 1.6F, 13.0F, 0.0D, 0.16D, 0.55D);
                shootArrow(target, bow, 1.6F, 13.0F, 0.0D, 0.16D, 0.55D);
            } else {
                shootArrow(target, bow, 2.9F, 0.35F, 1.0D, 0.0D, 0.42D);
            }

            this.skeleton.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.skeleton.getRandom().nextFloat() * 0.4F + 0.8F));
            bow.hurtAndBreak(1, this.skeleton, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }

        private void shootArrow(LivingEntity target, ItemStack bow, float velocity, float inaccuracy, double extraDamage, double arcCompensation, double targetHeightScale) {
            ItemStack arrowStack = this.skeleton.getProjectile(bow);
            if (arrowStack.isEmpty()) {
                arrowStack = new ItemStack(Items.ARROW);
            }

            AbstractArrow arrow = ProjectileUtil.getMobArrow(this.skeleton, arrowStack, velocity);
            applyBowEnchantments(arrow, bow);

            if (extraDamage != 0.0D) {
                arrow.setBaseDamage(arrow.getBaseDamage() + extraDamage);
            }

            double x = target.getX() - this.skeleton.getX();
            double y = getTargetAimY(target, targetHeightScale) - arrow.getY();
            double z = target.getZ() - this.skeleton.getZ();
            double horizontalDistance = Math.sqrt(x * x + z * z);

            arrow.shoot(x, y + horizontalDistance * arcCompensation, z, velocity, inaccuracy);
            this.skeleton.level().addFreshEntity(arrow);
        }

        private double getTargetAimY(LivingEntity target, double heightScale) {
            return target.getY() + target.getBbHeight() * heightScale;
        }

        private void applyBowEnchantments(AbstractArrow arrow, ItemStack bow) {
            int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
            if (power > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + (double) power * 0.5D + 0.5D);
            }

            int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
            if (punch > 0) {
                arrow.setKnockback(punch);
            }

            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) {
                arrow.setSecondsOnFire(100);
            }

            arrow.setCritArrow(true);
            arrow.setYRot(Mth.wrapDegrees(arrow.getYRot()));
        }
    }

    private static final class WeaponForEmeralds implements VillagerTrades.ItemListing {
        private final RegistryObject<Item> item;
        private final int emeraldCost;
        private final int maxUses;
        private final int villagerXp;

        private WeaponForEmeralds(RegistryObject<Item> item, int emeraldCost, int maxUses, int villagerXp) {
            this.item = item;
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.villagerXp = villagerXp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    new ItemStack(item.get()),
                    maxUses,
                    villagerXp,
                    0.05F
            );
        }
    }
}
