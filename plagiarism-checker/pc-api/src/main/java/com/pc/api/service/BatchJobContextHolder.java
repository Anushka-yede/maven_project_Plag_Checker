package com.pc.api.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory holder for file bytes between the REST layer and Batch layer.
 * In production, replace with a Redis cache or DB blob store.
 */
public final class BatchJobContextHolder {

    private static final Map<String, byte[]> STORE = new ConcurrentHashMap<>();

    private BatchJobContextHolder() {}

    public static void put(String key, byte[] data) { STORE.put(key, data); }
    public static byte[] get(String key)             { return STORE.get(key); }
    public static void remove(String key)            { STORE.remove(key); }
}
