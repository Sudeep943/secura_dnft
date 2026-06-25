package com.secura.dnft.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReceiptFontLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptFontLoader.class);

	private ReceiptFontLoader() {
	}

	static PDFont loadFont(PDDocument document, String[] resourceCandidates, String[] fileCandidates, PDFont fallback) {
		for (String resourceCandidate : resourceCandidates) {
			InputStream stream = ReceiptServices.class.getResourceAsStream(resourceCandidate);
			if (stream == null) {
				continue;
			}
			try (stream) {
				return PDType0Font.load(document, stream);
			} catch (IOException ex) {
				LOGGER.debug("Unable to load bundled receipt font from {}", resourceCandidate, ex);
			}
		}
		for (String candidate : fileCandidates) {
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
}
