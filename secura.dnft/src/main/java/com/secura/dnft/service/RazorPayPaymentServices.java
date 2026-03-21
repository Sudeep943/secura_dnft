package com.secura.dnft.service;

import org.json.JSONObject;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.secura.dnft.request.response.RazorPayPaymentRequest;
import com.secura.dnft.request.response.RazorPayPaymentResponse;

public class RazorPayPaymentServices {
	
	private final String key="rzp_test_SRxceBfBqGmeGy";
	private final String secret="RO2SpqK1H96ShjlmkttpA2cX";
	
	
	
	public RazorPayPaymentResponse pay(RazorPayPaymentRequest paymentRequest) {
		RazorPayPaymentResponse paymentResponse = new RazorPayPaymentResponse();
		try {
		RazorpayClient client = new RazorpayClient(key, secret);

		JSONObject options = new JSONObject();
		options.put("amount", 50000); // amount in paise (₹500)
		options.put("currency", "INR");
		options.put("receipt", "order_123");

		Order order;
		
			order = client.orders.create(options);
		    System.out.println(order);
		} catch (RazorpayException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return paymentResponse;
	}
	
	public RazorPayPaymentResponse creteUPIPaymentRequest(RazorPayPaymentRequest paymentRequest) {
		RazorpayClient client;
		try {
			client = new RazorpayClient(key,secret);
		

         // Create order request
         JSONObject options = new JSONObject();

         // amount in paise (₹500 = 50000)
         options.put("amount", 50000);

         options.put("currency", "INR");
         options.put("receipt", "receipt_order_001");

         // auto capture payment
         options.put("payment_capture", 1);

         // create order
         Order order = client.orders.create(options);

         System.out.println("Order created successfully");

         System.out.println("Order ID: " + order.get("id"));
         System.out.println("Amount: " + order.get("amount"));
         System.out.println("Currency: " + order.get("currency"));
		} catch (RazorpayException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         RazorPayPaymentResponse paymentResponse = new RazorPayPaymentResponse();
         return paymentResponse;
	}

}
