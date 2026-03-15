package com.lastbreath.hc.lastBreathHC.bootstrap;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.nemesis.*;
import com.lastbreath.hc.lastBreathHC.structures.PlayerPlacedBlockIndex;
import com.lastbreath.hc.lastBreathHC.structures.StructureFootprintRepository;
import com.lastbreath.hc.lastBreathHC.structures.StructureManager;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.stream.Collectors;

public final class NemesisModule implements PluginModule {
    private final LastBreathHC plugin;
    private final ListenerRegistrar listenerRegistrar;
    private final StructureModule structureModule;
    private final Runnable flushDirtyCaptains;

    private BukkitTask captainFlushTask;
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
    private StructureEventOrchestrator structureEventOrchestrator;
    private NemesisAdminWarbandService nemesisAdminWarbandService;
    private CaptainHabitatService captainHabitatService;
    private StructureRaidService structureRaidService;
    private NemesisWarbandCoordinator nemesisWarbandCoordinator;

    public NemesisModule(
            LastBreathHC plugin,
            ListenerRegistrar listenerRegistrar,
            StructureModule structureModule,
            Runnable flushDirtyCaptains
    ) {
        this.plugin = plugin;
        this.listenerRegistrar = listenerRegistrar;
        this.structureModule = structureModule;
        this.flushDirtyCaptains = flushDirtyCaptains;
    }

    @Override
    public void register() {
        StructureFootprintRepository structureFootprintRepository = structureModule.structureFootprintRepository();
        StructureManager structureManager = structureModule.structureManager();
        PlayerPlacedBlockIndex playerPlacedBlockIndex = structureModule.playerPlacedBlockIndex();
        NemesisBuildingService nemesisBuildingService = structureModule.nemesisBuildingService();
        captainRegistry = new CaptainRegistry();
        captainHabitatService = new CaptainHabitatService(plugin, captainRegistry, structureFootprintRepository);
        nemesisBuildingService.attachCaptainServices(captainRegistry, captainHabitatService);
        captainSerializer = new CaptainSerializer(plugin, new File(plugin.getDataFolder(), "nemesis-captains.yml"));
        captainRegistry.load(captainSerializer.load());
        armyGraphSerializer = new ArmyGraphSerializer(plugin, new File(plugin.getDataFolder(), "nemesis-army-graph.yml"));
        armyGraphService = new ArmyGraphService();
        armyGraphService.load(armyGraphSerializer.load());
        armyGraphService.seedFromCaptains(captainRegistry.getAll());
        structureEventOrchestrator = new StructureEventOrchestrator(captainRegistry, structureManager, captainHabitatService, armyGraphService, nemesisBuildingService);
        armyGraphService.pruneMissingCaptains(captainRegistry.getAll().stream().map(record -> record.identity().captainId()).collect(Collectors.toSet()));

        long captainFlushIntervalTicks = 20L * 60L * 3L;
        captainFlushTask = plugin.getServer().getScheduler().runTaskTimer(plugin, flushDirtyCaptains, captainFlushIntervalTicks, captainFlushIntervalTicks);

        killerResolver = new KillerResolver();
        captainTraitRegistry = new CaptainTraitRegistry(plugin);
        captainNameGenerator = new CaptainNameGenerator(plugin);
        captainEntityBinder = new CaptainEntityBinder(plugin, captainRegistry);
        captainTraitService = new CaptainTraitService(captainEntityBinder, captainTraitRegistry);
        captainEntityBinder.setTraitService(captainTraitService);
        nemesisProgressionService = new NemesisProgressionService(plugin, captainRegistry, captainEntityBinder, captainTraitRegistry, structureEventOrchestrator);
        nemesisProgressionService.start();

        captainSpawner = new CaptainSpawner(plugin, captainRegistry, captainEntityBinder, new CaptainSpawner.NoOpProtectedRegionChecker(), playerPlacedBlockIndex, captainNameGenerator, structureEventOrchestrator);
        captainSpawner.start();
        minionController = new MinionController(plugin, captainRegistry, captainEntityBinder, nemesisProgressionService, captainHabitatService);
        minionController.start();

        nemesisAdminWarbandService = new NemesisAdminWarbandService(captainSpawner, minionController, armyGraphService, structureEventOrchestrator);
        nemesisUI = new NemesisUI(plugin, captainRegistry, captainEntityBinder);
        nemesisUI.start();
        nemesisCaptainListGUI = new NemesisCaptainListGUI(captainRegistry, captainEntityBinder, captainTraitRegistry);
        nemesisRewardService = new NemesisRewardService(plugin, captainEntityBinder, captainRegistry);
        nemesisRivalryDirector = new NemesisRivalryDirector(plugin, captainRegistry, captainEntityBinder);
        nemesisRivalryDirector.start();

        promotionEvaluator = new PromotionEvaluator(plugin, captainRegistry, structureEventOrchestrator);
        promotionEvaluator.start();
        dialogueEngine = new DialogueEngine(plugin);
        loyaltyService = new LoyaltyService(plugin, captainRegistry, captainEntityBinder, armyGraphService, structureEventOrchestrator, dialogueEngine);
        nemesisWarbandCoordinator = new NemesisWarbandCoordinator(plugin, captainRegistry, captainEntityBinder, dialogueEngine);
        nemesisWarbandCoordinator.start();
        territoryPressureService = new TerritoryPressureService(plugin, structureEventOrchestrator);

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

        antiCheeseMonitor = new AntiCheeseMonitor(plugin, captainEntityBinder);
        structureRaidService = new StructureRaidService(plugin, captainRegistry, structureFootprintRepository, territoryPressureService, structureEventOrchestrator, captainEntityBinder);

        listenerRegistrar.register(killerResolver);
        listenerRegistrar.register(new CaptainCombatListener(plugin, captainRegistry, killerResolver, captainEntityBinder, captainTraitService, captainTraitRegistry, nemesisUI, nemesisProgressionService, new TokenAwareDeathOutcomeResolver(), captainNameGenerator, captainHabitatService, armyGraphService, dialogueEngine));
        listenerRegistrar.register(captainSpawner);
        listenerRegistrar.register(playerPlacedBlockIndex);
        listenerRegistrar.register(minionController);
        listenerRegistrar.register(nemesisCaptainListGUI);
        listenerRegistrar.register(nemesisRewardService);
        listenerRegistrar.register(loyaltyService);
        listenerRegistrar.register(new InfluenceItemHandler(plugin, captainRegistry, captainEntityBinder, territoryPressureService, loyaltyService));
        listenerRegistrar.register(antiCheeseMonitor);
        listenerRegistrar.register(structureRaidService);
    }

