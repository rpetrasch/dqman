package org.dqman.model;

import java.util.List;
import java.util.Map;

public record StepData(Map<String, List<List<String>>> data) {

    public StepData(String key, String value) {
        this(Map.of(key, List.of(List.of(value))));
    }

    public StepData(String url, List<List<String>> data) {
        this(Map.of(url, data));
    }

}
