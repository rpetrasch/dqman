package org.dqman.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.camel.builder.RouteBuilder;
import org.dqman.config.camelprocessor.FileMetadataProcessor;
import org.dqman.config.camelprocessor.FileReadDataProcessor;
import org.dqman.config.camelprocessor.FileWriteDataProcessor;
import org.dqman.config.camelprocessor.RdbmsDataProcessor;
import org.dqman.config.camelprocessor.RdbmsMetadataProcessor;
import org.springframework.stereotype.Component;

/**
 * CamelConfig is a class that is used to configure the camel routes.
 */
@Component
public class CamelConfig extends RouteBuilder {

        @Autowired
        private FileMetadataProcessor fileMetadataProcessor;

        @Autowired
        private RdbmsMetadataProcessor rdbmsMetadataProcessor;

        @Autowired
        private FileReadDataProcessor fileReadDataProcessor;

        @Autowired
        private FileWriteDataProcessor fileWriteDataProcessor;

        @Autowired
        private RdbmsDataProcessor rdbmsDataProcessor;

        @Override
        public void configure() throws Exception {
                // Timer test
                // from("timer:foo?period=5000")
                // .log("Camel is running: ${body}");

                // Fetch metadata from integration for postgres DB
                from("direct:fetchMetadataRdbms")
                                .routeId("fetch-metadata-rdbms-route")
                                .doTry()
                                .process(rdbmsMetadataProcessor)
                                .doCatch(Exception.class)
                                .log("fetchMetadata exception: ${exception}")
                                // .to("direct:dqmanError") // ToDo error handling
                                .doFinally()
                                .log("fetchMetadata finally");

                // Fetch metadata from CSV file
                from("direct:fetchMetadataFile")
                                .routeId("fetch-metadata-file-route")
                                .doTry()
                                .process(fileMetadataProcessor)
                                .doCatch(Exception.class)
                                .log("fetchMetadata exception: ${exception}")
                                // .to("direct:dqmanError") // ToDo error handling
                                .doFinally()
                                .log("fetchMetadata finally");

                // Fetch data from integration for postgres DB
                from("direct:fetchDataRdbms")
                                .routeId("fetch-data-rdbms-route")
                                .doTry()
                                .process(rdbmsDataProcessor)
                                .doCatch(Exception.class)
                                .log("fetchData exception: ${exception}")
                                // .to("direct:dqmanError") // ToDo error handling
                                .doFinally()
                                .log("fetchData finally");

                // Fetch data from CSV file
                from("direct:fetchDataFile")
                                .routeId("fetch-data-file-route")
                                .doTry()
                                .process(fileReadDataProcessor)
                                .doCatch(Exception.class)
                                .log("fetchData exception: ${exception}")
                                // .to("direct:dqmanError"). // ToDo error handling
                                .doFinally()
                                .log("fetchData finally");
                // Fetch data from CSV file

                from("direct:writeDataFile")
                                .routeId("write-data-file-route")
                                .doTry()
                                .process(fileWriteDataProcessor)
                                .doCatch(Exception.class)
                                .log("fetchData exception: ${exception}")
                                // .to("direct:dqmanError"). // ToDo error handling
                                .doFinally()
                                .log("fetchData finally");
        }
}
