package com.hedera.hashgraph.corda_hcs.notary;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.consensus.ConsensusMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import com.hedera.hashgraph.sdk.mirror.MirrorSubscriptionHandle;

import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.Crypto;
import net.corda.core.crypto.SecureHash;
import net.corda.core.crypto.SignableData;
import net.corda.core.crypto.SignatureMetadata;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.NotaryError;
import net.corda.core.flows.NotaryException;
import net.corda.core.flows.StateConsumptionDetails;
import net.corda.core.internal.notary.NotaryService;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.CoreTransaction;
import net.corda.node.services.api.ServiceHubInternal;
import net.corda.node.services.config.NotaryConfig;

import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public class HcsNotaryService extends NotaryService {

    private final ServiceHubInternal serviceHubInternal;
    private final PublicKey publicKey;
    private final NotaryConfig notaryConfig;

    private final Client sdkClient;
    private final MirrorClient mirrorClient;

    private static final ConsensusTopicId topicId = new ConsensusTopicId(157699);
    private static final Ed25519PrivateKey submitKey = Ed25519PrivateKey.fromString("302e020100300506032b657004220420bb92449a88df33967ae785f502c2a1853db99c10ebb3955fa94c022d9f53ccf4");

    private final ConcurrentHashMap<StateRef, StateDestruction> stateDestructions = new ConcurrentHashMap<>();

    private long sequenceNumber = -1;

    @Nullable
    private MirrorSubscriptionHandle subscriptionHandle;

    public HcsNotaryService(ServiceHubInternal serviceHubInternal, PublicKey publicKey) {
        super();
        this.serviceHubInternal = serviceHubInternal;
        this.publicKey = publicKey;
        this.notaryConfig = serviceHubInternal.getConfiguration().getNotary();

        sdkClient = Client.forTestnet()
            .setOperator(new AccountId(147704), Ed25519PrivateKey.fromString(
                "302e020100300506032b657004220420bffc5bc38cae07f381a5d5baa24086eb189b6f59f407ed87d7e3010814359843"
            ));

        mirrorClient = new MirrorClient("<FILL THIS OUT>");
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

        return new HcsNotaryServiceFlow(this, otherPartySession);
    }

    long getSequenceNumber() {
        return sequenceNumber;
    }

    long submitTransactionSpends(CoreTransaction transaction) throws HederaStatusException {
        return new ConsensusMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage(new SerializeTransaction(transaction).serialize())
                .execute(sdkClient)
                .getReceipt(sdkClient)
                .getConsensusTopicSequenceNumber();
    }

    boolean checkTransaction(CoreTransaction txn, long sequenceNumber) throws NotaryException {
        if (sequenceNumber > this.sequenceNumber) {
            return false;
        }

        HashMap<StateRef, StateConsumptionDetails> consumedStates = new HashMap<>();

        for (StateRef input : txn.getInputs()) {
            StateDestruction destruction = stateDestructions.get(input);

            if (destruction != null) {
                consumedStates.put(input,
                        new StateConsumptionDetails(destruction.txnId, StateConsumptionDetails.ConsumedStateType.INPUT_STATE));
            }
        }

        for (StateRef ref : txn.getReferences()) {
            StateDestruction destruction = stateDestructions.get(ref);

            if (destruction != null) {
                consumedStates.put(ref,
                        new StateConsumptionDetails(destruction.txnId, StateConsumptionDetails.ConsumedStateType.REFERENCE_INPUT_STATE));
            }
        }

        if (!consumedStates.isEmpty()) {
            throw new NotaryException(new NotaryError.Conflict(txn.getId(), consumedStates), txn.getId());
        }

        return true;
    }

    TransactionSignature signTransaction(SecureHash txId) {
        SignableData signableData = new SignableData(txId, new SignatureMetadata(serviceHubInternal.getMyInfo().getPlatformVersion(), Crypto.findSignatureScheme(publicKey).getSchemeNumberID()));
        return serviceHubInternal.getKeyManagementService().sign(signableData, publicKey);
    }


    private void onMessage(MirrorConsensusTopicResponse msg) {
        sequenceNumber = msg.sequenceNumber;
        SerializeTransaction txn = SerializeTransaction.deserialize(msg.message);

    }

    @Override
    public void start() {
        subscriptionHandle = new MirrorConsensusTopicQuery()
                .setTopicId(topicId)
                .subscribe(
                        mirrorClient,
                        this::onMessage,
                        e -> System.out.println("err: " + e)
                );
    }

    @Override
    public void stop() {
        if (subscriptionHandle != null) {
            subscriptionHandle.unsubscribe();
        }
    }
}
