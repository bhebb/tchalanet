package com.tchalanet.server.features.news;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NewsConfigProperties.class)
public class NewsConfig {}
