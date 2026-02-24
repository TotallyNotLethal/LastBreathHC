package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.util.BoundingBox;

import java.util.List;

public interface StructureTemplate {
    String getId();

    List<StructureBlockPlacement> getBlockPlacements();

    BoundingBox getRelativeBoundingBox();
}
