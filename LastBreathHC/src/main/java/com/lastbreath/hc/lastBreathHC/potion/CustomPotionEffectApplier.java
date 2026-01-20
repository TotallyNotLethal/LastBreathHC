package com.lastbreath.hc.lastBreathHC.potion;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Effect ID mapping table (event -> handler):
 * <ul>
 *     <li>Movement/ambient: ember_aura, lava_walker, clear_sight, silent_steps, steadfast, night_echo,
 *     water_affinity, stone_sense, windstep, shadow_veil, sun_blessed, echo_step, deep_breathing,
 *     restless_spirits, cursed_steps, noisy_presence, shadow_curse, heavy_lungs, gloom.</li>
 *     <li>Block damage/break: swift_miner, clumsy_hands, extra_ore_chance, keen_harvest, crystal_luck.</li>
 *     <li>Combat/impact: hardy_skin, steady_guard, frost_resist, nimble_feet, blaze_blood, hunter_focus,
 *     quick_reflexes, shielded_heart, thunder_guard, root_grip, quake_guard, brittle_bones, frail_skin,
 *     cold_sweat, burning_blood.</li>
 *     <li>Resource/utility: sturdy_tools, lucky_loot, lucky_salvage, exp_drain, hungry_mind, quick_heal,
 *     mana_leak, iron_stomach, poison_resist, shaky_aim, beast_tamer.</li>
 * </ul>
 */
public class CustomPotionEffectApplier implements Listener {

    private static final int TICKS_PER_SECOND = 20;

    private final LastBreathHC plugin;
    private final CustomPotionEffectManager effectManager;
    private final Random random = new Random();
    private final Map<UUID, Map<String, Long>> effectCooldowns = new HashMap<>();
    private final Map<String, Consumer<Player>> movementHandlers = new HashMap<>();

    public CustomPotionEffectApplier(LastBreathHC plugin, CustomPotionEffectManager effectManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
        registerMovementHandlers();
    }

