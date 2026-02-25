package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NemesisAdminWarbandService {
    private final CaptainSpawner captainSpawner;
    private final MinionController minionController;
    private final ArmyGraphService armyGraphService;
    private final StructureEventOrchestrator structureEventOrchestrator;

    public NemesisAdminWarbandService(CaptainSpawner captainSpawner,
                                      MinionController minionController,
                                      ArmyGraphService armyGraphService,
                                      StructureEventOrchestrator structureEventOrchestrator) {
        this.captainSpawner = captainSpawner;
        this.minionController = minionController;
        this.armyGraphService = armyGraphService;
        this.structureEventOrchestrator = structureEventOrchestrator;
    }

    public WarbandSpawnResult spawnArmyAt(Player initiator, Location location) {
        if (initiator == null || location == null || location.getWorld() == null) {
            return WarbandSpawnResult.failed("Invalid spawn location.");
        }

        String region = location.getWorld().getName().toLowerCase(Locale.ROOT) + "-" + location.getChunk().getX() + ":" + location.getChunk().getZ();
        UUID leaderId = UUID.randomUUID();
        UUID vanguardId = UUID.randomUUID();
        UUID rearguardId = UUID.randomUUID();
        String seatId = leaderId.toString();

        List<CaptainRecord> created = new ArrayList<>();

        CaptainRecord leader = captainSpawner.spawnAdminCaptain(
                leaderId,
                initiator.getUniqueId(),
                location.clone(),
                new CaptainRecord.Political(Rank.WARCHIEF.name(), region, seatId, 320.0, 0.9),
                new CaptainRecord.Relationships(List.of(vanguardId, rearguardId), List.of(), null, vanguardId),
                "War Captain"
        );
        if (leader != null) {
            created.add(leader);
        }

        CaptainRecord vanguard = captainSpawner.spawnAdminCaptain(
                vanguardId,
                initiator.getUniqueId(),
                offset(location, 4.0, 2.0),
                new CaptainRecord.Political(Rank.CAPTAIN.name(), region, seatId, 210.0, 0.55),
                new CaptainRecord.Relationships(List.of(leaderId, rearguardId), List.of(), leaderId, leaderId),
                "Vanguard"
        );
        if (vanguard != null) {
            created.add(vanguard);
        }

        CaptainRecord rearguard = captainSpawner.spawnAdminCaptain(
                rearguardId,
                initiator.getUniqueId(),
                offset(location, -4.0, -2.0),
                new CaptainRecord.Political(Rank.CAPTAIN.name(), region, seatId, 180.0, 0.5),
                new CaptainRecord.Relationships(List.of(leaderId, vanguardId), List.of(), leaderId, null),
                "Rearguard"
        );
        if (rearguard != null) {
            created.add(rearguard);
        }

        if (created.isEmpty()) {
            return WarbandSpawnResult.failed("No captains could be spawned at that location.");
        }

        if (created.stream().anyMatch(r -> r.identity().captainId().equals(vanguardId))) {
            armyGraphService.setBodyguardOf(vanguardId, leaderId);
            armyGraphService.setBloodBrother(leaderId, vanguardId);
        }
        if (created.stream().anyMatch(r -> r.identity().captainId().equals(rearguardId))) {
            armyGraphService.setBodyguardOf(rearguardId, leaderId);
        }

        int minionsCreated = 0;
        for (CaptainRecord record : created) {
            minionsCreated += minionController.spawnMinionsNow(record.identity().captainId());
        }

        UUID structureOwner = created.stream()
                .map(record -> record.identity().captainId())
                .filter(id -> id.equals(leaderId))
                .findFirst()
                .orElse(created.get(0).identity().captainId());
        int structuresTriggered = structureEventOrchestrator.spawnFortificationWave(structureOwner, "admin-spawnarmy");

        return new WarbandSpawnResult(true, null, created, minionsCreated, structuresTriggered, region, seatId);
    }

    private Location offset(Location base, double dx, double dz) {
        Location shifted = base.clone().add(dx, 0, dz);
        int y = shifted.getWorld() == null ? shifted.getBlockY() : shifted.getWorld().getHighestBlockYAt(shifted) + 1;
        shifted.setY(y);
        return shifted;
    }

    public record WarbandSpawnResult(boolean success,
                                     String message,
                                     List<CaptainRecord> captains,
                                     int minionsCreated,
                                     int structuresTriggered,
                                     String region,
                                     String seatId) {
        public static WarbandSpawnResult failed(String message) {
            return new WarbandSpawnResult(false, message, List.of(), 0, 0, "n/a", "n/a");
        }
    }
}
