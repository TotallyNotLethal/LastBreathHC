package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public final class BasicStructureTemplate implements StructureTemplate {
    private final String id;
    private final List<StructureBlockPlacement> blockPlacements;
    private final BoundingBox relativeBoundingBox;

    public BasicStructureTemplate(String id, List<StructureBlockPlacement> blockPlacements, BoundingBox relativeBoundingBox) {
        this.id = id;
        this.blockPlacements = new ArrayList<>(blockPlacements);
        this.relativeBoundingBox = relativeBoundingBox.clone();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<StructureBlockPlacement> getBlockPlacements() {
        return List.copyOf(blockPlacements);
    }

    @Override
    public BoundingBox getRelativeBoundingBox() {
        return relativeBoundingBox.clone();
    }
}
