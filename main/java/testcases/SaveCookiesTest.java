package testcases;

import base.BasePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.HomePage;
import pages.LoginPage;

public class SaveCookiesTest extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(SaveCookiesTest.class);

    public static void main(String[] args) {
        SaveCookiesTest test = new SaveCookiesTest();
        try {
            test.setUp();

            HomePage homePage = new HomePage(test.getDriver());
            LoginPage loginPage = homePage.clickOnSignIn();
            loginPage.login("ritikaj@skillup.online", "Pulse@458901");

            logger.info("Please solve the CAPTCHA manually and navigate to https://www.coursera.org/admin-v2/ibm-skills-network/home/courses within 60 seconds...");
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }

            test.saveCookies();
            logger.info("Cookies saved. You can now use them in tests.");
        } catch (Exception e) {
            logger.error("Error in SaveCookiesTest: {}", e.getMessage(), e);
            throw e;
        } finally {
            test.closeDriver();
            logger.info("Driver closed in finally block.");
            // Force JVM exit to ensure all threads are terminated
            System.exit(0);
        }
    }
}