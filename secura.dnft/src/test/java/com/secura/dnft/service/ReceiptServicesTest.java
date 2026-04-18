package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.Items;

class ReceiptServicesTest {

	private final ReceiptServices receiptServices = new ReceiptServices();

	@Test
	void createReceipt_shouldGeneratePdfReceiptWithConfiguredSections() throws Exception {
		CreateReceiptRequest request = new CreateReceiptRequest();
		request.setGenericHeader(buildHeader());
		request.setReceiptType("Payment Receipt");
		request.setPerheadFlag(true);
		request.setUnitPriceRequired(false);
		request.setTotalAmount("1234.50");
		request.setRemarks("Paid against club house booking.");
		request.setItems(List.of(buildItem("Club House Booking", "300", "4", "service", "1200")));
		request.setAddedCharges(List.of(buildCharge("GST", "percentage", "18", "216")));
		request.setDiscFinReceipt(buildDiscFinReceipt());

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_34, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_34, response.getMessageCode());
		assertNotNull(response.getReceipt());
		assertTrue(response.getReceipt().getReceiptNumber().matches("\\d{10}"));
		assertEquals("application/pdf", response.getReceipt().getReceiptFileType());
		assertTrue(response.getReceipt().getReceiptFileData().length() > 0);

		byte[] pdfBytes = Base64.getDecoder().decode(response.getReceipt().getReceiptFileData());
		assertEquals("%PDF", new String(pdfBytes, 0, 4));
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			String text = new PDFTextStripper().getText(document);
			assertTrue(text.contains("Payment Receipt"));
			assertTrue(text.contains("NO OF PERSON"));
			assertFalse(text.contains("UNIT PRICE"));
			assertTrue(text.contains("Taxes And Other Charges"));
			assertTrue(text.contains("Discount / Fine"));
			assertTrue(text.contains("Fine (cumulative)"));
			assertTrue(text.contains("1234.50"));
			assertTrue(text.contains("Paid against club house booking."));
		}
	}

	@Test
	void createReceipt_shouldIncludeUnitPriceColumnWhenRequested() throws Exception {
		CreateReceiptRequest request = new CreateReceiptRequest();
		request.setGenericHeader(buildHeader());
		request.setReceiptType("Maintenance Receipt");
		request.setUnitPriceRequired(true);
		request.setTotalAmount("500");
		request.setItems(List.of(buildItem("Maintenance", "500", "1", "mandatory", "500")));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		byte[] pdfBytes = Base64.getDecoder().decode(response.getReceipt().getReceiptFileData());
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			String text = new PDFTextStripper().getText(document);
			assertTrue(text.contains("UNIT PRICE"));
			assertTrue(text.contains("Maintenance"));
			assertTrue(text.contains("500"));
		}
	}

	private GenericHeader buildHeader() {
		GenericHeader header = new GenericHeader();
		header.setApartmentName("Secura Residency");
		header.setProfileName("Sudeep");
		header.setFlatNo("A-101");
		header.setProfilepic(
				"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==");
		return header;
	}

	private Items buildItem(String name, String unitPrice, String quantity, String type, String amount) {
		Items item = new Items();
		item.setItemName(name);
		item.setUnitPrice(unitPrice);
		item.setQuantity(quantity);
		item.setType(type);
		item.setAmount(amount);
		return item;
	}

	private AddedCharges buildCharge(String name, String type, String value, String finalValue) {
		AddedCharges charge = new AddedCharges();
		charge.setChargeName(name);
		charge.setChargeType(type);
		charge.setValue(value);
		charge.setFinalChargeValue(finalValue);
		return charge;
	}

	private DiscFinReceipt buildDiscFinReceipt() {
		DiscFinReceipt discFinReceipt = new DiscFinReceipt();
		discFinReceipt.setDiscountAmount("50");
		discFinReceipt.setDiscountType("percentage");
		discFinReceipt.setDiscountPercentage("5");
		discFinReceipt.setFineAmount("68.50");
		discFinReceipt.setFineType("percentage");
		discFinReceipt.setFinePercentage("2");
		discFinReceipt.setFineCycleMode("cumulative");
		return discFinReceipt;
	}
}
