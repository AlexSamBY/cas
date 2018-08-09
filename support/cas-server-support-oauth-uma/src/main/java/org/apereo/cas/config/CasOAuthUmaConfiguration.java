package org.apereo.cas.config;

import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.UniqueTicketIdGenerator;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.support.HardTimeoutExpirationPolicy;
import org.apereo.cas.uma.discovery.UmaServerDiscoverySettings;
import org.apereo.cas.uma.discovery.UmaServerDiscoverySettingsFactory;
import org.apereo.cas.uma.ticket.DefaultUmaPermissionTicketFactory;
import org.apereo.cas.uma.ticket.UmaPermissionTicketFactory;
import org.apereo.cas.uma.web.UmaRequestingPartyTokenAuthenticator;
import org.apereo.cas.uma.web.controllers.UmaPermissionRegistrationEndpointController;
import org.apereo.cas.uma.web.controllers.UmaWellKnownEndpointController;
import org.apereo.cas.util.DefaultUniqueTicketIdGenerator;

import lombok.val;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.springframework.web.SecurityInterceptor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apereo.cas.support.oauth.OAuth20Constants.BASE_OAUTH20_URL;

/**
 * This is {@link CasOAuthUmaConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Configuration("casOAuthUmaConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CasOAuthUmaConfiguration implements WebMvcConfigurer {
    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Autowired
    @Qualifier("ticketRegistry")
    private TicketRegistry ticketRegistry;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ServiceFactory<WebApplicationService> webApplicationServiceFactory;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    @ConditionalOnMissingBean(name = "umaServerDiscoverySettingsFactory")
    public FactoryBean<UmaServerDiscoverySettings> umaServerDiscoverySettingsFactory() {
        return new UmaServerDiscoverySettingsFactory(casProperties);
    }

    @Autowired
    @Bean
    public UmaWellKnownEndpointController umaWellKnownEndpointController(@Qualifier("umaServerDiscoverySettingsFactory") final UmaServerDiscoverySettings discoverySettings) {
        return new UmaWellKnownEndpointController(discoverySettings);
    }

    @RefreshScope
    @Bean
    public UmaPermissionRegistrationEndpointController umaPermissionRegistrationEndpointController() {
        return new UmaPermissionRegistrationEndpointController();
    }

    @ConditionalOnMissingBean(name = "umaPermissionTicketIdGenerator")
    @Bean
    @RefreshScope
    public UniqueTicketIdGenerator umaPermissionTicketIdGenerator() {
        return new DefaultUniqueTicketIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(name = "umaPermissionTicketExpirationPolicy")
    public ExpirationPolicy umaPermissionTicketExpirationPolicy() {
        val uma = casProperties.getAuthn().getUma();
        return new HardTimeoutExpirationPolicy(Beans.newDuration(uma.getPermissionTicket().getMaxTimeToLiveInSeconds()).getSeconds());
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "defaultUmaPermissionTicketFactory")
    public UmaPermissionTicketFactory defaultUmaPermissionTicketFactory() {
        return new DefaultUmaPermissionTicketFactory(umaPermissionTicketIdGenerator(), umaPermissionTicketExpirationPolicy());
    }

    @Bean
    public SecurityInterceptor umaSecurityInterceptor() {
        val authenticator = new UmaRequestingPartyTokenAuthenticator(ticketRegistry);
        val basicAuthClient = new HeaderClient(HttpHeaders.AUTHORIZATION, OAuth20Constants.TOKEN_TYPE_BEARER, authenticator);
        basicAuthClient.setName("CAS_UMA_CLIENT_BASIC_AUTH");
        val clients = Stream.of(basicAuthClient.getName()).collect(Collectors.joining(","));
        val config = new Config(OAuth20Utils.casOAuthCallbackUrl(casProperties.getServer().getPrefix()), basicAuthClient);
        config.setSessionStore(new J2ESessionStore());
        return new SecurityInterceptor(config, clients);
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(umaSecurityInterceptor())
            .addPathPatterns(BASE_OAUTH20_URL.concat("/").concat(OAuth20Constants.UMA_PERMISSION_URL).concat("*"));
    }
}