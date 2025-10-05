package com.shree.restci.service.studentService;

import com.shree.restci.mapper.StudentMapper;
import com.shree.restci.model.Student;
import com.shree.restci.model.entity.StudentEntity;
import com.shree.restci.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public Student getStudent(Long id) {
        return StudentMapper.toDto(studentRepository.findById(id).get());
    }

    @Override
    public List<Student> getStudents() {
        return StudentMapper.toDtos(studentRepository.findAll());
    }

    @Override
    public Student createStudent(Student student) {
        return StudentMapper.toDto(studentRepository.save(StudentMapper.toEntity(student)));
    }

    @Override
    public Student updateStudent(Long id, Student updatedStudent) {
        return studentRepository.findById(id)
                .map(existing -> {
                    existing.setFirstName(updatedStudent.firstName());
                    existing.setLastName(updatedStudent.lastName());
                    StudentEntity saved = studentRepository.save(existing);
                    return StudentMapper.toDto(saved);
                })
                .orElseThrow(() -> new RuntimeException("Student not found with id " + id));
    }

    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id " + id);
        }
        studentRepository.deleteById(id);
    }


}
