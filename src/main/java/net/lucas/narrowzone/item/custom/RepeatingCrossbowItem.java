package net.lucas.narrowzone.item.custom;

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
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
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

    private static final int MODE_NONE = 0;
    private static final int MODE_FIRE = 1;
    private static final int MODE_RELOAD = 2;

    public RepeatingCrossbowItem(Properties pProperties) {
        super(pProperties);
    }

    // Magazine: NBT helpers
    public static int getMagCount(ItemStack crossbow) {
        CompoundTag tag = crossbow.getTag();
        if (tag == null || !tag.contains(TAG_MAG, Tag.TAG_LIST)) return 0;
        return tag.getList(TAG_MAG, Tag.TAG_COMPOUND).size();
    }

    private static ItemStack popFirstAmmo(ItemStack crossbow) {
        CompoundTag tag = crossbow.getOrCreateTag();
        if (!tag.contains(TAG_MAG, Tag.TAG_LIST)) return ItemStack.EMPTY;

        ListTag list = tag.getList(TAG_MAG, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return ItemStack.EMPTY;

        CompoundTag ammoTag = list.getCompound(0);
        ItemStack ammo = ItemStack.of(ammoTag);

        list.remove(0);

        if (list.isEmpty()) tag.remove(TAG_MAG);
        else tag.put(TAG_MAG, list);

        return ammo;
    }

    private static void pushAmmo(ItemStack crossbow, ItemStack oneAmmo) {
        if (oneAmmo.isEmpty()) return;

        CompoundTag tag = crossbow.getOrCreateTag();
        ListTag list = tag.contains(TAG_MAG, Tag.TAG_LIST) ? tag.getList(TAG_MAG, Tag.TAG_COMPOUND) : new ListTag();

        CompoundTag ammoTag = new CompoundTag();
        ItemStack copy = oneAmmo.copy();
        copy.setCount(1);
        copy.save(ammoTag);

        list.add(ammoTag);
        tag.put(TAG_MAG, list);
    }

    // Reload logic
    public static int getReloadDurationFire(ItemStack stack) {
        int qc = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return Math.max(5, FIRE_TICKS - 2 * qc);
    }

    public static int getReloadDurationLoad(ItemStack stack) {
        int qc = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return Math.max(5, RELOAD_TICKS - 10 * qc);
    }

    private static boolean hasAnyAmmo(Player player) {
        // procura em offhand e inventário por qualquer ArrowItem ou FireworkRocket
        if (isValidAmmo(player.getOffhandItem())) return true;
        for (ItemStack s : player.getInventory().items) {
            if (isValidAmmo(s)) return true;
        }
        return false;
    }

    private static boolean isValidAmmo(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        return (s.getItem() instanceof ArrowItem) || s.is(Items.FIREWORK_ROCKET);
    }

    // Carrega até 'limit' munições no magazine (mistura permitida). Retorna quantas carregou
    private static int loadMagazine(Player player, ItemStack crossbow, int limit) {
        int loaded = 0;
        boolean creative = player.getAbilities().instabuild;

        while (getMagCount(crossbow) < limit) {
            ItemStack src = findNextAmmoStack(player);

            if (src.isEmpty()) {
                if (creative) {
                    // creative sem munição: coloca flecha normal
                    pushAmmo(crossbow, new ItemStack(Items.ARROW));
                    loaded++;
                    continue;
                }
                break;
            }

            pushAmmo(crossbow, src);
            loaded++;

            if (!creative) {
                src.shrink(1);
            }
        }
        return loaded;
    }

    // Retorna uma referência para o stack no inventário/offhand (para poder shrink)
    private static ItemStack findNextAmmoStack(Player player) {
        // prioridade: offhand
        ItemStack off = player.getOffhandItem();
        if (isValidAmmo(off)) return off;

        // depois inventário (ordem natural)
        for (ItemStack s : player.getInventory().items) {
            if (isValidAmmo(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void setChargedProjectiles(ItemStack crossbow, ItemStack ammo, int shots) {
        ListTag list = new ListTag();

        for (int i = 0; i < shots; i++) {
            CompoundTag ammoTag = new CompoundTag();
            ItemStack one = ammo.copy();
            one.setCount(1);
            one.save(ammoTag);
            list.add(ammoTag);
        }

        crossbow.getOrCreateTag().put("ChargedProjectiles", list);
    }


    // Mode + cooldown
    public static int getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? MODE_NONE : tag.getInt(TAG_MODE);
    }

    private static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode);
    }

    private static InteractionHand getHandHolding(Player player, ItemStack stack) {
        if (ItemStack.matches(player.getMainHandItem(), stack)) return InteractionHand.MAIN_HAND;
        if (ItemStack.matches(player.getOffhandItem(), stack)) return InteractionHand.OFF_HAND;
        return null;
    }

    public static ItemStack getNextAmmoType(ItemStack crossbow) {

        CompoundTag tag = crossbow.getTag();
        if (tag == null || !tag.contains(TAG_MAG, Tag.TAG_LIST)) return ItemStack.EMPTY;

        ListTag list = tag.getList(TAG_MAG, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return ItemStack.EMPTY;

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
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!(living instanceof Player player)) return;

        int mode = getMode(stack);

        if (mode == MODE_NONE) {
            mode = (getMagCount(stack) > 0) ? MODE_FIRE : MODE_RELOAD;
            setMode(stack, mode);
        }

        if (level.isClientSide) return;

        int usedTicks = this.getUseDuration(stack) - remainingUseDuration;

        //MODE RELOAD
        if (mode == MODE_RELOAD) {
            int reloadTicks = getReloadDurationLoad(stack);

            if (usedTicks >= reloadTicks) {
                int loaded = loadMagazine(player, stack, MAX_AMMO);

                if (loaded > 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);

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

// MODE FIRE
        if (mode == MODE_FIRE) {

            if (getMagCount(stack) <= 0) {
                setMode(stack, MODE_RELOAD);
                return;
            }

            InteractionHand realHand = getHandHolding(player, stack);
            if (realHand == null) {
                player.stopUsingItem();
                setMode(stack, MODE_NONE);
                return;
            }

            int fireTicks = getReloadDurationFire(stack);

            if (usedTicks < fireTicks) return;

            ItemStack ammo = popFirstAmmo(stack);
            if (ammo.isEmpty()) {
                setMode(stack, MODE_RELOAD);
                return;
            }

            boolean multishot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, stack) > 0;
            int shots = multishot ? 3 : 1;

            setChargedProjectiles(stack, ammo, shots);
            CrossbowItem.setCharged(stack, true);

            float velocity = ammo.is(Items.FIREWORK_ROCKET) ? 1.6F : VELOCITY_MULTIPLIER;
            CrossbowItem.performShooting(level, player, realHand, stack, velocity, INACCURACY);

            CrossbowItem.setCharged(stack, false);
            stack.removeTagKey("ChargedProjectiles");

            stack.getOrCreateTag().putInt(TAG_ANIM, 0);

            if (getMagCount(stack) <= 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
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
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {

        // sem super release using
        if (!(living instanceof Player)) return;

        if (level.isClientSide) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(TAG_STOP)) {
            tag.putBoolean(TAG_STOP, false);
            return;
        }

        // só se o player realmente soltou
        setMode(stack, MODE_NONE);
    }

    @Override
    public void appendHoverText (ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int mag = getMagCount(stack);

        tooltip.add(Component.literal("Ammo: [" + mag + "/" + MAX_AMMO + "]"));

        ItemStack next = getNextAmmoType(stack);

        if (!next.isEmpty()) {
            tooltip.add(Component.literal("Next Projectile: [")
                    .append(next.getHoverName())
                    .append("]")
            );
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        //debug(stack, player);

        // server: lógica
        if (!level.isClientSide) {
            CrossbowItem.setCharged(stack, getMagCount(stack) > 0);
            return;
        }

        // client: animação/render
        boolean usingThis = player.isUsingItem() && ItemStack.isSameItemSameTags(player.getUseItem(), stack);

        if (usingThis && getMode(stack) == MODE_FIRE) {
            int fireTicks = getReloadDurationFire(stack);
            int anim = stack.getOrCreateTag().getInt(TAG_ANIM);
            if (anim < fireTicks) stack.getOrCreateTag().putInt(TAG_ANIM, anim + 1);
        } else {
            stack.getOrCreateTag().putInt(TAG_ANIM, 0);
        }
    }


    private void debug (ItemStack stack, Player player) {
        if (player.tickCount % 10 == 0) {
            player.sendSystemMessage(Component.literal("mode = " + getMode(stack) + " anim = " + stack.getOrCreateTag().getInt(TAG_ANIM)));
        }
    }
}
