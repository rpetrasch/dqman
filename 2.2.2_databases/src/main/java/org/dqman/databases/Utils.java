package org.dqman.databases;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for performance test
 */
public class Utils {

    /**
     * Transpose matrix
     * @param matrix
     * @return transposed matrix
     */
    public static List<List<Long>> transpose(List<List<Long>> matrix) {
        // If matrix is empty or any row is empty, return it as-is
        if (matrix == null || matrix.isEmpty() || matrix.get(0).isEmpty()) {
            return matrix;
        }

        int rowCount = matrix.size();
        int colCount = matrix.get(0).size();

        // Create a new list of size colCount
        List<List<Long>> result = new ArrayList<>(colCount);

        // For each column in the original, create a new row in the transposed
        for (int col = 0; col < colCount; col++) {
            List<Long> newRow = new ArrayList<>(rowCount);
            for (int row = 0; row < rowCount; row++) {
                newRow.add(matrix.get(row).get(col));
            }
            result.add(newRow);
        }
        return result;
    }
}
