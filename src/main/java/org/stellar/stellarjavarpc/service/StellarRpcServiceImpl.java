package org.stellar.stellarjavarpc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.operations.CreateAccountOperation;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.stellar.stellarjavarpc.service.StellarRpcService.getKeyPair;

@Service
class StellarRpcServiceImpl implements StellarRpcService {
	private static final Logger log = LoggerFactory.getLogger(StellarRpcServiceImpl.class);

	@Value("${stellar.rpc.public.key}")
	private String keypairPublicKey;

	@Value("${stellar.rpc.secret.key}")
	private String keypairSecret;

	private final SorobanServer sorobanServer;

	public StellarRpcServiceImpl(SorobanServer sorobanServer) {
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
	public GetTransactionResponse getTransaction(String txId) {
		return asyncRpcCall(rpcServer -> rpcServer.get().getTransaction(txId))
				.join();
	}

	@Override
	public SendTransactionResponse createAccount() {

		final TransactionBuilderAccount sourceAccount = getSigner();
		final KeyPair destination = getKeyPair();
		final KeyPair signer = KeyPair
				.fromSecretSeed(keypairSecret);

		Transaction transaction =
				new TransactionBuilder(sourceAccount, Network.TESTNET)
						.setBaseFee(Transaction.MIN_BASE_FEE)
						.addOperation(CreateAccountOperation.builder()
								.destination(destination.getAccountId())
								.startingBalance(BigDecimal.valueOf(100))
								.build())
						.addPreconditions(StellarRpcService.getValidatedPreconditions(sourceAccount))
						.build();

		transaction.sign(signer);

		return asyncRpcCall(rpcServer -> rpcServer.get().sendTransaction(transaction))
				.thenApplyAsync(StellarRpcServiceImpl::accountCreationHandler)
				.join();
	}

	@Override
	public String getFriendbotUrl() {
		return asyncRpcCall(rpcServer -> rpcServer.get().getNetwork().getFriendbotUrl())
				.join();
	}

	private TransactionBuilderAccount getSigner() {
		return asyncRpcCall(rpcServer -> rpcServer.get().getAccount(keypairPublicKey))
				.join();
	}
}
