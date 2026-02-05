package com.lastbreath.hc.lastBreathHC.potion;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTameEvent;
//import org.bukkit.event.entity.EntityVelocityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CustomPotionEffectApplier implements Listener {

    private static final int TICKS_PER_SECOND = 20;
    private static final Set<Material> FORAGE_BLOCKS = Set.of(
            Material.SWEET_BERRY_BUSH,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT,
            Material.GLOW_BERRIES
    );

    private final LastBreathHC plugin;
    private final CustomPotionEffectManager effectManager;
    private final Random random = new Random();
    private final Map<UUID, Map<String, Long>> effectCooldowns = new HashMap<>();
    private final Map<UUID, Location> thermalVisionSpoofedBlocks = new HashMap<>();

    public CustomPotionEffectApplier(LastBreathHC plugin, CustomPotionEffectManager effectManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
        startContinuousEffectTask();
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
        if (hasEffect(player, "overclocked_tools")) {
            applyOverclockedTools(player, event);
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
        if (hasEffect(player, "ore_pulse")) {
            applyOrePulse(player, event);
        }
        if (hasEffect(player, "bounty_call")) {
            applyBountyCall(player, event);
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
        if (hasEffect(player, "warm_resilience")) {
            applyWarmResilience(player, event);
        }
        if (hasEffect(player, "burning_blood")) {
            applyBurningBlood(player);
        }
        if (hasEffect(player, "dousing_skin")) {
            applyDousingSkin(player, event);
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
            if (hasEffect(player, "sticky_shell")) {
                applyStickyShell(player);
            }
            if (hasEffect(player, "rebound_guard")) {
                applyReboundGuard(player, event);
            }
            if (hasEffect(player, "soulfire_blood")) {
                applySoulfireBlood(player, event);
            }
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter && hasEffect(shooter, "hunter_focus")) {
                applyHunterFocus(shooter, event);
            }
        }
    }

    /*@EventHandler(ignoreCancelled = true)
    public void onEntityVelocity(EntityVelocityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "root_grip")) {
            applyRootGrip(player, event);
        }
    }*/

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }
        if (hasEffect(player, "lucky_loot")) {
            applyLuckyLoot(player, event);
        }
        if (hasEffect(player, "exp_drain")) {
            applyExpDrain(player, event);
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearThermalVisionOverlay(event.getPlayer());
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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (hasEffect(player, "lava_walker")) {
            applyLavaWalker(player);
        }
        if (hasEffect(player, "windstep")) {
            applyWindstep(player);
        }
        if (hasEffect(player, "rift_step")) {
            applyRiftStep(player, event);
        }
        if (hasEffect(player, "forest_stride")) {
            applyForestStride(player);
        }
        if (hasEffect(player, "overgrowth")) {
            applyOvergrowth(player);
        }
        if (hasEffect(player, "undertow")) {
            applyUndertow(player);
        }
        if (hasEffect(player, "sea_sickness")) {
            applySeaSickness(player);
        }
        if (hasEffect(player, "current_dancer")) {
            applyCurrentDancer(player);
        }
        if (hasEffect(player, "surge_stride")) {
            applySurgeStride(player);
        }
        if (hasEffect(player, "pressure_sense")) {
            applyPressureSense(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        if (hasEffect(player, "shadow_veil")) {
            applyShadowVeilTarget(player, event);
        }
    }

    public void handlePlayerTick(Player player) {
        if (hasEffect(player, "ember_aura")) {
            applyEmberAura(player);
        }
        if (hasEffect(player, "clear_sight")) {
            applyClearSight(player);
        }
        if (hasEffect(player, "silent_steps")) {
            applySilentSteps(player);
        }
        if (hasEffect(player, "steadfast")) {
            applySteadfast(player);
        }
        if (hasEffect(player, "night_echo")) {
            applyNightEcho(player);
        }
        if (hasEffect(player, "water_affinity")) {
            applyWaterAffinity(player);
        }
        if (hasEffect(player, "stone_sense")) {
            applyStoneSense(player);
        }
        if (hasEffect(player, "shadow_veil")) {
            applyShadowVeil(player);
        }
        if (hasEffect(player, "sun_blessed")) {
            applySunBlessed(player);
        }
        if (hasEffect(player, "echo_step")) {
            applyEchoStep(player);
        }
        if (hasEffect(player, "deep_breathing")) {
            applyDeepBreathing(player);
        }
        if (hasEffect(player, "restless_spirits")) {
            applyRestlessSpirits(player);
        }
        if (hasEffect(player, "cursed_steps")) {
            applyCursedSteps(player);
        }
        if (hasEffect(player, "noisy_presence")) {
            applyNoisyPresence(player);
        }
        if (hasEffect(player, "shadow_curse")) {
            applyShadowCurse(player);
        }
        if (hasEffect(player, "heavy_lungs")) {
            applyHeavyLungs(player);
        }
        if (hasEffect(player, "gloom")) {
            applyGloom(player);
        }
        if (hasEffect(player, "crystal_focus")) {
            applyCrystalFocus(player);
        }
        if (hasEffect(player, "razor_thoughts")) {
            applyRazorThoughts(player);
        }
        if (hasEffect(player, "hush_aura")) {
            applyHushAura(player);
        }
        if (hasEffect(player, "echo_sense")) {
            applyEchoSense(player);
        }
        if (hasEffect(player, "sunflare")) {
            applySunflare(player);
        }
        if (hasEffect(player, "abyssal_lungs")) {
            applyAbyssalLungs(player);
        }
        if (hasEffect(player, "void_sight")) {
            applyVoidSight(player);
        }
        if (hasEffect(player, "thermal_vision")) {
            applyThermalVision(player);
        } else {
            clearThermalVisionOverlay(player);
        }
        if (hasEffect(player, "steam_blur")) {
            applySteamBlur(player);
        }
        if (hasEffect(player, "mind_fog")) {
            applyMindFog(player);
        }
        if (hasEffect(player, "strained_muscles")) {
            applyStrainedMuscles(player);
        }
        if (hasEffect(player, "ringing_head")) {
            applyRingingHead(player);
        }
        if (hasEffect(player, "sapped_steps")) {
            applySappedSteps(player);
        }
        if (hasEffect(player, "fevered_blood")) {
            applyFeveredBlood(player);
        }
        if (hasEffect(player, "reality_fray")) {
            applyRealityFray(player);
        }
        if (hasEffect(player, "searing_aura")) {
            applySearingAura(player);
        }
        if (hasEffect(player, "ashen_heart")) {
            applyAshenHeart(player);
        }
    }

    private void startContinuousEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    handlePlayerTick(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
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
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
        }
        player.playSound(event.getBlock().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
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
        if (event.getCause() != EntityDamageEvent.DamageCause.FREEZE) {
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
        revealNearbyClearSightOres(player);
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
        revealNearbyCaves(player);
    }

    private void revealNearbyCaves(Player player) {
        int radius = 6;
        Location origin = player.getLocation();
        int revealed = 0;
        int maxReveals = 25;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (revealed >= maxReveals) {
                        return;
                    }
                    Block block = origin.getBlock().getRelative(x, y, z);
                    if (!isCavePocket(block)) {
                        continue;
                    }
                    spawnStoneSenseSpoofStand(player, block.getLocation().add(0.5, 0.0, 0.5));
                    revealed++;
                }
            }
        }
    }

    private boolean isCavePocket(Block block) {
        if (block.getType() != Material.AIR) {
            return false;
        }
        int solidNeighbors = 0;
        if (block.getRelative(1, 0, 0).getType().isSolid()) {
            solidNeighbors++;
        }
        if (block.getRelative(-1, 0, 0).getType().isSolid()) {
            solidNeighbors++;
        }
        if (block.getRelative(0, 0, 1).getType().isSolid()) {
            solidNeighbors++;
        }
        if (block.getRelative(0, 0, -1).getType().isSolid()) {
            solidNeighbors++;
        }
        if (block.getRelative(0, -1, 0).getType().isSolid()) {
            solidNeighbors++;
        }
        return solidNeighbors >= 3;
    }

    private void spawnStoneSenseSpoofStand(Player viewer, Location location) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, spawned -> {
            spawned.setMarker(true);
            spawned.setInvisible(true);
            spawned.setGravity(false);
            spawned.setInvulnerable(true);
            spawned.setSilent(true);
            spawned.setCustomName("Cave");
            spawned.setCustomNameVisible(true);
            spawned.setGlowing(true);
            spawned.getEquipment().setHelmet(new ItemStack(Material.STONE));
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(viewer.getUniqueId())) {
                online.hideEntity(plugin, stand);
            }
        }

        new BukkitRunnable() {
            private int ticksElapsed = 0;

            @Override
            public void run() {
                if (!stand.isValid() || !viewer.isOnline()) {
                    cleanup();
                    return;
                }
                ticksElapsed += 5;
                stand.setGlowing((ticksElapsed / 5) % 2 == 1);
                if (ticksElapsed >= 40) {
                    cleanup();
                }
            }

            private void cleanup() {
                stand.remove();
                cancel();
            }
        }.runTaskTimer(plugin, 5L, 5L);
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
        int itemAmount = event.getItemAmount();
        if (itemAmount <= 0) {
            return;
        }
        for (int i = 0; i < itemAmount; i++) {
            if (random.nextDouble() > 0.25) {
                continue;
            }
            ItemStack extra = new ItemStack(event.getItemType(), 1);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), extra);
        }
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

    private void applyRootGrip(Player player) {//, EntityVelocityEvent event) {
        Material ground = player.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
        if (!(Tag.DIRT.isTagged(ground) || Tag.BASE_STONE_OVERWORLD.isTagged(ground))) {
            return;
        }
        if (!triggerWithCooldown(player, "root_grip", 5 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        /*Vector velocity = event.getVelocity();
        if (velocity.lengthSquared() <= 0.0) {
            return;
        }
        event.setVelocity(velocity.multiply(0.2));*/
    }

    private void applyWindstep(Player player) {
        if (!player.isSprinting()) {
            return;
        }
        if (!triggerWithCooldown(player, "windstep", 4 * TICKS_PER_SECOND, 0.35)) {
            return;
        }
        Vector forward = player.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() <= 0.0001) {
            return;
        }
        forward.normalize();
        Vector impulse = forward.multiply(1.15).setY(0.42);
        player.setVelocity(player.getVelocity().add(impulse));
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

    private void applyShadowVeilTarget(Player player, EntityTargetEvent event) {
        if (player.getLocation().getBlock().getLightLevel() > 7) {
            return;
        }
        event.setCancelled(true);
        event.setTarget(null);
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
        if (player.getFallDistance() <= 7.0f || event.getDamage() <= 6.0) {
            event.setCancelled(true);
            return;
        }
        event.setDamage(event.getDamage() * 0.7);
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

    private void applyDousingSkin(Player player, EntityDamageEvent event) {
        if (!(event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA
                || event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR)) {
            return;
        }
        if (!triggerWithCooldown(player, "dousing_skin", 2 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        event.setDamage(event.getDamage() * 0.8);
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

    private void applyCrystalFocus(Player player) {
        if (!triggerWithCooldown(player, "crystal_focus", 12 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        revealNearbyClearSightOres(player);
    }

    private void applyRazorThoughts(Player player) {
        if (!triggerWithCooldown(player, "razor_thoughts", 10 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2 * TICKS_PER_SECOND, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyOverclockedTools(Player player, BlockDamageEvent event) {
        if (!triggerWithCooldown(player, "overclocked_tools", 3 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 2 * TICKS_PER_SECOND, 1, true, true, true));
    }

    private void applyOrePulse(Player player, BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (!type.name().endsWith("_ORE") && type != Material.ANCIENT_DEBRIS) {
            return;
        }
        if (!triggerWithCooldown(player, "ore_pulse", 8 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        revealNearbyClearSightOres(player);
    }

    private void applyHushAura(Player player) {
        if (!triggerWithCooldown(player, "hush_aura", 8 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.setSilent(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setSilent(false);
            }
        }.runTaskLater(plugin, 4 * TICKS_PER_SECOND);
    }

    private void applyEchoSense(Player player) {
        if (!triggerWithCooldown(player, "echo_sense", 10 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        for (Entity entity : player.getNearbyEntities(8, 4, 8)) {
            if (entity instanceof LivingEntity livingEntity && !(entity instanceof Player)) {
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 3 * TICKS_PER_SECOND, 0, true, true, true));
            }
        }
    }

    private void applyStickyShell(Player player) {
        if (!triggerWithCooldown(player, "sticky_shell", 6 * TICKS_PER_SECOND, 0.35)) {
            return;
        }
        player.setVelocity(player.getVelocity().multiply(0.4));
    }

    private void applyReboundGuard(Player player, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }
        if (!triggerWithCooldown(player, "rebound_guard", 6 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        Vector knockback = attacker.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.6);
        knockback.setY(0.35);
        attacker.setVelocity(knockback);
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.1f);
    }

    private void applySunflare(Player player) {
        if (!triggerWithCooldown(player, "sunflare", 10 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 3 * TICKS_PER_SECOND, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyWarmResilience(Player player, EntityDamageEvent event) {
        if (player.getFireTicks() <= 0 && player.getLocation().getBlock().getType() != Material.LAVA) {
            return;
        }
        if (!triggerWithCooldown(player, "warm_resilience", 5 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        event.setDamage(event.getDamage() * 0.8);
    }

    private void applyAbyssalLungs(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "abyssal_lungs", 6 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        player.setRemainingAir(Math.min(player.getMaximumAir(), player.getRemainingAir() + 40));
    }

    private void applyPressureSense(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (player.getLocation().getY() > 40) {
            return;
        }
        if (!triggerWithCooldown(player, "pressure_sense", 6 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 4 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applySurgeStride(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "surge_stride", 4 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 3 * TICKS_PER_SECOND, 1, true, true, true));
    }

    private void applyCurrentDancer(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "current_dancer", 4 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyBountyCall(Player player, BlockBreakEvent event) {
        if (!FORAGE_BLOCKS.contains(event.getBlock().getType())) {
            return;
        }
        if (!triggerWithCooldown(player, "bounty_call", 4 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        ItemStack extra = new ItemStack(Material.SWEET_BERRIES, 1);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), extra);
    }

    private void applyForestStride(Player player) {
        Block block = player.getLocation().getBlock();
        if (!(Tag.LEAVES.isTagged(block.getType()) || block.getType() == Material.VINE)) {
            return;
        }
        if (!triggerWithCooldown(player, "forest_stride", 4 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applySoulfireBlood(Player player, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!triggerWithCooldown(player, "soulfire_blood", 5 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        target.setFireTicks(80);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.01);
    }

    private void applySearingAura(Player player) {
        if (!triggerWithCooldown(player, "searing_aura", 6 * TICKS_PER_SECOND, 0.35)) {
            return;
        }
        for (Entity entity : player.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof Monster monster) {
                monster.setFireTicks(60);
            }
        }
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 10, 0.5, 0.3, 0.5, 0.01);
    }

    private void applyRiftStep(Player player, PlayerMoveEvent event) {
        if (event.getFrom().getY() >= event.getTo().getY()) {
            return;
        }
        if (!triggerWithCooldown(player, "rift_step", 5 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        Vector forward = player.getLocation().getDirection().multiply(0.35);
        forward.setY(0.25);
        player.setVelocity(player.getVelocity().add(forward));
    }

    private void applyVoidSight(Player player) {
        if (player.getLocation().getBlock().getLightLevel() > 7) {
            return;
        }
        if (!triggerWithCooldown(player, "void_sight", 8 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 4 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyThermalVision(Player player) {
        Material eyeBlockType = player.getEyeLocation().getBlock().getType();
        boolean submerged = eyeBlockType == Material.WATER || eyeBlockType == Material.LAVA || player.isInWater();
        if (!submerged) {
            clearThermalVisionOverlay(player);
            return;
        }
        spoofThermalVisionOverlay(player, eyeBlockType);
        if (!triggerWithCooldown(player, "thermal_vision", 4 * TICKS_PER_SECOND, 1.0)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 6 * TICKS_PER_SECOND, 0, true, true, true));
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 3 * TICKS_PER_SECOND, 0, true, true, true));
        }
    }

    private void spoofThermalVisionOverlay(Player player, Material eyeBlockType) {
        if (eyeBlockType != Material.WATER && eyeBlockType != Material.LAVA) {
            clearThermalVisionOverlay(player);
            return;
        }
        Location eyeBlockLocation = player.getEyeLocation().getBlock().getLocation();
        Location lastSpoofed = thermalVisionSpoofedBlocks.get(player.getUniqueId());
        if (lastSpoofed != null && !sameBlock(lastSpoofed, eyeBlockLocation)) {
            player.sendBlockChange(lastSpoofed, lastSpoofed.getBlock().getBlockData());
            thermalVisionSpoofedBlocks.remove(player.getUniqueId());
        }
        if (lastSpoofed != null && sameBlock(lastSpoofed, eyeBlockLocation)) {
            return;
        }
        player.sendBlockChange(eyeBlockLocation, Material.AIR.createBlockData());
        thermalVisionSpoofedBlocks.put(player.getUniqueId(), eyeBlockLocation);
    }

    private void clearThermalVisionOverlay(Player player) {
        Location spoofedBlock = thermalVisionSpoofedBlocks.remove(player.getUniqueId());
        if (spoofedBlock == null) {
            return;
        }
        player.sendBlockChange(spoofedBlock, spoofedBlock.getBlock().getBlockData());
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() != null
                && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private void applySteamBlur(Player player) {
        boolean inHeatOrWater = player.isInWater() || player.getLocation().getBlock().getType() == Material.LAVA;
        if (!inHeatOrWater) {
            return;
        }
        if (!triggerWithCooldown(player, "steam_blur", 8 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyMindFog(Player player) {
        if (!triggerWithCooldown(player, "mind_fog", 8 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyStrainedMuscles(Player player) {
        if (!player.isSprinting()) {
            return;
        }
        if (!triggerWithCooldown(player, "strained_muscles", 6 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyRingingHead(Player player) {
        if (!triggerWithCooldown(player, "ringing_head", 8 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * TICKS_PER_SECOND, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applySappedSteps(Player player) {
        if (!triggerWithCooldown(player, "sapped_steps", 6 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyFeveredBlood(Player player) {
        if (player.getFireTicks() <= 0 && player.getLocation().getBlock().getType() != Material.LAVA) {
            return;
        }
        if (!triggerWithCooldown(player, "fevered_blood", 6 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyUndertow(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!triggerWithCooldown(player, "undertow", 4 * TICKS_PER_SECOND, 0.6)) {
            return;
        }
        player.setVelocity(player.getVelocity().add(new Vector(0, -0.2, 0)));
    }

    private void applySeaSickness(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (player.getVelocity().length() < 0.4) {
            return;
        }
        if (!triggerWithCooldown(player, "sea_sickness", 6 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyOvergrowth(Player player) {
        Block block = player.getLocation().getBlock();
        if (!(Tag.LEAVES.isTagged(block.getType()) || block.getType() == Material.VINE)) {
            return;
        }
        if (!triggerWithCooldown(player, "overgrowth", 5 * TICKS_PER_SECOND, 0.5)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyAshenHeart(Player player) {
        if (player.getFireTicks() <= 0 && player.getLocation().getBlock().getType() != Material.LAVA) {
            return;
        }
        if (!triggerWithCooldown(player, "ashen_heart", 8 * TICKS_PER_SECOND, 0.4)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 3 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void applyRealityFray(Player player) {
        if (!triggerWithCooldown(player, "reality_fray", 6 * TICKS_PER_SECOND, 0.3)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 2 * TICKS_PER_SECOND, 0, true, true, true));
    }

    private void revealNearbyClearSightOres(Player player) {
        int radius = 6;
        Location origin = player.getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = origin.getBlock().getRelative(x, y, z);
                    Material type = block.getType();
                    if (type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS) {
                        spawnClearSightSpoofStand(player, block);
                    }
                }
            }
        }
    }

    private void spawnClearSightSpoofStand(Player viewer, Block block) {
        Location standLocation = block.getLocation().add(0.5, 0.0, 0.5);
        ArmorStand stand = block.getWorld().spawn(standLocation, ArmorStand.class, spawned -> {
            spawned.setMarker(true);
            spawned.setInvisible(true);
            spawned.setGravity(false);
            spawned.setInvulnerable(true);
            spawned.setSilent(true);
            spawned.setCustomName(formatOreName(block.getType()));
            spawned.setCustomNameVisible(true);
            spawned.setGlowing(true);
            spawned.getEquipment().setHelmet(new ItemStack(block.getType()));
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(viewer.getUniqueId())) {
                online.hideEntity(plugin, stand);
            }
        }

        new BukkitRunnable() {
            private int ticksElapsed = 0;

            @Override
            public void run() {
                if (!stand.isValid() || !viewer.isOnline()) {
                    cleanup();
                    return;
                }
                ticksElapsed += 5;
                stand.setGlowing((ticksElapsed / 5) % 2 == 1);
                if (ticksElapsed >= 40) {
                    cleanup();
                }
            }

            private void cleanup() {
                stand.remove();
                cancel();
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private String formatOreName(Material material) {
        String raw = material.name().toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String token : raw.split(" ")) {
            if (token.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return builder.toString();
    }

    private void revealNearbyOres(Player player, Particle particle) {
        int radius = 6;
        Location origin = player.getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = origin.getBlock().getRelative(x, y, z);
                    Material type = block.getType();
                    if (type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS) {
                        Location location = block.getLocation().add(0.5, 0.5, 0.5);
                        if (particle == Particle.BLOCK_MARKER) {
                            player.spawnParticle(particle, location, 1, 0, 0, 0, 0, block.getBlockData());
                        } else {
                            player.spawnParticle(particle, location, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
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
