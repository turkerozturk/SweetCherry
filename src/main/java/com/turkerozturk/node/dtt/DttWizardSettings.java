package com.turkerozturk.node.dtt;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.server.ResponseStatusException;

public final class DttWizardSettings {
    public static final String MANAGED = "custom.dtt.wizardManaged";
    private static final String TOKEN_PREFIX = "DTT_BRIDGE_TOKEN";
    private static final String ADDRESS_PREFIX = "DTT_BRIDGE_URL";

    private DttWizardSettings() {}

    public static URI validate(String address, String tokenEnv) {
        return validate(address, tokenEnv, System::getenv);
    }

    static URI validate(String address, String tokenEnv, Function<String, String> environment) {
        String rawAddress = address == null ? "" : address.strip();
        String name = tokenEnv == null ? "" : tokenEnv.strip();
        if (rawAddress.isEmpty() && name.isEmpty()) return null;
        if (rawAddress.isEmpty() || name.isEmpty() || rawAddress.length() > 2048
                || !name.matches("DTT_BRIDGE_TOKEN(?:_[A-Z0-9_]{1,80})?")) {
            throw new IllegalArgumentException("wizard.dttInvalid");
        }
        URI endpoint = base(rawAddress);
        String approved = environment.apply(ADDRESS_PREFIX + name.substring(TOKEN_PREFIX.length()));
        if (approved != null && !approved.isBlank()) {
            URI configuredPeer = base(approved.strip());
            if (!endpoint.equals(configuredPeer)) throw new IllegalArgumentException("wizard.dttTarget");
            return configuredPeer;
        }
        if (Set.of("127.0.0.1", "[::1]", "localhost").contains(endpoint.getHost().toLowerCase(Locale.ROOT))) {
            return endpoint;
        }
        throw new IllegalArgumentException("wizard.dttTarget");
    }

    private static URI base(String address) {
        try {
            URI peer = DttBridge.peerUri(address);
            return URI.create(peer.toASCIIString().replaceFirst("/+$", "") + "/");
        } catch (ResponseStatusException | IllegalArgumentException error) {
            throw new IllegalArgumentException("wizard.dttInvalid");
        }
    }
}
