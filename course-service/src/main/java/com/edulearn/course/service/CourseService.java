package com.edulearn.course.service;

import com.edulearn.course.dto.CourseDto;
import com.edulearn.course.entity.Course;

import java.util.List;

/**
 * Business contract for all course lifecycle operations.
 */
public interface CourseService {

    /** Create a new course owned by instructorId. */
    CourseDto.CourseResponse createCourse(Long instructorId, String instructorName,
                                          CourseDto.CourseRequest request);

    /** Get full details of any course by ID (published or not). */
    CourseDto.CourseResponse getCourseById(Long courseId);

    /** Get all published courses (paginated). */
    CourseDto.PagedResponse<CourseDto.CourseSummary> getAllCourses(int page, int size, String sortBy);

    /** Get courses by category (published only, paginated). */
    CourseDto.PagedResponse<CourseDto.CourseSummary> getCoursesByCategory(
            Course.Category category, int page, int size);

    /** Get all courses by a specific instructor (including unpublished for the owner). */
    List<CourseDto.CourseResponse> getCoursesByInstructor(Long instructorId);

    /** Full-text + filter search across published courses. */
    CourseDto.PagedResponse<CourseDto.CourseSummary> searchCourses(CourseDto.SearchRequest request);

    /** Update course metadata; only the owning instructor or admin may do this. */
    CourseDto.CourseResponse updateCourse(Long courseId, Long requesterId,
                                          boolean isAdmin, CourseDto.CourseRequest request);

    /** Toggle publish/unpublish; only the owning instructor or admin may do this. */
    CourseDto.CourseResponse publishCourse(Long courseId, Long requesterId,
                                           boolean isAdmin, boolean publish);

    /** Hard-delete a course; only the owning instructor or admin may do this. */
    void deleteCourse(Long courseId, Long requesterId, boolean isAdmin);

    /** Return the featured courses list for the home page. */
    List<CourseDto.CourseSummary> getFeaturedCourses();

    /** Return the top N courses by enrollment count. */
    List<CourseDto.CourseSummary> getTopCourses(int limit);

    /** Return free (price = 0) published courses. */
    List<CourseDto.CourseSummary> getFreeCourses();

    /** Admin: toggle isFeatured flag. */
    CourseDto.CourseResponse toggleFeatured(Long courseId, boolean featured);

    /** Called by enrollment-service to keep totalEnrollments in sync. */
    void updateEnrollmentCount(Long courseId, int delta);

    /** Update totalDuration — called by lesson-service after lesson changes. */
    void updateTotalDuration(Long courseId, int totalMinutes);
}
