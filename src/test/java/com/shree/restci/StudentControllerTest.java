package com.shree.restci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shree.restci.controller.StudentController;
import com.shree.restci.model.Student;
import com.shree.restci.service.studentService.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateStudent() throws Exception {
        Student student = new Student(null, "Alice", "Smith");
        when(studentService.createStudent(any(Student.class))).thenReturn(student);

        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Alice")))
                .andExpect(jsonPath("$.lastName", is("Smith")));
    }

    @Test
    void testGetAllStudents() throws Exception {
        Student student = new Student(1L, "Bob", "Johnson");
        when(studentService.getStudents()).thenReturn(Arrays.asList(student));

        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName", is("Bob")));
    }

    @Test
    void testGetStudentById() throws Exception {
        Student student = new Student(1L, "Charlie", "Brown");
        when(studentService.getStudent(1L)).thenReturn(student);

        mockMvc.perform(get("/students/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Charlie")));
    }

    @Test
    void testUpdateStudent() throws Exception {
        Student updated = new Student(1L, "DavidUpdated", "LeeUpdated");
        when(studentService.updateStudent(anyLong(), any(Student.class))).thenReturn(updated);

        mockMvc.perform(put("/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("DavidUpdated")));
    }

    @Test
    void testDeleteStudent() throws Exception {
        doNothing().when(studentService).deleteStudent(1L);

        mockMvc.perform(delete("/students/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}