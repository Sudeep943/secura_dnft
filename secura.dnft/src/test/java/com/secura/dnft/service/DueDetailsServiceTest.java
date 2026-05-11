package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GenericHeader;

@ExtendWith(MockitoExtension.class)
class DueDetailsServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private DiscFinRepository discFinRepository;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private DueDetailsService dueDetailsService;

	@Test
	void calculateDuesForPayment_shouldCreatePerSqftDuesAndPersistEntities() {
		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY1001");
		payment.setAprmtId("APR001");
		payment.setPaymentCollectionCycle("QUATERLY");
		payment.setPaymentCollectionMode("PRE");
		payment.setCollectionStartDate(LocalDateTime.parse("2026-01-01T00:00:00"));
		payment.setCollectionEndDate(LocalDateTime.parse("2026-03-31T00:00:00"));
		payment.setPaymentAmount("2");
		payment.setPaymentCapita("per Sqft");
		payment.setPaymentName("Maintenance");
		payment.setPaymentType("MAINTENANCE");
		payment.setGst("10");
		payment.setCauseId("COMMON_AREA");

		Flat flat1000 = new Flat();
		flat1000.setFlatNo("A-101");
		flat1000.setFlatArea("1000");
		flat1000.setFlatPndngPaymntLst(null);
		Flat flat1200 = new Flat();
		flat1200.setFlatNo("A-201");
		flat1200.setFlatArea("1200");
		flat1200.setFlatPndngPaymntLst(null);

		GenericHeader header = new GenericHeader();
		header.setUserId("USR001");

		when(paymentRepository.findByPaymentId("PAY1001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR001")).thenReturn(List.of(flat1000, flat1200));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("[\"DUE\"]");

		Map<String, Map<String, DueAmountDetails>> response = dueDetailsService.calculateDuesForPayment("PAY1001", header);

		assertTrue(response.containsKey("QUATERLY"));
		Map<String, DueAmountDetails> duesByFlatType = response.get("QUATERLY");
		assertEquals(2, duesByFlatType.size());
		assertTrue(duesByFlatType.containsKey("1000"));
		assertTrue(duesByFlatType.containsKey("1200"));
		assertNotNull(duesByFlatType.get("1000").getEstimatedCollectionAmount());
		assertEquals("COMMON_AREA", duesByFlatType.get("1000").getCause());
		assertEquals("0", duesByFlatType.get("1000").getAdminDiscount());
		assertEquals("0", duesByFlatType.get("1000").getAlreadyPaidAmount());
		assertEquals(duesByFlatType.get("1000").getTotalAmount(), duesByFlatType.get("1000").getDueRemained());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		List<DueAmountDetailsEntity> savedDueEntities = dueEntityCaptor.getValue();
		assertEquals(2, savedDueEntities.size());
		assertEquals("USR001", savedDueEntities.get(0).getCreatUsrId());
		assertEquals("1000", savedDueEntities.get(0).getFlatArea());

		verify(flatRepository, times(1)).saveAll(any());
	}
}
