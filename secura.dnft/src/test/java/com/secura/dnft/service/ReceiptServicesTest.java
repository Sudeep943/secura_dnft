package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.ReceiptRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.Receipt;
import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.Items;
import com.secura.dnft.request.response.PaymentTenderData;

@ExtendWith(MockitoExtension.class)
class ReceiptServicesTest {

	private static final String RECEIPT_TITLE = "RECEIPT";
	private static final String RECEIPT_TYPE_LABEL = "Receipt Type :";
	private static final String SINGLE_LINE_ADDRESS = "12 Main Street";
	private static final float TWO_LINE_HEADER_BASELINE_GAP = 12f * 3f;
	private static final float RECEIPT_LOGO_RENDER_SIZE = 55f * 2.5f;

	@Mock
	private ApartmentRepository apartmentRepository;

	@Mock
	private ReceiptRepository receiptRepository;

	@Mock
	private GenericService genericServices;

	@InjectMocks
	private ReceiptServices receiptServices;

	@Test
	void createReceipt_shouldBuildBase64PdfWithRequestedSections() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.setUnitPriceRequired(true);
		request.setPerheadFlag(false);
		request.setRemarks("Paid via UPI");
		request.setTransactionId("TXN-1001");
		request.setTenderList(List.of(createTender("Online", "2500")));
		request.setAddedCharges(List.of(createCharge("GST", "percentage", "18", "180")));
		DiscFinReceipt discFinReceipt = new DiscFinReceipt();
		discFinReceipt.setDiscountCode("DISC10");
		discFinReceipt.setDiscountAmount("100");
		discFinReceipt.setDiscountType("percentage");
		discFinReceipt.setDiscountPercentage("10");
		discFinReceipt.setFineCode("FINE5");
		discFinReceipt.setFineAmount("50");
		discFinReceipt.setFineType("percentage");
		discFinReceipt.setFinePercentage("5");
		discFinReceipt.setFineCycleMode("cumulative");
		request.setDiscFinReceipt(discFinReceipt);

		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");
		apartment.setAprmntName("Secura Heights");
		apartment.setAprmntAddress("12 Main Street, Springfield");
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.of(apartment));
		when(genericServices.toJson(any())).thenReturn("{\"receiptType\":\"Maintenance\"}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_34, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_34, response.getMessageCode());
		assertNotNull(response.getReceipt());
		assertNotNull(response.getReceiptNumber());
		assertFalse(response.getReceipt().isBlank());

		String text = extractText(response.getReceipt());
		assertTrue(text.contains("Secura Heights"));
		assertTrue(text.contains("Receipt Type :"));
		assertTrue(text.contains("Maintenance"));
		assertTrue(text.contains("Date :"));
		assertTrue(text.contains("Transaction Id :"));
		assertTrue(text.contains("TXN-1001"));
		assertTrue(text.contains("Receipt Number :"));
		assertTrue(text.contains(response.getReceiptNumber()));
		assertTrue(text.contains("UNIT PRICE"));
		assertTrue(text.contains("Taxes And Other Charges"));
		assertTrue(text.contains("₹ 1000"));
		assertTrue(text.contains("₹ 2000"));
		assertTrue(text.contains("₹ 180 (18%)"));
		assertTrue(text.contains("Discount / Fine"));
		assertTrue(text.contains("Discount (CODE: DISC10)"));
		assertTrue(text.contains("₹ 100 (10%)"));
		assertTrue(text.contains("Fine (cumulative) (CODE: FINE5)"));
		assertTrue(text.contains("Tender Details"));
		assertTrue(text.contains("Online"));
		assertTrue(text.contains("Remarks"));
		assertTrue(text.contains("Paid via UPI"));
		assertTrue(text.contains("₹ 2500"));
		assertTrue(text.contains("Total Amount Paid (After Rounding)"));
		assertTrue(text.contains("This is an Electronic Generated Receipt and Required No Signature"));
		ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
		verify(receiptRepository).save(receiptCaptor.capture());
		assertEquals("APR-1", receiptCaptor.getValue().getAprmtId());
		assertEquals(response.getReceiptNumber(), receiptCaptor.getValue().getReceiptId());
		assertEquals("Maintenance", receiptCaptor.getValue().getReceiptType());
		assertEquals("{\"receiptType\":\"Maintenance\"}", receiptCaptor.getValue().getReceiptData());
	}

	@Test
	void createReceipt_shouldRenderApartmentAddressFromStoredJson() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");
		apartment.setAprmntName("Secura Heights");
		apartment.setAprmntAddress("{\"addressLine1\":\"12 Main Street\",\"city\":\"Springfield\"}");
		Address address = new Address();
		address.setAddressLine1("12 Main Street");
		address.setCity("Springfield");
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.of(apartment));
		when(genericServices.fromJson(apartment.getAprmntAddress(), Address.class)).thenReturn(address);
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		String text = extractText(response.getReceipt());
		assertTrue(text.contains("12 Main Street ,Springfield"));
	}

	@Test
	void createReceipt_shouldUseApartmentLogoAndRenderReceiptHeading() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.getGenericHeader().setProfilepic("not-base64");
		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");
		apartment.setAprmntName("Secura Heights");
		apartment.setAprmntAddress("12 Main Street, Springfield");
		apartment.setAprmnt_logo(createBase64Image());
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.of(apartment));
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		String text = extractText(response.getReceipt());
		ImageDimensions imageDimensions = extractImageDimensions(response.getReceipt());
		assertTrue(text.contains(RECEIPT_TITLE));
		assertTrue(text.indexOf(RECEIPT_TITLE) < text.indexOf(RECEIPT_TYPE_LABEL));
		assertTrue(hasImage(Base64.getDecoder().decode(response.getReceipt())));
		assertEquals(RECEIPT_LOGO_RENDER_SIZE, imageDimensions.width(), 0.01f);
		assertEquals(RECEIPT_LOGO_RENDER_SIZE, imageDimensions.height(), 0.01f);
	}

	@Test
	void createReceipt_shouldKeepTwoLineHeaderGapsAroundReceiptTitle() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");
		apartment.setAprmntName("Secura Heights");
		apartment.setAprmntAddress(SINGLE_LINE_ADDRESS);
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.of(apartment));
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		HeaderTextPositions positions = extractHeaderTextPositions(response.getReceipt(), SINGLE_LINE_ADDRESS, RECEIPT_TITLE, RECEIPT_TYPE_LABEL);
		assertEquals(TWO_LINE_HEADER_BASELINE_GAP, positions.receiptY() - positions.addressY(), 1f);
		assertEquals(TWO_LINE_HEADER_BASELINE_GAP, positions.receiptTypeY() - positions.receiptY(), 1f);
	}

	@Test
	void createReceipt_shouldHideUnitPriceAndRenameQuantityHeaderWhenRequested() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.setUnitPriceRequired(false);
		request.setPerheadFlag(true);
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.empty());
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		String text = extractText(response.getReceipt());
		assertTrue(text.contains("NO OF PERSON"));
		assertFalse(text.contains("UNIT PRICE"));
		assertTrue(text.contains("₹ 2500"));
	}

	@Test
	void createReceipt_shouldShowUnitPriceForPerheadItemsWhenRequested() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.setItems(List.of(createItem("Maintenance", "400", "3", "1200")));
		request.setTotalAmount("1200");
		request.setUnitPriceRequired(true);
		request.setPerheadFlag(true);
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.empty());
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		String text = extractText(response.getReceipt());
		assertTrue(text.contains("NO OF PERSON"));
		assertTrue(text.contains("UNIT PRICE"));
		assertTrue(text.contains("₹ 400"));
		assertTrue(text.contains("₹ 1200"));
	}

	@Test
	void createReceipt_shouldHideQuantityAndUnitPriceColumnsWhenItemQuantityIsAbsent() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.setItems(List.of(createItem("Maintenance", null, null, "1000")));
		request.setTotalAmount("1180");
		request.setUnitPriceRequired(false);
		request.setPerheadFlag(false);
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.empty());
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		String text = extractText(response.getReceipt());
		assertFalse(text.contains("NO OF PERSON"));
		assertFalse(text.contains("UNIT PRICE"));
		assertFalse(text.contains("QUANTITY"));
		assertTrue(text.contains("₹ 1000"));
	}

	@Test
	void createReceipt_shouldLimitSideBordersToReceiptBody() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.setRemarks("Paid via UPI");
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.empty());
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		byte[] pdfBytes = Base64.getDecoder().decode(response.getReceipt());
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			float rightBorderX = document.getPage(0).getMediaBox().getWidth() - 40f;
			assertEquals(2, countVerticalSegments(document.getPage(0), 40f, 18f));
			assertEquals(2, countVerticalSegments(document.getPage(0), rightBorderX, 18f));
			assertTrue(hasLineJoinStyle(document.getPage(0), 2));
		}
	}

	@Test
	void createReceipt_shouldLimitSideBordersWithAllReceiptSections() throws Exception {
		CreateReceiptRequest request = createBaseRequest();
		request.setUnitPriceRequired(true);
		request.setTransactionId("TXN-1001");
		request.setRemarks("Paid via UPI");
		request.setAddedCharges(List.of(createCharge("GST", "percentage", "18", "180")));
		DiscFinReceipt discFinReceipt = new DiscFinReceipt();
		discFinReceipt.setDiscountCode("DISC10");
		discFinReceipt.setDiscountAmount("100");
		discFinReceipt.setDiscountType("percentage");
		discFinReceipt.setDiscountPercentage("10");
		request.setDiscFinReceipt(discFinReceipt);
		request.setTenderList(List.of(createTender("Online", "2500")));
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.empty());
		when(genericServices.toJson(any())).thenReturn("{}");
		when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateReceiptResponse response = receiptServices.createReceipt(request);

		byte[] pdfBytes = Base64.getDecoder().decode(response.getReceipt());
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			float rightBorderX = document.getPage(0).getMediaBox().getWidth() - 40f;
			assertEquals(5, countVerticalSegments(document.getPage(0), 40f, 18f));
			assertEquals(5, countVerticalSegments(document.getPage(0), rightBorderX, 18f));
		}
	}

	private CreateReceiptRequest createBaseRequest() {
		CreateReceiptRequest request = new CreateReceiptRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setApartmentName("Fallback Name");
		request.setGenericHeader(header);
		request.setReceiptType("Maintenance");
		request.setItems(List.of(createItem("Maintenance", "1000", "2", "2000"), createItem("Parking", "500", "1", "500")));
		request.setTotalAmount("2500");
		return request;
	}

	private PaymentTenderData createTender(String tenderName, String amountPaid) {
		PaymentTenderData tenderData = new PaymentTenderData();
		tenderData.setTenderName(tenderName);
		tenderData.setAmountPaid(amountPaid);
		return tenderData;
	}

	private Items createItem(String itemName, String unitPrice, String quantity, String amount) {
		Items item = new Items();
		item.setItemName(itemName);
		item.setUnitPrice(unitPrice);
		item.setQuantity(quantity);
		item.setAmount(amount);
		return item;
	}

	private AddedCharges createCharge(String name, String type, String value, String finalChargeValue) {
		AddedCharges addedCharges = new AddedCharges();
		addedCharges.setChargeName(name);
		addedCharges.setChargeType(type);
		addedCharges.setValue(value);
		addedCharges.setFinalChargeValue(finalChargeValue);
		return addedCharges;
	}

	private String extractText(String base64Pdf) throws Exception {
		byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			return new PDFTextStripper().getText(document);
		}
	}

	private HeaderTextPositions extractHeaderTextPositions(String base64Pdf, String addressText, String receiptText, String receiptTypeText)
			throws Exception {
		byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			HeaderTextStripper stripper = new HeaderTextStripper(addressText, receiptText, receiptTypeText);
			stripper.getText(document);
			return stripper.getPositions();
		}
	}

	private int countVerticalSegments(PDPage page, float expectedX, float expectedGap) throws Exception {
		PDFStreamParser parser = new PDFStreamParser(page);
		List<Object> tokens = parser.parse();
		int matchCount = 0;
		for (int index = 0; index + 5 < tokens.size(); index++) {
			if (!(tokens.get(index) instanceof COSNumber moveX)
					|| !(tokens.get(index + 1) instanceof COSNumber moveY)
					|| !(tokens.get(index + 2) instanceof Operator moveOperator)
					|| !(tokens.get(index + 3) instanceof COSNumber lineX)
					|| !(tokens.get(index + 4) instanceof COSNumber lineY)
					|| !(tokens.get(index + 5) instanceof Operator lineOperator)) {
				continue;
			}
			if (!"m".equals(moveOperator.getName()) || !"l".equals(lineOperator.getName())) {
				continue;
			}
			if (Math.abs(moveX.floatValue() - expectedX) < 0.01f && Math.abs(lineX.floatValue() - expectedX) < 0.01f
					&& Math.abs(Math.abs(moveY.floatValue() - lineY.floatValue()) - expectedGap) < 0.01f) {
				matchCount++;
			}
		}
		return matchCount;
	}

	private boolean hasLineJoinStyle(PDPage page, int expectedLineJoinStyle) throws Exception {
		PDFStreamParser parser = new PDFStreamParser(page);
		List<Object> tokens = parser.parse();
		for (int index = 0; index + 1 < tokens.size(); index++) {
			if (!(tokens.get(index) instanceof COSNumber lineJoinStyle)
					|| !(tokens.get(index + 1) instanceof Operator operator)) {
				continue;
			}
			if ("j".equals(operator.getName()) && lineJoinStyle.intValue() == expectedLineJoinStyle) {
				return true;
			}
		}
		return false;
	}

	private boolean hasImage(byte[] pdfBytes) throws Exception {
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			PDFStreamParser parser = new PDFStreamParser(document.getPage(0));
			List<Object> tokens = parser.parse();
			return tokens.stream().anyMatch(token -> token instanceof Operator operator && "Do".equals(operator.getName()));
		}
	}

	private ImageDimensions extractImageDimensions(String base64Pdf) throws Exception {
		byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			PDFStreamParser parser = new PDFStreamParser(document.getPage(0));
			List<Object> tokens = parser.parse();
			for (int index = 0; index + 8 < tokens.size(); index++) {
				if (!(tokens.get(index) instanceof COSNumber width)
						|| !(tokens.get(index + 1) instanceof COSNumber shearY)
						|| !(tokens.get(index + 2) instanceof COSNumber shearX)
						|| !(tokens.get(index + 3) instanceof COSNumber height)
						|| !(tokens.get(index + 4) instanceof COSNumber translateX)
						|| !(tokens.get(index + 5) instanceof COSNumber translateY)
						|| !(tokens.get(index + 6) instanceof Operator transformOperator)
						|| !(tokens.get(index + 7) instanceof org.apache.pdfbox.cos.COSName)
						|| !(tokens.get(index + 8) instanceof Operator drawOperator)) {
					continue;
				}
				if (!"cm".equals(transformOperator.getName()) || !"Do".equals(drawOperator.getName())) {
					continue;
				}
				assertEquals(0f, shearY.floatValue(), 0.01f);
				assertEquals(0f, shearX.floatValue(), 0.01f);
				assertTrue(translateX.floatValue() > 0f);
				assertTrue(translateY.floatValue() > 0f);
				return new ImageDimensions(Math.abs(width.floatValue()), Math.abs(height.floatValue()));
			}
		}
		throw new AssertionError("Expected receipt logo image transform in PDF content stream");
	}

	private String createBase64Image() throws Exception {
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, 0xFFFFFF);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return Base64.getEncoder().encodeToString(outputStream.toByteArray());
	}

	private record HeaderTextPositions(float addressY, float receiptY, float receiptTypeY) {
	}

	private record ImageDimensions(float width, float height) {
	}

	private static class HeaderTextStripper extends PDFTextStripper {

		private final String addressText;
		private final String receiptText;
		private final String receiptTypeText;
		private Float addressY;
		private Float receiptY;
		private Float receiptTypeY;

		private HeaderTextStripper(String addressText, String receiptText, String receiptTypeText) throws Exception {
			this.addressText = addressText;
			this.receiptText = receiptText;
			this.receiptTypeText = receiptTypeText;
			setSortByPosition(true);
		}

		@Override
		protected void writeString(String text, List<TextPosition> textPositions) throws java.io.IOException {
			super.writeString(text, textPositions);
			if (textPositions.isEmpty()) {
				return;
			}
			if (addressY == null && addressText.equals(text)) {
				addressY = textPositions.get(0).getYDirAdj();
			}
			if (receiptY == null && receiptText.equals(text)) {
				receiptY = textPositions.get(0).getYDirAdj();
			}
			if (receiptTypeY == null && receiptTypeText.equals(text)) {
				receiptTypeY = textPositions.get(0).getYDirAdj();
			}
		}

		private HeaderTextPositions getPositions() {
			assertNotNull(addressY);
			assertNotNull(receiptY);
			assertNotNull(receiptTypeY);
			return new HeaderTextPositions(addressY, receiptY, receiptTypeY);
		}
	}
}
