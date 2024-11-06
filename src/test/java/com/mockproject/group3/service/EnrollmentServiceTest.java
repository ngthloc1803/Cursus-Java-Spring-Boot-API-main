package com.mockproject.group3.service;

import com.mockproject.group3.dto.request.PaginationParamReq;
import com.mockproject.group3.enums.EnrollmentStatus;
import com.mockproject.group3.enums.Status;
import com.mockproject.group3.exception.AppException;
import com.mockproject.group3.exception.ErrorCode;
import com.mockproject.group3.model.*;
import com.mockproject.group3.repository.CourseRepository;
import com.mockproject.group3.repository.EnrollmentRepository;
import com.mockproject.group3.repository.PaymentRepository;
import com.mockproject.group3.repository.StudentRepository;
import com.mockproject.group3.utils.GetAuthUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private GetAuthUserInfo getAuthUserInfo;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private int userId;
    private int courseId;
    private Student student;
    private Course course;
    private Payment payment;

    @BeforeEach
    void setUp() {
        userId = 1;
        courseId = 1;
        student = new Student();
        student.setId(userId);
        course = new Course();
        course.setId(courseId);
        PaymentDetail paymentDetail = new PaymentDetail();
        paymentDetail.setCourses(course);
        payment = new Payment();
        payment.setPaymentDetails(Set.of(paymentDetail));
    }

    @Test
    void testGetAllEnrollments() {
        PaginationParamReq req = new PaginationParamReq();
        req.setPage(1);
        req.setPageSize(10);

        List<Enrollment> enrollments = Arrays.asList(new Enrollment(), new Enrollment());
        Page<Enrollment> page = new PageImpl<>(enrollments);

        when(enrollmentRepository.findAll(PageRequest.of(0, 10, Sort.by("id").descending())))
                .thenReturn(page);

        Page<Enrollment> result = enrollmentService.getAllEnrollments(req);

        assertEquals(2, result.getTotalElements());
        assertEquals(enrollments, result.getContent());
    }

    @Test
    void testGetEnrollmentById() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(1);

        when(enrollmentRepository.findById(1)).thenReturn(Optional.of(enrollment));

        Optional<Enrollment> result = enrollmentService.getEnrollmentById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void testGetEnrollmentByStudentIdAndCourseId() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(1);

        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.of(enrollment));

        Enrollment result = enrollmentService.getEnrollmentByStudentIdAndCourseId(userId, courseId);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void testCreateEnrollment() {
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(paymentRepository.findByStudentIdAndStatus(userId, Status.APPROVED)).thenReturn(Collections.singletonList(payment));
        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment enrollment = enrollmentService.createEnrollment(courseId);

        assertNotNull(enrollment);
        assertEquals("Student " + userId + " enrolled in course " + courseId, enrollment.getDescription());
        assertEquals(EnrollmentStatus.ENROLLED, enrollment.getStatus());
        assertEquals(student, enrollment.getStudent());
        assertEquals(course, enrollment.getCourse());
    }

    @Test
    void testCreateEnrollmentThrowsExceptionIfStudentNotFound() {
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));

        assertEquals(ErrorCode.STUDENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testCreateEnrollmentThrowsExceptionIfCourseNotFound() {
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));

        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testCreateEnrollmentThrowsExceptionIfEnrollmentExists() {
        Enrollment existingEnrollment = new Enrollment();
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.of(existingEnrollment));
        when(paymentRepository.findByStudentIdAndStatus(userId, Status.APPROVED)).thenReturn(Collections.singletonList(payment));

        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));

        assertEquals(ErrorCode.ENROLLMENT_EXIST, exception.getErrorCode());
    }

    @Test
    void testCreateEnrollmentThrowsExceptionIfCourseNotPurchased() {
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());
        when(paymentRepository.findByStudentIdAndStatus(userId, Status.APPROVED)).thenReturn(Collections.emptyList());

        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));

        assertEquals(ErrorCode.COURSE_NOT_PURCHASED, exception.getErrorCode());
    }

    @Test
    void testCreateEnrollmentThrowsUnknownError() {
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());
        when(paymentRepository.findByStudentIdAndStatus(userId, Status.APPROVED)).thenReturn(Collections.singletonList(payment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenThrow(new DataIntegrityViolationException(""));

        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));

        assertEquals(ErrorCode.UNKNOWN_ERROR, exception.getErrorCode());
    }

    @Test
    void testCreateEnrollmentWhenCourseNotPaid() {
        // Setup
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        PaymentDetail paymentDetail = new PaymentDetail();
        Course paidCourse = new Course();
        paidCourse.setId(courseId + 1); // Not the course we're enrolling in
        paymentDetail.setCourses(paidCourse);

        Payment payment = new Payment();
        payment.setPaymentDetails(Set.of(paymentDetail));

        when(paymentRepository.findByStudentIdAndStatus(userId, Status.APPROVED)).thenReturn(List.of(payment));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));
        assertEquals(ErrorCode.COURSE_NOT_PURCHASED, exception.getErrorCode());
    }

    @Test
    void testGetProcess() {
        int enrollmentId = 1;
        when(enrollmentRepository.countCompletedLessonsByEnrollment(enrollmentId)).thenReturn(5L);
        when(enrollmentRepository.countTotalLessonsByCourse(courseId)).thenReturn(10L);

        double progress = enrollmentService.getProcess(enrollmentId, courseId);

        assertEquals(0.5, progress);
    }

    @Test
    void testGetProcessZeroTotalLessons() {
        int enrollmentId = 1;
        when(enrollmentRepository.countCompletedLessonsByEnrollment(enrollmentId)).thenReturn(5L);
        when(enrollmentRepository.countTotalLessonsByCourse(courseId)).thenReturn(0L);

        double progress = enrollmentService.getProcess(enrollmentId, courseId);

        assertEquals(0.0, progress);
    }
    @Test
    void testCreateEnrollmentThrowsExceptionIfPaymentDetailsEmpty() {
        when(getAuthUserInfo.getAuthUserId()).thenReturn(userId);
        when(studentRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());

        Payment emptyPayment = new Payment();
        emptyPayment.setPaymentDetails(Collections.emptySet());

        when(paymentRepository.findByStudentIdAndStatus(userId, Status.APPROVED)).thenReturn(Collections.singletonList(emptyPayment));

        AppException exception = assertThrows(AppException.class, () -> enrollmentService.createEnrollment(courseId));

        assertEquals(ErrorCode.COURSE_NOT_PURCHASED, exception.getErrorCode());
    }

    @Test
    void testGetProcessWithCompletedLessonsGreaterThanTotal() {
        int enrollmentId = 1;
        when(enrollmentRepository.countCompletedLessonsByEnrollment(enrollmentId)).thenReturn(15L);
        when(enrollmentRepository.countTotalLessonsByCourse(courseId)).thenReturn(10L);

        double progress = enrollmentService.getProcess(enrollmentId, courseId);

        assertEquals(1.5, progress);
    }
}
