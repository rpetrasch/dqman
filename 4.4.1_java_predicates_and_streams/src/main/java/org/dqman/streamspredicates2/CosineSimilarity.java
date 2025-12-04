package org.dqman.streamspredicates2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class the cosine similarity between two strings.
 */
public class CosineSimilarity {

    /**
     * Method for cosine similarity calculation
     * @param text1 first string
     * @param text2 second string
     * @return cosine similarity
     */
    public static double calculate(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;  // ToDo Is that logic correct? Hint: What is a similarity of zero?
        }
        // Vectorize the texts
        Map<Character, Integer> vec1 = toFrequencyVector(text1.toLowerCase());
        Map<Character, Integer> vec2 = toFrequencyVector(text2.toLowerCase());
        // Get the intersection of the two vectors
        Set<Character> commonChars = new HashSet<>(vec1.keySet()); // new HashSet containing all the keys (characters) from vec1
        commonChars.retainAll(vec2.keySet()); // keep only the elements that are also present in vec2.keySet(), effectively removing any elements that aren't in vec2
        // Calculate the dot product
        double dotProduct = 0.0;
        for (Character ch : commonChars) {
            dotProduct += vec1.get(ch) * vec2.get(ch);
        }
        // Calculate the magnitudes of the two vectors
        double magnitude1 = calculateMagnitude(vec1);
        double magnitude2 = calculateMagnitude(vec2);
        // Return the cosine similarity
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {  // Check for division by zero
            return 0.0;
        }
        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Creates a map of characters to their frequencies.
     */
    private static Map<Character, Integer> toFrequencyVector(String text) {
        Map<Character, Integer> vector = new HashMap<>();
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                vector.put(c, vector.getOrDefault(c, 0) + 1);
            }
        }
        return vector;
    }

    /**
     * Calculates the magnitude (Euclidean norm) of a vector.
     */
    private static double calculateMagnitude(Map<Character, Integer> vector) {
        double sumOfSquares = 0.0;
        for (int count : vector.values()) {
            sumOfSquares += count * count;
        }
        return Math.sqrt(sumOfSquares);
    }
}

