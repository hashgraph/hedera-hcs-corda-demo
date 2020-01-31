package com.hedera.hashgraph.corda_hcs.notary;

import com.hedera.hashgraph.sdk.HederaStatusException;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.NotarisationResponse;
import net.corda.core.transactions.CoreTransaction;

import java.time.Duration;
import java.util.Collections;

public class HcsNotaryServiceFlow extends FlowLogic<Void> {

    private final HcsNotaryService notaryService;
    private final FlowSession otherPartySession;

    public HcsNotaryServiceFlow(HcsNotaryService notaryService, FlowSession otherPartySession) {
        this.notaryService = notaryService;
        this.otherPartySession = otherPartySession;
    }

    @Override
    public Void call() throws FlowException {
        CoreTransaction txn = otherPartySession.receive(CoreTransaction.class)
                .unwrap(t -> t);

        long seqNumber;

        try {
            seqNumber = notaryService.submitTransactionSpends(txn);
        } catch (HederaStatusException e) {
            throw new FlowException(e);
        }

        while (!notaryService.checkTransaction(txn, seqNumber)) {
            FlowLogic.sleep(Duration.ofSeconds(5));
        }

        otherPartySession.send(new NotarisationResponse(Collections.singletonList(notaryService.signTransaction(txn.getId()))));

        return null;
    }
}
