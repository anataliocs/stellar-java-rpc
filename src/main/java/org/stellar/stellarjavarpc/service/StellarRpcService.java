package org.stellar.stellarjavarpc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.LedgerBounds;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.TimeBounds;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.TransactionPreconditions;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.String.valueOf;
import static java.util.concurrent.CompletableFuture.failedStage;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public interface StellarRpcService {

	Logger log = LoggerFactory.getLogger(StellarRpcService.class);

	static KeyPair getKeyPair() {
		KeyPair keyPair = KeyPair.random();
		StrKey.isValidEd25519PublicKey(keyPair.getAccountId());
		return keyPair;
	}

	static @lombok.NonNull TransactionPreconditions getValidatedPreconditions(TransactionBuilderAccount sourceAccount) {
		TransactionPreconditions preconditions = TransactionPreconditions.builder()
				.minSeqNumber(sourceAccount.getSequenceNumber())
				.timeBounds(TimeBounds.expiresAfter(1000))
				.ledgerBounds(new LedgerBounds(0, 0))
				.build();

		preconditions.validate();

		return preconditions;
	}

	/**
	 * Creates generic handlers for wrapping `SorobanServer` rpc calls in an async wrapper.
	 *
	 * @param rpcCallFunction A lambda or method reference that defines the rpc call to execute.
	 * @param <ResponseType>  Generic representing the return type
	 * @return ResponseType
	 */
	@Async("rpcTaskExecutor")
	default <ResponseType> CompletableFuture<ResponseType> asyncRpcCall(AsyncRpcCall<Supplier<SorobanServer>, ResponseType> rpcCallFunction) {

		return supplyAsync(() -> rpcCallFunction.makeRpcCall(getSorobanServer()))
				.completeOnTimeout(null, 100, TimeUnit.SECONDS)
				.exceptionallyCompose(throwable -> {
					log.error(valueOf(throwable));
					return failedStage(throwable);
				});
	}

	@FunctionalInterface
	interface AsyncRpcCall<Server extends Supplier<SorobanServer>, ResponseType> {
		ResponseType makeRpcCall(Server server);
	}

	String getFriendbotUrl();

	GetLatestLedgerResponse getLatestLedger();

	GetTransactionResponse getTransaction(String txId);

	SendTransactionResponse createAccount();

	Supplier<SorobanServer> getSorobanServer();
}
