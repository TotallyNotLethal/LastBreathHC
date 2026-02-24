package com.lastbreath.hc.lastBreathHC.structures;

public record StructureTemplateMetadata(
        StructureTemplate template,
        String structureType,
        int tier,
        int spawnRadiusExclusion
) {
}
