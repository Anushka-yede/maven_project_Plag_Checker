package com.pc.engine.similarity;

import com.pc.core.util.TokenUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 64-bit SimHash fingerprinter with Hamming distance comparator.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Extract word 3-grams from normalized text</li>
 *   <li>Hash each shingle with MD5, take first 64 bits</li>
 *   <li>Weight each bit: +1 if set, -1 if not</li>
 *   <li>Sum weights per bit position across all shingles</li>
 *   <li>Final fingerprint: bit[i] = sign(sum[i])</li>
 * </ol>
 *
 * <p>Similarity: 1.0 - (Hamming distance / 64)
 */
public class SimHashFingerprinter {

    private static final int BITS = 64;

    /**
     * Computes the 64-bit SimHash fingerprint for the given normalized text.
     */
    public long fingerprint(String normalizedText) {
        List<String> shingles = TokenUtils.wordNgrams(normalizedText, 3);
        if (shingles.isEmpty()) {
            // fallback: use unigrams
            shingles = List.of(normalizedText.split("\\s+"));
        }

        int[] weights = new int[BITS];

        for (String shingle : shingles) {
            long hash = hash64(shingle);
            for (int i = 0; i < BITS; i++) {
                if (((hash >> i) & 1L) == 1L) {
                    weights[i]++;
                } else {
                    weights[i]--;
                }
            }
        }

        long fingerprint = 0L;
        for (int i = 0; i < BITS; i++) {
            if (weights[i] > 0) {
                fingerprint |= (1L << i);
            }
        }
        return fingerprint;
    }

    /**
     * Computes Hamming distance between two SimHash fingerprints.
     *
     * @return number of differing bits (0–64)
     */
    public int hammingDistance(long fpA, long fpB) {
        return Long.bitCount(fpA ^ fpB);
    }

    /**
     * Converts Hamming distance to a similarity score in [0.0, 1.0].
     * Distance 0 → 1.0 (identical), Distance 64 → 0.0 (completely different).
     */
    public double similarity(long fpA, long fpB) {
        return 1.0 - ((double) hammingDistance(fpA, fpB) / BITS);
    }

    /**
     * Convenience: compute similarity between two texts directly.
     */
    public double similarity(String normalizedA, String normalizedB) {
        long fpA = fingerprint(normalizedA);
        long fpB = fingerprint(normalizedB);
        return similarity(fpA, fpB);
    }

    /**
     * MD5-based 64-bit hash of a string (takes first 8 bytes of MD5 digest).
     */
    private long hash64(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            long result = 0L;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (digest[i] & 0xFF);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
