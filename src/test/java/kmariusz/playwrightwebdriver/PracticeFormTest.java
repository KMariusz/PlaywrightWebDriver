package kmariusz.playwrightwebdriver;

import kmariusz.playwrightwebdriver.pages.PracticeFormPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class PracticeFormTest extends BaseTest {

    @Test
    @Tag("example")
    public void testPracticeFormSubmission() {
        // Test data
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@example.com";
        String mobile = "1234567890";
        String currentAddress = "123 Test Street, Test City";

        PracticeFormPage practiceFormPage = new PracticeFormPage(driver);
        practiceFormPage.navigateTo();

        // Fill and submit the form
        practiceFormPage.fillForm(firstName, lastName, email, mobile,
                currentAddress);
        practiceFormPage.submitForm();

        // Verify form submission
        assertTrue(practiceFormPage.isFormSubmitted(), "Form was not submitted successfully");
    }
}
