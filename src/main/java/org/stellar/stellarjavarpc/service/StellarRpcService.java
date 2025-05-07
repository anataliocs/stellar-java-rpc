package org.stellar.stellarjavarpc.service;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.stellar.sdk.*;
import org.stellar.sdk.operations.CreateAccountOperation;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static java.lang.String.valueOf;
import static java.util.concurrent.CompletableFuture.failedStage;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stellar.sdk.AbstractTransaction.MIN_BASE_FEE;
import static org.stellar.sdk.Network.TESTNET;
import static org.stellar.sdk.TimeBounds.expiresAfter;
import static org.stellar.sdk.TransactionPreconditions.builder;


/**
 * Service interface for making asynchronous RPC calls to the Stellar network.
 * This interface provides a clean abstraction layer for interacting with the Stellar blockchain
 * using CompletableFuture for non-blocking operations.
 */
public interface StellarRpcService {

	Logger log = LoggerFactory.getLogger(StellarRpcService.class);
	BigDecimal DEFAULT_STARTING_BALANCE = BigDecimal.valueOf(100);

	static KeyPair getKeyPair() {
		final KeyPair keyPair = KeyPair.random();
		StrKey.isValidEd25519PublicKey(keyPair.getAccountId());
		return keyPair;
	}

	static @NonNull TransactionPreconditions getValidatedPreconditions(TransactionBuilderAccount sourceAccount) {
		final TransactionPreconditions preconditions = builder()
				.minSeqNumber(sourceAccount.getSequenceNumber())
				.timeBounds(expiresAfter(1000))
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
	default <ResponseType> CompletableFuture<ResponseType> asyncRpcCall(
			AsyncRpcCall<Supplier<SorobanServer>, ResponseType> rpcCallFunction) {

		return supplyAsync(() -> rpcCallFunction.makeRpcCall(getSorobanServer()))
				.completeOnTimeout(null, 30, SECONDS)
				.exceptionallyCompose(StellarRpcService::exceptionHandler);
	}

	private static <ResponseType> CompletionStage<ResponseType> exceptionHandler(Throwable throwable) {
		log.error(valueOf(throwable));
		return failedStage(throwable);
	}

	@FunctionalInterface
	interface AsyncRpcCall<Server extends Supplier<SorobanServer>, ResponseType> {
		ResponseType makeRpcCall(Server server);
	}

	static CreateAccountOperation buildCreateAccountOperation(KeyPair destination) {
		return CreateAccountOperation.builder()
				.destination(destination.getAccountId())
				.startingBalance(DEFAULT_STARTING_BALANCE)
				.build();
	}

	/**
	 * Create a new ` TransactionBuilder ` with standard defaults.
	 *
	 * @param sourceAccount required source account for transaction
	 * @return `TransactionBuilder`
	 */
	static TransactionBuilder getTransactionBuilder(TransactionBuilderAccount sourceAccount) {
		return new TransactionBuilder(sourceAccount, TESTNET)
				.setBaseFee(MIN_BASE_FEE);
	}

	TransactionBuilderAccount getSourceAccount();

	String getFriendbotUrl();

	GetLatestLedgerResponse getLatestLedger();

	CompletableFuture<GetTransactionResponse> getTransaction(SendTransactionResponse transactionResponse);

	GetTransactionResponse createAccount();

	Supplier<SorobanServer> getSorobanServer();
}
