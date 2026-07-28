package net.lucas.narrowzone.enchantment;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.custom.LongBowItem;
import net.lucas.narrowzone.item.custom.ShortBowItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModRangedEnchantmentEffects {
    private static final String TAG_PREFIX = NarrowZone.MOD_ID + ":ranged_";

    private static final String TAG_ORIGIN_X = TAG_PREFIX + "origin_x";
    private static final String TAG_ORIGIN_Y = TAG_PREFIX + "origin_y";
    private static final String TAG_ORIGIN_Z = TAG_PREFIX + "origin_z";
    private static final String TAG_LONGSHOT_LEVEL = TAG_PREFIX + "longshot";
    private static final String TAG_DEADEYE_LEVEL = TAG_PREFIX + "deadeye";
    private static final String TAG_CHAIN_LEVEL = TAG_PREFIX + "chain_bolt";
    private static final String TAG_CHAIN_DEPTH = TAG_PREFIX + "chain_depth";
    private static final String TAG_CHAIN_GENERATED = TAG_PREFIX + "chain_generated";
    private static final String TAG_CHAIN_LAST_TARGET = TAG_PREFIX + "chain_last_target";
    private static final String TAG_SHOOTER_STATIONARY = TAG_PREFIX + "stationary_shot";

    private static final double STATIONARY_HORIZONTAL_SPEED_SQR = 0.0036D;

    private static final double LONGSHOT_BONUS_PER_BLOCK_PER_LEVEL = 0.025D;
    private static final double LONGSHOT_MAX_BONUS = 4.0D;

    private static final float DEADEYE_DAMAGE_PER_LEVEL = 0.75F;

    private static final float STEADY_DRAW_INPUT_MULTIPLIER = 2.50F;

    private static final double CHAIN_RADIUS = 7.0D;
    private static final float CHAIN_VELOCITY = 2.15F;
    private static final float CHAIN_INACCURACY = 1.5F;

    private ModRangedEnchantmentEffects() {
    }

    public static int applyQuickstring(ItemStack bow, int chargeTicks) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.QUICKSTRING.get(), bow);
        if (level <= 0 || !isBow(bow) || chargeTicks <= 0) {
            return chargeTicks;
        }

        int normalDrawDuration = getExpectedBowDrawDuration(bow);
        int quickDrawDuration = getQuickstringDrawDuration(bow, level);
        float multiplier = (float) normalDrawDuration / (float) quickDrawDuration;

        return Mth.clamp(Mth.ceil(chargeTicks * multiplier), 0, normalDrawDuration + 10);
    }

    public static int getQuickstringBonusTicks(ItemStack bow) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.QUICKSTRING.get(), bow);
        if (level <= 0 || !isBow(bow)) {
            return 0;
        }

        int normalDrawDuration = getExpectedBowDrawDuration(bow);
        int quickDrawDuration = getQuickstringDrawDuration(bow, level);
        return Math.max(1, normalDrawDuration - quickDrawDuration);
    }

    public static float getQuickstringAdjustedPullProgress(ItemStack bow, int rawUsedTicks, float normalDrawDuration) {
        if (normalDrawDuration <= 0.0F) {
            return 0.0F;
        }

        int adjustedCharge = applyQuickstring(bow, rawUsedTicks);
        return Mth.clamp((float) adjustedCharge / normalDrawDuration, 0.0F, 1.0F);
    }

    private static int getQuickstringDrawDuration(ItemStack bow, int level) {
        int normalDrawDuration = getExpectedBowDrawDuration(bow);
        float durationMultiplier = switch (Mth.clamp(level, 1, 3)) {
            case 1 -> 0.70F;
            case 2 -> 0.55F;
            default -> 0.40F;
        };

        return Math.max(3, Mth.ceil(normalDrawDuration * durationMultiplier));
    }

    private static int getExpectedBowDrawDuration(ItemStack bow) {
        if (bow.getItem() instanceof ShortBowItem) {
            return ShortBowItem.CHARGE_TICKS;
        }
        if (bow.getItem() instanceof LongBowItem) {
            return LongBowItem.CHARGE_TICKS;
        }
        return 20;
    }

    public static float getSteadyDrawInputMultiplier(int level) {
        return level > 0 ? STEADY_DRAW_INPUT_MULTIPLIER : 1.0F;
    }

    public static boolean hasSteadyDraw(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.STEADY_DRAW.get(), stack) > 0
                && isBowOrCrossbow(stack);
    }

    public static boolean isBow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BowItem;
    }

    public static boolean isCrossbow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CrossbowItem;
    }

    public static boolean isBowOrCrossbow(ItemStack stack) {
        return isBow(stack) || isCrossbow(stack);
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        ItemStack bow = event.getBow();
        if (!isBow(bow)) {
            return;
        }

        int adjustedCharge = applyQuickstring(bow, event.getCharge());
        if (adjustedCharge != event.getCharge()) {
            event.setCharge(adjustedCharge);
        }
    }

    @SubscribeEvent
    public static void onProjectileJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        CompoundTag data = arrow.getPersistentData();

        // Re-tagging a generated arrow would reset its chain depth and recurse forever.
        if (data.getBoolean(TAG_CHAIN_GENERATED)) {
            return;
        }

        if (!(arrow.getOwner() instanceof LivingEntity shooter)) {
            return;
        }

        ItemStack weapon = getLikelyRangedWeapon(shooter);
        if (!isBowOrCrossbow(weapon)) {
            return;
        }

        markArrowFromWeapon(arrow, weapon, shooter);
    }

    public static void markArrowFromWeapon(AbstractArrow arrow, ItemStack weapon, LivingEntity shooter) {
        if (arrow.level().isClientSide()) {
            return;
        }

        CompoundTag data = arrow.getPersistentData();
        data.putDouble(TAG_ORIGIN_X, shooter.getX());
        data.putDouble(TAG_ORIGIN_Y, shooter.getY(0.5D));
        data.putDouble(TAG_ORIGIN_Z, shooter.getZ());

        int longshot = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.LONGSHOT.get(), weapon);
        if (longshot > 0 && isBowOrCrossbow(weapon)) {
            data.putInt(TAG_LONGSHOT_LEVEL, longshot);
        }

        int deadeye = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DEADEYE.get(), weapon);
        if (deadeye > 0 && isBow(weapon)) {
            data.putInt(TAG_DEADEYE_LEVEL, deadeye);
            Vec3 movement = shooter.getDeltaMovement();
            boolean stationary = movement.horizontalDistanceSqr() <= STATIONARY_HORIZONTAL_SPEED_SQR;
            data.putBoolean(TAG_SHOOTER_STATIONARY, stationary);
        }

        int chainBolt = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CHAIN_BOLT.get(), weapon);
        if (chainBolt > 0 && isCrossbow(weapon)) {
            data.putInt(TAG_CHAIN_LEVEL, chainBolt);
            data.putInt(TAG_CHAIN_DEPTH, 0);
            data.putBoolean(TAG_CHAIN_GENERATED, false);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        CompoundTag data = arrow.getPersistentData();
        float amount = event.getAmount();

        int deadeye = data.getInt(TAG_DEADEYE_LEVEL);
        if (deadeye > 0 && data.getBoolean(TAG_SHOOTER_STATIONARY)) {
            amount += deadeye * DEADEYE_DAMAGE_PER_LEVEL;
        }

        int longshot = data.getInt(TAG_LONGSHOT_LEVEL);
        if (longshot > 0 && data.contains(TAG_ORIGIN_X)) {
            double dx = event.getEntity().getX() - data.getDouble(TAG_ORIGIN_X);
            double dy = event.getEntity().getY(0.5D) - data.getDouble(TAG_ORIGIN_Y);
            double dz = event.getEntity().getZ() - data.getDouble(TAG_ORIGIN_Z);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double bonus = Math.min(LONGSHOT_MAX_BONUS, distance * LONGSHOT_BONUS_PER_BLOCK_PER_LEVEL * longshot);
            amount += (float) bonus;
        }

        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }

        if (arrow.level().isClientSide()) {
            return;
        }

        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            return;
        }

        if (!(entityHit.getEntity() instanceof LivingEntity hitEntity)) {
            return;
        }

        CompoundTag data = arrow.getPersistentData();
        int chainLevel = data.getInt(TAG_CHAIN_LEVEL);
        if (chainLevel <= 0) {
            return;
        }

        int depth = data.getInt(TAG_CHAIN_DEPTH);
        int maxDepth = Math.min(3, Math.max(1, chainLevel));
        if (depth >= maxDepth) {
            return;
        }

        LivingEntity nextTarget = findNextChainTarget(arrow, hitEntity, CHAIN_RADIUS);
        if (nextTarget == null) {
            return;
        }

        spawnChainArrow(arrow, hitEntity, nextTarget, chainLevel, depth + 1);
    }

    private static ItemStack getLikelyRangedWeapon(LivingEntity shooter) {
        ItemStack useItem = shooter.getUseItem();
        if (isBowOrCrossbow(useItem) && hasAnyCustomRangedEnchant(useItem)) {
            return useItem;
        }

        ItemStack mainHand = shooter.getMainHandItem();
        if (isBowOrCrossbow(mainHand) && hasAnyCustomRangedEnchant(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = shooter.getOffhandItem();
        if (isBowOrCrossbow(offHand) && hasAnyCustomRangedEnchant(offHand)) {
            return offHand;
        }

        if (isBowOrCrossbow(useItem)) {
            return useItem;
        }
        if (isBowOrCrossbow(mainHand)) {
            return mainHand;
        }
        if (isBowOrCrossbow(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean hasAnyCustomRangedEnchant(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.STEADY_DRAW.get(), stack) > 0
                || EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.QUICKSTRING.get(), stack) > 0
                || EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DEADEYE.get(), stack) > 0
                || EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.LONGSHOT.get(), stack) > 0
                || EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CHAIN_BOLT.get(), stack) > 0;
    }

    private static LivingEntity findNextChainTarget(AbstractArrow sourceArrow, LivingEntity hitEntity, double radius) {
        Level level = sourceArrow.level();
        Entity owner = sourceArrow.getOwner();
        UUID lastTarget = getUuidFromTag(sourceArrow.getPersistentData(), TAG_CHAIN_LAST_TARGET);

        AABB searchBox = hitEntity.getBoundingBox().inflate(radius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate -> {
            if (!candidate.isAlive() || candidate.isSpectator()) {
                return false;
            }
            if (candidate == hitEntity || candidate == owner) {
                return false;
            }
            if (lastTarget != null && candidate.getUUID().equals(lastTarget)) {
                return false;
            }
            if (owner != null && owner.isAlliedTo(candidate)) {
                return false;
            }
            return candidate.distanceToSqr(hitEntity) <= radius * radius;
        });

        return candidates.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(hitEntity)))
                .orElse(null);
    }

    private static void spawnChainArrow(AbstractArrow sourceArrow, LivingEntity from, LivingEntity to, int chainLevel, int depth) {
        Level level = sourceArrow.level();
        Entity owner = sourceArrow.getOwner();

        Arrow chainedArrow;
        if (owner instanceof LivingEntity livingOwner) {
            chainedArrow = new Arrow(level, livingOwner);
        } else {
            chainedArrow = new Arrow(level, sourceArrow.getX(), sourceArrow.getY(), sourceArrow.getZ());
            if (owner != null) {
                chainedArrow.setOwner(owner);
            }
        }

        chainedArrow.setPos(from.getX(), from.getY(0.55D), from.getZ());
        chainedArrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        chainedArrow.setCritArrow(false);
        chainedArrow.setSecondsOnFire(sourceArrow.isOnFire() ? 4 : 0);

        double damageMultiplier = Math.max(0.35D, 0.75D - (depth - 1) * 0.15D);
        chainedArrow.setBaseDamage(Math.max(0.5D, sourceArrow.getBaseDamage() * damageMultiplier));

        CompoundTag sourceData = sourceArrow.getPersistentData();
        CompoundTag chainedData = chainedArrow.getPersistentData();
        chainedData.putBoolean(TAG_CHAIN_GENERATED, true);
        chainedData.putInt(TAG_CHAIN_LEVEL, chainLevel);
        chainedData.putInt(TAG_CHAIN_DEPTH, depth);
        chainedData.putUUID(TAG_CHAIN_LAST_TARGET, from.getUUID());

        if (sourceData.contains(TAG_ORIGIN_X)) {
            chainedData.putDouble(TAG_ORIGIN_X, sourceData.getDouble(TAG_ORIGIN_X));
            chainedData.putDouble(TAG_ORIGIN_Y, sourceData.getDouble(TAG_ORIGIN_Y));
            chainedData.putDouble(TAG_ORIGIN_Z, sourceData.getDouble(TAG_ORIGIN_Z));
        }
        if (sourceData.contains(TAG_LONGSHOT_LEVEL)) {
            chainedData.putInt(TAG_LONGSHOT_LEVEL, sourceData.getInt(TAG_LONGSHOT_LEVEL));
        }

        double dx = to.getX() - chainedArrow.getX();
        double dy = to.getY(0.5D) - chainedArrow.getY();
        double dz = to.getZ() - chainedArrow.getZ();
        chainedArrow.shoot(dx, dy, dz, CHAIN_VELOCITY, CHAIN_INACCURACY);

        level.addFreshEntity(chainedArrow);
    }

    private static UUID getUuidFromTag(CompoundTag tag, String key) {
        return tag.hasUUID(key) ? tag.getUUID(key) : null;
    }
}
