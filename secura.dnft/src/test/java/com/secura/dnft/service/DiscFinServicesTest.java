package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.DiscFinCycleDiscount;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.UpdateDiscfinRequest;
import com.secura.dnft.request.response.UpdateDiscfinResponse;

@ExtendWith(MockitoExtension.class)
class DiscFinServicesTest {

	@Mock
	private DiscFinRepository discFinRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private DiscFinServices discFinServices;

	@Test
	void addDiscfin_shouldSaveOneEntityPerCycleDiscountUsingCycleModeAndValue() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");

		DiscFinCycleDiscount halfYearly = new DiscFinCycleDiscount();
		halfYearly.setCycle("HALF_YEARLY");
		halfYearly.setType("PERCENTAGE");
		halfYearly.setValue("20");

		DiscFinCycleDiscount yearly = new DiscFinCycleDiscount();
		yearly.setCycle("YEARLY");
		yearly.setType("AMOUNT");
		yearly.setValue("100");

		DiscFinCycleDiscount quarterly = new DiscFinCycleDiscount();
		quarterly.setCycle("QUARTERLY");
		quarterly.setType("AMOUNT");
		quarterly.setValue("500");

		AddDiscfinRequest request = new AddDiscfinRequest();
		request.setGenericHeader(header);
		request.setDiscFnType("DISCOUNT");
		request.setDiscFnMode("REQUEST_MODE");
		request.setDiscFnValue("REQUEST_VALUE");
		request.setDiscFnCycleType("CUMULATIVE");
		request.setDiscFinCycleDiscountList(List.of(halfYearly, yearly, quarterly));
		request.setMinimumPaymentAmount("250");

		when(discFinRepository.existsByDiscFnId(anyString())).thenReturn(false);

		AddDiscfinResponse response = discFinServices.addDiscfin(request);

		ArgumentCaptor<DiscFin> captor = ArgumentCaptor.forClass(DiscFin.class);
		verify(discFinRepository, times(3)).save(captor.capture());
		List<DiscFin> savedEntities = captor.getAllValues();

