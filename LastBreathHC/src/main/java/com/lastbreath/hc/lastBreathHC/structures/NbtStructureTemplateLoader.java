package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Material;

import java.io.File;
import java.io.IOException;

/**
 * Lightweight loader for structure block NBT files.
 */
public final class NbtStructureTemplateLoader {

    public StructureTemplate load(String id, File nbtStructureFile) throws IOException {
        if (!nbtStructureFile.exists()) {
            throw new IOException("Missing NBT structure file: " + nbtStructureFile.getAbsolutePath());
        }

        // TODO: replace with a full NBT structure parser.
        return new PaletteStructureTemplateBuilder(id)
                .addBlock(0, 0, 0, Material.COBBLESTONE)
                .build();
    }
}
