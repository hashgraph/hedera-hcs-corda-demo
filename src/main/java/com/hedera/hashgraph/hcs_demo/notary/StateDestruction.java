package com.hedera.hashgraph.hcs_demo.notary;

import net.corda.core.crypto.SecureHash;

public class StateDestruction {
    public final SecureHash txnId;
    public final long sequenceNumber;

    public StateDestruction(SecureHash txnId, long sequenceNumber) {
        this.txnId = txnId;
        this.sequenceNumber = sequenceNumber;
    }
}
