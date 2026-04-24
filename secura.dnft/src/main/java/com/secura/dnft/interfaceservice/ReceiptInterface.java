package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.GenerateReceiptRequest;

public interface ReceiptInterface {

	CreateReceiptResponse createReceipt(CreateReceiptRequest request) throws Exception;

	CreateReceiptResponse generateReceipt(GenerateReceiptRequest request) throws Exception;
}
