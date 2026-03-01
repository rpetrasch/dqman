package org.dqman.dqbook.traceability.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class to compare two objects of the same type and return a map of differences.
 */
public class DiffUtil {

    /**
     * Compares two objects of the same type and returns a map of differences.
     * The key is the field name and the value is a DiffResult containing the old and new values.
     *
     * @param <T>    The type of objects being compared.
     * @param oldObj The original object.
     * @param newObj The modified object.
     * @return A map of differences.
     * @throws IllegalArgumentException if objects are null or of different classes.
     */
    public static <T> Map<String, DiffResult> diff(T oldObj, T newObj) {
        if (oldObj == null || newObj == null) {
            throw new IllegalArgumentException("Neither object can be null");
        }
        if (!oldObj.getClass().equals(newObj.getClass())) {
            throw new IllegalArgumentException("Objects must be of the same type");
        }

        Map<String, DiffResult> differences = new HashMap<>();
        Class<?> clazz = oldObj.getClass();

        // Iterate over all declared fields. You may want to filter out static or transient fields.
        for (Field field : clazz.getDeclaredFields()) {
            // Skip static fields if needed
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object oldValue = field.get(oldObj);
                Object newValue = field.get(newObj);
                // Compare the values. If they're not equal, record the difference.
                if (!Objects.equals(oldValue, newValue)) {
                    differences.put(field.getName(), new DiffResult(oldValue, newValue));
                }
            } catch (IllegalAccessException e) {
                // In case of an error, you can choose to handle it differently.
                throw new RuntimeException("Error accessing field " + field.getName(), e);
            }
        }
        return differences;
    }

    /**
     * A simple container to hold a difference result for a field.
     */
    public static class DiffResult {
        private final Object oldValue;
        private final Object newValue;

        public DiffResult(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        @Override
        public String toString() {
            return "DiffResult{oldValue=" + oldValue + ", newValue=" + newValue + '}';
        }
    }
}

