package com.tchalanet.server.catalog.pricing.internal.rest;

import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;

@Configuration
class PricingOddsRepositoryRestConfigurer implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, org.springframework.web.servlet.config.annotation.CorsRegistry cors) {
        configureExposure(config);
    }

    private void configureExposure(RepositoryRestConfiguration config) {
        config.getExposureConfiguration()
            .forDomainType(PricingOddsEntity.class)
            .withItemExposure((metadata, httpMethods) ->
                httpMethods.disable(HttpMethod.DELETE))
            .withCollectionExposure((metadata, httpMethods) ->
                httpMethods.disable(HttpMethod.DELETE));
    }
}
