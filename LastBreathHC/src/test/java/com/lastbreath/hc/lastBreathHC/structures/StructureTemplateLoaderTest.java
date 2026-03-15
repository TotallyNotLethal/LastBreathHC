package com.lastbreath.hc.lastBreathHC.structures;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureTemplateLoaderTest {
    private static final byte TAG_END = 0;
    private static final byte TAG_INT = 3;
    private static final byte TAG_LONG_ARRAY = 12;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;
    private static final byte TAG_INT_ARRAY = 11;

    @Test
    void loadLitematic_parsesKnownBlocksAndSkipsUnsupportedStatesDeterministically(@TempDir Path tempDir) throws Exception {
        File fixture = writeLitematicFixture(tempDir.resolve("sample_structure.litematic").toFile());
        StructureTemplate template = new LitematicStructureTemplateLoader(blockDataFactory()).load("litematic_fixture", fixture);

        Map<Vector, Material> placements = toPlacementMap(template);
        assertEquals(3, placements.size(), "Unsupported palette entries should be skipped");
        assertEquals(Material.STONE_BRICKS, placements.get(new Vector(0, 0, 0)));
        assertEquals(Material.OAK_PLANKS, placements.get(new Vector(1, 0, 0)));
        assertEquals(Material.STONE_BRICKS, placements.get(new Vector(1, 1, 0)));
    }

    @Test
    void loadNbt_parsesPaletteAndBlocksAndSkipsUnsupportedStatesDeterministically(@TempDir Path tempDir) throws Exception {
        File fixture = writeStructureNbtFixture(tempDir.resolve("sample_structure.nbt").toFile());
        StructureTemplate template = new NbtStructureTemplateLoader(blockDataFactory()).load("nbt_fixture", fixture);

        Map<Vector, Material> placements = toPlacementMap(template);
        assertEquals(2, placements.size(), "Unsupported palette entries should be skipped");
        assertEquals(Material.STONE, placements.get(new Vector(0, 0, 0)));
        assertEquals(Material.OAK_PLANKS, placements.get(new Vector(1, 0, 0)));
    }

    @Test
    void loadLitematic_missingFileThrowsIOException() {
        IOException ex = assertThrows(IOException.class,
                () -> new LitematicStructureTemplateLoader().load("missing_litematic", new File("does-not-exist.litematic")));
        assertTrue(ex.getMessage().contains("Missing litematic file"));
    }

    @Test
    void loadNbt_missingFileThrowsIOException() {
        IOException ex = assertThrows(IOException.class,
                () -> new NbtStructureTemplateLoader().load("missing_nbt", new File("does-not-exist.nbt")));
        assertTrue(ex.getMessage().contains("Missing NBT structure file"));
    }

    private static File writeStructureNbtFixture(File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeByte(TAG_COMPOUND);
            writeString(out, "structure");

            writeNamedListHeader(out, "size", TAG_INT, 3);
            out.writeInt(2);
            out.writeInt(2);
            out.writeInt(1);

            writeNamedListHeader(out, "palette", TAG_COMPOUND, 3);
            writePaletteEntry(out, "minecraft:stone");
            writePaletteEntry(out, "minecraft:oak_planks");
            writePaletteEntry(out, "minecraft:not_a_block");

            writeNamedListHeader(out, "blocks", TAG_COMPOUND, 3);
            writeBlockEntry(out, 0, 0, 0, 0);
            writeBlockEntry(out, 1, 0, 0, 1);
            writeBlockEntry(out, 0, 1, 0, 2);

            writeNamedListHeader(out, "entities", TAG_COMPOUND, 0);

            out.writeByte(TAG_END);
        }
        return file;
    }

    private static File writeLitematicFixture(File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeByte(TAG_COMPOUND);
            writeString(out, "Litematic");

            writeNamedInt(out, "Version", 6);
            writeNamedInt(out, "SubVersion", 1);
            writeNamedInt(out, "MinecraftDataVersion", 3955);

            out.writeByte(TAG_COMPOUND);
            writeString(out, "Metadata");
            writeNamedString(out, "Name", "fixture");
            out.writeByte(TAG_END);

            out.writeByte(TAG_COMPOUND);
            writeString(out, "Regions");
            out.writeByte(TAG_COMPOUND);
            writeString(out, "main");

            writeNamedIntArray(out, "Position", new int[]{0, 0, 0});
            writeNamedIntArray(out, "Size", new int[]{2, 2, 1});

            writeNamedListHeader(out, "BlockStatePalette", TAG_COMPOUND, 3);
            writePaletteEntry(out, "minecraft:stone_bricks");
            writePaletteEntry(out, "minecraft:oak_planks");
            writePaletteEntry(out, "minecraft:not_a_block");

            out.writeByte(TAG_LONG_ARRAY);
            writeString(out, "BlockStates");
            out.writeInt(1);
            out.writeLong(36L); // packed states [0,1,2,0] with 2 bits per block

            out.writeByte(TAG_END); // main region
            out.writeByte(TAG_END); // Regions
            out.writeByte(TAG_END); // root
        }
        return file;
    }

    private static void writePaletteEntry(DataOutputStream out, String blockName) throws IOException {
        writeNamedString(out, "Name", blockName);
        out.writeByte(TAG_END);
    }

    private static void writeBlockEntry(DataOutputStream out, int x, int y, int z, int state) throws IOException {
        writeNamedListHeader(out, "pos", TAG_INT, 3);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        writeNamedInt(out, "state", state);
        out.writeByte(TAG_END);
    }

    private static void writeNamedListHeader(DataOutputStream out, String name, byte elementType, int size) throws IOException {
        out.writeByte(TAG_LIST);
        writeString(out, name);
        out.writeByte(elementType);
        out.writeInt(size);
    }

    private static void writeNamedInt(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(TAG_INT);
        writeString(out, name);
        out.writeInt(value);
    }

    private static void writeNamedString(DataOutputStream out, String name, String value) throws IOException {
        out.writeByte(TAG_STRING);
        writeString(out, name);
        writeString(out, value);
    }

    private static void writeNamedIntArray(DataOutputStream out, String name, int[] values) throws IOException {
        out.writeByte(TAG_INT_ARRAY);
        writeString(out, name);
        out.writeInt(values.length);
        for (int value : values) {
            out.writeInt(value);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static Function<Material, org.bukkit.block.data.BlockData> blockDataFactory() {
        return material -> (org.bukkit.block.data.BlockData) Proxy.newProxyInstance(
                StructureTemplateLoaderTest.class.getClassLoader(),
                new Class[]{org.bukkit.block.data.BlockData.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMaterial" -> material;
                    case "clone" -> proxy;
                    case "getAsString" -> material.getKey().toString();
                    case "matches" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static Map<Vector, Material> toPlacementMap(StructureTemplate template) {
        return template.getBlockPlacements().stream().collect(Collectors.toMap(
                placement -> placement.relativeOffset().clone(),
                placement -> placement.blockData().getMaterial()
        ));
    }
}
