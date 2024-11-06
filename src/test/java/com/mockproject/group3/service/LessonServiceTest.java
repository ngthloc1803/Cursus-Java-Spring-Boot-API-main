package com.mockproject.group3.service;

import com.mockproject.group3.dto.LessonDTO;
import com.mockproject.group3.dto.request.PaginationParamReq;
import com.mockproject.group3.exception.AppException;
import com.mockproject.group3.exception.ErrorCode;
import com.mockproject.group3.model.Lesson;
import com.mockproject.group3.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private LessonService lessonService;

    private Lesson lesson;
    private LessonDTO lessonDTO;
    private PaginationParamReq paginationParamReq;

    @BeforeEach
    void setUp() {
        lesson = new Lesson();
        lesson.setId(1);
        lesson.setTitle("Lesson Title");
        lesson.setContent("Lesson Content");

        lessonDTO = new LessonDTO();
        lessonDTO.setTitle("Lesson Title");
        lessonDTO.setContent("Lesson Content");

        paginationParamReq = new PaginationParamReq();
        paginationParamReq.setPage(1);
        paginationParamReq.setPageSize(10);
    }

    @Test
    void testGetAllLessons() {
        Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson));
        when(lessonRepository.findAll(any(PageRequest.class))).thenReturn(lessonPage);

        Page<Lesson> result = lessonService.getAllLessons(paginationParamReq);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(lessonRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void testGetLessonById() {
        when(lessonRepository.findById(anyInt())).thenReturn(Optional.of(lesson));

        Optional<Lesson> result = lessonService.getLessonById(1);

        assertTrue(result.isPresent());
        assertEquals(lesson.getId(), result.get().getId());
        verify(lessonRepository, times(1)).findById(anyInt());
    }

    @Test
    void testGetLessonByCourseId() {
        when(lessonRepository.findByCourseId(anyInt())).thenReturn(List.of(lesson));

        List<Lesson> result = lessonService.getLessonByCourseId(1);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(lesson.getId(), result.getFirst().getId());
        verify(lessonRepository, times(1)).findByCourseId(anyInt());
    }

    @Test
    void testCreateLesson() {
        when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);

        Lesson result = lessonService.createLesson(lessonDTO);

        assertNotNull(result);
        assertEquals(lesson.getTitle(), result.getTitle());
        verify(lessonRepository, times(1)).save(any(Lesson.class));
    }

    @Test
    void testUpdateLesson() {
        when(lessonRepository.findById(anyInt())).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);

        Lesson result = lessonService.updateLesson(1, lessonDTO);

        assertNotNull(result);
        assertEquals(lesson.getTitle(), result.getTitle());
        verify(lessonRepository, times(1)).findById(anyInt());
        verify(lessonRepository, times(1)).save(any(Lesson.class));
    }

    @Test
    void testUpdateLessonNotFound() {
        when(lessonRepository.findById(anyInt())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> lessonService.updateLesson(1, lessonDTO));

        assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());
        verify(lessonRepository, times(1)).findById(anyInt());
        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void testDeleteLesson() {
        when(lessonRepository.findById(anyInt())).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(1);

        verify(lessonRepository, times(1)).findById(anyInt());
        verify(lessonRepository, times(1)).deleteById(anyInt());
    }

    @Test
    void testDeleteLessonNotFound() {
        when(lessonRepository.findById(anyInt())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> lessonService.deleteLesson(1));

        assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());
        verify(lessonRepository, times(1)).findById(anyInt());
        verify(lessonRepository, never()).deleteById(anyInt());
    }
}
