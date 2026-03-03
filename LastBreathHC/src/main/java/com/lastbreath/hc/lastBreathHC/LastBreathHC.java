package com.lastbreath.hc.lastBreathHC;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidBossMechanics;
import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidListener;
import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonListener;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonManager;
import com.lastbreath.hc.lastBreathHC.bloodmoon.BloodMoonScheduler;
import com.lastbreath.hc.lastBreathHC.bounty.BountyListener;
import com.lastbreath.hc.lastBreathHC.bounty.BountyManager;
import com.lastbreath.hc.lastBreathHC.chat.ChatInventoryShareListener;
import com.lastbreath.hc.lastBreathHC.chat.ChatPrefixListener;
import com.lastbreath.hc.lastBreathHC.commands.*;
import com.lastbreath.hc.lastBreathHC.combat.DispenserSwordListener;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticAuraService;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticTokenListener;
import com.lastbreath.hc.lastBreathHC.daily.DailyCosmeticListener;
import com.lastbreath.hc.lastBreathHC.daily.DailyJoinListener;
import com.lastbreath.hc.lastBreathHC.daily.DailyRewardManager;
import com.lastbreath.hc.lastBreathHC.heads.HeadListener;
import com.lastbreath.hc.lastBreathHC.heads.HeadManager;
import com.lastbreath.hc.lastBreathHC.heads.HeadTrackingLogger;
import com.lastbreath.hc.lastBreathHC.gui.CosmeticsGUI;
import com.lastbreath.hc.lastBreathHC.gui.DailyRewardGUI;
import com.lastbreath.hc.lastBreathHC.gui.EffectsStatusGUI;
import com.lastbreath.hc.lastBreathHC.gui.LeaderboardGUI;
import com.lastbreath.hc.lastBreathHC.gui.TeamManagementGUI;
import com.lastbreath.hc.lastBreathHC.gui.TitlesGUI;
import com.lastbreath.hc.lastBreathHC.mobs.MobScalingListener;
import com.lastbreath.hc.lastBreathHC.mobs.MobStackCombatListener;
import com.lastbreath.hc.lastBreathHC.mobs.MobStackManager;
import com.lastbreath.hc.lastBreathHC.mobs.MobStackSignListener;
import com.lastbreath.hc.lastBreathHC.mobs.AggressiveLogoutMobManager;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainCombatListener;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainEntityBinder;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainHabitatService;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainNameGenerator;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRegistry;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisTelemetry;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainRecord;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainSerializer;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainSpawner;
import com.lastbreath.hc.lastBreathHC.nemesis.ArmyGraphSerializer;
import com.lastbreath.hc.lastBreathHC.nemesis.ArmyGraphService;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainTraitRegistry;
import com.lastbreath.hc.lastBreathHC.nemesis.CaptainTraitService;
import com.lastbreath.hc.lastBreathHC.nemesis.KillerResolver;
import com.lastbreath.hc.lastBreathHC.nemesis.MinionController;
import com.lastbreath.hc.lastBreathHC.nemesis.PromotionEvaluator;
import com.lastbreath.hc.lastBreathHC.nemesis.TokenAwareDeathOutcomeResolver;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisProgressionService;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisRewardService;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisCaptainListGUI;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisRivalryDirector;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisUI;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisWarbandCoordinator;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisAdminWarbandService;
import com.lastbreath.hc.lastBreathHC.nemesis.NemesisBuildingService;
import com.lastbreath.hc.lastBreathHC.nemesis.AntiCheeseMonitor;
import com.lastbreath.hc.lastBreathHC.nemesis.DialogueEngine;
import com.lastbreath.hc.lastBreathHC.nemesis.InfluenceItemHandler;
import com.lastbreath.hc.lastBreathHC.nemesis.LoyaltyService;
import com.lastbreath.hc.lastBreathHC.nemesis.TerritoryPressureService;
import com.lastbreath.hc.lastBreathHC.nemesis.StructureEventOrchestrator;
import com.lastbreath.hc.lastBreathHC.nemesis.StructureRaidService;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateListener;
import com.lastbreath.hc.lastBreathHC.revive.ReviveStateManager;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprintRepository;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import com.lastbreath.hc.lastBreathHC.structures.StructureManagerImpl;
import com.lastbreath.hc.lastBreathHC.structures.PlayerPlacedBlockIndex;
import com.lastbreath.hc.lastBreathHC.structures.StructurePlacementValidator;
import com.lastbreath.hc.lastBreathHC.spawners.SpawnerListener;
import com.lastbreath.hc.lastBreathHC.spawners.SpawnerSpawnListener;
import com.lastbreath.hc.lastBreathHC.spectate.AdminSpectateHotbarListener;
import com.lastbreath.hc.lastBreathHC.spectate.SpectateMenuBlockListener;
import com.lastbreath.hc.lastBreathHC.stats.StatsListener;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.BossTitleLandingListener;
import com.lastbreath.hc.lastBreathHC.titles.TitleListener;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.titles.WorldScalerDamageListener;
import com.lastbreath.hc.lastBreathHC.team.TeamChatListener;
import com.lastbreath.hc.lastBreathHC.team.TeamChatService;
import com.lastbreath.hc.lastBreathHC.team.TeamManager;
import com.lastbreath.hc.lastBreathHC.team.TeamWaypointManager;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.BukkitTabMenuPlayerSource;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.BukkitTabMenuUpdateHandler;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModelProvider;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuRefreshScheduler;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.renderer.TabMenuRenderer;
import com.lastbreath.hc.lastBreathHC.worldboss.WorldBossManager;
import com.lastbreath.hc.lastBreathHC.commands.WorldBossCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import com.lastbreath.hc.lastBreathHC.token.TokenRecipe;
import com.lastbreath.hc.lastBreathHC.token.ReviveGuiTokenRecipe;
import com.lastbreath.hc.lastBreathHC.gui.ReviveGUI;
import com.lastbreath.hc.lastBreathHC.gui.ReviveNameGUI;
import com.lastbreath.hc.lastBreathHC.death.DeathListener;
import com.lastbreath.hc.lastBreathHC.death.DeathMarkerManager;
import com.lastbreath.hc.lastBreathHC.death.DeathRejoinListener;
import com.lastbreath.hc.lastBreathHC.death.BannedDeathZombieService;
import com.lastbreath.hc.lastBreathHC.death.DeathAuditLogger;
import com.lastbreath.hc.lastBreathHC.death.PlayerLastMessageTracker;
import com.lastbreath.hc.lastBreathHC.environment.AnvilCrushListener;
import com.lastbreath.hc.lastBreathHC.environment.EnvironmentalEffectsManager;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerDeathReactionHandler;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRepository;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerService;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayersSettings;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerWhisperAliasListener;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerTabSyncListener;
import com.lastbreath.hc.lastBreathHC.fakeplayer.SkinService;
import com.lastbreath.hc.lastBreathHC.gui.BountyBoardGUI;
import com.lastbreath.hc.lastBreathHC.integrations.discord.DiscordWebhookService;
import com.lastbreath.hc.lastBreathHC.integrations.lastbreath.ApiClient;
import com.lastbreath.hc.lastBreathHC.integrations.lastbreath.ApiEventListener;
import com.lastbreath.hc.lastBreathHC.items.CustomEnchantBookRecipeListener;
import com.lastbreath.hc.lastBreathHC.items.CustomItemCraftListener;
import com.lastbreath.hc.lastBreathHC.listeners.CustomEnchantAnvilListener;
import com.lastbreath.hc.lastBreathHC.listeners.CustomEnchantDamageListener;
import com.lastbreath.hc.lastBreathHC.listeners.CustomEnchantListener;
import com.lastbreath.hc.lastBreathHC.listeners.ServerListMotdListener;
import com.lastbreath.hc.lastBreathHC.items.CustomItemRecipes;
import com.lastbreath.hc.lastBreathHC.items.EnhancedGrindstoneListener;
import com.lastbreath.hc.lastBreathHC.items.GracestoneLifeListener;
import com.lastbreath.hc.lastBreathHC.items.GracestoneListener;
import com.lastbreath.hc.lastBreathHC.mobs.ArrowAggroListener;
import com.lastbreath.hc.lastBreathHC.onboarding.CustomGuideBookListener;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectApplier;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectManager;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectRegistry;
import com.lastbreath.hc.lastBreathHC.potion.CauldronBrewingListener;
import com.lastbreath.hc.lastBreathHC.potion.PotionHandler;
import com.lastbreath.hc.lastBreathHC.potion.PotionDefinitionRegistry;
import com.lastbreath.hc.lastBreathHC.nickname.NicknamePermissionMonitor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class LastBreathHC extends JavaPlugin {

    private static LastBreathHC instance;
    private final Random random = new Random();
    private BukkitTask asteroidTask;
    private BukkitTask bountyTimeTask;
    private BukkitTask bountyCleanupTask;
    private BukkitTask bloodMoonTask;
    private BukkitTask titleEffectTask;
    private BukkitTask nicknamePermissionTask;
    private BukkitTask statsAutosaveTask;
    private BukkitTask captainFlushTask;
    private BukkitTask apiStatsTask;
    private BloodMoonManager bloodMoonManager;
    private DeathMarkerManager deathMarkerManager;
    private EnvironmentalEffectsManager environmentalEffectsManager;
    private PotionDefinitionRegistry potionDefinitionRegistry;
    private CustomPotionEffectRegistry customPotionEffectRegistry;
    private CustomPotionEffectManager customPotionEffectManager;
    private EffectsStatusGUI effectsStatusGUI;
    private CosmeticsGUI cosmeticsGUI;
    private TitlesGUI titlesGUI;
    private TabMenuRefreshScheduler tabMenuRefreshScheduler;
    private TeamWaypointManager teamWaypointManager;
    private TeamManager teamManager;
    private WorldBossManager worldBossManager;
    private CosmeticAuraService cosmeticAuraService;
    private MobStackManager mobStackManager;
    private AggressiveLogoutMobManager aggressiveLogoutMobManager;
    private FakePlayerService fakePlayerService;
    private FakePlayersSettings fakePlayersSettings;
    private DailyRewardManager dailyRewardManager;
    private DailyRewardGUI dailyRewardGUI;
    private DailyCosmeticListener dailyCosmeticListener;
    private BannedDeathZombieService bannedDeathZombieService;
    private HeadTrackingLogger headTrackingLogger;
    private CaptainRegistry captainRegistry;
    private CaptainSerializer captainSerializer;
    private ArmyGraphSerializer armyGraphSerializer;
    private ArmyGraphService armyGraphService;
    private KillerResolver killerResolver;
    private CaptainEntityBinder captainEntityBinder;
    private CaptainSpawner captainSpawner;
    private CaptainTraitRegistry captainTraitRegistry;
    private CaptainTraitService captainTraitService;
    private MinionController minionController;
    private NemesisUI nemesisUI;
    private NemesisCaptainListGUI nemesisCaptainListGUI;
    private CaptainNameGenerator captainNameGenerator;
    private NemesisProgressionService nemesisProgressionService;
    private NemesisRewardService nemesisRewardService;
    private NemesisRivalryDirector nemesisRivalryDirector;
    private AntiCheeseMonitor antiCheeseMonitor;
    private PromotionEvaluator promotionEvaluator;
    private LoyaltyService loyaltyService;
    private DialogueEngine dialogueEngine;
    private TerritoryPressureService territoryPressureService;
    private StructureFootprintRepository structureFootprintRepository;
    private StructureManager structureManager;
    private StructureEventOrchestrator structureEventOrchestrator;
    private PlayerPlacedBlockIndex playerPlacedBlockIndex;
    private NemesisBuildingService nemesisBuildingService;
    private NemesisAdminWarbandService nemesisAdminWarbandService;
    private CaptainHabitatService captainHabitatService;
    private StructureRaidService structureRaidService;
    private NemesisWarbandCoordinator nemesisWarbandCoordinator;
    private DiscordWebhookService discordWebhookService;
    private ApiClient apiClient;
    private ApiEventListener apiEventListener;


    @Override
    public void onLoad() {
        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {
                    event.registrar().register("daily", new DailyCommand(() -> dailyRewardGUI));
                    event.registrar().register("leaderboard", new LeaderboardCommand());
                }
        );
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("LastBreathHC enabled.");
        fakePlayersSettings = FakePlayersSettings.load(this);
        fakePlayerService = new FakePlayerService(
                this,
                new FakePlayerRepository(this, new java.io.File(getDataFolder(), "fake-players.yml")),
                new SkinService(this),
                fakePlayersSettings
        );
        fakePlayerService.startup();
        structureFootprintRepository = new StructureFootprintRepository(this, new java.io.File(getDataFolder(), "nemesis-structures.yml"));
        structureFootprintRepository.load();
        playerPlacedBlockIndex = new PlayerPlacedBlockIndex(this, new java.io.File(getDataFolder(), "player-placed-blocks.yml"));
        playerPlacedBlockIndex.load();
        StructurePlacementValidator structurePlacementValidator = new StructurePlacementValidator(
                structureFootprintRepository,
                new StructurePlacementValidator.NoOpProtectedRegionAdapter(),
                playerPlacedBlockIndex
        );
        structureManager = new StructureManagerImpl(structurePlacementValidator, structureFootprintRepository);
        nemesisBuildingService = new NemesisBuildingService(this, structureManager);
        nemesisBuildingService.initialize();
        captainRegistry = new CaptainRegistry();
        captainHabitatService = new CaptainHabitatService(this, captainRegistry, structureFootprintRepository);
        captainSerializer = new CaptainSerializer(this, new java.io.File(getDataFolder(), "nemesis-captains.yml"));
        captainRegistry.load(captainSerializer.load());
        armyGraphSerializer = new ArmyGraphSerializer(this, new java.io.File(getDataFolder(), "nemesis-army-graph.yml"));
        armyGraphService = new ArmyGraphService();
        armyGraphService.load(armyGraphSerializer.load());
        armyGraphService.seedFromCaptains(captainRegistry.getAll());
        structureEventOrchestrator = new StructureEventOrchestrator(captainRegistry, structureManager, captainHabitatService, armyGraphService, nemesisBuildingService);
        armyGraphService.pruneMissingCaptains(captainRegistry.getAll().stream().map(record -> record.identity().captainId()).collect(java.util.stream.Collectors.toSet()));
        long captainFlushIntervalTicks = 20L * 60L * 3L;
        captainFlushTask = getServer().getScheduler().runTaskTimer(this, this::flushDirtyCaptains, captainFlushIntervalTicks, captainFlushIntervalTicks);
        killerResolver = new KillerResolver();
        captainTraitRegistry = new CaptainTraitRegistry(this);
        captainNameGenerator = new CaptainNameGenerator(this);
        captainEntityBinder = new CaptainEntityBinder(this, captainRegistry);
        captainTraitService = new CaptainTraitService(captainEntityBinder, captainTraitRegistry);
        captainEntityBinder.setTraitService(captainTraitService);
        nemesisProgressionService = new NemesisProgressionService(this, captainRegistry, captainEntityBinder, captainTraitRegistry, structureEventOrchestrator);
        nemesisProgressionService.start();
        captainSpawner = new CaptainSpawner(this, captainRegistry, captainEntityBinder, new CaptainSpawner.NoOpProtectedRegionChecker(), playerPlacedBlockIndex, captainNameGenerator, structureEventOrchestrator);
        captainSpawner.start();
        minionController = new MinionController(this, captainRegistry, captainEntityBinder, nemesisProgressionService, captainHabitatService);
        minionController.start();
        nemesisAdminWarbandService = new NemesisAdminWarbandService(captainSpawner, minionController, armyGraphService, structureEventOrchestrator);
        nemesisUI = new NemesisUI(this, captainRegistry, captainEntityBinder);
        nemesisUI.start();
        nemesisCaptainListGUI = new NemesisCaptainListGUI(captainRegistry, captainEntityBinder, captainTraitRegistry);
        nemesisRewardService = new NemesisRewardService(this, captainEntityBinder, captainRegistry);
        nemesisRivalryDirector = new NemesisRivalryDirector(this, captainRegistry, captainEntityBinder);
        nemesisRivalryDirector.start();
        promotionEvaluator = new PromotionEvaluator(this, captainRegistry, structureEventOrchestrator);
        promotionEvaluator.start();
        dialogueEngine = new DialogueEngine(this);
        loyaltyService = new LoyaltyService(this, captainRegistry, captainEntityBinder, armyGraphService, structureEventOrchestrator, dialogueEngine);
        nemesisWarbandCoordinator = new NemesisWarbandCoordinator(this, captainRegistry, captainEntityBinder, dialogueEngine);
        nemesisWarbandCoordinator.start();
        territoryPressureService = new TerritoryPressureService(this, structureEventOrchestrator);
        dialogueEngine.setActionHook((actionType, speaker, listener, channelKey, location) -> {
            if (actionType == null || speaker == null || speaker.identity() == null) {
                return;
            }

            CaptainRecord updated = speaker;
            switch (actionType) {
                case BETRAYAL, BLOOD_FEUD, AGGRESSION -> {
                    if (listener != null && listener.identity() != null) {
                        armyGraphService.addRivalry(speaker.identity().captainId(), listener.identity().captainId());
                    }
                    updated = NemesisTelemetry.incrementCounter(updated, "dialogue.hostile", 1);
                }
                case UNITY, FORTIFY -> updated = NemesisTelemetry.incrementCounter(updated, "dialogue.unity", 1);
                case STAND_DOWN -> updated = NemesisTelemetry.incrementCounter(updated, "dialogue.standDown", 1);
            }

            if (territoryPressureService != null && territoryPressureService.enabled()) {
                String region = updated.political().map(CaptainRecord.Political::region).orElse("");
                if (region != null && !region.isBlank()) {
                    double delta = switch (actionType) {
                        case BETRAYAL, BLOOD_FEUD, AGGRESSION -> 3.0;
                        case UNITY, FORTIFY -> 1.5;
                        case STAND_DOWN -> -1.0;
                    };
                    territoryPressureService.applyChange(region, "dialogue." + channelKey, delta);
                }
            }

            captainRegistry.upsert(updated);
        });
        antiCheeseMonitor = new AntiCheeseMonitor(this, captainEntityBinder);
        structureRaidService = new StructureRaidService(this, captainRegistry, structureFootprintRepository, territoryPressureService, structureEventOrchestrator, captainEntityBinder);
        getServer().getPluginManager().registerEvents(killerResolver, this);
        getServer().getPluginManager().registerEvents(new CaptainCombatListener(this, captainRegistry, killerResolver, captainEntityBinder, captainTraitService, captainTraitRegistry, nemesisUI, nemesisProgressionService, new TokenAwareDeathOutcomeResolver(), captainNameGenerator, captainHabitatService, armyGraphService, dialogueEngine), this);
        getServer().getPluginManager().registerEvents(captainSpawner, this);
        getServer().getPluginManager().registerEvents(playerPlacedBlockIndex, this);
        getServer().getPluginManager().registerEvents(minionController, this);
        getServer().getPluginManager().registerEvents(nemesisCaptainListGUI, this);
        getServer().getPluginManager().registerEvents(nemesisRewardService, this);
        getServer().getPluginManager().registerEvents(loyaltyService, this);
        getServer().getPluginManager().registerEvents(new InfluenceItemHandler(this, captainRegistry, captainEntityBinder, territoryPressureService, loyaltyService), this);
        getServer().getPluginManager().registerEvents(antiCheeseMonitor, this);
        getServer().getPluginManager().registerEvents(structureRaidService, this);
        dailyRewardManager = new DailyRewardManager(this);
        potionDefinitionRegistry = PotionDefinitionRegistry.load(this, "potion-definitions.yml");
        customPotionEffectRegistry = CustomPotionEffectRegistry.load(this, "custom-effects.yml");
        HeadManager.init();
        headTrackingLogger = new HeadTrackingLogger(this);
        BountyManager.load();
        AsteroidManager.initialize(this);
        ReviveStateManager.initialize(this);
        bloodMoonManager = new BloodMoonManager(this);
        teamManager = new TeamManager(this);
        TeamChatService teamChatService = new TeamChatService(this, teamManager);
        discordWebhookService = new DiscordWebhookService(this);
        teamWaypointManager = new TeamWaypointManager(new java.io.File(getDataFolder(), "teams.yml"));
        teamWaypointManager.load();
        TeamManagementGUI teamManagementGUI = new TeamManagementGUI(this, teamManager);
        int deathMarkerDurationSeconds = getConfig().getInt("deathMarker.durationSeconds", 180);
        deathMarkerManager = new DeathMarkerManager(this, teamManager, deathMarkerDurationSeconds);

        FakePlayerDeathReactionHandler fakePlayerDeathReactionHandler = new FakePlayerDeathReactionHandler(this, fakePlayerService);
        PlayerLastMessageTracker playerLastMessageTracker = new PlayerLastMessageTracker();
        DeathAuditLogger deathAuditLogger = new DeathAuditLogger(this);
        DeathListener deathListener = new DeathListener(
                deathMarkerManager,
                teamChatService,
                discordWebhookService,
                fakePlayerDeathReactionHandler,
                playerLastMessageTracker,
                deathAuditLogger
        );
        getServer().getPluginManager().registerEvents(
                playerLastMessageTracker, this
        );
        getServer().getPluginManager().registerEvents(
                deathListener, this
        );
        getServer().getPluginManager().registerEvents(
                new DeathRejoinListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new HeadListener(this, headTrackingLogger), this
        );
        bannedDeathZombieService = new BannedDeathZombieService(this, headTrackingLogger);
        bannedDeathZombieService.start();
        getServer().getPluginManager().registerEvents(
                new ReviveGUI(deathListener), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveNameGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveStateListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new AsteroidListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new AsteroidBossMechanics(this), this
        );
        getServer().getPluginManager().registerEvents(
                new TitleListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new BossTitleLandingListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new WorldScalerDamageListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new MobScalingListener(bloodMoonManager), this
        );
        getServer().getPluginManager().registerEvents(
                new MobStackSignListener(this), this
        );
        mobStackManager = new MobStackManager(this);
        getServer().getPluginManager().registerEvents(
                new MobStackCombatListener(mobStackManager), this
        );
        aggressiveLogoutMobManager = new AggressiveLogoutMobManager(this);
        getServer().getPluginManager().registerEvents(
                aggressiveLogoutMobManager, this
        );
        getServer().getPluginManager().registerEvents(
                new BountyListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new BountyBoardGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new SpawnerListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new SpawnerSpawnListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new StatsListener(), this
        );

        String apiBaseUrl = getConfig().getString("lastbreath.api.baseUrl",
                getConfig().getString("lastBreathApi.baseUrl", "https://www.lastbreath.net"));
        String apiKey = getConfig().getString("lastbreath.api.apiKey",
                getConfig().getString("lastBreathApi.apiKey", "LASTBREATH_PLUGIN_TEST_KEY_CHANGE_ME"));
        apiClient = new ApiClient(this, apiBaseUrl, apiKey);
        getLogger().info("LastBreath API startup self-test: resolved event URL = " + apiClient.getResolvedPluginEventUrl());
        apiEventListener = new ApiEventListener(apiClient);
        getServer().getPluginManager().registerEvents(apiEventListener, this);
        getServer().getScheduler().runTaskAsynchronously(this, () -> apiEventListener.sendBulkStatsStartup());
        scheduleApiStatsUpdates();
        getServer().getPluginManager().registerEvents(
                new BloodMoonListener(bloodMoonManager), this
        );
        getServer().getPluginManager().registerEvents(
                new EnhancedGrindstoneListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new CustomEnchantAnvilListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new CustomEnchantListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new CustomEnchantDamageListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new CustomEnchantBookRecipeListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new CustomItemCraftListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new GracestoneListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new GracestoneLifeListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ArrowAggroListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new CustomGuideBookListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new AnvilCrushListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new RtpUsageListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new NickUsageListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new DispenserSwordListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new TeamChatListener(teamChatService), this
        );
        getServer().getPluginManager().registerEvents(
                teamManagementGUI, this
        );
        environmentalEffectsManager = new EnvironmentalEffectsManager(this);
        getServer().getPluginManager().registerEvents(
                environmentalEffectsManager, this
        );
        PotionHandler potionHandler = new PotionHandler(this, potionDefinitionRegistry, customPotionEffectRegistry);
        getServer().getPluginManager().registerEvents(
                potionHandler, this
        );
        getServer().getPluginManager().registerEvents(
                new CauldronBrewingListener(this, potionHandler, potionDefinitionRegistry), this
        );
        customPotionEffectManager = new CustomPotionEffectManager(this, potionDefinitionRegistry, customPotionEffectRegistry);
        effectsStatusGUI = new EffectsStatusGUI(customPotionEffectManager, customPotionEffectRegistry);
        titlesGUI = new TitlesGUI();
        cosmeticsGUI = new CosmeticsGUI(dailyRewardManager);
        LeaderboardGUI leaderboardGUI = new LeaderboardGUI();
        dailyRewardGUI = new DailyRewardGUI(dailyRewardManager);
        dailyCosmeticListener = new DailyCosmeticListener(this, dailyRewardManager);
        cosmeticAuraService = new CosmeticAuraService();
        getServer().getPluginManager().registerEvents(
                customPotionEffectManager, this
        );
        getServer().getPluginManager().registerEvents(
                new CustomPotionEffectApplier(this, customPotionEffectManager), this
        );
        getServer().getPluginManager().registerEvents(
                effectsStatusGUI, this
        );
        getServer().getPluginManager().registerEvents(
                titlesGUI, this
        );
        getServer().getPluginManager().registerEvents(
                cosmeticsGUI, this
        );
        getServer().getPluginManager().registerEvents(
                leaderboardGUI, this
        );
        getServer().getPluginManager().registerEvents(
                dailyRewardGUI, this
        );
        getServer().getPluginManager().registerEvents(
                new DailyJoinListener(this, dailyRewardManager, dailyRewardGUI), this
        );
        getServer().getPluginManager().registerEvents(
                dailyCosmeticListener, this
        );
        getServer().getPluginManager().registerEvents(
                new CosmeticTokenListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ChatPrefixListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ChatInventoryShareListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new ServerListMotdListener(fakePlayerService), this
        );
        getServer().getPluginManager().registerEvents(
                new FakePlayerWhisperAliasListener(this, fakePlayerService), this
        );
        getServer().getPluginManager().registerEvents(
                new FakePlayerTabSyncListener(this, fakePlayerService), this
        );
        worldBossManager = new WorldBossManager(this, bloodMoonManager);
        getServer().getPluginManager().registerEvents(
                worldBossManager, this
        );
        SpectateCommand spectateCommand = new SpectateCommand(this);
        AdminSpectateHotbarListener adminSpectateHotbarListener = new AdminSpectateHotbarListener(this, spectateCommand);
        spectateCommand.setAdminHotbarListener(adminSpectateHotbarListener);
        getServer().getPluginManager().registerEvents(
                spectateCommand, this
        );
        getServer().getPluginManager().registerEvents(
                adminSpectateHotbarListener, this
        );
        getServer().getPluginManager().registerEvents(
                new SpectateMenuBlockListener(spectateCommand), this
        );

        TokenRecipe.register();
        ReviveGuiTokenRecipe.register();
        CustomItemRecipes.register();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> discordWebhookService.clearAsteroidWebhookMessages());
        scheduleNextAsteroid();
        scheduleBountyTimers();
        scheduleBloodMoonChecks();
        scheduleTitleEffects();
        scheduleNicknamePermissionChecks();
        scheduleStatsAutosave();
        scheduleTabMenuRefresh();
        cosmeticAuraService.start(this);
        worldBossManager.start();
        mobStackManager.start();
        dailyCosmeticListener.start();

        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {
                    event.registrar().register("asteroid", new AsteroidCommand());
                    event.registrar().register("bloodmoon", new BloodMoonCommand());
                    event.registrar().register("titles", new TitlesCommand(titlesGUI));
                    event.registrar().register("bounty", new BountyCommand());
                    event.registrar().register("effects", new EffectsCommand(customPotionEffectManager, customPotionEffectRegistry, effectsStatusGUI));
                    event.registrar().register("rtp", new RtpCommand(this));
                    event.registrar().register("nick", new NickCommand(this));
                    event.registrar().register("discord", new DiscordCommand());
                    event.registrar().register("t", new TeamChatCommand(teamChatService));
                    event.registrar().register("tping", new EmergencyPingCommand(this, teamManager));
                    event.registrar().register("team", new TeamCommand(teamManager, teamManagementGUI));
                    event.registrar().register("waypoint", new WaypointCommand(this, teamManager, teamWaypointManager));
                    event.registrar().register("worldboss", new WorldBossCommand());
                    event.registrar().register("cosmetics", new CosmeticsCommand(cosmeticsGUI));
                    event.registrar().register("spectate", spectateCommand);
                    event.registrar().register("lbshowinv", new ChatInventoryShareCommand());
                    event.registrar().register("fake", new FakeCommand(this));
                    event.registrar().register("chat", new FakeChatCommand(this));
                    event.registrar().register("list", new ListCommand(this));
                    event.registrar().register("nemesis", new NemesisCommands(captainRegistry, captainSpawner, nemesisCaptainListGUI, killerResolver, minionController, captainTraitRegistry, armyGraphService, territoryPressureService, structureFootprintRepository, captainHabitatService, dialogueEngine, nemesisAdminWarbandService));
                }
        );
    }

    @Override
    public void onDisable() {
        if (asteroidTask != null) {
            asteroidTask.cancel();
            asteroidTask = null;
        }
        if (bountyTimeTask != null) {
            bountyTimeTask.cancel();
            bountyTimeTask = null;
        }
        if (bountyCleanupTask != null) {
            bountyCleanupTask.cancel();
            bountyCleanupTask = null;
        }
        if (bloodMoonTask != null) {
            bloodMoonTask.cancel();
            bloodMoonTask = null;
        }
        if (titleEffectTask != null) {
            titleEffectTask.cancel();
            titleEffectTask = null;
        }
        if (nicknamePermissionTask != null) {
            nicknamePermissionTask.cancel();
            nicknamePermissionTask = null;
        }
        if (tabMenuRefreshScheduler != null) {
            tabMenuRefreshScheduler.stop();
            tabMenuRefreshScheduler = null;
        }
        if (statsAutosaveTask != null) {
            statsAutosaveTask.cancel();
            statsAutosaveTask = null;
        }
        if (captainFlushTask != null) {
            captainFlushTask.cancel();
            captainFlushTask = null;
        }
        if (apiStatsTask != null) {
            apiStatsTask.cancel();
            apiStatsTask = null;
        }
        if (cosmeticAuraService != null) {
            cosmeticAuraService.stop();
            cosmeticAuraService = null;
        }
        if (mobStackManager != null) {
            mobStackManager.stop();
            mobStackManager = null;
        }
        if (aggressiveLogoutMobManager != null) {
            aggressiveLogoutMobManager.shutdown();
            aggressiveLogoutMobManager = null;
        }
        if (worldBossManager != null) {
            worldBossManager.shutdown();
            worldBossManager = null;
        }
        if (environmentalEffectsManager != null) {
            environmentalEffectsManager.shutdown();
            environmentalEffectsManager = null;
        }
        if (bloodMoonManager != null) {
            bloodMoonManager.shutdown();
        }
        if (deathMarkerManager != null) {
            deathMarkerManager.shutdown();
            deathMarkerManager = null;
        }
        if (teamWaypointManager != null) {
            teamWaypointManager.save();
            teamWaypointManager = null;
        }
        if (teamManager != null) {
            teamManager.saveAll();
            teamManager = null;
        }
        if (fakePlayerService != null) {
            fakePlayerService.shutdown();
            fakePlayerService = null;
        }
        if (nemesisBuildingService != null) {
            nemesisBuildingService.shutdown();
            nemesisBuildingService = null;
        }
        if (structureFootprintRepository != null) {
            structureFootprintRepository.saveIfDirty();
            structureFootprintRepository = null;
        }
        if (playerPlacedBlockIndex != null) {
            playerPlacedBlockIndex.saveIfDirty();
            playerPlacedBlockIndex = null;
        }
        structureManager = null;
        structureEventOrchestrator = null;
        nemesisAdminWarbandService = null;
        captainHabitatService = null;
        structureRaidService = null;
        flushDirtyCaptains();
        captainRegistry = null;
        captainSerializer = null;
        armyGraphSerializer = null;
        killerResolver = null;
        captainEntityBinder = null;
        if (captainSpawner != null) {
            captainSpawner.stop();
            captainSpawner = null;
        }
        captainTraitService = null;
        captainTraitRegistry = null;
        if (minionController != null) {
            minionController.stop();
            minionController = null;
        }
        if (nemesisUI != null) {
            nemesisUI.stop();
            nemesisUI = null;
        }
        nemesisCaptainListGUI = null;
        if (nemesisProgressionService != null) {
            nemesisProgressionService.stop();
            nemesisProgressionService = null;
        }
        nemesisRewardService = null;
        if (nemesisRivalryDirector != null) {
            nemesisRivalryDirector.stop();
            nemesisRivalryDirector = null;
        }
        loyaltyService = null;
        dialogueEngine = null;
        if (nemesisWarbandCoordinator != null) {
            nemesisWarbandCoordinator.stop();
            nemesisWarbandCoordinator = null;
        }
        territoryPressureService = null;
        if (promotionEvaluator != null) {
            promotionEvaluator.stop();
            promotionEvaluator = null;
        }
        armyGraphService = null;
        antiCheeseMonitor = null;
        int removedAsteroidMobs = AsteroidManager.clearAsteroidMobsForShutdown();
        AsteroidManager.clearAllAsteroids();
        if (removedAsteroidMobs > 0) {
            getLogger().info("Removed " + removedAsteroidMobs + " asteroid mobs during shutdown cleanup.");
        }
        BountyManager.save();
        ReviveStateManager.save();
        StatsManager.saveAll();
        if (dailyCosmeticListener != null) {
            dailyCosmeticListener.stop();
            dailyCosmeticListener = null;
        }
        if (dailyRewardManager != null) {
            dailyRewardManager.saveAll();
            dailyRewardManager = null;
        }
        if (bannedDeathZombieService != null) {
            bannedDeathZombieService.stop();
            bannedDeathZombieService = null;
        }
        if (apiClient != null) {
            apiClient.close();
            apiClient = null;
        }
        apiEventListener = null;
        dailyRewardGUI = null;
        potionDefinitionRegistry = null;
        customPotionEffectRegistry = null;
        customPotionEffectManager = null;
        effectsStatusGUI = null;
        titlesGUI = null;
        tabMenuRefreshScheduler = null;
        getLogger().info("LastBreathHC disabled.");
    }

    private void flushDirtyCaptains() {
        if (captainRegistry == null || captainSerializer == null) {
            return;
        }
        captainSerializer.saveDirty(captainRegistry.getAll(), captainRegistry.snapshotAndClearDirtyCaptainIds());
        if (armyGraphService != null && armyGraphSerializer != null) {
            armyGraphSerializer.saveDirty(armyGraphService.snapshot(), armyGraphService.consumeDirty());
        }
        if (playerPlacedBlockIndex != null) {
            playerPlacedBlockIndex.saveIfDirty();
        }
    }

    public static LastBreathHC getInstance() {
        return instance;
    }

    public BloodMoonManager getBloodMoonManager() {
        return bloodMoonManager;
    }

    public WorldBossManager getWorldBossManager() {
        return worldBossManager;
    }

    public FakePlayerService getFakePlayerService() {
        return fakePlayerService;
    }

    public FakePlayersSettings getFakePlayersSettings() {
        return fakePlayersSettings;
    }

    public DiscordWebhookService getDiscordWebhookService() {
        return discordWebhookService;
    }

    public CaptainRegistry getCaptainRegistry() {
        return captainRegistry;
    }

    private void scheduleNextAsteroid() {
        if (asteroidTask != null) {
            asteroidTask.cancel();
        }

        int minSeconds = getConfig().getInt("asteroid.spawn.minSeconds");
        int maxSeconds = getConfig().getInt("asteroid.spawn.maxSeconds");
        int countdownSeconds = getConfig().getInt("asteroid.countdownSeconds");

        if (minSeconds <= 0 || maxSeconds <= 0) {
            getLogger().warning("Asteroid spawn interval must be positive. Skipping scheduling.");
            return;
        }

        if (maxSeconds < minSeconds) {
            int swap = minSeconds;
            minSeconds = maxSeconds;
            maxSeconds = swap;
        }

        int delaySeconds = minSeconds + random.nextInt(maxSeconds - minSeconds + 1);

        asteroidTask = new BukkitRunnable() {
            private int remainingSeconds = delaySeconds;

            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    spawnScheduledAsteroid();
                    cancel();
                    scheduleNextAsteroid();
                    return;
                }

                /*if (remainingSeconds <= countdownSeconds) {
                    Bukkit.broadcastMessage("☄ Asteroid in " + remainingSeconds + " seconds!");
                }*/

                remainingSeconds--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void scheduleTitleEffects() {
        if (titleEffectTask != null) {
            titleEffectTask.cancel();
        }
        titleEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                TitleManager.refreshEquippedTitleEffects();
            }
        }.runTaskTimer(this, 20L, TitleManager.getTitleEffectRefreshTicks());
    }

    private void scheduleNicknamePermissionChecks() {
        if (nicknamePermissionTask != null) {
            nicknamePermissionTask.cancel();
        }
        nicknamePermissionTask = new BukkitRunnable() {
            private final NicknamePermissionMonitor monitor = new NicknamePermissionMonitor(LastBreathHC.this);

            @Override
            public void run() {
                monitor.run();
            }
        }.runTaskTimer(this, 20L, 100L);
    }

    private void scheduleStatsAutosave() {
        if (statsAutosaveTask != null) {
            statsAutosaveTask.cancel();
        }
        statsAutosaveTask = getServer().getScheduler().runTaskTimer(this, StatsManager::saveDirty, 20L * 60L, 20L * 60L);
    }

    private void scheduleApiStatsUpdates() {
        if (apiStatsTask != null) {
            apiStatsTask.cancel();
        }

        long periodTicks = Math.max(20L, getConfig().getLong("lastbreath.api.statsIntervalTicks",
                getConfig().getLong("lastBreathApi.statsIntervalTicks", 20L * 60L)));
        apiStatsTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (apiEventListener == null) {
                return;
            }
            for (Player player : getServer().getOnlinePlayers()) {
                apiEventListener.sendStatsFor(player);
            }
        }, periodTicks, periodTicks);
    }

    private void scheduleTabMenuRefresh() {
        long refreshTicks = Math.max(1L, getConfig().getLong("tabMenu.refreshTicks", 40L));
        long statsRefreshSeconds = Math.max(1L, getConfig().getLong("tabMenu.statsRefreshSeconds", 10L));
        TabMenuModelProvider modelProvider = new TabMenuModelProvider(this, fakePlayerService, Duration.ofSeconds(statsRefreshSeconds));
        tabMenuRefreshScheduler = new TabMenuRefreshScheduler(
                this,
                modelProvider,
                new TabMenuRenderer(),
                new BukkitTabMenuPlayerSource(fakePlayerService),
                new BukkitTabMenuUpdateHandler(fakePlayerService),
                refreshTicks
        );
        tabMenuRefreshScheduler.start();
    }

    private void spawnScheduledAsteroid() {
        if (!spawnRandomAsteroid()) {
            getLogger().warning("Unable to find a valid asteroid spawn location.");
        }
    }

    private void scheduleBountyTimers() {
        if (bountyTimeTask != null) {
            bountyTimeTask.cancel();
        }
        if (bountyCleanupTask != null) {
            bountyCleanupTask.cancel();
        }

        bountyTimeTask = new BukkitRunnable() {
            @Override
            public void run() {
                BountyManager.incrementOnlineTimeForOnlinePlayers(20L, 1L);
            }
        }.runTaskTimer(this, 20L, 20L);

        long dailyTicks = 20L * 60L * 60L * 24L;
        bountyCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                Instant cutoff = Instant.now().minus(Duration.ofDays(30));
                int removed = BountyManager.purgeExpiredLogouts(cutoff);
                if (removed > 0) {
                    getLogger().info("Removed " + removed + " expired bounty record(s).");
                }
            }
        }.runTaskTimer(this, dailyTicks, dailyTicks);
    }

    private void scheduleBloodMoonChecks() {
        if (bloodMoonTask != null) {
            bloodMoonTask.cancel();
        }

        bloodMoonTask = new BloodMoonScheduler(bloodMoonManager).runTaskTimer(this, 0L, 100L);
    }

    private World pickAsteroidWorld() {
        List<String> worldNames = getConfig().getStringList("asteroid.worlds");
        if (worldNames.isEmpty()) {
            return null;
        }

        World world = null;
        int attempts = worldNames.size();
        for (int i = 0; i < attempts; i++) {
            String name = worldNames.get(random.nextInt(worldNames.size()));
            world = getServer().getWorld(name);
            if (world != null) {
                return world;
            }
        }

        return null;
    }

    private Location pickAsteroidLocation(World world, int tier) {
        WorldBorder border = world.getWorldBorder();
        double borderRadius = border.getSize() / 2.0;
        double maxDistance = AsteroidManager.getMaxAsteroidDistance();
        if (maxDistance > 0) {
            borderRadius = Math.min(borderRadius, maxDistance);
        }
        double minRadius = 0.0;
        double maxRadius;
        if (tier == 3) {
            minRadius = 20_000.0;
            maxRadius = borderRadius;
        } else if (tier == 2) {
            minRadius = 10_000.0;
            maxRadius = Math.min(20_000.0, borderRadius);
        } else {
            maxRadius = Math.min(10_000.0, borderRadius);
        }
        int attempts = 30;

        if (maxRadius < minRadius) {
            maxRadius = minRadius;
        }

        Location location = findAsteroidLocationInBand(world, 0.0, 0.0, minRadius, maxRadius, attempts);
        if (location != null) {
            return location;
        }
        return findAsteroidLocation(world, 0.0, 0.0, Math.max(maxRadius, 0.0), attempts);
    }

    private Location findAsteroidLocation(World world, double centerX, double centerZ, double radius, int attempts) {
        WorldBorder border = world.getWorldBorder();
        double borderRadius = border.getSize() / 2.0;
        double minX = border.getCenter().getX() - borderRadius;
        double maxX = border.getCenter().getX() + borderRadius;
        double minZ = border.getCenter().getZ() - borderRadius;
        double maxZ = border.getCenter().getZ() + borderRadius;
        double maxDistance = AsteroidManager.getMaxAsteroidDistance();
        if (maxDistance > 0) {
            minX = Math.max(minX, -maxDistance);
            maxX = Math.min(maxX, maxDistance);
            minZ = Math.max(minZ, -maxDistance);
            maxZ = Math.min(maxZ, maxDistance);
        }

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight() - 1;

        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = centerX + Math.cos(angle) * distance;
            double z = centerZ + Math.sin(angle) * distance;
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                continue;
            }
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            int scanY = world.getHighestBlockYAt(blockX, blockZ);
            if (scanY < minHeight || scanY > maxHeight) {
                continue;
            }

            int groundY = -1;
            for (int y = scanY; y >= minHeight; y--) {
                Material groundType = world.getBlockAt(blockX, y, blockZ).getType();
                if (!groundType.isSolid()) {
                    continue;
                }
                if (isLogOrLeaf(groundType)) {
                    continue;
                }
                Material aboveType = y + 1 > maxHeight
                        ? Material.AIR
                        : world.getBlockAt(blockX, y + 1, blockZ).getType();
                if (isLogOrLeaf(aboveType)) {
                    continue;
                }
                groundY = y;
                break;
            }

            if (groundY < minHeight || groundY >= maxHeight) {
                continue;
            }

            Location candidate = new Location(world, blockX, groundY, blockZ);
            if (!border.isInside(candidate)) {
                continue;
            }

            return candidate;
        }

        return null;
    }

    private Location findAsteroidLocationInBand(World world, double centerX, double centerZ, double minRadius,
                                                double maxRadius, int attempts) {
        if (maxRadius <= 0) {
            return null;
        }
        double clampedMin = Math.max(0.0, Math.min(minRadius, maxRadius));
        double minSquared = clampedMin * clampedMin;
        double maxSquared = maxRadius * maxRadius;
        WorldBorder border = world.getWorldBorder();
        double borderRadius = border.getSize() / 2.0;
        double minX = border.getCenter().getX() - borderRadius;
        double maxX = border.getCenter().getX() + borderRadius;
        double minZ = border.getCenter().getZ() - borderRadius;
        double maxZ = border.getCenter().getZ() + borderRadius;
        double maxDistance = AsteroidManager.getMaxAsteroidDistance();
        if (maxDistance > 0) {
            minX = Math.max(minX, -maxDistance);
            maxX = Math.min(maxX, maxDistance);
            minZ = Math.max(minZ, -maxDistance);
            maxZ = Math.min(maxZ, maxDistance);
        }
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight() - 1;

        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(minSquared + random.nextDouble() * (maxSquared - minSquared));
            double x = centerX + Math.cos(angle) * distance;
            double z = centerZ + Math.sin(angle) * distance;
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                continue;
            }
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            int scanY = world.getHighestBlockYAt(blockX, blockZ);
            if (scanY < minHeight || scanY > maxHeight) {
                continue;
            }

            int groundY = -1;
            for (int y = scanY; y >= minHeight; y--) {
                Material groundType = world.getBlockAt(blockX, y, blockZ).getType();
                if (!groundType.isSolid()) {
                    continue;
                }
                if (isLogOrLeaf(groundType)) {
                    continue;
                }
                Material aboveType = y + 1 > maxHeight
                        ? Material.AIR
                        : world.getBlockAt(blockX, y + 1, blockZ).getType();
                if (isLogOrLeaf(aboveType)) {
                    continue;
                }
                groundY = y;
                break;
            }

            if (groundY < minHeight || groundY >= maxHeight) {
                continue;
            }

            Location candidate = new Location(world, blockX, groundY, blockZ);
            if (!border.isInside(candidate)) {
                continue;
            }

            return candidate;
        }

        return null;
    }

    private boolean isLogOrLeaf(Material material) {
        return Tag.LOGS.isTagged(material) || Tag.LEAVES.isTagged(material);
    }

    private Location pickAsteroidLocationNear(World world, Location origin, double radius, int attempts) {
        return findAsteroidLocation(world, origin.getX(), origin.getZ(), radius, attempts);
    }

    public World resolveAsteroidCommandWorld(CommandSender sender) {
        if (sender instanceof BlockCommandSender blockSender) {
            return blockSender.getBlock().getWorld();
        }
        return pickAsteroidWorld();
    }

    private int pickWeightedAsteroidTier() {
        int roll = random.nextInt(100);
        if (roll < 50) {
            return 1;
        }
        if (roll < 75) {
            return 2;
        }
        return 3;
    }

    public boolean spawnRandomAsteroid() {
        World world = pickAsteroidWorld();
        if (world == null) {
            getLogger().warning("No valid asteroid worlds configured. Skipping asteroid spawn.");
            return false;
        }

        int tier = pickWeightedAsteroidTier();
        Location location = pickAsteroidLocation(world, tier);
        if (location == null) {
            getLogger().warning("Unable to find a valid asteroid location in world " + world.getName() + ".");
            return false;
        }

        double meteorShowerChance = getConfig().getDouble("asteroid.spawn.meteorShowerChance", 0.0);
        boolean meteorShower = random.nextDouble() < meteorShowerChance;
        if (!meteorShower) {
            AsteroidManager.spawnAsteroid(world, location, tier);
            discordWebhookService.sendAsteroidCrashWebhook(location, tier, false);
            return true;
        }

        int showerCount = 3 + random.nextInt(3);
        double showerRadius = 50.0;
        int offsetAttempts = 3;
        int offsetSearchAttempts = 6;
        long cumulativeDelay = 0L;
        List<MeteorShowerSpawnPlan> spawnPlan = new ArrayList<>();
        spawnPlan.add(new MeteorShowerSpawnPlan(location, tier, 0L));

        for (int spawnIndex = 1; spawnIndex < showerCount; spawnIndex++) {
            Location offsetLocation = null;
            for (int attempt = 0; attempt < offsetAttempts; attempt++) {
                offsetLocation = pickAsteroidLocationNear(world, location, showerRadius, offsetSearchAttempts);
                if (offsetLocation != null) {
                    break;
                }
            }
            if (offsetLocation == null) {
                continue;
            }

            int offsetTier = pickWeightedAsteroidTier();
            long delay = 2L + random.nextInt(4);
            cumulativeDelay += delay;
            spawnPlan.add(new MeteorShowerSpawnPlan(offsetLocation, offsetTier, cumulativeDelay));
        }

        MeteorShowerAccumulator showerAccumulator = new MeteorShowerAccumulator(spawnPlan.size());
        Bukkit.broadcastMessage("☄ Meteor shower incoming!");

        for (MeteorShowerSpawnPlan plannedSpawn : spawnPlan) {
            Runnable spawnTask = () -> {
                AsteroidManager.spawnAsteroid(world, plannedSpawn.location(), plannedSpawn.tier());
                showerAccumulator.recordSpawn(plannedSpawn.location(), plannedSpawn.tier());
                showerAccumulator.completeSpawn(discordWebhookService);
            };

            if (plannedSpawn.delayTicks() == 0L) {
                spawnTask.run();
            } else {
                Bukkit.getScheduler().runTaskLater(this, spawnTask, plannedSpawn.delayTicks());
            }
        }

        return true;
    }

    private record MeteorShowerSpawnPlan(Location location, int tier, long delayTicks) {
    }

    private static final class MeteorShowerAccumulator {
        private final int expectedSpawnCount;
        private final List<DiscordWebhookService.AsteroidSpawnInfo> spawnedAsteroids;
        private final AtomicInteger completedSpawns = new AtomicInteger();

        private MeteorShowerAccumulator(int expectedSpawnCount) {
            this.expectedSpawnCount = expectedSpawnCount;
            this.spawnedAsteroids = Collections.synchronizedList(new ArrayList<>());
        }

        private void recordSpawn(Location location, int tier) {
            spawnedAsteroids.add(new DiscordWebhookService.AsteroidSpawnInfo(location, tier));
        }

        private void completeSpawn(DiscordWebhookService webhookService) {
            if (completedSpawns.incrementAndGet() != expectedSpawnCount) {
                return;
            }

            List<DiscordWebhookService.AsteroidSpawnInfo> snapshot;
            synchronized (spawnedAsteroids) {
                snapshot = new ArrayList<>(spawnedAsteroids);
            }
            webhookService.sendMeteorShowerWebhook(snapshot);
        }
    }
}
