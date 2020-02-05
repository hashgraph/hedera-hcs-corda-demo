### Corda HCS Demo

This demo marries Corda with Hedera via the Hedera Consensus Service, providing double-spend
checks by submitting transaction spends to HCS and ordering them with other spends on the same
network.


### Running

You must have JDK 8 installed and set up (the `JAVA_HOME` environment variable must be set).

##### 1. Clone the repository:

Open a terminal in the parent directory where you want to clone the repo. Unless otherwise
specified, all following code blocks are entered in this terminal window with "Enter" being
pressed after each line.

```bash
git clone git@github.com:hashgraph/hedera-hcs-corda
cd hedera-hcs-corda
```

##### 2. Build the Nodes

This will also remove node data from previous runs (for consistency)
```sh
./gradlew clean deployNodes
```

##### 3. Run the nodes

```bash
./build/nodes/runnodes
```

3 new terminal windows will open: "Notary", "PartyA" and "PartyB". They will go through
an initialization process that may take a minute or so. You will know when all the nodes are ready
when you see a `>>>` prompt in each terminal window.

##### 4. Issue some cash to Party A

The contract doesn't actually require the lending party to have sufficient balance for a loan
but for the demo to make sense it's kind of important to show this step anyway.

In the **"Party A"** terminal window, type the following and press "Enter":

```bash
flow start CashIssueFlow amount: $100, issuerBankPartyRef: "some bank", notary: "Notary"
```

`amount` may be any USD, GBP or Euro amount with their respective currency symbol prefixes; 
this procedure assumes a US keyboard layout and so uses USD.

`issuerBankPartyRef` may be any text and exists purely for recording purposes; in the context of the
demo it would be the name of the bank that recorded the cash deposit.

`notary` must be "Notary", although notarisation is apparently not required for `CashIssueFlow`
as it never actually talks to the notary.

##### 5. Party B borrows money from Party A

In the **"Party B"** terminal window, type the following and press "Enter":

```bash
flow start IssueObligation amount: $25, lender: "Party A", anonymous: false
```

`amount` has the same semantics as the previous step; for consistency purposes you should
use the same currency for all amounts. 

`lender` must be a valid party name on the network, though the only other party who may
lend money in the current network configuration is "Party A".

`anonymous` may be set to `true` or `false`; `true` means that the obligation will not 
record who is requesting the loan. For demonstration purposes we want the obligation to show
both parties so we specify `false`.

When you enter this command, when the transaction reaches the "Requesting signature from Notary
service" step you will see activity in the "Notary" terminal window showing it submitting the 
HCS message to Hedera with a sequence number and then receiving back a message with that same
sequence number.

##### 6. See the obligation in Party B's vault

In the **"Party B"** terminal window, enter:

```bash
run vaultQuery contractStateType: net.corda.examples.obligation.Obligation
```

This will print out a bunch of information about our IOU. The important keys to note are
the "Lender" and the "Borrower" parties, which should be "Party A" and "Party B", respectively,
and the `amount:` key showing the balance of our IOU.

Then you want to look for the `linearId:` key and the `id:` key underneath it. Copy that UUID
(that will look something like this: `"2767ac1e-a8ec-4a1a-a0ab-1bafd4d98df2"`) as you will need
it in step 8.

##### 7. Issue cash to Party B

This is the same as **step 4**, except this time we enter the command in the **"Party B"**
terminal window. Make sure the `amount` is greater than or equal to the balance of our IOU
(although you can actually make partial payments on IOUs, we won't be doing that for this demo
as those steps look identical to steps 6 - 8).

##### 8. Repay the IOU

In the **"Party B"** terminal window, enter the following:

```bash
flow start SettleObligation linearId: <linear ID from vaultQuery>, amount: $25, anonymous: false
```

For `linearId`, paste in the UUID that you got from **step 6**, including the double quotes `""`.

`amount` doesn't have to be the full balance of our IOU but would require repeating this step
(as well as the previous step if Party B's cash balance is insufficent).

`anonymous` has the same semantics as in **step 5** but we're not going to bother with anonymity.

When you enter this command, you will see more activity in the "Notary" terminal window,
again submitting the transaction to HCS for global ordering before checking for double-spends.

##### 9. See that the obligation is destroyed

In the **"Party B"** terminal window, run the same command as **step 6** again; this time you
will see a much shorter response with no obligations listed. Our debt has been repaid.

##### 10. Rinse and repeat, or Cleanly shut down the nodes

If you wish to repeat the demo, return to **step 4**. 

Otherwise, in each of "Notary", "Party A" and "Party B" terminal windows, type `bye` and press 
"Enter" to shut down the nodes. Each terminal window should close a few moments after entering the 
command.
