package com.springshop.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 컬렉션 처리 유틸리티.
 */
public final class CollectionUtils {

    private CollectionUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> boolean isEmpty(Collection<T> c) {
        return c == null || c.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> c) {
        return !isEmpty(c);
    }

    public static <K, V> boolean isEmpty(Map<K, V> m) {
        return m == null || m.isEmpty();
    }

    /**
     * 리스트를 size 단위로 분할.
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (isEmpty(list) || size <= 0) return List.of();
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return result;
    }

    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyMapper) {
        if (isEmpty(collection)) return Map.of();
        Map<K, T> result = new HashMap<>();
        for (T item : collection) {
            K key = keyMapper.apply(item);
            if (key != null) result.put(key, item);
        }
        return result;
    }

    public static <T, K, V> Map<K, V> toMap(Collection<T> collection,
                                            Function<T, K> keyMapper,
                                            Function<T, V> valueMapper) {
        if (isEmpty(collection)) return Map.of();
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(keyMapper, valueMapper, (a, b) -> a));
    }

    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection)) return Map.of();
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(classifier));
    }

    public static <T> Set<T> intersection(Collection<T> a, Collection<T> b) {
        if (isEmpty(a) || isEmpty(b)) return Set.of();
        Set<T> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    public static <T> Set<T> difference(Collection<T> a, Collection<T> b) {
        if (isEmpty(a)) return Set.of();
        if (isEmpty(b)) return new HashSet<>(a);
        Set<T> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }

    public static <T> Set<T> union(Collection<T> a, Collection<T> b) {
        Set<T> result = new LinkedHashSet<>();
        if (a != null) result.addAll(a);
        if (b != null) result.addAll(b);
        return result;
    }

    public static <T> T safeGet(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public static <T> T safeGet(List<T> list, int index, T defaultValue) {
        T value = safeGet(list, index);
        return value != null ? value : defaultValue;
    }

    public static <T, R> List<R> mapToList(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) return List.of();
        return collection.stream().map(mapper).toList();
    }

    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) return List.of();
        return collection.stream().filter(predicate).toList();
    }

    public static <T> List<T> filterNull(Collection<T> collection) {
        if (isEmpty(collection)) return List.of();
        return collection.stream().filter(Objects::nonNull).toList();
    }

    public static <T, K> List<T> distinctBy(Collection<T> collection, Function<T, K> keyMapper) {
        if (isEmpty(collection)) return List.of();
        Set<K> seen = new HashSet<>();
        List<T> result = new ArrayList<>();
        for (T item : collection) {
            K key = keyMapper.apply(item);
            if (seen.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    public static <T> List<T> reverse(List<T> list) {
        if (isEmpty(list)) return List.of();
        List<T> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    public static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    public static <T> Set<T> safeSet(Set<T> set) {
        return set == null ? Set.of() : set;
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T>... lists) {
        if (lists == null) return List.of();
        List<T> result = new ArrayList<>();
        for (List<T> list : lists) {
            if (list != null) result.addAll(list);
        }
        return result;
    }
}
