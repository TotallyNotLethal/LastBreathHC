package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PaletteStructureTemplateBuilder {
    private final String id;
    private final List<StructureBlockPlacement> placements = new ArrayList<>();
    private final Function<Material, BlockData> blockDataFactory;

    public PaletteStructureTemplateBuilder(String id) {
        this(id, Material::createBlockData);
    }

    public PaletteStructureTemplateBuilder(String id, Function<Material, BlockData> blockDataFactory) {
        this.id = id;
        this.blockDataFactory = blockDataFactory;
    }

    public PaletteStructureTemplateBuilder addBlock(int x, int y, int z, Material material) {
        BlockData data = blockDataFactory.apply(material);
        return addBlock(x, y, z, data);
    }

    public PaletteStructureTemplateBuilder addBlock(int x, int y, int z, BlockData blockData) {
        placements.add(new StructureBlockPlacement(new Vector(x, y, z), blockData.clone()));
        return this;
    }

    public StructureTemplate build() {
        if (placements.isEmpty()) {
            return new BasicStructureTemplate(id, List.of(), new BoundingBox(0, 0, 0, 0, 0, 0));
        }

        BoundingBox box = new BoundingBox(
                placements.getFirst().relativeOffset().getX(),
                placements.getFirst().relativeOffset().getY(),
                placements.getFirst().relativeOffset().getZ(),
                placements.getFirst().relativeOffset().getX(),
                placements.getFirst().relativeOffset().getY(),
                placements.getFirst().relativeOffset().getZ()
        );

        for (StructureBlockPlacement placement : placements) {
            box.expand(placement.relativeOffset());
        }

        return new BasicStructureTemplate(id, placements, box);
    }
}
