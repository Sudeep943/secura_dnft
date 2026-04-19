package com.secura.dnft.generic.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AddressTest {

	@Test
	void toString_shouldJoinOnlyPresentFields() {
		Address address = new Address();
		address.setAddressLine1("Line 1");
		address.setAddressLine2("Line 2");
		address.setCity("Springfield");
		address.setPin("751001");

		assertEquals("Line 1 ,Line 2 ,Springfield ,751001", address.toString());
	}
}
