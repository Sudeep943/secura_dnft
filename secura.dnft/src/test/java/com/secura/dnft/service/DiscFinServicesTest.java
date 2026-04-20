package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
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
		updated.setDiscFnStrtDt(LocalDateTime.of(2026, 4, 1, 0, 0));
		updated.setDiscFnEndDt(LocalDateTime.of(2026, 4, 30, 0, 0));
		updated.setDiscFnMode("PERCENTAGE");
		updated.setDiscFnCumlatonCycle("MONTHLY");
		updated.setDiscFnCycleType("ONCE");
		updated.setDiscFinValue("10");

		UpdateDiscfinRequest request = new UpdateDiscfinRequest();
		request.setGenericHeader(header);
		request.setDiscFinId("DFN1");
		request.setDiscfinEntity(updated);

		when(discFinRepository.findById("DFN1")).thenReturn(Optional.of(existing));

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

		when(discFinRepository.findById("DFN1")).thenReturn(Optional.of(existing));

		UpdateDiscfinResponse response = discFinServices.updateDiscfin(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_48, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_48, response.getMessageCode());
		assertNull(existing.getLstUpdtUsrId());
	}
}
