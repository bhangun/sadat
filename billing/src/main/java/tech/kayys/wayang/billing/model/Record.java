package tech.kayys.wayang.billing.model;

public record Record<K, V>(K key, V value) {
    public static <K, V> Record<K, V> of(K key, V value) {
        return new Record<>(key, value);
    }
}