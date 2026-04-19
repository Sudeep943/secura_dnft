package com.secura.dnft.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.ReceiptInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.Items;

@Service
public class ReceiptServices implements ReceiptInterface {

	private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private static final PDFont BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
	private static final float TITLE_FONT_SIZE = 12f;
	private static final float TEXT_FONT_SIZE = 10f;
	private static final float SMALL_FONT_SIZE = 9f;
	private static final float TOP_MARGIN = 780f;
	private static final float BOTTOM_MARGIN = 50f;
	private static final float LEFT_MARGIN = 40f;
	private static final float RIGHT_MARGIN = 40f;
	private static final float LINE_HEIGHT = 12f;
	private static final float CELL_PADDING = 4f;
	private static final float SECTION_GAP = 12f;
	private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);
	private static final AtomicLong LAST_RECEIPT_NUMBER = new AtomicLong();

	@Autowired
	private ApartmentRepository apartmentRepository;

	@Override
	public CreateReceiptResponse createReceipt(CreateReceiptRequest request) throws Exception {
		CreateReceiptResponse response = new CreateReceiptResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setReceipt(buildReceiptBase64(request));
		response.setMessage(SuccessMessage.SUCC_MESSAGE_34);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_34);
		return response;
	}

	private String buildReceiptBase64(CreateReceiptRequest request) throws Exception {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PdfCanvas canvas = new PdfCanvas(document);
			ApartmentMaster apartment = resolveApartment(request != null ? request.getGenericHeader() : null);
			drawHeader(canvas, request, apartment);
			drawMetaTable(canvas, request);
			drawItemsSection(canvas, request);
			drawAddedChargesSection(canvas, request != null ? request.getAddedCharges() : null);
			drawDiscountFineSection(canvas, request != null ? request.getDiscFinReceipt() : null);
			drawTotal(canvas, request != null ? request.getTotalAmount() : null);
			drawRemarks(canvas, request != null ? request.getRemarks() : null);
			canvas.close();
			document.save(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
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
			canvas.drawCenteredImage(logo, 110f, 55f);
		}
		canvas.drawCenteredText(defaultValue(apartment != null ? apartment.getAprmntName() : request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentName() : null), BOLD_FONT, TITLE_FONT_SIZE);
		List<String> addressLines = canvas.wrapText(defaultValue(apartment != null ? apartment.getAprmntAddress() : null), FONT, TEXT_FONT_SIZE, canvas.getUsableWidth());
		if (addressLines.isEmpty()) {
			addressLines = Collections.singletonList("");
		}
		for (String line : addressLines) {
			canvas.drawCenteredText(line, FONT, TEXT_FONT_SIZE);
		}
		canvas.addGap(SECTION_GAP);
	}

	private void drawMetaTable(PdfCanvas canvas, CreateReceiptRequest request) throws Exception {
		float[] widths = new float[] { canvas.getUsableWidth() / 2, canvas.getUsableWidth() / 2 };
		canvas.drawTableRow(new String[] { "Receipt Type", "Date" }, widths, true);
		canvas.drawTableRow(new String[] { defaultValue(request != null ? request.getReceiptType() : null),
				LocalDate.now().format(RECEIPT_DATE_FORMATTER) }, widths, false);
		canvas.drawTableRow(new String[] { "Receipt Number", "" }, widths, true);
		canvas.drawTableRow(new String[] { generateReceiptNumber(), "" }, widths, false);
		canvas.addGap(SECTION_GAP);
	}

	private void drawItemsSection(PdfCanvas canvas, CreateReceiptRequest request) throws Exception {
		boolean unitPriceRequired = request != null && request.isUnitPriceRequired();
		String quantityHeader = request != null && request.isPerheadFlag() ? "NO OF PERSON" : "QUANTITY";
		float usableWidth = canvas.getUsableWidth();
		float[] widths = unitPriceRequired ? new float[] { usableWidth * 0.12f, usableWidth * 0.34f, usableWidth * 0.17f, usableWidth * 0.17f, usableWidth * 0.20f }
				: new float[] { usableWidth * 0.12f, usableWidth * 0.44f, usableWidth * 0.22f, usableWidth * 0.22f };
		String[] headers = unitPriceRequired ? new String[] { "Sl No", "ITEM NAME", "UNIT PRICE", quantityHeader, "AMOUNT" }
				: new String[] { "Sl No", "ITEM NAME", quantityHeader, "AMOUNT" };
		canvas.drawTableRow(headers, widths, true);
		List<Items> items = request != null && request.getItems() != null ? request.getItems() : Collections.emptyList();
		for (int index = 0; index < items.size(); index++) {
			Items item = items.get(index);
			String[] values = unitPriceRequired
					? new String[] { String.valueOf(index + 1), defaultValue(item != null ? item.getItemName() : null),
							defaultValue(item != null ? item.getUnitPrice() : null), defaultValue(item != null ? item.getQuantity() : null),
							defaultValue(item != null ? item.getAmount() : null) }
					: new String[] { String.valueOf(index + 1), defaultValue(item != null ? item.getItemName() : null),
							defaultValue(item != null ? item.getQuantity() : null), defaultValue(item != null ? item.getAmount() : null) };
			canvas.drawTableRow(values, widths, false);
		}
		canvas.addGap(SECTION_GAP);
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
		canvas.addGap(SECTION_GAP);
	}

	private void drawDiscountFineSection(PdfCanvas canvas, DiscFinReceipt discFinReceipt) throws Exception {
		List<String[]> rows = new ArrayList<>();
		if (discFinReceipt != null && hasText(discFinReceipt.getDiscountAmount())) {
			rows.add(new String[] { appendCodeLabel("Discount", discFinReceipt.getDiscountCode()),
					formatAmountWithPercentage(discFinReceipt.getDiscountAmount(), discFinReceipt.getDiscountType(),
							discFinReceipt.getDiscountPercentage()) });
		}
		if (discFinReceipt != null && hasText(discFinReceipt.getFineAmount())) {
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
		canvas.addGap(SECTION_GAP);
	}

	private void drawTotal(PdfCanvas canvas, String totalAmount) throws Exception {
		float usableWidth = canvas.getUsableWidth();
		canvas.drawTableRow(new String[] { "Total", defaultValue(totalAmount) }, new float[] { usableWidth * 0.70f, usableWidth * 0.30f }, true);
		canvas.addGap(SECTION_GAP);
	}

	private void drawRemarks(PdfCanvas canvas, String remarks) throws Exception {
		if (!hasText(remarks)) {
			return;
		}
		canvas.drawSectionTitle("Remarks");
		float width = canvas.getUsableWidth();
		canvas.drawTableRow(new String[] { remarks.trim() }, new float[] { width }, false);
	}

	private String resolveLogo(GenericHeader header, ApartmentMaster apartment) {
		if (header != null && hasText(header.getProfilepic())) {
			return header.getProfilepic();
		}
		if (apartment != null && hasText(apartment.getAprmnt_logo())) {
			return apartment.getAprmnt_logo();
		}
		return null;
	}

	private String defaultValue(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String formatAmountWithPercentage(String amount, String type, String percentage) {
		String formattedAmount = defaultValue(amount);
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

	private String generateReceiptNumber() {
		long candidate = Instant.now().toEpochMilli() % 10_000_000_000L;
		long next = LAST_RECEIPT_NUMBER.updateAndGet(previous -> candidate > previous ? candidate : previous + 1);
		return String.format(Locale.ENGLISH, "%010d", next % 10_000_000_000L);
	}

	private static final class PdfCanvas {

		private final PDDocument document;
		private PDPage page;
		private PDPageContentStream stream;
		private float y;

		private PdfCanvas(PDDocument document) throws IOException {
			this.document = document;
			newPage();
		}

		private float getUsableWidth() {
			return page.getMediaBox().getWidth() - LEFT_MARGIN - RIGHT_MARGIN;
		}

		private void addGap(float gap) {
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
			if (ImageIO.read(new ByteArrayInputStream(imageBytes)) == null) {
				return;
			}
			PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "receipt-logo");
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
			y -= fontSize + 4f;
		}

		private void drawSectionTitle(String title) throws IOException {
			ensureSpace(24f);
			stream.addRect(LEFT_MARGIN, y - 18f, getUsableWidth(), 18f);
			stream.stroke();
			float textWidth = BOLD_FONT.getStringWidth(title) / 1000f * TEXT_FONT_SIZE;
			float x = LEFT_MARGIN + Math.max(0f, (getUsableWidth() - textWidth) / 2);
			drawText(title, x, y - 13f, BOLD_FONT, TEXT_FONT_SIZE);
			y -= 18f;
		}

		private void drawTableRow(String[] values, float[] widths, boolean bold) throws IOException {
			List<List<String>> wrappedValues = new ArrayList<>();
			int maxLines = 1;
			for (int index = 0; index < values.length; index++) {
				List<String> lines = wrapText(values[index], bold ? BOLD_FONT : FONT, SMALL_FONT_SIZE, widths[index] - (CELL_PADDING * 2));
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
					drawText(line, currentX + CELL_PADDING, textY, bold ? BOLD_FONT : FONT, SMALL_FONT_SIZE);
					textY -= LINE_HEIGHT;
				}
				currentX += widths[index];
			}
			y -= rowHeight;
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
