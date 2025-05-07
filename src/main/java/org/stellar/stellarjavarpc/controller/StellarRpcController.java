package org.stellar.stellarjavarpc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.stellar.sdk.responses.sorobanrpc.GetLatestLedgerResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.stellarjavarpc.service.StellarRpcService;

@Slf4j
@Controller
@RequestMapping("/stellar/v1")
class StellarRpcController {

	private final StellarRpcService stellarRpcService;

	StellarRpcController(StellarRpcService stellarRpcService) {
		this.stellarRpcService = stellarRpcService;
	}

	@GetMapping("/ledger")
	public ResponseEntity<GetLatestLedgerResponse> getLatestLedger() {
		return ResponseEntity.ok(stellarRpcService.getLatestLedger());
	}

	@PostMapping("/account")
	public ResponseEntity<GetTransactionResponse> post() {
		return ResponseEntity.ok(stellarRpcService.createAccount());
	}
}
