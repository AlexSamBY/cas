package org.apereo.cas.uma.ticket.rpt;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.ticket.BaseIdTokenGeneratorService;
import org.apereo.cas.ticket.IdTokenSigningAndEncryptionService;
import org.apereo.cas.ticket.accesstoken.AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.uma.ticket.permission.UmaPermissionTicket;
import org.apereo.cas.util.Pac4jUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.profile.UserProfile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This is {@link UmaIdTokenGeneratorService}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Slf4j
public class UmaIdTokenGeneratorService extends BaseIdTokenGeneratorService {
    public UmaIdTokenGeneratorService(final CasConfigurationProperties casProperties,
                                      final IdTokenSigningAndEncryptionService signingService,
                                      final ServicesManager servicesManager,
                                      final TicketRegistry ticketRegistry) {
        super(casProperties, signingService, servicesManager, ticketRegistry);
    }

    @Override
    public String generate(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final AccessToken accessToken,
                           final long timeoutInSeconds,
                           final OAuth20ResponseTypes responseType,
                           final OAuthRegisteredService registeredService) {

        val context = Pac4jUtils.getPac4jJ2EContext(request, response);
        LOGGER.debug("Attempting to produce claims for the rpt access token [{}]", accessToken);
        val authenticatedProfile = getAuthenticatedProfile(request, response);
        val claims = buildJwtClaims(request, accessToken, timeoutInSeconds,
            registeredService, authenticatedProfile, context, responseType);

        return encodeAndFinalizeToken(claims, registeredService, accessToken);
    }

    /**
     * Build jwt claims jwt claims.
     *
     * @param request          the request
     * @param accessTokenId    the access token id
     * @param timeoutInSeconds the timeout in seconds
     * @param service          the service
     * @param profile          the profile
     * @param context          the context
     * @param responseType     the response type
     * @return the jwt claims
     */
    protected JwtClaims buildJwtClaims(final HttpServletRequest request,
                                       final AccessToken accessTokenId,
                                       final long timeoutInSeconds,
                                       final OAuthRegisteredService service,
                                       final UserProfile profile,
                                       final J2EContext context,
                                       final OAuth20ResponseTypes responseType) {

        val permissionTicket = (UmaPermissionTicket) request.getAttribute(UmaPermissionTicket.class.getName());

        val claims = new JwtClaims();
        claims.setJwtId(UUID.randomUUID().toString());
        claims.setIssuer(casProperties.getAuthn().getUma().getIssuer());
        claims.setAudience(String.valueOf(permissionTicket.getResourceSet().getId()));

        val expirationDate = NumericDate.now();
        expirationDate.addSeconds(timeoutInSeconds);
        claims.setExpirationTime(expirationDate);
        claims.setIssuedAtToNow();
        claims.setSubject(profile.getId());

        permissionTicket.getClaims().forEach((k, v) -> claims.setStringListClaim(k, v.toString()));
        claims.setStringListClaim("scopes", new ArrayList<>(permissionTicket.getScopes()));

        return claims;
    }
}