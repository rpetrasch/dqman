package org.dqman;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StreamsExamples {

    public static void main(String[] args) {

        // Partitioning a stream
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        Map<Boolean, List<Integer>> evenOddMap = numbers.stream()
                .collect(Collectors.partitioningBy(n -> n % 2 == 0));
        System.out.println(evenOddMap); // Output: {false=[1, 3, 5], true=[2, 4, 6]}
    }
}
