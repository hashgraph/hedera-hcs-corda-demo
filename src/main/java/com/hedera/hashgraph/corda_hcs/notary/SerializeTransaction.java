package com.hedera.hashgraph.corda_hcs.notary;

import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.transactions.CoreTransaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SerializeTransaction {
    public final List<StateRef> inputs;
    public final List<StateRef> refs;

    public SerializeTransaction(CoreTransaction txn) {
        this.inputs = txn.getInputs();
        this.refs = txn.getReferences();
    }

    public SerializeTransaction(List<StateRef> inputs, List<StateRef> refs) {
        this.inputs = inputs;
        this.refs = refs;
    }

    public byte[] serialize() {
        int capacity = 8 + inputs.size() * 96 + refs.size() * 96;

        ByteBuffer out = ByteBuffer.allocateDirect(capacity);
        out.putInt(inputs.size());
        out.putInt(refs.size());

        for (StateRef input : inputs) {
            out.put(input.getTxhash().copyBytes());
            out.putInt(input.getIndex());
        }

        for (StateRef ref : refs) {
            out.put(ref.getTxhash().copyBytes());
            out.putInt(ref.getIndex());
        }

        return out.array();
    }

    public static SerializeTransaction deserialize(byte[] data) {
        ByteBuffer in = ByteBuffer.wrap(data).asReadOnlyBuffer();

        int inputsLen = in.getInt();
        int refsLen = in.getInt();

        List<StateRef> inputs = new ArrayList<>(inputsLen);
        List<StateRef> refs = new ArrayList<>(refsLen);

        for (int i = 0; i < inputsLen; i++) {
            byte[] hash = new byte[32];
            in.get(hash);
            int index = in.getInt();

            inputs.add(new StateRef(new SecureHash.SHA256(hash), index));
        }

        for (int i = 0; i < refsLen; i++) {
            byte[] hash = new byte[32];
            in.get(hash);
            int index = in.getInt();

            refs.add(new StateRef(new SecureHash.SHA256(hash), index));
        }

        return new SerializeTransaction(inputs, refs);
    }
}