		assertEquals(3, savedEntities.size());
		assertEquals(response.getDiscFnId(), savedEntities.get(0).getDiscFnId());
		assertEquals(response.getDiscFnId(), savedEntities.get(1).getDiscFnId());
		assertEquals(response.getDiscFnId(), savedEntities.get(2).getDiscFnId());
		assertEquals("HALF_YEARLY", savedEntities.get(0).getDiscFnCycleType());
		assertEquals("PERCENTAGE", savedEntities.get(0).getDiscFnMode());
		assertEquals("20", savedEntities.get(0).getDiscFinValue());
		assertEquals("YEARLY", savedEntities.get(1).getDiscFnCycleType());
		assertEquals("AMOUNT", savedEntities.get(1).getDiscFnMode());
		assertEquals("100", savedEntities.get(1).getDiscFinValue());
		assertEquals("QUARTERLY", savedEntities.get(2).getDiscFnCycleType());
		assertEquals("AMOUNT", savedEntities.get(2).getDiscFnMode());
		assertEquals("500", savedEntities.get(2).getDiscFinValue());
		assertEquals("APR-1", savedEntities.get(0).getAprmtId());
		assertEquals("APR-1", savedEntities.get(1).getAprmtId());
		assertEquals("APR-1", savedEntities.get(2).getAprmtId());
		assertEquals("USR-1", savedEntities.get(0).getCreatUsrId());
		assertEquals("USR-1", savedEntities.get(1).getCreatUsrId());
		assertEquals("USR-1", savedEntities.get(2).getCreatUsrId());
		assertEquals("250", savedEntities.get(0).getMinimumPaymentAmount());
		assertEquals("250", savedEntities.get(1).getMinimumPaymentAmount());
		assertEquals("250", savedEntities.get(2).getMinimumPaymentAmount());
		assertEquals("CUMULATIVE", savedEntities.get(0).getFnCalculationType());
		assertEquals("CUMULATIVE", savedEntities.get(1).getFnCalculationType());
		assertEquals("CUMULATIVE", savedEntities.get(2).getFnCalculationType());
		assertEquals(SuccessMessage.SUCC_MESSAGE_29, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_29, response.getMessageCode());
	}

	@Test
	void addDiscfin_shouldSaveSingleFixedEntryWhenCycleDiscountListIsNull() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");

		AddDiscfinRequest request = new AddDiscfinRequest();
		request.setGenericHeader(header);
		request.setDiscFnType("DISCOUNT");
		request.setDiscFnMode("PERCENTAGE");
		request.setDiscFnValue("10");
		request.setDiscFnCycleType("SIMPLE");
		request.setMinimumPaymentAmount("250");

		when(discFinRepository.existsByDiscFnId(anyString())).thenReturn(false);

		discFinServices.addDiscfin(request);

		ArgumentCaptor<DiscFin> captor = ArgumentCaptor.forClass(DiscFin.class);
		verify(discFinRepository).save(captor.capture());
		DiscFin savedEntity = captor.getValue();

		assertEquals(SecuraConstants.DISC_FN_CYCLE_FIXED, savedEntity.getDiscFnCycleType());
		assertEquals("PERCENTAGE", savedEntity.getDiscFnMode());
		assertEquals("10", savedEntity.getDiscFinValue());
		assertEquals("APR-1", savedEntity.getAprmtId());
		assertEquals("USR-1", savedEntity.getCreatUsrId());
		assertEquals("250", savedEntity.getMinimumPaymentAmount());
		assertEquals("SIMPLE", savedEntity.getFnCalculationType());
	}

	@Test
	void addDiscfin_shouldSaveSingleFixedEntryWhenCycleDiscountListIsEmpty() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");

		AddDiscfinRequest request = new AddDiscfinRequest();
		request.setGenericHeader(header);
		request.setDiscFnType("DISCOUNT");
		request.setDiscFnMode("AMOUNT");
		request.setDiscFnValue("500");
		request.setDiscFinCycleDiscountList(List.of());

		when(discFinRepository.existsByDiscFnId(anyString())).thenReturn(false);

		discFinServices.addDiscfin(request);

		ArgumentCaptor<DiscFin> captor = ArgumentCaptor.forClass(DiscFin.class);
		verify(discFinRepository).save(captor.capture());
		DiscFin savedEntity = captor.getValue();

		assertEquals(SecuraConstants.DISC_FN_CYCLE_FIXED, savedEntity.getDiscFnCycleType());
		assertEquals("AMOUNT", savedEntity.getDiscFnMode());
		assertEquals("500", savedEntity.getDiscFinValue());
	}

	@Test
	void updateDiscfin_shouldUpdateExistingDiscfinForApartment() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");

		DiscFin existing = new DiscFin();
		existing.setDiscFnId("DFN1");
		existing.setAprmtId("APR-1");

		DiscFin updated = new DiscFin();
		updated.setDiscFnType("DISCOUNT");
		updated.setDueDateAsStartDateFlag(Boolean.TRUE);
		updated.setDiscFnStrtDt(LocalDate.of(2026, 4, 1));
		updated.setDiscFnEndDt(LocalDate.of(2026, 4, 30));
		updated.setDiscFnMode("PERCENTAGE");
		updated.setDiscFnCumlatonCycle("MONTHLY");
		updated.setDiscFnCycleType("ONCE");
		updated.setDiscFinValue("10");

		UpdateDiscfinRequest request = new UpdateDiscfinRequest();
		request.setGenericHeader(header);
		request.setDiscFinId("DFN1");
		request.setDiscfinEntity(updated);

		when(discFinRepository.findByDiscFnId("DFN1")).thenReturn(List.of(existing));

		UpdateDiscfinResponse response = discFinServices.updateDiscfin(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_39, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_39, response.getMessageCode());
		assertEquals("DISCOUNT", existing.getDiscFnType());
		assertEquals(Boolean.TRUE, existing.getDueDateAsStartDateFlag());
		assertEquals("USR-1", existing.getLstUpdtUsrId());
		verify(discFinRepository).save(existing);
	}

	@Test
	void updateDiscfin_shouldRejectWhenApartmentDoesNotOwnDiscfin() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");

		DiscFin existing = new DiscFin();
		existing.setDiscFnId("DFN1");
		existing.setAprmtId("APR-2");

		UpdateDiscfinRequest request = new UpdateDiscfinRequest();
		request.setGenericHeader(header);
		request.setDiscFinId("DFN1");
		request.setDiscfinEntity(new DiscFin());

		when(discFinRepository.findByDiscFnId("DFN1")).thenReturn(List.of(existing));

		UpdateDiscfinResponse response = discFinServices.updateDiscfin(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_48, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_48, response.getMessageCode());
		assertNull(existing.getLstUpdtUsrId());
	}
}
