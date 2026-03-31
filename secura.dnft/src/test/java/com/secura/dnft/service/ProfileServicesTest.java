package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ProfileServicesTest {

	@Test
	void hasClassLogger() throws NoSuchFieldException {
		Field loggerField = ProfileServices.class.getDeclaredField("LOGGER");
		assertEquals(Logger.class, loggerField.getType());
		assertTrue(Modifier.isStatic(loggerField.getModifiers()));
		assertTrue(Modifier.isFinal(loggerField.getModifiers()));
	}
}
