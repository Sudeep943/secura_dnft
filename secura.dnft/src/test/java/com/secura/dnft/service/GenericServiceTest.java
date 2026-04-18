package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.WorkListAssignment;
import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.SecuraConstants;

import jakarta.persistence.Column;

@ExtendWith(MockitoExtension.class)
class GenericServiceTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private ProfileRepository profileRepository;

	@Mock
	private WorklistRepository worklistRepository;

	@InjectMocks
	private GenericService genericService;

	@Test
	void createWorklistAssignmentFlow_shouldPersistNewActiveAssignmentFlow() {
		Worklist worklist = new Worklist();
		worklist.setWorklistTaskId("WL-1");
		when(worklistRepository.findById("WL-1")).thenReturn(Optional.of(worklist));
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

		genericService.createWorklistAssignmentFlow("WL-1", List.of("PRFL-1", "PRFL-2"));

		ArgumentCaptor<Worklist> worklistCaptor = ArgumentCaptor.forClass(Worklist.class);
		verify(worklistRepository).save(worklistCaptor.capture());
		List<WorkListAssignment> assignments = genericService.fromJson(worklistCaptor.getValue().getWorklistsAssignFlow(),
				new TypeReference<List<WorkListAssignment>>() {
				});

		assertEquals(1, assignments.size());
		assertEquals(List.of("PRFL-1", "PRFL-2"), assignments.get(0).getAssignedPersonList());
		assertEquals(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE, assignments.get(0).getCurrentStatus());
		assertEquals("new", assignments.get(0).getAssignedBy());
		assertEquals(Date.valueOf(LocalDate.now()), assignments.get(0).getAssignmentDate());
		assertNull(assignments.get(0).getCompletedDate());
	}

	@Test
	void reassignWorklistFlowService_shouldTransferActiveAssignmentAndAppendNewOne() {
		WorkListAssignment activeAssignment = new WorkListAssignment();
		activeAssignment.setAssignmentDate(Date.valueOf(LocalDate.of(2026, 4, 1)));
		activeAssignment.setAssignedBy("new");
		activeAssignment.setAssignedPersonList(new ArrayList<>(List.of("PRFL-1", "PRFL-2")));
		activeAssignment.setCurrentStatus(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE);

		Worklist worklist = new Worklist();
		worklist.setWorklistTaskId("WL-2");
		worklist.setWorklistsAssignFlow(genericService.toJson(new ArrayList<>(List.of(activeAssignment))));

		when(worklistRepository.findById("WL-2")).thenReturn(Optional.of(worklist));
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

		genericService.reassignWorklistFlowService("WL-2", "PRFL-3", "PRFL-1");

		ArgumentCaptor<Worklist> worklistCaptor = ArgumentCaptor.forClass(Worklist.class);
		verify(worklistRepository).save(worklistCaptor.capture());
		List<WorkListAssignment> assignments = genericService.fromJson(worklistCaptor.getValue().getWorklistsAssignFlow(),
				new TypeReference<List<WorkListAssignment>>() {
				});

		assertEquals(2, assignments.size());
		assertEquals(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_TRANSFERRED, assignments.get(0).getCurrentStatus());
		assertEquals(Date.valueOf(LocalDate.now()), assignments.get(0).getCompletedDate());
		assertEquals(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE, assignments.get(1).getCurrentStatus());
		assertEquals("PRFL-1", assignments.get(1).getAssignedBy());
		assertEquals(List.of("PRFL-3"), assignments.get(1).getAssignedPersonList());
		assertNotNull(assignments.get(1).getAssignmentDate());
		assertNull(assignments.get(1).getCompletedDate());
	}

	@Test
	void reassignWorklistFlowService_shouldRejectWhenCurrentAssigneeIsNotActiveAssignee() {
		WorkListAssignment activeAssignment = new WorkListAssignment();
		activeAssignment.setAssignmentDate(Date.valueOf(LocalDate.of(2026, 4, 1)));
		activeAssignment.setAssignedPersonList(List.of("PRFL-1"));
		activeAssignment.setCurrentStatus(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE);

		Worklist worklist = new Worklist();
		worklist.setWorklistTaskId("WL-3");
		worklist.setWorklistsAssignFlow(genericService.toJson(List.of(activeAssignment)));

		when(worklistRepository.findById("WL-3")).thenReturn(Optional.of(worklist));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genericService.reassignWorklistFlowService("WL-3", "PRFL-3", "PRFL-2"));

		assertEquals("You Are Not Allowed To Reassign", exception.getMessage());
		verify(worklistRepository, never()).save(any(Worklist.class));
	}

	@Test
	void worklistsAssignFlowColumnShouldBeText() throws NoSuchFieldException {
		Field field = Worklist.class.getDeclaredField("worklistsAssignFlow");
		Column column = field.getAnnotation(Column.class);

		assertEquals("TEXT", column.columnDefinition());
	}
}