    private void registerMovementHandlers() {
        movementHandlers.put("ember_aura", this::applyEmberAura);
        movementHandlers.put("lava_walker", this::applyLavaWalker);
        movementHandlers.put("clear_sight", this::applyClearSight);
        movementHandlers.put("silent_steps", this::applySilentSteps);
        movementHandlers.put("steadfast", this::applySteadfast);
        movementHandlers.put("night_echo", this::applyNightEcho);
        movementHandlers.put("water_affinity", this::applyWaterAffinity);
        movementHandlers.put("stone_sense", this::applyStoneSense);
        movementHandlers.put("windstep", this::applyWindstep);
        movementHandlers.put("shadow_veil", this::applyShadowVeil);
        movementHandlers.put("sun_blessed", this::applySunBlessed);
        movementHandlers.put("echo_step", this::applyEchoStep);
        movementHandlers.put("deep_breathing", this::applyDeepBreathing);
        movementHandlers.put("restless_spirits", this::applyRestlessSpirits);
        movementHandlers.put("cursed_steps", this::applyCursedSteps);
        movementHandlers.put("noisy_presence", this::applyNoisyPresence);
        movementHandlers.put("shadow_curse", this::applyShadowCurse);
        movementHandlers.put("heavy_lungs", this::applyHeavyLungs);
        movementHandlers.put("gloom", this::applyGloom);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        for (Map.Entry<String, Consumer<Player>> entry : movementHandlers.entrySet()) {
            if (hasEffect(player, entry.getKey())) {
                entry.getValue().accept(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (hasEffect(player, "swift_miner")) {
            applySwiftMiner(player, event);
        }
        if (hasEffect(player, "clumsy_hands")) {
            applyClumsyHands(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hasEffect(player, "extra_ore_chance")) {
            applyExtraOreChance(player, event);
        }
        if (hasEffect(player, "keen_harvest")) {
            applyKeenHarvest(player, event);
        }
        if (hasEffect(player, "crystal_luck")) {
            applyCrystalLuck(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "hardy_skin")) {
            applyHardySkin(player, event);
        }
        if (hasEffect(player, "frost_resist")) {
            applyFrostResist(player, event);
        }
        if (hasEffect(player, "thunder_guard")) {
            applyThunderGuard(player, event);
        }
        if (hasEffect(player, "quake_guard")) {
            applyQuakeGuard(player, event);
        }
        if (hasEffect(player, "brittle_bones")) {
            applyBrittleBones(player, event);
        }
        if (hasEffect(player, "frail_skin")) {
            applyFrailSkin(player, event);
        }
        if (hasEffect(player, "cold_sweat")) {
            applyColdSweat(player, event);
        }
        if (hasEffect(player, "nimble_feet")) {
            applyNimbleFeet(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (hasEffect(player, "steady_guard")) {
                applySteadyGuard(player);
            }
            if (hasEffect(player, "blaze_blood")) {
                applyBlazeBlood(player, event);
            }
            if (hasEffect(player, "quick_reflexes")) {
                applyQuickReflexes(player, event);
            }
            if (hasEffect(player, "shielded_heart")) {
                applyShieldedHeart(player);
            }
            if (hasEffect(player, "root_grip")) {
                applyRootGrip(player);
            }
            if (hasEffect(player, "burning_blood")) {
                applyBurningBlood(player);
            }
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter && hasEffect(shooter, "hunter_focus")) {
                applyHunterFocus(shooter, event);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (hasEffect(killer, "lucky_loot")) {
            applyLuckyLoot(killer, event);
        }
        if (hasEffect(killer, "exp_drain")) {
            applyExpDrain(killer, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (hasEffect(player, "sturdy_tools")) {
            applySturdyTools(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "hungry_mind")) {
            applyHungryMind(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "quick_heal")) {
            applyQuickHeal(player, event);
        }
        if (hasEffect(player, "mana_leak")) {
            applyManaLeak(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getNewEffect() == null) {
            return;
        }
        if (hasEffect(player, "iron_stomach")) {
            applyIronStomach(player, event);
        }
        if (hasEffect(player, "poison_resist")) {
            applyPoisonResist(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (hasEffect(player, "lucky_salvage")) {
            applyLuckySalvage(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "shaky_aim")) {
            applyShakyAim(player, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "beast_tamer")) {
            applyBeastTamer(player, event);
        }
    }

    private void applyExtraOreChance(Player player, BlockBreakEvent event) {
        Material type = event.getBlock().getType();

        if (!type.name().endsWith("_ORE") && type != Material.ANCIENT_DEBRIS) {
            return;
        }

        if (!triggerWithCooldown(player, "extra_ore_chance", 2 * TICKS_PER_SECOND, 0.2)) {
            return;
        }

        for (ItemStack drop : event.getBlock().getDrops(player.getInventory().getItemInMainHand(), player)) {
            event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation(),
                    drop
            );
        }

        player.playSound(
                event.getBlock().getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.6f,
                1.2f
        );
    }


    private void applyHardySkin(Player player, EntityDamageEvent event) {
        if (!triggerWithCooldown(player, "hardy_skin", 3 * TICKS_PER_SECOND, 0.35)) {
            return;
        }
        event.setDamage(Math.max(0, event.getDamage() - 1.0));
    }

    private void applyEmberAura(Player player) {
        if (!triggerWithCooldown(player, "ember_aura", 6 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        for (Entity entity : player.getNearbyEntities(6, 4, 6)) {
            if (entity instanceof Monster monster) {
                monster.setFireTicks(60);
            }
        }
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 12, 0.6, 0.6, 0.6, 0.01);
    }

    private void applySwiftMiner(Player player, BlockDamageEvent event) {
        Material type = event.getBlock().getType();
        if (!(type == Material.STONE || type == Material.COBBLESTONE || type == Material.DEEPSLATE
                || type == Material.ANDESITE || type == Material.GRANITE || type == Material.DIORITE)) {
            return;
        }
        if (!triggerWithCooldown(player, "swift_miner", 2 * TICKS_PER_SECOND, 0.2)) {
            return;
        }
        event.setInstaBreak(true);
        event.getBlock().breakNaturally(player.getInventory().getItemInMainHand());
    }

    private void applyKeenHarvest(Player player, BlockBreakEvent event) {
        if (!(event.getBlock().getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }
        if (!triggerWithCooldown(player, "keen_harvest", 2 * TICKS_PER_SECOND, 0.2)) {
            return;
        }
        List<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand(), player).stream().toList();
        if (!drops.isEmpty()) {
            ItemStack extra = drops.get(0).clone();
            extra.setAmount(1);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), extra);
        }
    }

    private void applyLuckyLoot(Player player, EntityDeathEvent event) {
        if (!triggerWithCooldown(player, "lucky_loot", 4 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        if (event.getDrops().isEmpty()) {
            return;
        }
        ItemStack extra = event.getDrops().get(random.nextInt(event.getDrops().size())).clone();
        extra.setAmount(1);
        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), extra);
    }

    private void applySteadyGuard(Player player) {
        if (!triggerWithCooldown(player, "steady_guard", 5 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setVelocity(player.getVelocity().multiply(0.1));
            }
        }.runTask(plugin);
    }

    private void applyFrostResist(Player player, EntityDamageEvent event) {
        if (!(event.getCause() == EntityDamageEvent.DamageCause.FREEZE)) {
            return;
        }
        if (!triggerWithCooldown(player, "frost_resist", 2 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        event.setDamage(Math.max(0, event.getDamage() - 1.0));
        player.setFreezeTicks(Math.max(0, player.getFreezeTicks() - 40));
    }

    private void applyLavaWalker(Player player) {
        Block below = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (below.getType() != Material.LAVA) {
            return;
        }
        if (!triggerWithCooldown(player, "lava_walker", TICKS_PER_SECOND, 1.0)) {
            return;
        }
        below.setType(Material.MAGMA_BLOCK);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (below.getType() == Material.MAGMA_BLOCK) {
                    below.setType(Material.LAVA);
                }
            }
        }.runTaskLater(plugin, 3 * TICKS_PER_SECOND);
    }

    private void applyNimbleFeet(Player player) {
        if (!triggerWithCooldown(player, "nimble_feet", 5 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3 * TICKS_PER_SECOND, 1, true, true, true));
    }

    private void applySturdyTools(Player player, PlayerItemDamageEvent event) {
        if (!triggerWithCooldown(player, "sturdy_tools", 2 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        event.setCancelled(true);
    }

    private void applyHunterFocus(Player shooter, EntityDamageByEntityEvent event) {
        if (!triggerWithCooldown(shooter, "hunter_focus", 2 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * TICKS_PER_SECOND, 0, true, true, true));
        }
    }

    private void applyClearSight(Player player) {
        if (!triggerWithCooldown(player, "clear_sight", 10 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        int radius = 6;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = player.getLocation().getBlock().getRelative(x, y, z);
                    Material type = block.getType();
                    if (type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS) {
                        player.getWorld().spawnParticle(Particle.END_ROD, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }
    }

    private void applyIronStomach(Player player, EntityPotionEffectEvent event) {
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) {
            return;
        }
        PotionEffectType type = newEffect.getType();
        if (!(type == PotionEffectType.HUNGER || type == PotionEffectType.POISON)) {
            return;
        }
        if (!triggerWithCooldown(player, "iron_stomach", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(type, Math.max(20, newEffect.getDuration() / 2), newEffect.getAmplifier(), true, true, true));
    }

    private void applyQuickReflexes(Player player, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) {
            return;
        }
        if (!triggerWithCooldown(player, "quick_reflexes", 5 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        event.setCancelled(true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
    }

    private void applySilentSteps(Player player) {
        if (!triggerWithCooldown(player, "silent_steps", 6 * TICKS_PER_SECOND, 0.35)) {
            return;
        }
        player.setSilent(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setSilent(false);
            }
        }.runTaskLater(plugin, 3 * TICKS_PER_SECOND);
    }

    private void applySteadfast(Player player) {
        if (player.getHealth() > player.getMaxHealth() / 2) {
            return;
        }
        if (!triggerWithCooldown(player, "steadfast", 8 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyNightEcho(Player player) {
        long time = player.getWorld().getTime();
        if (time < 13000 || time > 23000) {
            return;
        }
        if (!triggerWithCooldown(player, "night_echo", 8 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3 * TICKS_PER_SECOND, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyWaterAffinity(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "water_affinity", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 3 * TICKS_PER_SECOND, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyBlazeBlood(Player player, EntityDamageByEntityEvent event) {
        if (!triggerWithCooldown(player, "blaze_blood", 4 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        if (event.getDamager() instanceof LivingEntity attacker) {
            attacker.setFireTicks(60);
        }
    }

    private void applyShieldedHeart(Player player) {
        if (!player.isBlocking()) {
            return;
        }
        if (!triggerWithCooldown(player, "shielded_heart", 4 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyStoneSense(Player player) {
        if (!triggerWithCooldown(player, "stone_sense", 12 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        int radius = 6;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = player.getLocation().getBlock().getRelative(x, y, z);
                    if (block.getType() == Material.AIR && block.getRelative(0, -1, 0).getType().isSolid()) {
                        player.getWorld().spawnParticle(Particle.SCULK_SOUL, block.getLocation().add(0.5, 0.5, 0.5), 1, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }
    }

    private void applyBeastTamer(Player player, EntityTameEvent event) {
        if (!(event.getEntity() instanceof Tameable tameable)) {
            return;
        }
        if (!triggerWithCooldown(player, "beast_tamer", 10 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        if (tameable instanceof LivingEntity livingEntity) {
            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 10 * TICKS_PER_SECOND, 0, true, true, true));
        }
    }

    private void applyLuckySalvage(Player player, FurnaceExtractEvent event) {
        if (!triggerWithCooldown(player, "lucky_salvage", 6 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        ItemStack extra = new ItemStack(event.getItemType(), 1);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), extra);
    }

    private void applyThunderGuard(Player player, EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) {
            return;
        }
        if (!triggerWithCooldown(player, "thunder_guard", 6 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setDamage(event.getDamage() * 0.5);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyRootGrip(Player player) {
        Material ground = player.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
        if (!(Tag.DIRT.isTagged(ground) || Tag.BASE_STONE_OVERWORLD.isTagged(ground))) {
            return;
        }
        if (!triggerWithCooldown(player, "root_grip", 5 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.setVelocity(player.getVelocity().multiply(0.2));
    }

    private void applyWindstep(Player player) {
        if (!player.isSprinting()) {
            return;
        }
        if (!triggerWithCooldown(player, "windstep", 4 * TICKS_PER_SECOND, 0.2)) {
            return;
        }
        player.setVelocity(player.getVelocity().add(new Vector(0, 0.35, 0)));
    }

    private void applyQuickHeal(Player player, EntityRegainHealthEvent event) {
        if (!triggerWithCooldown(player, "quick_heal", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setAmount(event.getAmount() * 1.2);
    }

    private void applyShadowVeil(Player player) {
        if (player.getLocation().getBlock().getLightLevel() > 7) {
            return;
        }
        if (!triggerWithCooldown(player, "shadow_veil", 6 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applySunBlessed(Player player) {
        long time = player.getWorld().getTime();
        if (time > 12300) {
            return;
        }
        if (!triggerWithCooldown(player, "sun_blessed", 8 * TICKS_PER_SECOND, 0.7)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyPoisonResist(Player player, EntityPotionEffectEvent event) {
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null || newEffect.getType() != PotionEffectType.POISON) {
            return;
        }
        if (!triggerWithCooldown(player, "poison_resist", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, Math.max(20, newEffect.getDuration() / 2), newEffect.getAmplifier(), true, true, true));
    }

    private void applyQuakeGuard(Player player, EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (event.getDamage() > 4.0) {
            return;
        }
        if (!triggerWithCooldown(player, "quake_guard", 4 * TICKS_PER_SECOND, 0.7)) {
            return;
        }
        event.setDamage(event.getDamage() * 0.5);
    }

    private void applyEchoStep(Player player) {
        if (!triggerWithCooldown(player, "echo_step", 6 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.setSilent(true);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.4, 0.1, 0.4, 0.02);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setSilent(false);
            }
        }.runTaskLater(plugin, 3 * TICKS_PER_SECOND);
    }

    private void applyCrystalLuck(Player player, BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (!(type == Material.AMETHYST_BLOCK || type == Material.BUDDING_AMETHYST
                || type == Material.AMETHYST_CLUSTER || type == Material.LARGE_AMETHYST_BUD
                || type == Material.MEDIUM_AMETHYST_BUD || type == Material.SMALL_AMETHYST_BUD)) {
            return;
        }
        if (!triggerWithCooldown(player, "crystal_luck", 3 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.AMETHYST_SHARD, 1));
    }

    private void applyDeepBreathing(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "deep_breathing", 4 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 4 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyRestlessSpirits(Player player) {
        if (!triggerWithCooldown(player, "restless_spirits", 15 * TICKS_PER_SECOND, 0.2)) {
            return;
        }
        player.getWorld().spawnEntity(player.getLocation().add(random.nextInt(4) - 2, 0, random.nextInt(4) - 2),
                random.nextBoolean() ? org.bukkit.entity.EntityType.ZOMBIE : org.bukkit.entity.EntityType.SKELETON);
    }

    private void applyBrittleBones(Player player, EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!triggerWithCooldown(player, "brittle_bones", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setDamage(event.getDamage() * 1.25);
    }

    private void applyExpDrain(Player player, EntityDeathEvent event) {
        if (!triggerWithCooldown(player, "exp_drain", 4 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setDroppedExp((int) Math.max(0, event.getDroppedExp() * 0.75));
    }

    private void applyHungryMind(Player player, FoodLevelChangeEvent event) {
        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }
        if (!triggerWithCooldown(player, "hungry_mind", 3 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        event.setFoodLevel(Math.max(0, event.getFoodLevel() - 1));
    }

    private void applyClumsyHands(Player player, BlockDamageEvent event) {
        if (!triggerWithCooldown(player, "clumsy_hands", 6 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyCursedSteps(Player player) {
        if (!triggerWithCooldown(player, "cursed_steps", 6 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyFrailSkin(Player player, EntityDamageEvent event) {
        if (!triggerWithCooldown(player, "frail_skin", 4 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        event.setDamage(event.getDamage() + 1.0);
    }

    private void applyNoisyPresence(Player player) {
        if (!triggerWithCooldown(player, "noisy_presence", 6 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        for (Entity entity : player.getNearbyEntities(8, 4, 8)) {
            if (entity instanceof Monster monster) {
                monster.setTarget(player);
            }
        }
    }

    private void applyColdSweat(Player player, EntityDamageEvent event) {
        if (!(event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            return;
        }
        if (!triggerWithCooldown(player, "cold_sweat", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setDamage(event.getDamage() * 1.25);
    }

    private void applyManaLeak(Player player, EntityRegainHealthEvent event) {
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.REGEN
                && event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) {
            return;
        }
        if (!triggerWithCooldown(player, "mana_leak", 3 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setAmount(event.getAmount() * 0.7);
    }

    private void applyShadowCurse(Player player) {
        if (!triggerWithCooldown(player, "shadow_curse", 7 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyHeavyLungs(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "heavy_lungs", 5 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyBurningBlood(Player player) {
        if (!triggerWithCooldown(player, "burning_blood", 5 * TICKS_PER_SECOND, 0.25)) {
            return;
        }
        player.setFireTicks(60);
    }

    private void applyShakyAim(Player player, EntityShootBowEvent event) {
        if (!triggerWithCooldown(player, "shaky_aim", 3 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        if (event.getProjectile() instanceof Projectile projectile) {
            Vector velocity = projectile.getVelocity();
            Vector jitter = new Vector((random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.05, (random.nextDouble() - 0.5) * 0.1);
            projectile.setVelocity(velocity.add(jitter));
        }
    }

    private void applyGloom(Player player) {
        if (!triggerWithCooldown(player, "gloom", 10 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 10 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private boolean hasEffect(Player player, String effectId) {
        return effectManager.hasActiveEffect(player, effectId);
    }

    private boolean triggerWithCooldown(Player player, String effectId, int cooldownTicks, double chance) {
        if (chance < 1.0 && random.nextDouble() > chance) {
            return false;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = effectCooldowns.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        long lastUsed = playerCooldowns.getOrDefault(effectId, 0L);
        long cooldownMillis = ticksToMillis(cooldownTicks);
        if (now - lastUsed < cooldownMillis) {
            return false;
        }
        playerCooldowns.put(effectId, now);
        return true;
    }

    private long ticksToMillis(int ticks) {
        return (long) ticks * 50L;
    }
}
