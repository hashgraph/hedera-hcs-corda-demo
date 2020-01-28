package com.hedera.hashgraph.corda_hcs.notary;

import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.internal.notary.SinglePartyNotaryService;
import net.corda.core.internal.notary.UniquenessProvider;
import net.corda.core.node.ServiceHub;
import net.corda.node.services.api.ServiceHubInternal;
import net.corda.node.services.config.NotaryConfig;
import net.corda.node.services.transactions.NonValidatingNotaryFlow;
import net.corda.node.services.transactions.ValidatingNotaryFlow;

import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Duration;

public class HcsNotaryService extends SinglePartyNotaryService {

    private final ServiceHubInternal serviceHubInternal;
    private final PublicKey publicKey;
    private final NotaryConfig notaryConfig;

    private final HcsUniquenessProvider uniquenessProvider = new HcsUniquenessProvider();

    public HcsNotaryService(ServiceHubInternal serviceHubInternal, PublicKey publicKey) {
        super();
        this.serviceHubInternal = serviceHubInternal;
        this.publicKey = publicKey;
        this.notaryConfig = serviceHubInternal.getConfiguration().getNotary();
    }


    @NotNull
    @Override
    public PublicKey getNotaryIdentityKey() {
        return publicKey;
    }

    @NotNull
    @Override
    public ServiceHub getServices() {
        return serviceHubInternal;
    }

    @NotNull
    @Override
    public FlowLogic<Void> createServiceFlow(@NotNull FlowSession otherPartySession) {
        final Duration eta = Duration.ofSeconds(notaryConfig.getEtaMessageThresholdSeconds());

        if (notaryConfig.getValidating()) {
            return new ValidatingNotaryFlow(otherPartySession, this, eta);
        } else {
            return new NonValidatingNotaryFlow(otherPartySession, this, eta);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @NotNull
    @Override
    protected UniquenessProvider getUniquenessProvider() {
        return uniquenessProvider;
    }
}
