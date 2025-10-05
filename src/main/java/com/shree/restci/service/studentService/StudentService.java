package com.shree.restci.service.studentService;

import com.shree.restci.model.Student;

import java.util.List;

public interface StudentService {

    Student getStudent(Long id);

    List<Student> getStudents();

    Student createStudent(Student student);

    Student updateStudent(Long id, Student updatedStudent);

    void deleteStudent(Long id);

}
