package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Material;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loader for vanilla structure block NBT files.
 */
public final class NbtStructureTemplateLoader {

    public StructureTemplate load(String id, File nbtStructureFile) throws IOException {
        if (!nbtStructureFile.exists()) {
            throw new IOException("Missing NBT structure file: " + nbtStructureFile.getAbsolutePath());
        }

        NbtBinaryReader.NbtCompound root;
        try (FileInputStream in = new FileInputStream(nbtStructureFile)) {
            root = NbtBinaryReader.readRootCompound(in, "Structure NBT '" + id + "' (" + nbtStructureFile.getAbsolutePath() + ")");
        } catch (IOException e) {
            throw new IOException("Failed parsing structure NBT '" + id + "' at " + nbtStructureFile.getAbsolutePath() + " [root]: " + e.getMessage(), e);
        }

        String paletteContext = parseContext(id, nbtStructureFile, "palette");
        NbtBinaryReader.NbtList paletteList = asList(root.require("palette", paletteContext), paletteContext);
        if (paletteList.size() == 0) {
            throw new IOException(paletteContext + ": palette cannot be empty");
        }
        List<Material> palette = new ArrayList<>(paletteList.size());
        for (int i = 0; i < paletteList.size(); i++) {
            String entryContext = paletteContext + "[" + i + "]";
            NbtBinaryReader.NbtCompound entry = asCompound(paletteList.get(i), entryContext);
            String blockName = asString(entry.require("Name", entryContext), entryContext + ".Name");
            palette.add(resolveMaterial(blockName));
        }

        NbtBinaryReader.NbtList blocks = asList(root.require("blocks", parseContext(id, nbtStructureFile, "blocks")), parseContext(id, nbtStructureFile, "blocks"));
        if (blocks.size() == 0) {
            throw new IOException(parseContext(id, nbtStructureFile, "blocks") + ": blocks list cannot be empty");
        }

        // Vanilla structure NBT positions are already relative to the structure origin.
        // We keep this origin convention unchanged and emit each decoded block at its listed `pos`.
        PaletteStructureTemplateBuilder builder = new PaletteStructureTemplateBuilder(id);
        int placements = 0;
        for (int i = 0; i < blocks.size(); i++) {
            String blockContext = parseContext(id, nbtStructureFile, "blocks[" + i + "]");
            NbtBinaryReader.NbtCompound blockEntry = asCompound(blocks.get(i), blockContext);
            int[] pos = asIntList3(blockEntry.require("pos", blockContext), blockContext + ".pos");
            int stateIndex = asInt(blockEntry.require("state", blockContext), blockContext + ".state");
            if (stateIndex < 0 || stateIndex >= palette.size()) {
                throw new IOException(blockContext + ": state index " + stateIndex + " is outside palette size " + palette.size());
            }

            Material material = palette.get(stateIndex);
            if (material == null || material == Material.AIR) {
                continue;
            }
            builder.addBlock(pos[0], pos[1], pos[2], material);
            placements++;
        }

        if (placements == 0) {
            throw new IOException(parseContext(id, nbtStructureFile, "blocks") + ": no placeable blocks decoded from palette/state data");
        }

        return builder.build();
    }

    private static Material resolveMaterial(String namespacedBlockId) {
        if (namespacedBlockId == null) {
            return null;
        }
        String normalized = namespacedBlockId.toUpperCase();
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        return Material.matchMaterial(normalized, false);
    }

    private static String parseContext(String id, File sourceFile, String section) {
        return "Structure NBT '" + id + "' (" + sourceFile.getAbsolutePath() + ") [" + section + "]";
    }

    private static NbtBinaryReader.NbtCompound asCompound(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtCompound compound) {
            return compound;
        }
        throw new IOException(context + ": expected COMPOUND tag");
    }

    private static NbtBinaryReader.NbtList asList(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtList list) {
            return list;
        }
        throw new IOException(context + ": expected LIST tag");
    }

    private static String asString(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtString str) {
            return str.value();
        }
        throw new IOException(context + ": expected STRING tag");
    }

    private static int asInt(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtInt intTag) {
            return intTag.value();
        }
        throw new IOException(context + ": expected INT tag");
    }

    private static int[] asIntList3(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (!(tag instanceof NbtBinaryReader.NbtList list)) {
            throw new IOException(context + ": expected LIST tag");
        }
        if (list.size() != 3) {
            throw new IOException(context + ": expected exactly 3 list values");
        }
        int[] values = new int[3];
        for (int i = 0; i < 3; i++) {
            NbtBinaryReader.NbtTag valueTag = list.get(i);
            if (!(valueTag instanceof NbtBinaryReader.NbtInt intTag)) {
                throw new IOException(context + "[" + i + "]: expected INT value");
            }
            values[i] = intTag.value();
        }
        return values;
    }
}
