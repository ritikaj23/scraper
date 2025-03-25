package testcases;

import base.BasePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AdminCoursesPage;
import pages.AdminCoursesPage.CourseVersion;
import pages.CourseRatingsScraper;

import java.util.List;
import java.util.Map;

public class IdentifyCoursesTest extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(IdentifyCoursesTest.class);
    private CourseRatingsScraper scraper;

    @BeforeMethod
    public void setUp() {
        logger.info("Setting up test...");
        super.setUp();
        if (this.adminCoursesPage == null) {
            logger.error("adminCoursesPage is null after super.setUp(). Check BasePage initialization.");
            throw new RuntimeException("adminCoursesPage is not initialized in BasePage.");
        }
        scraper = this.courseRatingsScraper;
    }

    @Test
    public void testScrapeCourseRatings() {
        logger.info("Starting testScrapeCourseRatings...");
        String adminUrl = "https://www.coursera.org/admin-v2/ibm-skills-network/home/courses";
        adminCoursesPage.navigateToAdminCourses(adminUrl);

        String courseName = "Node.js & MongoDB: Developing Back-end Database Applications";
        List<CourseVersion> courseVersions = adminCoursesPage.searchAndGetCourseVersions(courseName);

        if (courseVersions.isEmpty()) {
            logger.warn("No versions found for course: {}. Check the course name, logs, and screenshots for details.", courseName);
            Assert.fail("No versions found for course: " + courseName + ". Verify the course exists on the admin page.");
        }

        for (CourseVersion courseVersion : courseVersions) {
            logger.info("Processing course: {}", courseVersion.getVersion());
            scraper.processCourse(courseVersion.getLink(), courseVersion.getVersion());
        }

        for (Map<String, String> data : scraper.getCourseData()) {
            logger.info("Course Version: {}, Rating: {}, Stats: {}", 
                data.get("Course Version"), data.get("Rating"), data.get("Rating Stats"));
        }

        boolean hasRating = scraper.getCourseData().stream()
                .anyMatch(data -> !data.get("Rating").equals("Not found"));
        Assert.assertTrue(hasRating, "No ratings found for any version of course: " + courseName);

        scraper.writeToExcel("coursera_course_ratings.xlsx");
        logger.info("Test completed successfully.");
    }

    @AfterMethod
    public void tearDown() {
        logger.info("Tearing down test...");
        closeDriver();
        logger.info("Test cleanup completed.");
    }
}