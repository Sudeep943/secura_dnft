package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;

public interface ReceiptInterface {

	CreateReceiptResponse createReceipt(CreateReceiptRequest request) throws Exception;
}
