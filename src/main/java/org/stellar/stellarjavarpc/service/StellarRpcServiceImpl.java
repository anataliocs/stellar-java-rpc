package org.stellar.stellarjavarpc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.LedgerBounds;
import org.stellar.sdk.Network;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.TimeBounds;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.TransactionPreconditions;
import org.stellar.sdk.operations.CreateAccountOperation;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

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

	private static SendTransactionResponse accountCreationHandler(SendTransactionResponse sendTransactionResponse) {

		log.info("Account Creation Response: {}", sendTransactionResponse);
		log.info("https://stellar.expert/explorer/testnet/tx/{}", sendTransactionResponse.getHash());

		return sendTransactionResponse;
	}

	@Override
	public GetLatestLedgerResponse getLatestLedger() {
		return sorobanServer.getLatestLedger();
	}

	@Override
	public SendTransactionResponse createAccount() {

		final TransactionBuilderAccount sourceAccount = getSigner();
		String friendbotUrl = sorobanServer.getNetwork().getFriendbotUrl();

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
						.addPreconditions(getValidatedPreconditions(sourceAccount))
						.build();

		transaction.sign(signer);

		return CompletableFuture.supplyAsync(() -> sorobanServer.sendTransaction(transaction))
				.thenApplyAsync(StellarRpcServiceImpl::accountCreationHandler)
				.join();
	}

	private static @lombok.NonNull TransactionPreconditions getValidatedPreconditions(TransactionBuilderAccount sourceAccount) {
		TransactionPreconditions preconditions = TransactionPreconditions.builder()
				.minSeqNumber(sourceAccount.getSequenceNumber())
				.timeBounds(TimeBounds.expiresAfter(1000))
				.ledgerBounds(new LedgerBounds(0, 0))
				.build();

		preconditions.validate();

		return preconditions;
	}

	private TransactionBuilderAccount getSigner() {
		return sorobanServer.getAccount(keypairPublicKey);
	}
}
