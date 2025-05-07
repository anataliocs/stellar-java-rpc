package org.stellar.stellarjavarpc.service;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse;

public interface StellarRpcService {

    static KeyPair getKeyPair() {
        KeyPair keyPair = KeyPair.random();
        StrKey.isValidEd25519PublicKey(keyPair.getAccountId());
        return keyPair;
    }


    GetLatestLedgerResponse getLatestLedger();

    SendTransactionResponse createAccount();
}
