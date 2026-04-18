package com.secura.dnft.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.ReceiptInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.Items;
import com.secura.dnft.request.response.Receipt;

@Service
public class ReceiptServices implements ReceiptInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptServices.class);

	private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yyyy",
			Locale.ENGLISH);
	private static final long RECEIPT_NUMBER_MIN = 1_000_000_000L;
	private static final long RECEIPT_NUMBER_MODULUS = 10_000_000_000L;
	private static final AtomicLong RECEIPT_SEQUENCE = new AtomicLong(
			(System.currentTimeMillis() % 9_000_000_000L) + RECEIPT_NUMBER_MIN);
	private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
	private static final float PAGE_MARGIN = 40f;
	private static final float ROW_PADDING = 4f;
	private static final float TEXT_FONT_SIZE = 10f;

	@Override
	public CreateReceiptResponse createReceipt(CreateReceiptRequest request) throws Exception {
		CreateReceiptResponse response = new CreateReceiptResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		String receiptNumber = generateReceiptNumber();
		String receiptDate = LocalDate.now().format(RECEIPT_DATE_FORMAT);
		byte[] pdfBytes = buildReceiptPdf(request, receiptNumber, receiptDate);

		Receipt receipt = new Receipt();
		receipt.setReceiptNumber(receiptNumber);
		receipt.setReceiptType(request != null ? request.getReceiptType() : null);
		receipt.setReceiptDate(receiptDate);
		receipt.setReceiptFileType("application/pdf");
		receipt.setReceiptFileName("receipt-" + receiptNumber + ".pdf");
		receipt.setReceiptFileData(Base64.getEncoder().encodeToString(pdfBytes));

		response.setReceipt(receipt);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_34);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_34);
		return response;
	}

	private byte[] buildReceiptPdf(CreateReceiptRequest request, String receiptNumber, String receiptDate) throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PdfCanvas canvas = new PdfCanvas(document);
			drawHeader(canvas, request, receiptNumber, receiptDate);
			drawItemsTable(canvas, request);
			drawAddedChargesTable(canvas, request);
			drawDiscountFineTable(canvas, request);
			drawTotalSection(canvas, request);
			drawRemarksSection(canvas, request != null ? request.getRemarks() : null);
			canvas.close();
			document.save(outputStream);
			return outputStream.toByteArray();
		}
	}

	private void drawHeader(PdfCanvas canvas, CreateReceiptRequest request, String receiptNumber, String receiptDate)
			throws IOException {
		GenericHeader header = request != null ? request.getGenericHeader() : null;
		byte[] logoBytes = extractImageBytes(header != null ? header.getProfilepic() : null);
		if (logoBytes != null) {
			try {
				PDImageXObject logo = PDImageXObject.createFromByteArray(canvas.document, logoBytes, "receipt-logo");
				canvas.contentStream.drawImage(logo, PAGE_MARGIN, canvas.y - 48, 48, 48);
			} catch (IOException e) {
				LOGGER.warn("Unable to render receipt logo", e);
			}
		}

		float textStartX = PAGE_MARGIN + 60;
		canvas.drawText(valueOrFallback(header != null ? header.getApartmentName() : null, "Society Receipt"), textStartX,
				canvas.y, FONT_BOLD, 16);
		canvas.drawText("RECEIPT", canvas.pageWidth - PAGE_MARGIN - 75, canvas.y, FONT_BOLD, 14);
		canvas.y -= 18;
		canvas.drawText(valueOrEmpty(header != null ? header.getProfileName() : null), textStartX, canvas.y, FONT_REGULAR,
				11);
		canvas.y -= 10;
		canvas.drawText(valueOrEmpty(header != null ? header.getFlatNo() : null), textStartX, canvas.y, FONT_REGULAR, 11);
		canvas.y -= 22;

		canvas.drawLabelValue("Receipt Type", request != null ? request.getReceiptType() : null, PAGE_MARGIN, canvas.y, 220);
		canvas.drawLabelValue("Date", receiptDate, PAGE_MARGIN + 260, canvas.y, 120);
		canvas.y -= 16;
		canvas.drawLabelValue("Receipt Number", receiptNumber, PAGE_MARGIN, canvas.y, 220);
		canvas.drawLabelValue("Issued To", header != null ? header.getProfileName() : null, PAGE_MARGIN + 260, canvas.y, 180);
		canvas.y -= 20;
		canvas.drawLine(PAGE_MARGIN, canvas.y, canvas.pageWidth - PAGE_MARGIN, canvas.y);
		canvas.y -= 16;
	}

	private void drawItemsTable(PdfCanvas canvas, CreateReceiptRequest request) throws IOException {
		List<Items> items = request != null && request.getItems() != null ? request.getItems() : Collections.emptyList();
		List<String> headers = new ArrayList<>();
		headers.add("SL NO");
		headers.add("ITEM NAME");
		headers.add(request != null && request.isPerheadFlag() ? "NO OF PERSON" : "QUANTITY");
		if (request != null && request.isUnitPriceRequired()) {
			headers.add("UNIT PRICE");
		}
		headers.add("AMOUNT");

		float[] widths = request != null && request.isUnitPriceRequired() ? new float[] { 45f, 200f, 90f, 90f, 90f }
				: new float[] { 45f, 250f, 110f, 110f };

		List<List<String>> rows = new ArrayList<>();
		for (int index = 0; index < items.size(); index++) {
			Items item = items.get(index);
			List<String> row = new ArrayList<>();
			row.add(String.valueOf(index + 1));
			row.add(valueOrEmpty(item.getItemName()));
			row.add(valueOrEmpty(item.getQuantity()));
			if (request != null && request.isUnitPriceRequired()) {
				row.add(valueOrEmpty(item.getUnitPrice()));
			}
			row.add(valueOrEmpty(item.getAmount()));
			rows.add(row);
		}
		canvas.drawTable("Items", headers, widths, rows);
	}

	private void drawAddedChargesTable(PdfCanvas canvas, CreateReceiptRequest request) throws IOException {
		List<AddedCharges> charges = request != null && request.getAddedCharges() != null ? request.getAddedCharges()
				: Collections.emptyList();
		if (charges.isEmpty()) {
			return;
		}
		List<List<String>> rows = new ArrayList<>();
		for (int index = 0; index < charges.size(); index++) {
			AddedCharges charge = charges.get(index);
			List<String> row = new ArrayList<>();
			row.add(String.valueOf(index + 1));
			row.add(valueOrEmpty(charge.getChargeName()));
			row.add(formatAmountWithPercentage(charge.getFinalChargeValue(), charge.getChargeType(), charge.getValue()));
			rows.add(row);
		}
		canvas.drawTable("Taxes And Other Charges", List.of("SL NO", "ADDED CHARGES", "AMOUNT"),
				new float[] { 45f, 300f, 170f }, rows);
	}

	private void drawDiscountFineTable(PdfCanvas canvas, CreateReceiptRequest request) throws IOException {
		DiscFinReceipt discFinReceipt = request != null ? request.getDiscFinReceipt() : null;
		if (discFinReceipt == null) {
			return;
		}
		List<List<String>> rows = new ArrayList<>();
		int index = 1;
		if (hasText(discFinReceipt.getDiscountAmount())) {
			rows.add(List.of(String.valueOf(index++), "Discount", formatAmountWithPercentage(discFinReceipt.getDiscountAmount(),
					discFinReceipt.getDiscountType(), discFinReceipt.getDiscountPercentage())));
		}
		if (hasText(discFinReceipt.getFineAmount())) {
			String label = "Fine";
			if (hasText(discFinReceipt.getFineCycleMode())
					&& "cumulative".equalsIgnoreCase(discFinReceipt.getFineCycleMode().trim())) {
				label = "Fine (" + discFinReceipt.getFineCycleMode().trim() + ")";
			}
			rows.add(List.of(String.valueOf(index), label,
					formatAmountWithPercentage(discFinReceipt.getFineAmount(), discFinReceipt.getFineType(),
							discFinReceipt.getFinePercentage())));
		}
		if (!rows.isEmpty()) {
			canvas.drawTable("Discount / Fine", List.of("SL NO", "DESCRIPTION", "AMOUNT"),
					new float[] { 45f, 300f, 170f }, rows);
		}
	}

	private void drawTotalSection(PdfCanvas canvas, CreateReceiptRequest request) throws IOException {
		canvas.ensureSpace(36f);
		float boxX = canvas.pageWidth - PAGE_MARGIN - 220f;
		float boxY = canvas.y - 24f;
		canvas.contentStream.setNonStrokingColor(new Color(240, 240, 240));
		canvas.contentStream.addRect(boxX, boxY, 220f, 24f);
		canvas.contentStream.fill();
		canvas.contentStream.setNonStrokingColor(Color.BLACK);
		canvas.contentStream.addRect(boxX, boxY, 220f, 24f);
		canvas.contentStream.stroke();
		canvas.drawText("Total", boxX + 8f, canvas.y - 16f, FONT_BOLD, 11f);
		canvas.drawRightAlignedText(valueOrEmpty(request != null ? request.getTotalAmount() : null), boxX + 212f,
				canvas.y - 16f, FONT_BOLD, 11f);
		canvas.y -= 40f;
	}

	private void drawRemarksSection(PdfCanvas canvas, String remarks) throws IOException {
		if (!hasText(remarks)) {
			return;
		}
		List<String> lines = canvas.wrapText(remarks.trim(), FONT_REGULAR, TEXT_FONT_SIZE, canvas.pageWidth - (PAGE_MARGIN * 2) - 16f);
		float height = Math.max(28f, (lines.size() * 12f) + 16f);
		canvas.ensureSpace(height + 18f);
		canvas.drawText("Remarks", PAGE_MARGIN, canvas.y, FONT_BOLD, 12f);
		canvas.y -= 8f;
		float boxY = canvas.y - height;
		canvas.contentStream.addRect(PAGE_MARGIN, boxY, canvas.pageWidth - (PAGE_MARGIN * 2), height);
		canvas.contentStream.stroke();
		float textY = canvas.y - 14f;
		for (String line : lines) {
			canvas.drawText(line, PAGE_MARGIN + 8f, textY, FONT_REGULAR, TEXT_FONT_SIZE);
			textY -= 12f;
		}
		canvas.y = boxY - 16f;
	}

	private byte[] extractImageBytes(String value) {
		if (!hasText(value)) {
			return null;
		}
		String base64Value = value.trim();
		int commaIndex = base64Value.indexOf(',');
		if (commaIndex >= 0) {
			base64Value = base64Value.substring(commaIndex + 1);
		}
		try {
			return Base64.getDecoder().decode(base64Value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private String generateReceiptNumber() {
		long receiptValue = RECEIPT_SEQUENCE.updateAndGet(current -> {
			long next = current + 1;
			if (next >= RECEIPT_NUMBER_MODULUS) {
				next = RECEIPT_NUMBER_MIN;
			}
			return next;
		});
		return String.format("%010d", receiptValue);
	}

	private String formatAmountWithPercentage(String amount, String type, String percentage) {
		String formattedAmount = valueOrEmpty(amount);
		if (hasText(type) && type.trim().equalsIgnoreCase("percentage") && hasText(percentage)) {
			return formattedAmount + " (" + sanitizeNumericDisplay(percentage) + "%)";
		}
		return formattedAmount;
	}

	private String sanitizeNumericDisplay(String value) {
		if (!hasText(value)) {
			return "";
		}
		try {
			return new BigDecimal(value.replace("%", "").trim()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
					.toPlainString();
		} catch (NumberFormatException e) {
			return value.trim().replace("%", "");
		}
	}

	private String valueOrFallback(String value, String fallback) {
		return hasText(value) ? value.trim() : fallback;
	}

	private String valueOrEmpty(String value) {
		return hasText(value) ? value.trim() : "";
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static final class PdfCanvas {
		private final PDDocument document;
		private PDPage page;
		private PDPageContentStream contentStream;
		private final float pageWidth;
		private float y;

		private PdfCanvas(PDDocument document) throws IOException {
			this.document = document;
			this.pageWidth = PDRectangle.A4.getWidth();
			addPage();
		}

		private void addPage() throws IOException {
			if (contentStream != null) {
				contentStream.close();
			}
			page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			contentStream = new PDPageContentStream(document, page);
			y = PDRectangle.A4.getHeight() - PAGE_MARGIN;
		}

		private void ensureSpace(float requiredHeight) throws IOException {
			if (y - requiredHeight < PAGE_MARGIN) {
				addPage();
			}
		}

		private void drawLabelValue(String label, String value, float x, float y, float labelWidth) throws IOException {
			drawText(label + ":", x, y, FONT_BOLD, TEXT_FONT_SIZE);
			drawText(valueOrEmpty(value), x + labelWidth, y, FONT_REGULAR, TEXT_FONT_SIZE);
		}

		private void drawText(String text, float x, float y, PDFont font, float fontSize) throws IOException {
			contentStream.beginText();
			contentStream.setFont(font, fontSize);
			contentStream.newLineAtOffset(x, y);
			contentStream.showText(text == null ? "" : text);
			contentStream.endText();
		}

		private void drawRightAlignedText(String text, float rightX, float y, PDFont font, float fontSize) throws IOException {
			float width = font.getStringWidth(text == null ? "" : text) / 1000 * fontSize;
			drawText(text, rightX - width, y, font, fontSize);
		}

		private void drawLine(float startX, float startY, float endX, float endY) throws IOException {
			contentStream.moveTo(startX, startY);
			contentStream.lineTo(endX, endY);
			contentStream.stroke();
		}

		private void drawTable(String title, List<String> headers, float[] widths, List<List<String>> rows) throws IOException {
			if (rows == null || rows.isEmpty()) {
				return;
			}
			boolean firstPageForSection = true;
			int index = 0;
			while (index < rows.size()) {
				if (firstPageForSection) {
					ensureSpace(42f);
					drawText(title, PAGE_MARGIN, y, FONT_BOLD, 12f);
					y -= 10f;
					drawTableHeader(headers, widths);
					firstPageForSection = false;
				}
				float rowHeight = calculateRowHeight(headers, widths, rows.get(index));
				if (y - rowHeight < PAGE_MARGIN) {
					addPage();
					drawText(title, PAGE_MARGIN, y, FONT_BOLD, 12f);
					y -= 10f;
					drawTableHeader(headers, widths);
				}
				drawTableRow(headers, widths, rows.get(index), rowHeight);
				index++;
			}
			y -= 14f;
		}

		private void drawTableHeader(List<String> headers, float[] widths) throws IOException {
			float rowHeight = 20f;
			float currentX = PAGE_MARGIN;
			float rowTop = y;
			contentStream.setNonStrokingColor(new Color(240, 240, 240));
			contentStream.addRect(PAGE_MARGIN, rowTop - rowHeight, totalWidth(widths), rowHeight);
			contentStream.fill();
			contentStream.setNonStrokingColor(Color.BLACK);
			for (int i = 0; i < headers.size(); i++) {
				contentStream.addRect(currentX, rowTop - rowHeight, widths[i], rowHeight);
				contentStream.stroke();
				drawText(headers.get(i), currentX + ROW_PADDING, rowTop - 14f, FONT_BOLD, 9f);
				currentX += widths[i];
			}
			y -= rowHeight;
		}

		private void drawTableRow(List<String> headers, float[] widths, List<String> row, float rowHeight) throws IOException {
			float currentX = PAGE_MARGIN;
			float rowTop = y;
			for (int i = 0; i < headers.size(); i++) {
				contentStream.addRect(currentX, rowTop - rowHeight, widths[i], rowHeight);
				contentStream.stroke();
				List<String> lines = wrapText(row.size() > i ? row.get(i) : "", FONT_REGULAR, 9f, widths[i] - (ROW_PADDING * 2));
				float textY = rowTop - 12f;
				for (String line : lines) {
					drawText(line, currentX + ROW_PADDING, textY, FONT_REGULAR, 9f);
					textY -= 11f;
				}
				currentX += widths[i];
			}
			y -= rowHeight;
		}

		private float calculateRowHeight(List<String> headers, float[] widths, List<String> row) throws IOException {
			int maxLines = 1;
			for (int i = 0; i < headers.size(); i++) {
				List<String> lines = wrapText(row.size() > i ? row.get(i) : "", FONT_REGULAR, 9f,
						widths[i] - (ROW_PADDING * 2));
				maxLines = Math.max(maxLines, lines.size());
			}
			return Math.max(20f, (maxLines * 11f) + 8f);
		}

		private float totalWidth(float[] widths) {
			float total = 0f;
			for (float width : widths) {
				total += width;
			}
			return total;
		}

		private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
			if (text == null || text.isBlank()) {
				return List.of("");
			}
			List<String> lines = new ArrayList<>();
			for (String paragraph : text.split("\\R")) {
				if (paragraph.isBlank()) {
					lines.add("");
					continue;
				}
				StringBuilder currentLine = new StringBuilder();
				for (String word : paragraph.split("\\s+")) {
					String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
					if (textWidth(candidate, font, fontSize) <= maxWidth) {
						currentLine.setLength(0);
						currentLine.append(candidate);
					} else if (currentLine.length() > 0) {
						lines.add(currentLine.toString());
						currentLine.setLength(0);
						appendWord(lines, currentLine, word, font, fontSize, maxWidth);
					} else {
						appendWord(lines, currentLine, word, font, fontSize, maxWidth);
					}
				}
				if (currentLine.length() > 0) {
					lines.add(currentLine.toString());
				}
			}
			return lines.isEmpty() ? List.of("") : lines;
		}

		private void appendWord(List<String> lines, StringBuilder currentLine, String word, PDFont font, float fontSize,
				float maxWidth) throws IOException {
			if (textWidth(word, font, fontSize) <= maxWidth) {
				currentLine.append(word);
				return;
			}
			StringBuilder segment = new StringBuilder();
			for (char character : word.toCharArray()) {
				String candidate = segment.toString() + character;
				if (textWidth(candidate, font, fontSize) > maxWidth && segment.length() > 0) {
					lines.add(segment.toString());
					segment.setLength(0);
				}
				segment.append(character);
			}
			currentLine.append(segment);
		}

		private float textWidth(String text, PDFont font, float fontSize) throws IOException {
			return font.getStringWidth(text) / 1000 * fontSize;
		}

		private void close() throws IOException {
			if (contentStream != null) {
				contentStream.close();
			}
		}

		private String valueOrEmpty(String value) {
			return value != null ? value : "";
		}
	}
}
