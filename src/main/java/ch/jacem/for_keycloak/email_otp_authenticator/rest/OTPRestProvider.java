package ch.jacem.for_keycloak.email_otp_authenticator.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class OTPRestProvider implements RealmResourceProvider {

    private KeycloakSession session;

    public OTPRestProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new OTPRestResource(session);
    }

    @Override
    public void close() {
    }
}