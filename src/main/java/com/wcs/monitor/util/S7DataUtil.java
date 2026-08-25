package com.wcs.monitor.util;

import com.wcs.monitor.enums.S7DataType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class S7DataUtil {

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", data[i]));
        }
        return sb.toString();
    }

    public static List<String> decode(byte[] data, S7DataType type) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        int count = data.length / type.getLength();
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            switch (type) {
                case BYTE -> values.add(String.valueOf(data[i] & 0xFF));
                case INT -> values.add(String.valueOf(buffer.getShort()));
                case DINT -> values.add(String.valueOf(buffer.getInt()));
                case REAL -> values.add(String.valueOf(buffer.getFloat()));
            }
        }
        return values;
    }

    private S7DataUtil() {
    }
}
