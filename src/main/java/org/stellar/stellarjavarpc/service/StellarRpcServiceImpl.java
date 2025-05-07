package org.stellar.stellarjavarpc.service;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static org.stellar.stellarjavarpc.service.StellarRpcService.*;


/**
 * Implementation of the StellarRpcService interface that provides asynchronous operations
 * for interacting with the Stellar network using the official Java SDK.
 * This implementation leverages Spring Framework's async capabilities and CompletableFuture
 * for non-blocking operations.
 */
@Service
class StellarRpcServiceImpl implements StellarRpcService {
	private static final Logger log = LoggerFactory.getLogger(StellarRpcServiceImpl.class);

	@Value("${stellar.rpc.public.key}")
	private String keypairPublicKey;

	@Value("${stellar.rpc.secret.key}")
	private String keypairSecret;

	private final SorobanServer sorobanServer;

	/**
	 * Constructs a new StellarRpcServiceImpl with the specified Horizon server.
	 *
	 * @param sorobanServer The Stellar Horizon server instance to connect to
	 * @throws IllegalArgumentException if server is null
	 */
	public StellarRpcServiceImpl(@NonNull SorobanServer sorobanServer) {
		this.sorobanServer = sorobanServer;
	}

	@Override
	public Supplier<SorobanServer> getSorobanServer() {
		return () -> sorobanServer;
	}

	private static SendTransactionResponse accountCreationHandler(SendTransactionResponse sendTransactionResponse) {

		log.info("Account Creation Response: {}", sendTransactionResponse);
		log.info("https://stellar.expert/explorer/testnet/tx/{}", sendTransactionResponse.getHash());

		return sendTransactionResponse;
	}

	@Override
	public GetLatestLedgerResponse getLatestLedger() {
		return asyncRpcCall(rpcServer -> rpcServer.get().getLatestLedger())
				.join();
	}

	@Override
	public CompletableFuture<GetTransactionResponse> getTransaction(SendTransactionResponse transactionResponse) {
		return asyncRpcCall(rpcServer ->
				rpcServer.get().getTransaction(transactionResponse.getHash()));
	}

	@Override
	public GetTransactionResponse createAccount() {

		final TransactionBuilderAccount sourceAccount = getSourceAccount();
		final KeyPair destination = getKeyPair();
		final KeyPair signer = KeyPair
				.fromSecretSeed(keypairSecret);

		final Transaction transaction =
				getTransactionBuilder(sourceAccount)
						.addOperation(buildCreateAccountOperation(destination))
						.addPreconditions(getValidatedPreconditions(sourceAccount))
						.build();

		transaction.sign(signer);

		return asyncRpcCall(rpcServer -> rpcServer.get().sendTransaction(transaction))
				.thenApplyAsync(StellarRpcServiceImpl::accountCreationHandler)
				.thenComposeAsync(this::getTransaction, delayedExecutor(10, TimeUnit.SECONDS))
				.join();
	}

	@Override
	public String getFriendbotUrl() {
		return asyncRpcCall(rpcServer -> rpcServer.get().getNetwork().getFriendbotUrl())
				.join();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation uses the Stellar SDK's Server.accounts() method to fetch
	 * account details asynchronously. The operation is executed in a non-blocking manner
	 * using CompletableFuture.
	 */
	@Override
	public TransactionBuilderAccount getSourceAccount() {
		return asyncRpcCall(rpcServer -> rpcServer.get().getAccount(keypairPublicKey))
				.join();
	}
}
