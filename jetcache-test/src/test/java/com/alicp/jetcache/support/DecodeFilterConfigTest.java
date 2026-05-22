package com.alicp.jetcache.support;

import com.alicp.jetcache.anno.support.ConfigProvider;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for decode filter configuration via GlobalCacheConfig and ConfigProvider.
 */
public class DecodeFilterConfigTest {

    private DecodeFilter decodeFilter = DecodeFilter.getDefault();

    @AfterEach
    public void tearDown() {
        decodeFilter.reset();
    }

    @Test
    public void testProgrammaticApi() {
        decodeFilter.setEnabled(true);
        decodeFilter.addAllowPatterns("com.test.");
        assertTrue(decodeFilter.isAllowed("com.test.MyClass"));
        assertFalse(decodeFilter.isAllowed("com.other.MyClass"));
    }

    @Test
    public void testSetPatternsReplacesUserPatterns() {
        decodeFilter.clearAllowPatterns();
        decodeFilter.addAllowPatterns("com.a.");
        decodeFilter.addAllowPatterns("com.b.");
        assertTrue(decodeFilter.isAllowed("com.a.X"));
        assertTrue(decodeFilter.isAllowed("com.b.X"));
        assertFalse(decodeFilter.isAllowed("com.c.X"));
    }

    @Test
    public void testDisableFilter() {
        decodeFilter.setEnabled(false);
        assertTrue(decodeFilter.isAllowed("any.random.Class"));
    }

    @Test
    public void testGlobalCacheConfigDefaults() {
        GlobalCacheConfig config = new GlobalCacheConfig();
        assertTrue(config.isDecodeFilterEnabled(), "decodeFilterEnabled should default to true");
        assertNull(config.getDecodeFilterPatterns(), "decodeFilterPatterns should default to null");
    }

    @Test
    public void testGlobalCacheConfigSetters() {
        GlobalCacheConfig config = new GlobalCacheConfig();
        config.setDecodeFilterEnabled(false);
        assertFalse(config.isDecodeFilterEnabled());

        config.setDecodeFilterPatterns(List.of("com.example."));
        assertEquals(List.of("com.example."), config.getDecodeFilterPatterns());
    }

    @Test
    public void testConfigProviderInitDecodeFilter() {
        GlobalCacheConfig config = new GlobalCacheConfig();
        config.setLocalCacheBuilders(new HashMap<>());
        config.setRemoteCacheBuilders(new HashMap<>());
        config.setDecodeFilterEnabled(true);
        config.setDecodeFilterPatterns(List.of("com.cfgprovider."));

        ConfigProvider provider = new ConfigProvider();
        provider.setGlobalCacheConfig(config);
        provider.init();

        assertTrue(decodeFilter.isAllowed("com.cfgprovider.MyClass"));
        assertFalse(decodeFilter.isAllowed("com.other.MyClass"));

        provider.shutdown();
    }

    @Test
    public void testConfigProviderInitDecodeFilterDisabled() {
        GlobalCacheConfig config = new GlobalCacheConfig();
        config.setLocalCacheBuilders(new HashMap<>());
        config.setRemoteCacheBuilders(new HashMap<>());
        config.setDecodeFilterEnabled(false);

        ConfigProvider provider = new ConfigProvider();
        provider.setGlobalCacheConfig(config);
        provider.init();

        assertTrue(decodeFilter.isAllowed("com.anything.MyClass"));

        provider.shutdown();
    }
}
