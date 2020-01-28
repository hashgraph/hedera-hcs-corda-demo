package com.hedera.hashgraph.corda_hcs.notary;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.NotarisationRequestSignature;
import net.corda.core.identity.Party;
import net.corda.core.internal.notary.UniquenessProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

public class HcsUniquenessProvider implements UniquenessProvider {
    @NotNull
    @Override
    public CordaFuture<Result> commit(@NotNull List<StateRef> states, @NotNull SecureHash txId, @NotNull Party callerIdentity, @NotNull NotarisationRequestSignature requestSignature, @Nullable TimeWindow timeWindow, @NotNull List<StateRef> references) {
        return null;
    }

    @NotNull
    @Override
    public Duration getEta(int numStates) {
        return Duration.ofMinutes(5);
    }
}
