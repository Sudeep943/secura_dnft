package com.secura.dnft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
	void shouldHaveTextColumnDefinitionForPrflOthrAdrss() throws NoSuchFieldException {
		Field field = Profile.class.getDeclaredField("prflOthrAdrss");
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column);
		assertEquals("prfl_othr_adrss", column.name());
		assertEquals("TEXT", column.columnDefinition());
	}

}
