package com.alicp.jetcache.support;

abstract class AbstractKryoClassResolver {

    private static final String[] PRIMITIVE_TYPE_NAMES = {
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"
    };

    protected static void checkAllowed(Class<?> type) {
        if (type != null && !type.isPrimitive()) {
            checkAllowed(type.getName());
        }
    }

    protected static void checkAllowed(String className) {
        if (isPrimitiveTypeName(className)) {
            return;
        }
        if (DecodeFilter.getDefault().isEnabled() && !DecodeFilter.getDefault().isAllowed(className)) {
            DecodeFilter.logBlocked(className);
            throw new DecodeFilterException(className);
        }
    }

    protected static Class<?> loadClass(String className, ClassLoader primary, ClassLoader fallback) throws ClassNotFoundException {
        try {
            return Class.forName(className, false, primary);
        } catch (ClassNotFoundException e) {
            return Class.forName(className, false, fallback);
        }
    }

    private static boolean isPrimitiveTypeName(String className) {
        if (className == null) {
            return false;
        }
        for (String primitiveTypeName : PRIMITIVE_TYPE_NAMES) {
            if (primitiveTypeName.equals(className)) {
                return true;
            }
        }
        return false;
    }
}
