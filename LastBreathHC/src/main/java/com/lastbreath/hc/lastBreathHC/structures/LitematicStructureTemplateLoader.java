package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Material;

import java.io.File;
import java.io.IOException;

/**
 * Lightweight placeholder loader that can be backed by a full Litematic parser later.
 */
public final class LitematicStructureTemplateLoader {

    public StructureTemplate load(String id, File litematicFile) throws IOException {
        if (!litematicFile.exists()) {
            throw new IOException("Missing litematic file: " + litematicFile.getAbsolutePath());
        }

        // TODO: replace with a full parser implementation.
        return new PaletteStructureTemplateBuilder(id)
                .addBlock(0, 0, 0, Material.STONE_BRICKS)
                .build();
    }
}
