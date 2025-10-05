package com.shree.restci;


import com.shree.restci.model.Student;
import com.shree.restci.model.entity.StudentEntity;
import com.shree.restci.repository.StudentRepository;
import com.shree.restci.service.studentService.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StudentServiceImplTest {

    private StudentRepository studentRepository;
    private StudentServiceImpl studentService;

    @BeforeEach
    void setUp() {
        studentRepository = Mockito.mock(StudentRepository.class);
        studentService = new StudentServiceImpl(studentRepository);
    }

    @Test
    void testGetStudents() {
        when(studentRepository.findAll()).thenReturn(
                List.of(new StudentEntity(1L,"Alice", "Smith"))
        );

        List<Student> students = studentService.getStudents();
        assertEquals(1, students.size());
        assertEquals("Alice", students.get(0).firstName());
    }

    @Test
    void testGetStudentById() {
        StudentEntity entity = new StudentEntity(1L,"Bob", "Johnson");
        entity.setId(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));

        Student student = studentService.getStudent(1L);
        assertEquals("Bob", student.firstName());
    }

    @Test
    void testSaveStudent() {
        Student dto = new Student(null, "Charlie", "Brown");
        StudentEntity savedEntity = new StudentEntity(1L,"Charlie", "Brown");
        savedEntity.setId(2L);

        when(studentRepository.save(any(StudentEntity.class))).thenReturn(savedEntity);

        Student result = studentService.createStudent(dto);
        assertEquals(2L, result.id());
        assertEquals("Charlie", result.firstName());
    }

    @Test
    void testUpdateStudent() {
        StudentEntity existing = new StudentEntity(1L,"David", "Lee");
        existing.setId(3L);

        when(studentRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(i -> i.getArgument(0));

        Student updated = new Student(3L, "DavidUpdated", "LeeUpdated");
        Student result = studentService.updateStudent(3L, updated);

        assertEquals("DavidUpdated", result.firstName());
        assertEquals("LeeUpdated", result.lastName());
    }

    @Test
    void testDeleteStudent() {
        when(studentRepository.existsById(4L)).thenReturn(true);
        doNothing().when(studentRepository).deleteById(4L);

        assertDoesNotThrow(() -> studentService.deleteStudent(4L));
        verify(studentRepository, times(1)).deleteById(4L);
    }
}