    @Override
    public void shutdown() {
        if (captainFlushTask != null) {
            captainFlushTask.cancel();
            captainFlushTask = null;
        }
        flushDirtyCaptains.run();
        if (captainSpawner != null) {
            captainSpawner.stop();
        }
        if (minionController != null) {
            minionController.stop();
        }
        if (nemesisUI != null) {
            nemesisUI.stop();
        }
        if (nemesisProgressionService != null) {
            nemesisProgressionService.stop();
        }
        if (nemesisRivalryDirector != null) {
            nemesisRivalryDirector.stop();
        }
        if (nemesisWarbandCoordinator != null) {
            nemesisWarbandCoordinator.stop();
        }
        if (promotionEvaluator != null) {
            promotionEvaluator.stop();
        }
    }

    public BukkitTask captainFlushTask() { return captainFlushTask; }
    public CaptainRegistry captainRegistry() { return captainRegistry; }
    public CaptainSerializer captainSerializer() { return captainSerializer; }
    public ArmyGraphSerializer armyGraphSerializer() { return armyGraphSerializer; }
    public ArmyGraphService armyGraphService() { return armyGraphService; }
    public KillerResolver killerResolver() { return killerResolver; }
    public CaptainEntityBinder captainEntityBinder() { return captainEntityBinder; }
    public CaptainSpawner captainSpawner() { return captainSpawner; }
    public CaptainTraitRegistry captainTraitRegistry() { return captainTraitRegistry; }
    public CaptainTraitService captainTraitService() { return captainTraitService; }
    public MinionController minionController() { return minionController; }
    public NemesisUI nemesisUI() { return nemesisUI; }
    public NemesisCaptainListGUI nemesisCaptainListGUI() { return nemesisCaptainListGUI; }
    public CaptainNameGenerator captainNameGenerator() { return captainNameGenerator; }
    public NemesisProgressionService nemesisProgressionService() { return nemesisProgressionService; }
    public NemesisRewardService nemesisRewardService() { return nemesisRewardService; }
    public NemesisRivalryDirector nemesisRivalryDirector() { return nemesisRivalryDirector; }
    public AntiCheeseMonitor antiCheeseMonitor() { return antiCheeseMonitor; }
    public PromotionEvaluator promotionEvaluator() { return promotionEvaluator; }
    public LoyaltyService loyaltyService() { return loyaltyService; }
    public DialogueEngine dialogueEngine() { return dialogueEngine; }
    public TerritoryPressureService territoryPressureService() { return territoryPressureService; }
    public StructureEventOrchestrator structureEventOrchestrator() { return structureEventOrchestrator; }
    public NemesisAdminWarbandService nemesisAdminWarbandService() { return nemesisAdminWarbandService; }
    public CaptainHabitatService captainHabitatService() { return captainHabitatService; }
    public StructureRaidService structureRaidService() { return structureRaidService; }
    public NemesisWarbandCoordinator nemesisWarbandCoordinator() { return nemesisWarbandCoordinator; }
}
