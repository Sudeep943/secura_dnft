package com.secura.dnft.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.ReceiptRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.Receipt;
import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.ReceiptInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.Items;
import com.secura.dnft.request.response.PaymentTenderData;

@Service
public class ReceiptServices implements ReceiptInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptServices.class);
	private static final PDFont FALLBACK_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private static final PDFont FALLBACK_BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
	private static final String RUPEE_SYMBOL = "\u20B9";
	private static final float TITLE_FONT_SIZE = 12f;
	private static final float TEXT_FONT_SIZE = 10f;
	private static final float SMALL_FONT_SIZE = 9f;
	private static final float TOP_MARGIN = 780f;
	private static final float BOTTOM_MARGIN = 50f;
	private static final float LEFT_MARGIN = 40f;
	private static final float RIGHT_MARGIN = 40f;
	private static final float LINE_HEIGHT = 12f;
	private static final float CENTERED_TEXT_BOTTOM_PADDING = 4f;
	private static final float UNDERLINED_TEXT_BOTTOM_PADDING = 8f;
	private static final float CELL_PADDING = 4f;
	private static final float RECEIPT_LOGO_BASE_WIDTH = 110f;
	private static final float RECEIPT_LOGO_BASE_HEIGHT = 55f;
	private static final float RECEIPT_LOGO_SCALE_MULTIPLIER = 2.5f;
	private static final float RECEIPT_LOGO_MAX_WIDTH = RECEIPT_LOGO_BASE_WIDTH * RECEIPT_LOGO_SCALE_MULTIPLIER;
	private static final float RECEIPT_LOGO_MAX_HEIGHT = RECEIPT_LOGO_BASE_HEIGHT * RECEIPT_LOGO_SCALE_MULTIPLIER;
	// Keep three line-heights between header text baselines, which leaves two blank lines visually.
	private static final float HEADER_BASELINE_GAP = LINE_HEIGHT * 3;
	// drawCenteredText already advances by the text height plus its bottom padding, so only the remainder is added here.
	private static final float HEADER_ADDRESS_TO_RECEIPT_GAP = HEADER_BASELINE_GAP - (TEXT_FONT_SIZE + CENTERED_TEXT_BOTTOM_PADDING);
	// The meta row text sits below the table top by cell padding and text height, so subtract those existing offsets.
	private static final float HEADER_RECEIPT_TO_META_GAP = HEADER_BASELINE_GAP
			- ((TITLE_FONT_SIZE + UNDERLINED_TEXT_BOTTOM_PADDING) + CELL_PADDING + SMALL_FONT_SIZE);
	private static final float SECTION_TITLE_HEIGHT = 18f;
	private static final float SECTION_GAP = SECTION_TITLE_HEIGHT;
	private static final float BORDER_LINE_WIDTH = 0.75f;
	private static final int LINE_JOIN_BEVEL = 2;
	private static final int LINE_CAP_BUTT = 0;
	private static final String ELECTRONIC_RECEIPT_NOTE = "* This is an Electronic Generated Receipt and Required No Signature";
	private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);
	private static final AtomicLong LAST_RECEIPT_NUMBER = new AtomicLong();
	private static final String[] REGULAR_FONT_PATHS = new String[] { "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
			"/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf", "/Library/Fonts/Arial Unicode.ttf",
			"/System/Library/Fonts/Supplemental/Arial Unicode.ttf", "C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/segoeui.ttf" };
	private static final String[] BOLD_FONT_PATHS = new String[] { "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
			"/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf", "/Library/Fonts/Arial Bold.ttf",
			"/System/Library/Fonts/Supplemental/Arial Bold.ttf", "C:/Windows/Fonts/arialbd.ttf", "C:/Windows/Fonts/seguisb.ttf" };

	@Autowired
	private ApartmentRepository apartmentRepository;

	@Autowired
	private ReceiptRepository receiptRepository;


	
	@Autowired
	private GenericService genericServices;

	@Override
	public CreateReceiptResponse createReceipt(CreateReceiptRequest request) throws Exception {
		LocalDateTime currentTimestamp = LocalDateTime.now();
		String receiptNumber = generateReceiptNumber();
		CreateReceiptResponse response = new CreateReceiptResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setReceipt(buildReceiptBase64(request, receiptNumber, currentTimestamp));
		response.setReceiptNumber(receiptNumber);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_34);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_34);
		receiptRepository.save(buildReceiptEntity(request, receiptNumber, currentTimestamp));
		return response;
	}

	public CreateReceiptResponse previewReceipt(CreateReceiptRequest request) throws Exception {
		LocalDateTime currentTimestamp = LocalDateTime.now();
		CreateReceiptResponse response = new CreateReceiptResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setReceipt(buildReceiptImageBase64(request, currentTimestamp));
		response.setMessage(SuccessMessage.SUCC_MESSAGE_34);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_34);
		return response;
	}

	private PDDocument buildPdfDocument(CreateReceiptRequest request, String receiptNumber, LocalDateTime receiptDate) throws Exception {
		PDDocument document = new PDDocument();
		PdfCanvas canvas = new PdfCanvas(document);
		ApartmentMaster apartment = resolveApartment(request != null ? request.getGenericHeader() : null);
		drawHeader(canvas, request, apartment);
		drawMetaTable(canvas, request, receiptNumber, receiptDate);
		drawItemsSection(canvas, request);
		drawAddedChargesSection(canvas, request != null ? request.getAddedCharges() : null);
		drawDiscountFineSection(canvas, request != null ? request.getDiscFinReceipt() : null);
		drawTenderDetailsSection(canvas, request != null ? request.getPaymentTenderDataList() : null);
		drawTotal(canvas, request != null ? request.getTotalAmount() : null);
		drawRemarks(canvas, request != null ? request.getRemarks() : null);
		drawElectronicReceiptNote(canvas);
		canvas.close();
		return document;
	}

	private String buildReceiptBase64(CreateReceiptRequest request, String receiptNumber, LocalDateTime receiptDate) throws Exception {
		try (PDDocument document = buildPdfDocument(request, receiptNumber, receiptDate);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			document.save(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}

	private String buildReceiptImageBase64(CreateReceiptRequest request, LocalDateTime receiptDate) throws Exception {
		try (PDDocument document = buildPdfDocument(request, null, receiptDate);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PDFRenderer renderer = new PDFRenderer(document);
			BufferedImage image = renderer.renderImageWithDPI(0, 300, ImageType.RGB);
			ImageIO.write(image, "JPEG", outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}

	private Receipt buildReceiptEntity(CreateReceiptRequest request, String receiptNumber, LocalDateTime currentTimestamp)
			throws Exception {
		Receipt receipt = new Receipt();
		receipt.setAprmtId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		receipt.setReceiptId(receiptNumber);
		receipt.setReceiptDate(currentTimestamp);
		receipt.setReceiptType(request != null ? request.getReceiptType() : null);
		receipt.setReceiptData(genericServices.toJson(request));
		receipt.setCreatTs(currentTimestamp);
		receipt.setCreatUsrId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		receipt.setLstUpdtTs(null);
		receipt.setLstUpdtUsrId(null);
		return receipt;
	}

	private ApartmentMaster resolveApartment(GenericHeader header) {
		if (header == null || !hasText(header.getApartmentId())) {
			return null;
		}
		Optional<ApartmentMaster> apartment = apartmentRepository.findById(header.getApartmentId());
		return apartment.orElse(null);
	}

	private void drawHeader(PdfCanvas canvas, CreateReceiptRequest request, ApartmentMaster apartment) throws Exception {
		String logo = resolveLogo(request != null ? request.getGenericHeader() : null, apartment);
		if (hasText(logo)) {
			canvas.drawCenteredImage(logo, RECEIPT_LOGO_MAX_WIDTH, RECEIPT_LOGO_MAX_HEIGHT);
			canvas.addGap(HEADER_BASELINE_GAP);
		}
		canvas.drawCenteredText(defaultValue(apartment != null ? apartment.getAprmntName() : request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentName() : null), canvas.getBoldFont(), TITLE_FONT_SIZE);
		List<String> addressLines = canvas.wrapText(defaultValue(resolveApartmentAddress(apartment)), canvas.getFont(), TEXT_FONT_SIZE,
				canvas.getUsableWidth());
		if (addressLines.isEmpty()) {
			addressLines = Collections.singletonList("");
		}
		for (String line : addressLines) {
			canvas.drawCenteredText(line, canvas.getFont(), TEXT_FONT_SIZE);
		}
		canvas.addGap(HEADER_ADDRESS_TO_RECEIPT_GAP);
		canvas.drawCenteredUnderlinedText("RECEIPT", canvas.getBoldFont(), TITLE_FONT_SIZE, 2f);
		canvas.addGap(HEADER_RECEIPT_TO_META_GAP);
	}

	private String resolveApartmentAddress(ApartmentMaster apartment) {
		if (apartment == null || !hasText(apartment.getAprmntAddress())) {
			return null;
		}
		try {
			Address address = genericServices.fromJson(apartment.getAprmntAddress(), Address.class);
			return address != null ? address.toString() : apartment.getAprmntAddress();
		} catch (RuntimeException exception) {
			LOGGER.debug("Unable to deserialize apartment address for receipt. Falling back to stored value.");
			return apartment.getAprmntAddress();
		}
	}

	private void drawMetaTable(PdfCanvas canvas, CreateReceiptRequest request, String receiptNumber, LocalDateTime receiptDate)
			throws Exception {
		float usableWidth = canvas.getUsableWidth();
		canvas.drawTableRow(
				new String[] { "Receipt Type :", defaultValue(request != null ? request.getReceiptType() : null), "Date :",
						receiptDate != null ? receiptDate.toLocalDate().format(RECEIPT_DATE_FORMATTER) : "" },
				new float[] { usableWidth * 0.20f, usableWidth * 0.34f, usableWidth * 0.12f, usableWidth * 0.34f },
				new boolean[] { true, false, true, false });
		canvas.drawTableRow(new String[] { "Receipt Number :", defaultValue(receiptNumber) }, new float[] { usableWidth * 0.20f, usableWidth * 0.80f },
				new boolean[] { true, false });
		canvas.drawTableRow(new String[] { "Transaction Id :", defaultValue(request != null ? request.getTransactionId() : null) },
				new float[] { usableWidth * 0.20f, usableWidth * 0.80f }, new boolean[] { true, false });
		canvas.drawSectionGap(SECTION_GAP);
	}

	private void drawItemsSection(PdfCanvas canvas, CreateReceiptRequest request) throws Exception {
		List<Items> items = request != null && request.getItems() != null ? request.getItems() : Collections.emptyList();
		boolean unitPriceRequired = request != null && request.isUnitPriceRequired();
		boolean quantityRequired = request != null && (request.isPerheadFlag()
				|| items.stream().filter(Objects::nonNull).anyMatch(item -> hasText(item.getQuantity())));
		String quantityHeader = request != null && request.isPerheadFlag() ? "NO OF PERSON" : "QUANTITY";
		float usableWidth = canvas.getUsableWidth();
		float[] widths = unitPriceRequired
				? new float[] { usableWidth * 0.12f, usableWidth * 0.34f, usableWidth * 0.17f, usableWidth * 0.17f, usableWidth * 0.20f }
				: quantityRequired ? new float[] { usableWidth * 0.12f, usableWidth * 0.44f, usableWidth * 0.22f, usableWidth * 0.22f }
						: new float[] { usableWidth * 0.12f, usableWidth * 0.58f, usableWidth * 0.30f };
		String[] headers = unitPriceRequired
				? new String[] { "Sl No", "ITEM NAME", "UNIT PRICE", quantityHeader, "AMOUNT" }
				: quantityRequired ? new String[] { "Sl No", "ITEM NAME", quantityHeader, "AMOUNT" }
						: new String[] { "Sl No", "ITEM NAME", "AMOUNT" };
		canvas.drawTableRow(headers, widths, true);
		for (int index = 0; index < items.size(); index++) {
			Items item = items.get(index);
			String[] values = unitPriceRequired
					? new String[] { String.valueOf(index + 1), defaultValue(item != null ? item.getItemName() : null),
							formatCurrency(item != null ? item.getUnitPrice() : null), defaultValue(item != null ? item.getQuantity() : null),
							formatCurrency(item != null ? item.getAmount() : null) }
					: quantityRequired
							? new String[] { String.valueOf(index + 1), defaultValue(item != null ? item.getItemName() : null),
									defaultValue(item != null ? item.getQuantity() : null), formatCurrency(item != null ? item.getAmount() : null) }
							: new String[] { String.valueOf(index + 1), defaultValue(item != null ? item.getItemName() : null),
									formatCurrency(item != null ? item.getAmount() : null) };
			canvas.drawTableRow(values, widths, false);
		}
		canvas.drawSectionGap(SECTION_GAP);
	}

	private void drawAddedChargesSection(PdfCanvas canvas, List<AddedCharges> addedCharges) throws Exception {
		if (addedCharges == null || addedCharges.isEmpty()) {
			return;
		}
		canvas.drawSectionTitle("Taxes And Other Charges");
		float usableWidth = canvas.getUsableWidth();
		float[] widths = new float[] { usableWidth * 0.12f, usableWidth * 0.58f, usableWidth * 0.30f };
		canvas.drawTableRow(new String[] { "Sl No", "Added Charges", "Amount" }, widths, true);
		for (int index = 0; index < addedCharges.size(); index++) {
			AddedCharges charge = addedCharges.get(index);
			canvas.drawTableRow(new String[] { String.valueOf(index + 1), defaultValue(charge != null ? charge.getChargeName() : null),
					formatAmountWithPercentage(charge != null ? charge.getFinalChargeValue() : null, charge != null ? charge.getChargeType() : null,
							charge != null ? charge.getValue() : null) }, widths, false);
		}
		canvas.drawSectionGap(SECTION_GAP);
	}

	private void drawDiscountFineSection(PdfCanvas canvas, DiscFinReceipt discFinReceipt) throws Exception {
		List<String[]> rows = new ArrayList<>();
		if (discFinReceipt != null && isNonZeroAmount(discFinReceipt.getDiscountAmount())) {
			rows.add(new String[] { appendCodeLabel("Discount", discFinReceipt.getDiscountCode()),
					formatAmountWithPercentage(discFinReceipt.getDiscountAmount(), discFinReceipt.getDiscountType(),
							discFinReceipt.getDiscountPercentage()) });
		}
		if (discFinReceipt != null && isNonZeroAmount(discFinReceipt.getFineAmount())) {
			String fineLabel = "Fine";
			if (hasText(discFinReceipt.getFineCycleMode()) && "cumulative".equalsIgnoreCase(discFinReceipt.getFineCycleMode().trim())) {
				fineLabel = "Fine (" + discFinReceipt.getFineCycleMode().trim() + ")";
			}
			rows.add(new String[] { appendCodeLabel(fineLabel, discFinReceipt.getFineCode()),
					formatAmountWithPercentage(discFinReceipt.getFineAmount(), discFinReceipt.getFineType(),
							discFinReceipt.getFinePercentage()) });
		}
		if (rows.isEmpty()) {
			return;
		}
		canvas.drawSectionTitle("Discount / Fine");
		float usableWidth = canvas.getUsableWidth();
		float[] widths = new float[] { usableWidth * 0.12f, usableWidth * 0.58f, usableWidth * 0.30f };
		canvas.drawTableRow(new String[] { "Sl No", "Description", "Amount" }, widths, true);
		for (int index = 0; index < rows.size(); index++) {
			String[] row = rows.get(index);
			canvas.drawTableRow(new String[] { String.valueOf(index + 1), row[0], row[1] }, widths, false);
		}
		canvas.drawSectionGap(SECTION_GAP);
	}

	private void drawTenderDetailsSection(PdfCanvas canvas, List<PaymentTenderData> tenderList) throws Exception {
		if (tenderList == null || tenderList.isEmpty()) {
			return;
		}
		canvas.drawSectionTitle("Tender Details");
		float usableWidth = canvas.getUsableWidth();
		float[] widths = new float[] { usableWidth * 0.12f, usableWidth * 0.58f, usableWidth * 0.30f };
		canvas.drawTableRow(new String[] { "Sl No", "Description", "Amount" }, widths, true);
		for (int index = 0; index < tenderList.size(); index++) {
			PaymentTenderData tenderData = tenderList.get(index);
			canvas.drawTableRow(new String[] { String.valueOf(index + 1),
					defaultValue(tenderData != null ? tenderData.getTenderName() : null),
					formatCurrency(tenderData != null ? tenderData.getAmountPaid() : null) }, widths, false);
		}
		canvas.drawSectionGap(SECTION_GAP);
	}

	private void drawTotal(PdfCanvas canvas, String totalAmount) throws Exception {
		float usableWidth = canvas.getUsableWidth();
		canvas.drawTableRow(new String[] { "Total Amount Paid (After Rounding)", formatCurrency(totalAmount) },
				new float[] { usableWidth * 0.70f, usableWidth * 0.30f }, true);
		canvas.addGap(SECTION_GAP);
	}

	private void drawRemarks(PdfCanvas canvas, String remarks) throws Exception {
		if (!hasText(remarks)) {
			return;
		}
		canvas.addGap(LINE_HEIGHT);
		canvas.drawTextBlock("Remarks", canvas.getBoldFont(), TEXT_FONT_SIZE);
		canvas.drawTextBlock(remarks.trim(), canvas.getFont(), SMALL_FONT_SIZE);
	}

	private void drawElectronicReceiptNote(PdfCanvas canvas) throws Exception {
		canvas.addGap(LINE_HEIGHT * 3);
		canvas.drawTextBlock(ELECTRONIC_RECEIPT_NOTE, canvas.getFont(), SMALL_FONT_SIZE);
	}

	private String resolveLogo(GenericHeader header, ApartmentMaster apartment) {
		if (apartment != null && hasText(apartment.getAprmnt_logo())) {
			return apartment.getAprmnt_logo();
		}
		if (header != null && hasText(header.getProfilepic())) {
			return header.getProfilepic();
		}
		return null;
	}

	private String defaultValue(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private boolean isNonZeroAmount(String value) {
		if (!hasText(value)) {
			return false;
		}
		try {
			return new BigDecimal(value.trim()).compareTo(BigDecimal.ZERO) != 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String formatAmountWithPercentage(String amount, String type, String percentage) {
		String formattedAmount = formatCurrency(amount);
		if (!hasText(formattedAmount)) {
			return formattedAmount;
		}
		if (hasText(type) && "percentage".equalsIgnoreCase(type.trim()) && hasText(percentage)) {
			return formattedAmount + " (" + percentage.trim() + "%)";
		}
		return formattedAmount;
	}

	private String appendCodeLabel(String label, String code) {
		if (!hasText(code)) {
			return label;
		}
		return label + " (CODE: " + code.trim() + ")";
	}

	private String formatCurrency(String amount) {
		String formattedAmount = defaultValue(amount);
		if (!hasText(formattedAmount)) {
			return formattedAmount;
		}
		if (formattedAmount.startsWith(RUPEE_SYMBOL)) {
			return formattedAmount;
		}
		return RUPEE_SYMBOL + " " + formattedAmount;
	}

	private static PDFont loadFont(PDDocument document, String[] candidates, PDFont fallback) {
		for (String candidate : candidates) {
			Path path = Path.of(candidate);
			if (!Files.isRegularFile(path)) {
				continue;
			}
			try (InputStream stream = Files.newInputStream(path)) {
				return PDType0Font.load(document, stream);
			} catch (IOException ex) {
				LOGGER.debug("Unable to load receipt font from {}", candidate, ex);
			}
		}
		LOGGER.warn("Falling back to built-in PDF font; rupee symbol rendering may be limited");
		return fallback;
	}

	private String generateReceiptNumber() {
		long candidate = Instant.now().toEpochMilli() % 10_000_000_000L;
		long next = LAST_RECEIPT_NUMBER.updateAndGet(previous -> candidate > previous ? candidate : previous + 1);
		return String.format(Locale.ENGLISH, "%010d", next % 10_000_000_000L);
	}

	private static final class PdfCanvas {

		private final PDDocument document;
		private final PDFont font;
		private final PDFont boldFont;
		private PDPage page;
		private PDPageContentStream stream;
		private float y;

		private PdfCanvas(PDDocument document) throws IOException {
			this.document = document;
			this.font = loadFont(document, REGULAR_FONT_PATHS, FALLBACK_FONT);
			this.boldFont = loadFont(document, BOLD_FONT_PATHS, FALLBACK_BOLD_FONT);
			newPage();
		}

		private PDFont getFont() {
			return font;
		}

		private PDFont getBoldFont() {
			return boldFont;
		}

		private float getUsableWidth() {
			return page.getMediaBox().getWidth() - LEFT_MARGIN - RIGHT_MARGIN;
		}

		private void addGap(float gap) {
			y -= gap;
		}

		private void drawSectionGap(float gap) throws IOException {
			if (gap <= 0f) {
				return;
			}
			ensureSpace(gap);
			float rightX = LEFT_MARGIN + getUsableWidth();
			stream.moveTo(LEFT_MARGIN, y);
			stream.lineTo(LEFT_MARGIN, y - gap);
			stream.moveTo(rightX, y);
			stream.lineTo(rightX, y - gap);
			stream.stroke();
			y -= gap;
		}

		private void ensureSpace(float requiredHeight) throws IOException {
			if (y - requiredHeight < BOTTOM_MARGIN) {
				newPage();
			}
		}

		private void newPage() throws IOException {
			if (stream != null) {
				stream.close();
			}
			page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			stream = new PDPageContentStream(document, page);
			stream.setLineWidth(BORDER_LINE_WIDTH);
			stream.setLineJoinStyle(LINE_JOIN_BEVEL);
			stream.setLineCapStyle(LINE_CAP_BUTT);
			y = TOP_MARGIN;
		}

		private void close() throws IOException {
			if (stream != null) {
				stream.close();
			}
		}

		private void drawCenteredImage(String imageValue, float maxWidth, float maxHeight) throws IOException {
			byte[] imageBytes = decodeImage(imageValue);
			if (imageBytes == null || imageBytes.length == 0) {
				return;
			}
			ensureSpace(maxHeight + 10f);
			BufferedImage bufferedImage;
			try {
				bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
				if (bufferedImage == null) {
					return;
				}
			} catch (IOException exception) {
				LOGGER.debug("Failed to read image data for receipt logo.", exception);
				return;
			}
			PDImageXObject image;
			try {
				image = LosslessFactory.createFromImage(document, bufferedImage);
			} catch (IOException | RuntimeException exception) {
				LOGGER.debug("Failed to create PDImageXObject from BufferedImage for receipt logo.", exception);
				return;
			}
			float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
			float width = image.getWidth() * scale;
			float height = image.getHeight() * scale;
			float x = LEFT_MARGIN + (getUsableWidth() - width) / 2;
			stream.drawImage(image, x, y - height, width, height);
			y -= height + 8f;
		}

		private void drawCenteredText(String text, PDFont font, float fontSize) throws IOException {
			ensureSpace(fontSize + 6f);
			float textWidth = font.getStringWidth(text) / 1000f * fontSize;
			float x = LEFT_MARGIN + Math.max(0f, (getUsableWidth() - textWidth) / 2);
			drawText(text, x, y, font, fontSize);
			y -= fontSize + CENTERED_TEXT_BOTTOM_PADDING;
		}

		private void drawCenteredUnderlinedText(String text, PDFont font, float fontSize, float underlineOffset) throws IOException {
			ensureSpace(fontSize + 10f);
			float textWidth = font.getStringWidth(text) / 1000f * fontSize;
			float x = LEFT_MARGIN + Math.max(0f, (getUsableWidth() - textWidth) / 2);
			drawText(text, x, y, font, fontSize);
			float underlineY = y - underlineOffset;
			stream.moveTo(x, underlineY);
			stream.lineTo(x + textWidth, underlineY);
			stream.stroke();
			y -= fontSize + UNDERLINED_TEXT_BOTTOM_PADDING;
		}

		private void drawSectionTitle(String title) throws IOException {
			ensureSpace(SECTION_TITLE_HEIGHT + 6f);
			stream.addRect(LEFT_MARGIN, y - SECTION_TITLE_HEIGHT, getUsableWidth(), SECTION_TITLE_HEIGHT);
			stream.stroke();
			float textWidth = boldFont.getStringWidth(title) / 1000f * TEXT_FONT_SIZE;
			float x = LEFT_MARGIN + Math.max(0f, (getUsableWidth() - textWidth) / 2);
			drawText(title, x, y - 13f, boldFont, TEXT_FONT_SIZE);
			y -= SECTION_TITLE_HEIGHT;
		}

		private void drawTableRow(String[] values, float[] widths, boolean bold) throws IOException {
			boolean[] boldFlags = new boolean[values.length];
			for (int index = 0; index < boldFlags.length; index++) {
				boldFlags[index] = bold;
			}
			drawTableRow(values, widths, boldFlags);
		}

		private void drawTableRow(String[] values, float[] widths, boolean[] boldFlags) throws IOException {
			List<List<String>> wrappedValues = new ArrayList<>();
			int maxLines = 1;
			for (int index = 0; index < values.length; index++) {
				List<String> lines = wrapText(values[index], boldFlags[index] ? boldFont : font, SMALL_FONT_SIZE, widths[index] - (CELL_PADDING * 2));
				if (lines.isEmpty()) {
					lines = Collections.singletonList("");
				}
				wrappedValues.add(lines);
				maxLines = Math.max(maxLines, lines.size());
			}
			float rowHeight = (maxLines * LINE_HEIGHT) + (CELL_PADDING * 2);
			ensureSpace(rowHeight + 2f);
			float currentX = LEFT_MARGIN;
			for (int index = 0; index < widths.length; index++) {
				stream.addRect(currentX, y - rowHeight, widths[index], rowHeight);
				currentX += widths[index];
			}
			stream.stroke();

			currentX = LEFT_MARGIN;
			for (int index = 0; index < widths.length; index++) {
				float textY = y - CELL_PADDING - SMALL_FONT_SIZE;
				for (String line : wrappedValues.get(index)) {
					drawText(line, currentX + CELL_PADDING, textY, boldFlags[index] ? boldFont : font, SMALL_FONT_SIZE);
					textY -= LINE_HEIGHT;
				}
				currentX += widths[index];
			}
			y -= rowHeight;
		}

		private void drawTextBlock(String text, PDFont font, float fontSize) throws IOException {
			List<String> lines = wrapText(text, font, fontSize, getUsableWidth());
			if (lines.isEmpty()) {
				return;
			}
			ensureSpace((lines.size() * LINE_HEIGHT) + CELL_PADDING);
			float textY = y - fontSize;
			for (String line : lines) {
				drawText(line, LEFT_MARGIN, textY, font, fontSize);
				textY -= LINE_HEIGHT;
			}
			y = textY;
		}

		private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
			if (text == null) {
				return Collections.emptyList();
			}
			String normalized = text.replace("\r", " ").trim();
			if (normalized.isEmpty()) {
				return Collections.singletonList("");
			}
			List<String> lines = new ArrayList<>();
			for (String paragraph : normalized.split("\n")) {
				String[] words = paragraph.trim().split("\\s+");
				StringBuilder line = new StringBuilder();
				for (String word : words) {
					String candidate = line.length() == 0 ? word : line + " " + word;
					float width = font.getStringWidth(candidate) / 1000f * fontSize;
					if (width <= maxWidth || line.length() == 0) {
						line.setLength(0);
						line.append(candidate);
					} else {
						lines.add(line.toString());
						line.setLength(0);
						line.append(word);
					}
				}
				if (line.length() > 0) {
					lines.add(line.toString());
				}
			}
			return lines;
		}

		private void drawText(String text, float x, float yPosition, PDFont font, float fontSize) throws IOException {
			stream.beginText();
			stream.setFont(font, fontSize);
			stream.newLineAtOffset(x, yPosition);
			stream.showText(text == null ? "" : text);
			stream.endText();
		}

		private byte[] decodeImage(String value) {
			if (value == null) {
				return null;
			}
			String base64 = value;
			int commaIndex = base64.indexOf(',');
			if (commaIndex >= 0) {
				base64 = base64.substring(commaIndex + 1);
			}
			try {
				return Base64.getDecoder().decode(base64.trim());
			} catch (IllegalArgumentException ex) {
				return null;
			}
		}
	}
}
