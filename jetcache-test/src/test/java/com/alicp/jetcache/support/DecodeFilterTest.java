package com.alicp.jetcache.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputFilter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DecodeFilterTest {

    private DecodeFilter decodeFilter = DecodeFilter.getDefault();

    @AfterEach
    public void tearDown() {
        decodeFilter.reset();
    }

    @Test
    public void testDefaultFilterAllowsJavaLang() {
        assertTrue(decodeFilter.isAllowed("java.lang.String"));
        assertTrue(decodeFilter.isAllowed("java.lang.Integer"));
        assertTrue(decodeFilter.isAllowed("java.lang.Long"));
        assertTrue(decodeFilter.isAllowed("[Ljava.lang.String;"));
    }

    @Test
    public void testDefaultFilterAllowsJavaUtil() {
        assertTrue(decodeFilter.isAllowed("java.util.HashMap"));
        assertTrue(decodeFilter.isAllowed("java.util.ArrayList"));
        assertTrue(decodeFilter.isAllowed("java.util.Date"));
        assertTrue(decodeFilter.isAllowed("java.util.UUID"));
    }

    @Test
    public void testDefaultFilterAllowsJavaTime() {
        assertTrue(decodeFilter.isAllowed("java.time.LocalDateTime"));
        assertTrue(decodeFilter.isAllowed("java.time.Instant"));
        assertTrue(decodeFilter.isAllowed("java.time.Duration"));
    }

    @Test
    public void testDefaultFilterAllowsJavaMath() {
        assertTrue(decodeFilter.isAllowed("java.math.BigDecimal"));
        assertTrue(decodeFilter.isAllowed("java.math.BigInteger"));
    }

    @Test
    public void testDefaultFilterAllowsJetCacheTypes() {
        assertTrue(decodeFilter.isAllowed("com.alicp.jetcache.CacheValueHolder"));
        assertTrue(decodeFilter.isAllowed("com.alicp.jetcache.support.CacheMessage"));
    }

    @Test
    public void testDefaultFilterBlocksRiskyJavaPackages() {
        assertFalse(decodeFilter.isAllowed("java.rmi.server.UnicastRemoteObject"));
        assertFalse(decodeFilter.isAllowed("java.beans.EventHandler"));
        assertFalse(decodeFilter.isAllowed("java.lang.reflect.Proxy"));
    }

    @Test
    public void testDefaultFilterRejectsCustomClass() {
        assertFalse(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testUserPatternAllowsCustomClass() {
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testExactMatch() {
        // Package-only matching: "org.myapp.dto" matches direct members only
        decodeFilter.addAllowPatterns("org.myapp.dto");
        assertTrue(decodeFilter.isAllowed("org.myapp.dto.UserDTO"));
        assertTrue(decodeFilter.isAllowed("org.myapp.dto.OrderDTO"));
        assertFalse(decodeFilter.isAllowed("org.myapp.dto.sub.Service"));
    }

    @Test
    public void testDisabledFilter() {
        decodeFilter.setEnabled(false);
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testSetAllowPatternsReplacesAll() {
        decodeFilter.clearAllowPatterns();
        decodeFilter.addAllowPatterns("com.test.");
        assertFalse(decodeFilter.isAllowed("java.lang.String"));
        assertFalse(decodeFilter.isAllowed("java.util.HashMap"));
        assertTrue(decodeFilter.isAllowed("com.test.Foo"));
    }

    @Test
    public void testArrayTypeMatching() {
        assertFalse(decodeFilter.isAllowed("[Lcom.example.User;"));
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("[Lcom.example.User;"));
    }

    @Test
    public void testPrimitiveArrayAllowed() {
        assertTrue(decodeFilter.isAllowed("[B"));
        assertTrue(decodeFilter.isAllowed("[I"));
        assertTrue(decodeFilter.isAllowed("[[I"));
    }

    @Test
    public void testInnerClassMatching() {
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.Outer$Inner"));
    }

    @Test
    public void testNegativeResultNotCached() {
        assertFalse(decodeFilter.isAllowed("com.example.User"));
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int iterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean hasError = new AtomicBoolean(false);
        AtomicReference<Boolean> expectedResult = new AtomicReference<>(false);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        boolean result = decodeFilter.isAllowed("com.example.User");
                        if (i == 0) {
                            expectedResult.set(result);
                        }
                        assertEquals(expectedResult.get(), result);
                    }
                } catch (Exception e) {
                    hasError.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertFalse(hasError.get());
    }

    @Test
    public void testEmptyClassName() {
        assertTrue(decodeFilter.isAllowed(""));
        assertTrue(decodeFilter.isAllowed(null));
    }

    @Test
    public void testDenyListBlocksJavaLangInvoke() {
        assertFalse(decodeFilter.isAllowed("java.lang.invoke.SerializedLambda"));
        assertFalse(decodeFilter.isAllowed("java.lang.invoke.MethodHandles"));
    }

    @Test
    public void testDenyListOverridesUserPattern() {
        decodeFilter.addAllowPatterns("java.lang.reflect.");
        assertFalse(decodeFilter.isAllowed("java.lang.reflect.Proxy"));
    }

    @Test
    public void testDenyListBlocksJndiAndRmi() {
        assertFalse(decodeFilter.isAllowed("javax.naming.InitialContext"));
        assertFalse(decodeFilter.isAllowed("java.rmi.server.UnicastRemoteObject"));
    }

    @Test
    public void testDenyListBlocksComSunDangerousClasses() {
        assertFalse(decodeFilter.isAllowed("com.sun.rowset.JdbcRowSetImpl"));
        assertFalse(decodeFilter.isAllowed("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"));
    }

    @Test
    public void testDenyListOverridesUserPatternForJndi() {
        decodeFilter.addAllowPatterns("javax.");
        assertFalse(decodeFilter.isAllowed("javax.naming.InitialContext"));
    }

    @Test
    public void testMalformedArrayDescriptor() {
        assertFalse(decodeFilter.isAllowed("[[L"));
        assertFalse(decodeFilter.isAllowed("[Qcom.example.Evil;"));
    }

    @Test
    public void testDenyListBlocksJavaLangManagement() {
        assertFalse(decodeFilter.isAllowed("java.lang.management.ManagementFactory"));
    }

    @Test
    public void testDenyListBlocksDangerousJavaLangClasses() {
        assertFalse(decodeFilter.isAllowed("java.lang.Runtime"));
        assertFalse(decodeFilter.isAllowed("java.lang.ProcessBuilder"));
        assertFalse(decodeFilter.isAllowed("java.lang.Thread"));
        assertFalse(decodeFilter.isAllowed("java.lang.ThreadGroup"));
        assertFalse(decodeFilter.isAllowed("java.lang.ClassLoader"));
        assertFalse(decodeFilter.isAllowed("java.lang.System"));
        assertFalse(decodeFilter.isAllowed("java.lang.SecurityManager"));
        assertFalse(decodeFilter.isAllowed("java.lang.StackWalker"));
    }

    @Test
    public void testJavaIoNotInDefaultFilter() {
        assertFalse(decodeFilter.isAllowed("java.io.File"));
        assertFalse(decodeFilter.isAllowed("java.io.RandomAccessFile"));
    }

    @Test
    public void testJavaNetInDefaultFilter() {
        assertTrue(decodeFilter.isAllowed("java.net.URL"));
        assertTrue(decodeFilter.isAllowed("java.net.InetAddress"));
        assertTrue(decodeFilter.isAllowed("java.net.URI"));
        assertTrue(decodeFilter.isAllowed("java.net.InetSocketAddress"));
    }

    @Test
    public void testUserCanAddJavaIo() {
        decodeFilter.addAllowPatterns("java.io.");
        assertTrue(decodeFilter.isAllowed("java.io.File"));
    }

    @Test
    public void testJavaLangStillAllowsCoreTypes() {
        assertTrue(decodeFilter.isAllowed("java.lang.String"));
        assertTrue(decodeFilter.isAllowed("java.lang.StringBuilder"));
        assertTrue(decodeFilter.isAllowed("java.lang.Exception"));
    }

    @Test
    public void testPackageOnlyMatchingBlocksSubpackages() {
        // java.lang uses package-only matching, so subpackages are blocked automatically
        assertFalse(decodeFilter.isAllowed("java.lang.reflect.Proxy"));
        assertFalse(decodeFilter.isAllowed("java.lang.invoke.SerializedLambda"));
        assertFalse(decodeFilter.isAllowed("java.lang.management.ManagementFactory"));
    }

    @Test
    public void testPackageOnlyMatchingAllowsDirectClasses() {
        // java.lang uses package-only matching, direct classes still allowed
        assertTrue(decodeFilter.isAllowed("java.lang.String"));
        assertTrue(decodeFilter.isAllowed("java.lang.Integer"));
        assertTrue(decodeFilter.isAllowed("java.lang.Exception"));
        assertTrue(decodeFilter.isAllowed("java.lang.StringBuilder"));
    }

    @Test
    public void testPrefixMatchingAllowsSubpackages() {
        // java.util. uses prefix matching, subpackages are included
        assertTrue(decodeFilter.isAllowed("java.util.concurrent.ConcurrentHashMap"));
        assertTrue(decodeFilter.isAllowed("java.util.concurrent.atomic.AtomicInteger"));
    }

    @Test
    public void testUserPackageOnlyPattern() {
        decodeFilter.addAllowPatterns("com.example");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
        assertFalse(decodeFilter.isAllowed("com.example.sub.Service"));
    }

    @Test
    public void testUserPrefixPattern() {
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
        assertTrue(decodeFilter.isAllowed("com.example.sub.Service"));
    }

    @Test
    public void testExactClassNameMatch() {
        decodeFilter.addAllowPatterns("com.example.dto.UserDTO");
        assertTrue(decodeFilter.isAllowed("com.example.dto.UserDTO"));
        assertFalse(decodeFilter.isAllowed("com.example.dto.OrderDTO"));
        assertFalse(decodeFilter.isAllowed("com.example.dto.UserDTOService"));
        assertFalse(decodeFilter.isAllowed("com.example.dto.sub.UserDTO"));
    }

    @Test
    public void testExactMatchDoesNotMatchInnerClassOrSimilarName() {
        decodeFilter.addAllowPatterns("com.example.MyClass");
        assertFalse(decodeFilter.isAllowed("com.example.MyClassExtra"));
        assertFalse(decodeFilter.isAllowed("com.example.MyClass$Inner"));
    }

    @Test
    public void testCustomConstructor() {
        DecodeFilter custom = new DecodeFilter(
                Set.of("com.foo."),
                Set.of("com.foo.evil.")
        );
        assertTrue(custom.isAllowed("com.foo.Bar"));
        assertFalse(custom.isAllowed("com.foo.evil.Attack"));
        assertFalse(custom.isAllowed("java.lang.String"));
        assertFalse(custom.isEnabled() == false);
    }

    @Test
    public void testRemoveAllowPatterns() {
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
        decodeFilter.removeAllowPatterns("com.example.");
        assertFalse(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testRemoveAllowPatternsNullSkipped() {
        decodeFilter.addAllowPatterns("com.example.");
        decodeFilter.removeAllowPatterns((String) null);
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testRemoveAllowPatternsNonExistentNoCacheClear() {
        decodeFilter.isAllowed("java.lang.String");
        decodeFilter.removeAllowPatterns("nonexistent.");
        assertTrue(decodeFilter.isAllowed("java.lang.String"));
    }

    @Test
    public void testAddDenyPatterns() {
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
        decodeFilter.addDenyPatterns("com.example.User");
        assertFalse(decodeFilter.isAllowed("com.example.User"));
        assertTrue(decodeFilter.isAllowed("com.example.Other"));
    }

    @Test
    public void testAddDenyPatternsPrefix() {
        decodeFilter.addAllowPatterns("com.example.");
        decodeFilter.addDenyPatterns("com.example.evil.");
        assertFalse(decodeFilter.isAllowed("com.example.evil.Attack"));
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testAddDenyPatternsNullAndEmptySkipped() {
        decodeFilter.addAllowPatterns("com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
        decodeFilter.addDenyPatterns(null, "");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testRemoveDenyPatterns() {
        assertFalse(decodeFilter.isAllowed("java.lang.Runtime"));
        decodeFilter.removeDenyPatterns("java.lang.Runtime");
        decodeFilter.addAllowPatterns("java.lang.");
        assertTrue(decodeFilter.isAllowed("java.lang.Runtime"));
    }

    @Test
    public void testRemoveDenyPatternsPrefix() {
        assertFalse(decodeFilter.isAllowed("java.rmi.server.UnicastRemoteObject"));
        decodeFilter.removeDenyPatterns("java.rmi.");
        decodeFilter.addAllowPatterns("java.rmi.");
        assertTrue(decodeFilter.isAllowed("java.rmi.server.UnicastRemoteObject"));
    }

    @Test
    public void testRemoveDenyPatternsNullSkipped() {
        decodeFilter.removeDenyPatterns((String) null);
        assertFalse(decodeFilter.isAllowed("java.lang.Runtime"));
    }

    @Test
    public void testRemoveDenyPatternsNonExistentNoCacheClear() {
        decodeFilter.isAllowed("java.lang.Runtime");
        decodeFilter.removeDenyPatterns("nonexistent.");
        assertFalse(decodeFilter.isAllowed("java.lang.Runtime"));
    }

    @Test
    public void testClearDenyPatterns() {
        assertFalse(decodeFilter.isAllowed("java.lang.Runtime"));
        decodeFilter.clearDenyPatterns();
        decodeFilter.addAllowPatterns("java.lang.");
        assertTrue(decodeFilter.isAllowed("java.lang.Runtime"));
    }

    @Test
    public void testClearCache() {
        assertTrue(decodeFilter.isAllowed("java.lang.String"));
        decodeFilter.clearAllowPatterns();
        decodeFilter.addAllowPatterns("com.test.");
        decodeFilter.clearCache();
        assertFalse(decodeFilter.isAllowed("java.lang.String"));
    }

    @Test
    public void testSetEnabledClearsCache() {
        assertTrue(decodeFilter.isAllowed("java.lang.String"));
        decodeFilter.clearAllowPatterns();
        decodeFilter.addAllowPatterns("com.test.");
        decodeFilter.setEnabled(true);
        assertFalse(decodeFilter.isAllowed("java.lang.String"));
    }

    @Test
    public void testAddAllowPatternsNullAndEmptySkipped() {
        decodeFilter.addAllowPatterns(null, "", "com.example.");
        assertTrue(decodeFilter.isAllowed("com.example.User"));
    }

    @Test
    public void testJavaFilterAllowedClass() {
        ObjectInputFilter.FilterInfo info = mockFilterInfo(java.lang.String.class);
        assertEquals(ObjectInputFilter.Status.ALLOWED, DecodeFilter.javaFilter(info));
    }

    @Test
    public void testJavaFilterBlockedClass() {
        ObjectInputFilter.FilterInfo info = mockFilterInfo(java.lang.Runtime.class);
        assertEquals(ObjectInputFilter.Status.REJECTED, DecodeFilter.javaFilter(info));
    }

    @Test
    public void testJavaFilterNullSerialClass() {
        ObjectInputFilter.FilterInfo info = mockFilterInfo(null);
        assertEquals(ObjectInputFilter.Status.UNDECIDED, DecodeFilter.javaFilter(info));
    }

    @Test
    public void testJavaFilterDisabled() {
        decodeFilter.setEnabled(false);
        ObjectInputFilter.FilterInfo info = mockFilterInfo(java.lang.Runtime.class);
        assertEquals(ObjectInputFilter.Status.UNDECIDED, DecodeFilter.javaFilter(info));
    }

    private ObjectInputFilter.FilterInfo mockFilterInfo(Class<?> clazz) {
        return new ObjectInputFilter.FilterInfo() {
            @Override
            public Class<?> serialClass() { return clazz; }
            @Override
            public long arrayLength() { return 0; }
            @Override
            public long depth() { return 0; }
            @Override
            public long references() { return 0; }
            @Override
            public long streamBytes() { return 0; }
        };
    }
}
