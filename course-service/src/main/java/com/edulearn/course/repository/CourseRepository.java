package com.edulearn.course.repository;

import com.edulearn.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // ── Basic finders ─────────────────────────────────────────────────────────

    List<Course> findByTitleContainingIgnoreCase(String title);

    Page<Course> findByCategory(Course.Category category, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    Page<Course> findByLevel(Course.Level level, Pageable pageable);

    Page<Course> findByIsPublishedTrue(Pageable pageable);

    List<Course> findByInstructorId(Long instructorId);

    // ── Keyword search across title + description ─────────────────────────────

    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ── Full filter search ────────────────────────────────────────────────────

    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:level IS NULL OR c.level = :level) AND " +
           "(:language IS NULL OR c.language = :language) AND " +
           "(:maxPrice IS NULL OR c.price <= :maxPrice)")
    Page<Course> searchWithFilters(
            @Param("keyword")  String keyword,
            @Param("category") Course.Category category,
            @Param("level")    Course.Level level,
            @Param("language") Course.Language language,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // ── Price filter ──────────────────────────────────────────────────────────

    Page<Course> findByPriceLessThanEqual(BigDecimal price, Pageable pageable);

    // ── Free courses ──────────────────────────────────────────────────────────

    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND c.price = 0")
    List<Course> findFreeCourses();

    // ── Featured courses ──────────────────────────────────────────────────────

    List<Course> findByIsFeaturedTrueAndIsPublishedTrue();

    // ── Count per instructor ──────────────────────────────────────────────────

    long countByInstructorId(Long instructorId);

    // ── Top courses by enrollment ─────────────────────────────────────────────

    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.totalEnrollments DESC")
    List<Course> findTopByEnrollments(Pageable pageable);

    // ── Instructor published courses ──────────────────────────────────────────

    List<Course> findByInstructorIdAndIsPublishedTrue(Long instructorId);

    // ── Category + published ──────────────────────────────────────────────────

    List<Course> findByCategoryAndIsPublishedTrue(Course.Category category);
}
