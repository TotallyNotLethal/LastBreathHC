package com.lastbreath.hc.lastBreathHC.structures;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

final class NbtBinaryReader {
    private static final byte TAG_END = 0;
    private static final byte TAG_BYTE = 1;
    private static final byte TAG_SHORT = 2;
    private static final byte TAG_INT = 3;
    private static final byte TAG_LONG = 4;
    private static final byte TAG_FLOAT = 5;
    private static final byte TAG_DOUBLE = 6;
    private static final byte TAG_BYTE_ARRAY = 7;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;
    private static final byte TAG_INT_ARRAY = 11;
    private static final byte TAG_LONG_ARRAY = 12;

    private NbtBinaryReader() {
    }

    static NbtCompound readRootCompound(InputStream in, String context) throws IOException {
        try (BufferedInputStream buffered = new BufferedInputStream(in)) {
            buffered.mark(2);
            int b0 = buffered.read();
            int b1 = buffered.read();
            buffered.reset();

            InputStream decoded = (b0 == 0x1f && b1 == 0x8b)
                    ? new GZIPInputStream(buffered)
                    : buffered;
            DataInputStream data = new DataInputStream(decoded);

            byte rootType = data.readByte();
            if (rootType != TAG_COMPOUND) {
                throw new IOException(context + ": root tag must be COMPOUND but was " + rootType);
            }
            readString(data); // root name, ignored
            return readCompound(data, context + "/root");
        } catch (EOFException e) {
            throw new IOException(context + ": unexpected end of file while reading NBT", e);
        }
    }

    private static NbtTag readTagPayload(DataInputStream in, byte tagType, String path) throws IOException {
        return switch (tagType) {
            case TAG_BYTE -> new NbtByte(in.readByte());
            case TAG_SHORT -> new NbtShort(in.readShort());
            case TAG_INT -> new NbtInt(in.readInt());
            case TAG_LONG -> new NbtLong(in.readLong());
            case TAG_FLOAT -> new NbtFloat(in.readFloat());
            case TAG_DOUBLE -> new NbtDouble(in.readDouble());
            case TAG_BYTE_ARRAY -> {
                int len = in.readInt();
                if (len < 0) {
                    throw new IOException(path + ": negative byte-array length " + len);
                }
                byte[] value = in.readNBytes(len);
                if (value.length != len) {
                    throw new EOFException(path + ": truncated byte-array");
                }
                yield new NbtByteArray(value);
            }
            case TAG_STRING -> new NbtString(readString(in));
            case TAG_LIST -> readList(in, path);
            case TAG_COMPOUND -> readCompound(in, path);
            case TAG_INT_ARRAY -> {
                int len = in.readInt();
                if (len < 0) {
                    throw new IOException(path + ": negative int-array length " + len);
                }
                int[] values = new int[len];
                for (int i = 0; i < len; i++) {
                    values[i] = in.readInt();
                }
                yield new NbtIntArray(values);
            }
            case TAG_LONG_ARRAY -> {
                int len = in.readInt();
                if (len < 0) {
                    throw new IOException(path + ": negative long-array length " + len);
                }
                long[] values = new long[len];
                for (int i = 0; i < len; i++) {
                    values[i] = in.readLong();
                }
                yield new NbtLongArray(values);
            }
            default -> throw new IOException(path + ": unsupported tag type " + tagType);
        };
    }

    private static NbtList readList(DataInputStream in, String path) throws IOException {
        byte elementType = in.readByte();
        int size = in.readInt();
        if (size < 0) {
            throw new IOException(path + ": negative list size " + size);
        }
        List<NbtTag> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(readTagPayload(in, elementType, path + "[" + i + "]"));
        }
        return new NbtList(elementType, values);
    }

    private static NbtCompound readCompound(DataInputStream in, String path) throws IOException {
        Map<String, NbtTag> values = new LinkedHashMap<>();
        while (true) {
            byte type = in.readByte();
            if (type == TAG_END) {
                return new NbtCompound(values);
            }
            String name = readString(in);
            String childPath = path + "/" + name;
            values.put(name, readTagPayload(in, type, childPath));
        }
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] bytes = in.readNBytes(len);
        if (bytes.length != len) {
            throw new EOFException("truncated string");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    sealed interface NbtTag permits NbtByte, NbtShort, NbtInt, NbtLong, NbtFloat, NbtDouble, NbtByteArray, NbtString, NbtList, NbtCompound, NbtIntArray, NbtLongArray {
    }

    record NbtByte(byte value) implements NbtTag {
    }

    record NbtShort(short value) implements NbtTag {
    }

    record NbtInt(int value) implements NbtTag {
    }

    record NbtLong(long value) implements NbtTag {
    }

    record NbtFloat(float value) implements NbtTag {
    }

    record NbtDouble(double value) implements NbtTag {
    }

    record NbtByteArray(byte[] value) implements NbtTag {
    }

    record NbtString(String value) implements NbtTag {
    }

    record NbtList(byte elementType, List<NbtTag> value) implements NbtTag {
        NbtTag get(int idx) {
            return value.get(idx);
        }

        int size() {
            return value.size();
        }
    }

    record NbtCompound(Map<String, NbtTag> value) implements NbtTag {
        NbtTag require(String key, String context) throws IOException {
            NbtTag tag = value.get(key);
            if (tag == null) {
                throw new IOException(context + ": missing required key '" + key + "'");
            }
            return tag;
        }
    }

    record NbtIntArray(int[] value) implements NbtTag {
    }

    record NbtLongArray(long[] value) implements NbtTag {
    }
}
