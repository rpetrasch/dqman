package org.dqman.redundancy;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;


/**
 * Configuration for logger
 */
public class LogConfig {
    private static boolean configured = false;

    public static synchronized void configure() {
        if (configured) return;
        try {
            ConsoleHandler handler = new ConsoleHandler() {
                @Override
                protected synchronized void setOutputStream(OutputStream out) {
                    super.setOutputStream(System.out);
                }
            };
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.ALL);

            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler h : handlers) {
                rootLogger.removeHandler(h);
            }
            rootLogger.addHandler(handler);
            rootLogger.setLevel(Level.ALL);
            rootLogger.setUseParentHandlers(false);

            configured = true;
        } catch (Exception e) {
            System.err.println("Failed to configure logging: " + e.getMessage());
        }
    }

    /**
     * Custom formatter class for single-line logs with colors
     */
    public static class SimpleFormatter extends Formatter {
        private static final DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // ANSI color codes
        private static final String RESET = "\u001B[0m";
        private static final String YELLOW = "\u001B[33m";
        private static final String RED = "\u001B[31m";

        @Override
        public String format(LogRecord record) {
            String color = "";
            String levelName = record.getLevel().getName();

            // Apply colors based on level
            if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                color = RED;  // ERROR/SEVERE
            } else if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                color = YELLOW;  // WARNING
            }

            return String.format("%s%s [%s] %s%s%n",
                    color,
                    LocalDateTime.now().format(dateFormatter),
                    levelName,
                    record.getMessage(),
                    RESET
            );
        }
    }
}