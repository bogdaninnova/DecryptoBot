package com.bope;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("com.bope")
@PropertySource("classpath:constants.properties")
public class SpringConfig {
}
