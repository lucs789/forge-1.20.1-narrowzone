package net.lucas.narrowzone.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

public class RepeatingCrossbowItem extends CrossbowItem {
    public static final int MAX_AMMO = 12;
    public static final float VELOCITY_MULTIPLIER = 2.0F;
    public static final float INACCURACY = 1.0F;
    public static final int RELOAD_TICKS = 80;
    public static final int FIRE_TICKS = 10;

    private static final String TAG_MAG = "tm_magazine";
    private static final String TAG_MODE = "tm_mode";
    public static final String TAG_ANIM = "tm_fire_anim";
    public static final String TAG_STOP = "tm_stop";

    public static final int MODE_NONE = 0;
    public static final int MODE_FIRE = 1;
    public static final int MODE_RELOAD = 2;

    public RepeatingCrossbowItem(Properties properties) {
        super(properties);
    }

    public static int getMagCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MAG, Tag.TAG_LIST)) {
            return 0;
        }
        return tag.getList(TAG_MAG, Tag.TAG_COMPOUND).size();
    }

    private static ItemStack popFirstAmmo(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_MAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        ListTag list = tag.getList(TAG_MAG, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return ItemStack.EMPTY;
        }

        CompoundTag ammoTag = list.getCompound(0);
        ItemStack ammo = ItemStack.of(ammoTag);
        list.remove(0);

        if (list.isEmpty()) {
            tag.remove(TAG_MAG);
        } else {
            tag.put(TAG_MAG, list);
        }

        return ammo;
    }

    private static void pushAmmo(ItemStack crossbow, ItemStack ammo) {
        if (ammo.isEmpty()) {
            return;
        }

        CompoundTag tag = crossbow.getOrCreateTag();
        ListTag list = tag.contains(TAG_MAG, Tag.TAG_LIST) ? tag.getList(TAG_MAG, Tag.TAG_COMPOUND) : new ListTag();

        CompoundTag ammoTag = new CompoundTag();
        ItemStack copy = ammo.copy();
        copy.setCount(1);
        copy.save(ammoTag);
        list.add(ammoTag);

        tag.put(TAG_MAG, list);
    }

    public static int getReloadDurationFire(ItemStack stack) {
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return Math.max(5, FIRE_TICKS - 2 * quickCharge);
    }

    public static int getReloadDurationLoad(ItemStack stack) {
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return Math.max(5, RELOAD_TICKS - 10 * quickCharge);
    }

    private static boolean hasAnyAmmo(Player player) {
        if (isValidAmmo(player.getOffhandItem())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isValidAmmo(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidAmmo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof ArrowItem || stack.is(Items.FIREWORK_ROCKET);
    }

    private static int loadMagazine(Player player, ItemStack crossbow, int maxAmmo) {
        int loaded = 0;
        boolean creative = player.getAbilities().instabuild;

        while (getMagCount(crossbow) < maxAmmo) {
            ItemStack ammo = findNextAmmoStack(player);
            if (ammo.isEmpty()) {
                if (!creative) {
                    break;
                }
                pushAmmo(crossbow, new ItemStack(Items.ARROW));
                loaded++;
                continue;
            }

            pushAmmo(crossbow, ammo);
            loaded++;
            if (!creative) {
                ammo.shrink(1);
            }
        }

        return loaded;
    }

    private static ItemStack findNextAmmoStack(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (isValidAmmo(offhand)) {
            return offhand;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (isValidAmmo(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static void setChargedProjectiles(ItemStack crossbow, ItemStack projectile, int count) {
        ListTag list = new ListTag();
        for (int i = 0; i < count; i++) {
            CompoundTag tag = new CompoundTag();
            ItemStack copy = projectile.copy();
            copy.setCount(1);
            copy.save(tag);
            list.add(tag);
        }
        crossbow.getOrCreateTag().put("ChargedProjectiles", list);
    }

    public static int getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? MODE_NONE : tag.getInt(TAG_MODE);
    }

    private static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode);
    }

    private static InteractionHand getHandHolding(Player player, ItemStack stack) {
        if (player.getMainHandItem() == stack || ItemStack.matches(player.getMainHandItem(), stack)) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem() == stack || ItemStack.matches(player.getOffhandItem(), stack)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static ItemStack getNextAmmoType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        ListTag list = tag.getList(TAG_MAG, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return ItemStack.of(list.getCompound(0));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (getMagCount(stack) > 0) {
            setMode(stack, MODE_FIRE);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!player.getAbilities().instabuild && !hasAnyAmmo(player)) {
            return InteractionResultHolder.fail(stack);
        }

        setMode(stack, MODE_RELOAD);
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int mode = getMode(stack);
        if (mode == MODE_NONE) {
            mode = getMagCount(stack) > 0 ? MODE_FIRE : MODE_RELOAD;
            setMode(stack, mode);
        }

        if (level.isClientSide) {
            return;
        }

        int usedTicks = getUseDuration(stack) - remainingUseDuration;

        if (mode == MODE_RELOAD) {
            int reloadDuration = getReloadDurationLoad(stack);
            if (usedTicks >= reloadDuration) {
                int loaded = loadMagazine(player, stack, MAX_AMMO);
                if (loaded > 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);
                    setMode(stack, MODE_NONE);
                    stack.getOrCreateTag().putBoolean(TAG_STOP, true);
                    player.stopUsingItem();
                } else {
                    player.stopUsingItem();
                    setMode(stack, MODE_NONE);
                }
            }
            return;
        }

        if (mode == MODE_FIRE) {
            if (getMagCount(stack) <= 0) {
                setMode(stack, MODE_RELOAD);
                return;
            }

            InteractionHand hand = getHandHolding(player, stack);
            if (hand == null) {
                player.stopUsingItem();
                setMode(stack, MODE_NONE);
                return;
            }

            int fireDuration = getReloadDurationFire(stack);
            if (usedTicks < fireDuration) {
                return;
            }

            ItemStack ammo = popFirstAmmo(stack);
            if (ammo.isEmpty()) {
                setMode(stack, MODE_RELOAD);
                return;
            }

            boolean multishot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, stack) > 0;
            int projectileCount = multishot ? 3 : 1;
            setChargedProjectiles(stack, ammo, projectileCount);
            CrossbowItem.setCharged(stack, true);

            float velocity = ammo.is(Items.FIREWORK_ROCKET) ? 1.6F : VELOCITY_MULTIPLIER;
            CrossbowItem.performShooting(level, player, hand, stack, velocity, INACCURACY);

            CrossbowItem.setCharged(stack, false);
            stack.removeTagKey("ChargedProjectiles");
            stack.getOrCreateTag().putInt(TAG_ANIM, 0);

            if (getMagCount(stack) <= 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_QUICK_CHARGE_1, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            setMode(stack, MODE_NONE);
            stack.getOrCreateTag().putBoolean(TAG_STOP, true);
            player.stopUsingItem();
        }

        if (getMagCount(stack) <= 0) {
            if (!player.getAbilities().instabuild && !hasAnyAmmo(player)) {
                player.stopUsingItem();
                setMode(stack, MODE_NONE);
                return;
            }
            setMode(stack, MODE_RELOAD);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player) || level.isClientSide) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(TAG_STOP)) {
            tag.putBoolean(TAG_STOP, false);
            return;
        }

        setMode(stack, MODE_NONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int count = getMagCount(stack);
        tooltip.add(Component.literal("Ammo: " + count + "/" + MAX_AMMO).withStyle(ChatFormatting.GRAY));

        ItemStack next = getNextAmmoType(stack);
        if (!next.isEmpty()) {
            tooltip.add(Component.literal("Next Projectile: [").append(next.getHoverName()).append("]").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!level.isClientSide) {
            CrossbowItem.setCharged(stack, getMagCount(stack) > 0);
            return;
        }

        boolean usingThis = player.isUsingItem() && ItemStack.matches(player.getUseItem(), stack);
        if (usingThis && getMode(stack) == MODE_FIRE) {
            int fireDuration = getReloadDurationFire(stack);
            int anim = stack.getOrCreateTag().getInt(TAG_ANIM);
            if (anim < fireDuration) {
                stack.getOrCreateTag().putInt(TAG_ANIM, anim + 1);
            }
        } else {
            stack.getOrCreateTag().putInt(TAG_ANIM, 0);
        }
    }
}
