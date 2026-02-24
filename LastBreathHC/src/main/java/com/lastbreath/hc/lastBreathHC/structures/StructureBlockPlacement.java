package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public record StructureBlockPlacement(Vector relativeOffset, BlockData blockData) {
}
