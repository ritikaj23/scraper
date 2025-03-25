package pages;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.By;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCoursesPage {
    private static final Logger logger = LoggerFactory.getLogger(AdminCoursesPage.class);
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(xpath = "//input[@placeholder='Search']")
    private WebElement searchField;

    @FindBy(xpath = "//td[contains(@class, 'css-1vekh47')]//a[contains(@href, '/teach/')]")
    private List<WebElement> courseLinks;

    @FindBy(xpath = "//button[@aria-label='Next page']")
    private WebElement nextPageButton;

    @FindBy(xpath = "//table[contains(@class, 'css-1vzbk0')]")
    private WebElement courseTable;

    public AdminCoursesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        PageFactory.initElements(driver, this);
    }

    public void navigateToAdminCourses(String adminUrl) {
        logger.info("Navigating to Admin Courses page: {}", adminUrl);
        
        driver.get(adminUrl);
        
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> ((org.openqa.selenium.JavascriptExecutor) d)
                        .executeScript("return document.readyState").equals("complete"));
        
        try {
            String currentUrl = driver.getCurrentUrl();
            logger.info("Current URL after navigation: {}", currentUrl);

            if (currentUrl.contains("login") || currentUrl.contains("signin")) {
                logger.error("Redirected to login page. Authentication may have failed.");
                throw new RuntimeException("Authentication failed. Redirected to login page: " + currentUrl);
            }

            String pageTitle = driver.getTitle();
            logger.info("Page title: {}", pageTitle);

            logger.info("Page source after navigation: {}", driver.getPageSource());

            wait.until(ExpectedConditions.presenceOfElementLocated(
                org.openqa.selenium.By.tagName("body")
            ));

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    org.openqa.selenium.By.xpath("//div[contains(@class, 'rc-')]")
                ));
                logger.info("Found a div with class containing 'rc-'");
            } catch (Exception e) {
                logger.warn("No div with class containing 'rc-' found. The page structure might have changed.");
            }

            logger.info("Waiting for search field to be visible...");
            wait.until(ExpectedConditions.visibilityOf(searchField));
            logger.info("Successfully navigated to Admin Courses page: {}", driver.getCurrentUrl());
        } catch (Exception e) {
            logger.error("Failed to navigate to Admin Courses page. Current URL: {}", driver.getCurrentUrl());
            logger.error("Page source on failure: {}", driver.getPageSource());
            try {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Files.copy(screenshot.toPath(), new File("admin-navigation-failure-screenshot.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Screenshot saved as admin-navigation-failure-screenshot.png");
            } catch (Exception ex) {
                logger.error("Failed to save screenshot: {}", ex.getMessage());
            }
            throw new RuntimeException("Failed to navigate to Admin Courses page: " + e.getMessage(), e);
        }
    }

    public List<CourseVersion> searchAndGetCourseVersions(String courseName) {
        List<CourseVersion> versions = new ArrayList<>();
        try {
            logger.info("Searching for course: {}", courseName);
            logger.info("Search field located: {}", searchField != null);
            wait.until(ExpectedConditions.elementToBeClickable(searchField));
            searchField.clear();
            searchField.sendKeys(courseName);
            searchField.sendKeys(Keys.ENTER);
            logger.info("Search submitted for course: {}", courseName);

            // Wait for the table to load after search
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    org.openqa.selenium.By.xpath("//table[contains(@class, 'css-1vzbk0')]")
                ));
                logger.info("Search results table found.");
            } catch (Exception e) {
                logger.warn("Search results table not found. The search might have returned no results or the XPath is incorrect.");
            }

            logger.info("Page source after search: {}", driver.getPageSource());

            // Split course name into keywords for more flexible matching
            List<String> keywords = Arrays.asList(courseName.toLowerCase().replace(":", "").split("\\s+"));
            logger.info("Keywords for matching: {}", keywords);

            // Handle pagination
            int pageNumber = 1;
            while (true) {
                logger.info("Processing page {} of search results.", pageNumber);

                // Wait for the table to be visible
                try {
                    wait.until(ExpectedConditions.visibilityOf(courseTable));
                    logger.info("Course table is visible on page {}.", pageNumber);
                } catch (Exception e) {
                    logger.warn("Course table not visible on page {}.", pageNumber);
                }

                logger.info("Number of course links found on page {}: {}", pageNumber, courseLinks.size());
                if (courseLinks.isEmpty()) {
                    logger.info("No course links found for '{}'. The search might have returned no results.", courseName);
                    try {
                        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                        Files.copy(screenshot.toPath(), new File("no-course-links-screenshot-page-" + pageNumber + ".png").toPath(), StandardCopyOption.REPLACE_EXISTING);
                        logger.info("Screenshot saved as no-course-links-screenshot-page-{}.png", pageNumber);
                    } catch (Exception ex) {
                        logger.error("Failed to save screenshot: {}", ex.getMessage());
                    }
                } else {
                    wait.until(ExpectedConditions.visibilityOfAllElements(courseLinks));
                    logger.info("Found {} course links after waiting for visibility on page {}.", courseLinks.size(), pageNumber);

                    for (WebElement link : courseLinks) {
                        String href = link.getAttribute("href");
                        String versionText = link.getText().toLowerCase();
                        logger.debug("Course link - Text: {}, Href: {}", versionText, href);

                        // Check if all keywords are present in the course name
                        boolean matches = keywords.stream().allMatch(keyword -> versionText.contains(keyword));
                        if (matches) {
                            versions.add(new CourseVersion(link.getText(), href));
                            logger.info("Added version: {} with link: {}", link.getText(), href);
                        }
                    }
                }

                // Check for next page
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@aria-label='Next page']")));
                    if (nextPageButton.isEnabled()) {
                        logger.info("Navigating to next page of search results.");
                        String previousPageSource = driver.getPageSource();
                        nextPageButton.click();
                        // Wait for the page to refresh by checking staleness of the table
                        wait.until(ExpectedConditions.stalenessOf(courseTable));
                        // Wait for the new table to load
                        wait.until(ExpectedConditions.visibilityOf(courseTable));
                        pageNumber++;
                    } else {
                        logger.info("Next page button is not enabled. Stopping pagination.");
                        break;
                    }
                } catch (Exception e) {
                    logger.info("No next page button found or not enabled. Stopping pagination.");
                    break;
                }
            }

            logger.info("Found {} versions for course '{}'", versions.size(), courseName);
            if (versions.isEmpty()) {
                logger.warn("No versions matched for '{}'. The course name might be incorrect or not present.", courseName);
                try {
                    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    Files.copy(screenshot.toPath(), new File("final-no-versions-screenshot.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Final screenshot saved as final-no-versions-screenshot.png");
                } catch (Exception ex) {
                    logger.error("Failed to save screenshot: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error searching for course '{}': {}", courseName, e.getMessage());
            logger.error("Page source on search failure: {}", driver.getPageSource());
            try {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Files.copy(screenshot.toPath(), new File("search-failure-screenshot.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Screenshot saved as search-failure-screenshot.png");
            } catch (Exception ex) {
                logger.error("Failed to save screenshot: {}", ex.getMessage());
            }
            throw new RuntimeException("Failed to search for course: " + courseName, e);
        }
        return versions;
    }

    public static class CourseVersion {
        private final String version;
        private final String link;

        public CourseVersion(String version, String link) {
            this.version = version;
            this.link = link;
        }

        public String getVersion() {
            return version;
        }

        public String getLink() {
            return link;
        }
    }
}