package pages;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver; // Added import for WebDriver
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseRatingsScraper {
    private static final Logger logger = LoggerFactory.getLogger(CourseRatingsScraper.class);
    private final WebDriver driver;
    private final List<Map<String, String>> courseData;
    private final AnalyticsPage analyticsPage;
    private final CoursePage coursePage; // This should now resolve

    public CourseRatingsScraper(WebDriver driver) {
        this.driver = driver;
        this.courseData = new ArrayList<>();
        this.analyticsPage = new AnalyticsPage(driver);
        this.coursePage = new CoursePage(driver);
    }

    public void processCourse(String courseLink, String courseName) {
        try {
            logger.info("Processing course: {} with link: {}", courseName, courseLink);
            // Navigate to the course page to get versions
            driver.get(courseLink);
            List<String> versions = coursePage.getVersions();

            if (versions.isEmpty()) {
                logger.warn("No versions found for course: {}. Treating as a single version.", courseName);
                // If no versions are found, treat it as a single version
                processSingleVersion(courseLink, courseName, "Default Version");
            } else {
                for (String version : versions) {
                    // Navigate back to the course page to select the version
                    driver.get(courseLink);
                    coursePage.selectVersion(version);
                    processSingleVersion(courseLink, courseName, version);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing course {}: {}", courseName, e.getMessage());
            Map<String, String> data = new HashMap<>();
            data.put("Course Version", courseName + " - Unknown Version");
            data.put("Rating", "Not found");
            data.put("Rating Stats", "Not found");
            courseData.add(data);
        }
    }

    private void processSingleVersion(String courseLink, String courseName, String version) {
        try {
            logger.info("Processing version: {} for course: {}", version, courseName);
            analyticsPage.goToAnalytics();
            analyticsPage.goToRatingSection();
            String rating = analyticsPage.getRating();
    

            Map<String, String> data = new HashMap<>();
            data.put("Course Version", courseName + " - " + version);
            data.put("Rating", rating);
            courseData.add(data);
        } catch (Exception e) {
            logger.error("Error processing version {} for course {}: {}", version, courseName, e.getMessage());
            Map<String, String> data = new HashMap<>();
            data.put("Course Version", courseName + " - " + version);
            data.put("Rating", "Not found");
            data.put("Rating Stats", "Not found");
            courseData.add(data);
        }
    }

    public List<Map<String, String>> getCourseData() {
        return courseData;
    }

    public void writeToExcel(String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Course Ratings");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Course Version");
            headerRow.createCell(1).setCellValue("Rating");
            headerRow.createCell(2).setCellValue("Rating Stats");

            int rowNum = 1;
            for (Map<String, String> data : courseData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data.get("Course Version"));
                row.createCell(1).setCellValue(data.get("Rating"));
                row.createCell(2).setCellValue(data.get("Rating Stats"));
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            logger.info("Excel file written successfully to: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to write to Excel: {}", e.getMessage());
        }
    }
}
