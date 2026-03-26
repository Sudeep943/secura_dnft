package com.secura.dnft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.secura.dnft.entity.Profile;

import jakarta.persistence.Column;

@SpringBootTest
class ApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void profileOtherAddressColumnIsText() throws NoSuchFieldException {
		Field field = Profile.class.getDeclaredField("prflOthrAdrss");
		Column column = field.getAnnotation(Column.class);
		assertEquals("TEXT", column.columnDefinition());
	}

}
