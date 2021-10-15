package cloud.appuio.keycloak.extensions.mappers;

import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;

import java.util.List;
import java.util.Optional;

class ClaimListExtractor {

    static Optional<List<String>> extractClaim(BrokeredIdentityContext context, String claimName) {
        var claim = AbstractClaimMapper.getClaimValue(context, claimName);
        if (claim == null) {
            return Optional.empty();
        }
        return Optional.of(toListFromClaim(claim));
    }

    private static List<String> toListFromClaim(Object claim) {
        // convert to string list if not list
        if (List.class.isAssignableFrom(claim.getClass())) {
            return castToList(claim);
        }
        return List.of(claim.toString());
    }

    @SuppressWarnings("unchecked")
    private static List<String> castToList(Object newGroupsObj) {
        return (List<String>) newGroupsObj;
    }

}
