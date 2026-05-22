package com.alicp.jetcache.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDecoderFilterTest {

    private DecodeFilter decodeFilter = DecodeFilter.getDefault();

    @AfterEach
    public void tearDown() {
        decodeFilter.reset();
    }

    static class TestJsonDecoder extends AbstractJsonDecoder {
        public TestJsonDecoder() {
            super(false);
        }

        @Override
        protected Object parseObject(byte[] buffer, int index, int len, Class clazz) {
            if (clazz == String.class) {
                return "";
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        }
    }

    private byte[] buildDecodeBuffer(String className) {
        byte[] classNameBytes = className.getBytes(StandardCharsets.UTF_8);
        int totalLen = 2 + 2 + classNameBytes.length + 4;
        byte[] buf = new byte[totalLen];
        int index = 0;

        buf[index++] = 0;
        buf[index++] = 1;

        buf[index++] = (byte) ((classNameBytes.length >> 8) & 0xFF);
        buf[index++] = (byte) (classNameBytes.length & 0xFF);

        System.arraycopy(classNameBytes, 0, buf, index, classNameBytes.length);
        index += classNameBytes.length;

        buf[index++] = 0;
        buf[index++] = 0;
        buf[index++] = 0;
        buf[index++] = 0;

        return buf;
    }

    private byte[] buildNullObjectBuffer() {
        return new byte[]{(byte) 0xFF, (byte) 0xFF};
    }

    @Test
    public void testDefaultFilterAllowsJavaLangString() {
        TestJsonDecoder decoder = new TestJsonDecoder();
        byte[] buf = buildDecodeBuffer("java.lang.String");
        Object result = decoder.apply(buf);
        assertNotNull(result);
    }

    @Test
    public void testDefaultFilterRejectsCustomClass() {
        TestJsonDecoder decoder = new TestJsonDecoder();
        byte[] buf = buildDecodeBuffer("com.example.User");
        CacheEncodeException ex = assertThrows(CacheEncodeException.class, () -> {
            decoder.apply(buf);
        });
        assertTrue(ex.getCause() instanceof DecodeFilterException);
        assertTrue(ex.getCause().getMessage().contains("com.example.User"));
    }

    @Test
    public void testFilterDisabledAllowsAnyClass() {
        decodeFilter.setEnabled(false);
        TestJsonDecoder decoder = new TestJsonDecoder();
        byte[] buf = buildDecodeBuffer("org.example.KryoFilterTestUser");
        Object result = decoder.apply(buf);
        assertNotNull(result);
    }

    @Test
    public void testUserPatternAllowsCustomClass() {
        byte[] buf = buildDecodeBuffer("org.example.KryoFilterTestUser");
        TestJsonDecoder decoder = new TestJsonDecoder();
        CacheEncodeException ex = assertThrows(CacheEncodeException.class, () -> decoder.apply(buf));
        assertTrue(ex.getCause() instanceof DecodeFilterException);

        decodeFilter.addAllowPatterns("org.example.");
        Object result = decoder.apply(buf);
        assertNotNull(result);
    }

    @Test
    public void testNullObjectReturnsNullWithoutFilterCheck() {
        TestJsonDecoder decoder = new TestJsonDecoder();
        byte[] buf = buildNullObjectBuffer();
        Object result = decoder.apply(buf);
        assertNull(result);
    }

    @Test
    public void testDefaultFilterBlocksRiskyJavaPackages() {
        TestJsonDecoder decoder = new TestJsonDecoder();
        byte[] buf = buildDecodeBuffer("java.rmi.server.UnicastRef");
        CacheEncodeException ex = assertThrows(CacheEncodeException.class, () -> decoder.apply(buf));
        assertTrue(ex.getCause() instanceof DecodeFilterException);
    }
}
