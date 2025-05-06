package org.stellar.stellarjavarpc.service;

import org.springframework.stereotype.Service;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;

@Service
class StellarRpcServiceImpl implements StellarRpcService {

    private final SorobanServer sorobanServer;

    public StellarRpcServiceImpl(SorobanServer sorobanServer) {
        this.sorobanServer = sorobanServer;
    }

    @Override
    public GetLatestLedgerResponse getLatestLedger() {
        return sorobanServer.getLatestLedger();
    }
}
