package com.edulearn.lesson.repository;

import com.edulearn.lesson.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByLesson_LessonId(Long lessonId);

    void deleteByLesson_LessonId(Long lessonId);

    long countByLesson_LessonId(Long lessonId);
}
