package org.dqman.dqbook;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

/**
 * Time complexity analysis for consistency check
 */
public class TimeComplexity {
    private final static Logger LOGGER = LoggerFactory.getLogger(TimeComplexity.class);

    public static void main(String[] args) throws InterruptedException {

        LOGGER.info("Time complexity analysis for consistency check");
        LOGGER.info("a) naive implementation (quadratic time complexity)");
//        naiveImplementation();
        LOGGER.info("b) optimized implementation (linear time complexity)");
        optimizedImplementation();
    }

    /**
     * Naive implementation: quadratic time complexity
     * @throws InterruptedException
     */
    public static void naiveImplementation() throws InterruptedException {
        long startTime, endTime, duration;
        List<Pair<Integer, Long>> log = new ArrayList<>();

        for (int n = 10; n <= 300; n += 10) {
            startTime = System.currentTimeMillis();
            int numberOfOrders = n / 2;
            int numberOfCustomers = n / 2;
            for (int o = 0; o < numberOfOrders; o++) { // loop: for all orders
                for (int c = 0; c < numberOfCustomers; c++) { // loop: for all customers
                    sleep(1); // simulate some processing
                }
            }
            endTime = System.currentTimeMillis();
            duration = endTime - startTime;
            LOGGER.info("n: " + n + ", time: " + duration + " ms");
            log.add(Pair.of(n, duration));
        }
        printLogAsDesmosPoints(log);
    }


    /**
     * Optimized implementation: linear time complexity
     * @throws InterruptedException
     */
    public static void optimizedImplementation() throws InterruptedException {
        long startTime, endTime, duration;
        List<Pair<Integer, Long>> log = new ArrayList<>();

        for (int n = 10; n <= 300; n += 10) {
            startTime = System.currentTimeMillis();
            int numberOfOrders = n / 2;
            int numberOfCustomers = n / 2;
            Map<Long, Customer> customerMap = new HashMap<>();
            for (int c = 0; c < numberOfCustomers; c++) {
                customerMap.put((long) c, new Customer((long) c, "Customer" + c));
            }
            for (int o = 0; o < numberOfOrders; o++) { // loop: for all orders
                sleep(1); // simulate some processing
                if (!customerMap.containsKey((long) o)) {
                    LOGGER.error("Order " + o + ": no matching customer found");
                }
            }
            endTime = System.currentTimeMillis();
            duration = endTime - startTime;
            LOGGER.info("n: " + n + ", time: " + duration + " ms");
            log.add(Pair.of(n, duration));
        }
        printLogAsDesmosPoints(log);
    }


    /**
     * Print log as Desmos points (copy and paste into Desmos website to show graph)
     * @param log
     */
    private static void printLogAsDesmosPoints(List<Pair<Integer, Long>> log) {
        StringBuilder desmosPoints = new StringBuilder();
        for (Pair<Integer, Long> pair : log) {
            desmosPoints.append("(").append(pair.getKey()).append(",")
                    .append(pair.getValue()).append("),");
        }
        LOGGER.info("Desmos points: " + desmosPoints);
    }
}