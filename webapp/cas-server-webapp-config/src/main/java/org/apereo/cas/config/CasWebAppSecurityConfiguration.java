package org.apereo.cas.config;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.security.CasWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * This is {@link CasWebAppSecurityConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Configuration("casWebAppSecurityConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CasWebAppSecurityConfiguration {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Bean
    @ConditionalOnMissingBean(name = "casWebSecurityConfigurerAdapter")
    @ConditionalOnProperty(name = "cas.monitor.endpoints.enableEndpointSecurity", matchIfMissing = true)
    public WebSecurityConfigurerAdapter casWebSecurityConfigurerAdapter() {
        return new CasWebSecurityConfigurerAdapter(casProperties);
    }
}