package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArmyGraphService {
    private final Map<UUID, UUID> bodyguardOf = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> rivals = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> bloodBrother = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public synchronized void load(List<EdgeRecord> edges) {
        bodyguardOf.clear();
        rivals.clear();
        bloodBrother.clear();
        dirty = false;

        if (edges == null) {
            return;
        }

        for (EdgeRecord edge : edges) {
            if (edge == null || edge.from() == null || edge.to() == null) {
                continue;
            }
            switch (edge.type()) {
                case "bodyguardOf" -> bodyguardOf.put(edge.from(), edge.to());
                case "rivals" -> addRivalPair(edge.from(), edge.to());
                case "bloodBrother" -> setBloodBrotherPair(edge.from(), edge.to());
                default -> {
                }
            }
        }
        dirty = false;
    }

    public synchronized void seedFromCaptains(Collection<CaptainRecord> captains) {
        if (!isEmpty() || captains == null) {
            return;
        }
        for (CaptainRecord record : captains) {
            if (record == null || record.identity() == null || record.relationships().isEmpty()) {
                continue;
            }
            UUID from = record.identity().captainId();
            CaptainRecord.Relationships rel = record.relationships().orElse(null);
            if (rel == null) {
                continue;
            }
            if (rel.bodyguardOf() != null) {
                bodyguardOf.put(from, rel.bodyguardOf());
            }
            for (UUID rival : rel.rivals()) {
                if (rival != null) {
                    addRivalPair(from, rival);
                }
            }
            if (rel.bloodBrotherOf() != null) {
                setBloodBrotherPair(from, rel.bloodBrotherOf());
            }
        }
        dirty = true;
    }

    public synchronized void pruneMissingCaptains(Set<UUID> captainIds) {
        if (captainIds == null) {
            return;
        }
        bodyguardOf.entrySet().removeIf(entry -> !captainIds.contains(entry.getKey()) || !captainIds.contains(entry.getValue()));
        bloodBrother.entrySet().removeIf(entry -> !captainIds.contains(entry.getKey()) || !captainIds.contains(entry.getValue()));
        rivals.entrySet().removeIf(entry -> !captainIds.contains(entry.getKey()));
        for (Set<UUID> links : rivals.values()) {
            links.removeIf(id -> !captainIds.contains(id));
        }
        rivals.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        dirty = true;
    }

    public synchronized ArmyLinks linksOf(UUID captainId) {
        Set<UUID> rivalCopy = new HashSet<>(rivals.getOrDefault(captainId, Set.of()));
        return new ArmyLinks(bodyguardOf.get(captainId), bloodBrother.get(captainId), rivalCopy);
    }

    public synchronized List<EdgeRecord> snapshot() {
        List<EdgeRecord> edges = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : bodyguardOf.entrySet()) {
            edges.add(new EdgeRecord("bodyguardOf", entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<UUID, Set<UUID>> entry : rivals.entrySet()) {
            UUID left = entry.getKey();
            for (UUID right : entry.getValue()) {
                if (left.compareTo(right) < 0) {
                    edges.add(new EdgeRecord("rivals", left, right));
                }
            }
        }
        for (Map.Entry<UUID, UUID> entry : bloodBrother.entrySet()) {
            UUID left = entry.getKey();
            UUID right = entry.getValue();
            if (left.compareTo(right) < 0) {
                edges.add(new EdgeRecord("bloodBrother", left, right));
            }
        }
        return edges;
    }

    public synchronized boolean consumeDirty() {
        boolean current = dirty;
        dirty = false;
        return current;
    }

    private boolean isEmpty() {
        return bodyguardOf.isEmpty() && rivals.isEmpty() && bloodBrother.isEmpty();
    }

    private void addRivalPair(UUID left, UUID right) {
        if (Objects.equals(left, right)) {
            return;
        }
        rivals.computeIfAbsent(left, ignored -> ConcurrentHashMap.newKeySet()).add(right);
        rivals.computeIfAbsent(right, ignored -> ConcurrentHashMap.newKeySet()).add(left);
    }

    private void setBloodBrotherPair(UUID left, UUID right) {
        if (Objects.equals(left, right)) {
            return;
        }
        bloodBrother.put(left, right);
        bloodBrother.put(right, left);
    }

    public record EdgeRecord(String type, UUID from, UUID to) {
    }

    public record ArmyLinks(UUID bodyguardOf, UUID bloodBrotherOf, Set<UUID> rivals) {
    }
}
