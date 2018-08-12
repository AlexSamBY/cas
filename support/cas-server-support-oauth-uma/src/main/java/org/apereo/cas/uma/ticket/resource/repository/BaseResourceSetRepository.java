package org.apereo.cas.uma.ticket.resource.repository;

import org.apereo.cas.uma.ticket.resource.ResourceSet;

import lombok.val;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * This is {@link BaseResourceSetRepository}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
public abstract class BaseResourceSetRepository implements ResourceSetRepository {
    @Override
    public Collection<ResourceSet> getByOwner(final String owner) {
        return getAll().stream().filter(s -> s.getOwner().equalsIgnoreCase(owner)).collect(Collectors.toSet());
    }

    @Override
    public Collection<ResourceSet> getByClient(final String clientId) {
        return getAll().stream().filter(s -> s.getClientId().equalsIgnoreCase(clientId)).collect(Collectors.toSet());
    }

    @Override
    public long count() {
        return getAll().size();
    }

    @Override
    public ResourceSet save(final ResourceSet set) {
        if (!validateResourceSetScopes(set)) {
            throw new IllegalArgumentException("Cannot save a resource set with inconsistent scopes.");
        }
        return saveInternal(set);
    }

    @Override
    public ResourceSet update(final ResourceSet currentResource, final ResourceSet newResource) {
        if (currentResource.getId() <= 0 || newResource.getId() <= 0) {
            throw new IllegalArgumentException("Cannot update a resource set without identifiers.");
        }
        if (currentResource.getId() != newResource.getId()) {
            throw new IllegalArgumentException("Cannot update a resource set with inconsistent/mismatched identifiers.");
        }
        if (!validateResourceSetScopes(newResource)) {
            throw new IllegalArgumentException("Cannot save a resource set with inconsistent scopes.");
        }

        newResource.setOwner(currentResource.getOwner());
        newResource.setClientId(currentResource.getClientId());
        val saved = saveInternal(newResource);

        return saved;
    }

    /**
     * Save internal resource set.
     *
     * @param set the set
     * @return the resource set
     */
    protected abstract ResourceSet saveInternal(ResourceSet set);

    /**
     * Validate resource set scopes.
     *
     * @param rs the rs
     * @return the boolean
     */
    protected boolean validateResourceSetScopes(final ResourceSet rs) {
        if (rs.getPolicies() == null) {
            return true;
        }
        for (val policy : rs.getPolicies()) {
            if (!rs.getScopes().containsAll(policy.getScopes())) {
                return false;
            }
        }
        return true;
    }
}