package io.github.kmariusz.playwrightwebdriver.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class PracticeFormPage extends BasePage {
    private static final String BASE_URL = "https://demoqa.com/automation-practice-form";

    // Form fields
    private static final By FIRST_NAME_INPUT = By.id("firstName");
    private static final By LAST_NAME_INPUT = By.id("lastName");
    private static final By EMAIL_INPUT = By.id("userEmail");
    private static final By GENDER_RADIO = By.cssSelector("label[for='gender-radio-1']");
    private static final By MOBILE_INPUT = By.id("userNumber");
    private static final By CURRENT_ADDRESS_INPUT = By.id("currentAddress");
    private static final By SUBMIT_BUTTON = By.id("submit");

    // Modal elements
    private static final By MODAL_TITLE = By.id("example-modal-sizes-title-lg");

    // Constants
    private static final String MODAL_TITLE_TEXT = "Thanks for submitting the form";

    public PracticeFormPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Fills out the practice form with the provided details.
     *
     * @param firstName      The first name to enter
     * @param lastName       The last name to enter
     * @param email          The email to enter
     * @param mobile         The mobile number to enter
     * @param currentAddress The current address to enter
     */
    public void fillForm(String firstName, String lastName, String email, String mobile,
                         String currentAddress) {
        type(FIRST_NAME_INPUT, firstName);
        type(LAST_NAME_INPUT, lastName);
        type(EMAIL_INPUT, email);
        click(GENDER_RADIO);
        type(MOBILE_INPUT, mobile);
        type(CURRENT_ADDRESS_INPUT, currentAddress);
    }

    /**
     * Submits the form.
     */
    public void submitForm() {
        click(SUBMIT_BUTTON);
    }

    /**
     * Checks if the form was successfully submitted by verifying the modal title.
     *
     * @return true if the form was submitted successfully, false otherwise
     */
    public boolean isFormSubmitted() {
        return isDisplayed(MODAL_TITLE) &&
                driver.findElement(MODAL_TITLE).getText().equals(MODAL_TITLE_TEXT);
    }

    @Override
    public void navigateTo() {
        driver.get(BASE_URL);
    }
}
