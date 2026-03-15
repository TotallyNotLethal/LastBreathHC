package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.Map;

/**
 * Loader for modern .litematic files.
 */
public final class LitematicStructureTemplateLoader {

    private final Function<Material, BlockData> blockDataFactory;

    public LitematicStructureTemplateLoader() {
        this(Material::createBlockData);
    }

    LitematicStructureTemplateLoader(Function<Material, BlockData> blockDataFactory) {
        this.blockDataFactory = blockDataFactory;
    }

    public StructureTemplate load(String id, File litematicFile) throws IOException {
        if (!litematicFile.exists()) {
            throw new IOException("Missing litematic file: " + litematicFile.getAbsolutePath());
        }

        NbtBinaryReader.NbtCompound root;
        try (FileInputStream in = new FileInputStream(litematicFile)) {
            root = NbtBinaryReader.readRootCompound(in, "Litematic structure '" + id + "' (" + litematicFile.getAbsolutePath() + ")");
        } catch (IOException e) {
            throw new IOException("Failed parsing Litematic structure '" + id + "' at " + litematicFile.getAbsolutePath() + " [root]: " + e.getMessage(), e);
        }

        NbtBinaryReader.NbtCompound regions = asCompound(
                root.require("Regions", parseContext(id, litematicFile, "Regions")),
                parseContext(id, litematicFile, "Regions")
        );
        if (regions.value().isEmpty()) {
            throw new IOException(parseContext(id, litematicFile, "Regions") + ": no regions found");
        }

        PaletteStructureTemplateBuilder builder = new PaletteStructureTemplateBuilder(id, blockDataFactory);
        int placements = 0;

        for (Map.Entry<String, NbtBinaryReader.NbtTag> regionEntry : regions.value().entrySet()) {
            String regionName = regionEntry.getKey();
            String regionContext = parseContext(id, litematicFile, "Regions." + regionName);
            NbtBinaryReader.NbtCompound region = asCompound(regionEntry.getValue(), regionContext);

            int[] size = asIntArray(region.require("Size", regionContext), regionContext + ".Size");
            if (size.length != 3) {
                throw new IOException(regionContext + ".Size must contain exactly 3 values");
            }
            int sizeX = size[0];
            int sizeY = size[1];
            int sizeZ = size[2];
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                throw new IOException(regionContext + ".Size values must be positive, got [" + sizeX + ", " + sizeY + ", " + sizeZ + "]");
            }

            int[] position = asIntArray(region.require("Position", regionContext), regionContext + ".Position");
            if (position.length != 3) {
                throw new IOException(regionContext + ".Position must contain exactly 3 values");
            }
            int posX = position[0];
            int posY = position[1];
            int posZ = position[2];

            NbtBinaryReader.NbtList paletteList = asList(region.require("BlockStatePalette", regionContext), regionContext + ".BlockStatePalette");
            if (paletteList.size() == 0) {
                throw new IOException(regionContext + ".BlockStatePalette cannot be empty");
            }
            List<Material> palette = new ArrayList<>(paletteList.size());
            for (int i = 0; i < paletteList.size(); i++) {
                String paletteContext = regionContext + ".BlockStatePalette[" + i + "]";
                NbtBinaryReader.NbtCompound paletteEntry = asCompound(paletteList.get(i), paletteContext);
                String blockName = asString(paletteEntry.require("Name", paletteContext), paletteContext + ".Name");
                palette.add(resolveMaterial(blockName));
            }

            long[] blockStates = asLongArray(region.require("BlockStates", regionContext), regionContext + ".BlockStates");
            int totalBlocks = safeVolume(sizeX, sizeY, sizeZ, regionContext);
            int bitsPerBlock = Math.max(2, ceilLog2(palette.size()));
            int requiredLongs = (int) Math.ceil((double) (totalBlocks * bitsPerBlock) / 64.0);
            if (blockStates.length < requiredLongs) {
                throw new IOException(regionContext + ".BlockStates is too short: expected at least " + requiredLongs + " longs but found " + blockStates.length);
            }

            for (int index = 0; index < totalBlocks; index++) {
                int paletteIndex = readPackedValue(blockStates, bitsPerBlock, index);
                if (paletteIndex < 0 || paletteIndex >= palette.size()) {
                    throw new IOException(regionContext + ".BlockStates[" + index + "] references invalid palette index " + paletteIndex + " (palette size " + palette.size() + ")");
                }
                Material material = palette.get(paletteIndex);
                if (material == null || material == Material.AIR) {
                    continue;
                }

                int x = index % sizeX;
                int z = (index / sizeX) % sizeZ;
                int y = index / (sizeX * sizeZ);

                builder.addBlock(posX + x, posY + y, posZ + z, material);
                placements++;
            }
        }

        if (placements == 0) {
            throw new IOException(parseContext(id, litematicFile, "BlockStates") + ": no placeable blocks decoded from palette/state data");
        }

        return builder.build();
    }

    private static int safeVolume(int x, int y, int z, String context) throws IOException {
        long volume = (long) x * y * z;
        if (volume > Integer.MAX_VALUE) {
            throw new IOException(context + ": region volume too large (" + volume + ")");
        }
        return (int) volume;
    }

    private static int readPackedValue(long[] data, int bitsPerBlock, int index) {
        long bitIndex = (long) index * bitsPerBlock;
        int startLong = (int) (bitIndex >>> 6);
        int startOffset = (int) (bitIndex & 63L);
        long mask = (1L << bitsPerBlock) - 1L;

        long value = data[startLong] >>> startOffset;
        int endBits = startOffset + bitsPerBlock;
        if (endBits > 64) {
            int spill = endBits - 64;
            value |= data[startLong + 1] << (bitsPerBlock - spill);
        }
        return (int) (value & mask);
    }

    private static int ceilLog2(int value) {
        int bits = 0;
        int n = value - 1;
        while (n > 0) {
            bits++;
            n >>= 1;
        }
        return Math.max(bits, 1);
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
        return "Litematic structure '" + id + "' (" + sourceFile.getAbsolutePath() + ") [" + section + "]";
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

    private static int[] asIntArray(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtIntArray array) {
            return array.value();
        }
        throw new IOException(context + ": expected INT_ARRAY tag");
    }

    private static long[] asLongArray(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtLongArray array) {
            return array.value();
        }
        throw new IOException(context + ": expected LONG_ARRAY tag");
    }

    private static String asString(NbtBinaryReader.NbtTag tag, String context) throws IOException {
        if (tag instanceof NbtBinaryReader.NbtString str) {
            return str.value();
        }
        throw new IOException(context + ": expected STRING tag");
    }
}
