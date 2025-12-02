package org.dqman.config;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class CamelConfig extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // ToDo This is just a test. We need to add real configurable camel routes
        // from("timer:foo?period=5000")
        // .log("Camel is running: ${body}");
    }
}
