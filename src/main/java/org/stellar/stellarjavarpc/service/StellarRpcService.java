package org.stellar.stellarjavarpc.service;

import org.springframework.stereotype.Service;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.SorobanRpcResponse;

public interface StellarRpcService {

    GetLatestLedgerResponse getLatestLedger();
}
