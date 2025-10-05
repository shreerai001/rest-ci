package com.shree.restci.mapper;


import com.shree.restci.model.Student;
import com.shree.restci.model.entity.StudentEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentMapper {

    public static Student toDto(StudentEntity entity) {
        if (entity == null) return null;
        return new Student(entity.getId(), entity.getFirstName(), entity.getLastName());
    }

    public static StudentEntity toEntity(Student dto) {
        if (dto == null) return null;
        StudentEntity entity = new StudentEntity();
        entity.setId(dto.id());
        entity.setFirstName(dto.firstName());
        entity.setLastName(dto.lastName());
        return entity;
    }

    public static List<Student> toDtos(List<StudentEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(e -> new Student(e.getId(), e.getFirstName(), e.getLastName()))
                .toList();
    }

}

