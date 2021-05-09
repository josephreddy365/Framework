package com.ui.stepdefinitions;

import com.ui.pages.HomePage;
import com.ui.pages.SignInPage;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;



public class HomePageSteps extends BaseSteps {
    @Given("^I go to home page$")
    public void iGoToHomePage() {
            HomePage.getHomePage();
    }

    @When("^I enter \"([^\"]*)\" and \"([^\"]*)\"$")
    public void iEnterSomethingAndSomething(String username, String password) throws Throwable {
        HomePage.enterUsernameAndPassword(username,password);
    }

    @And("^I click on submit button$")
    public void iClickOnSubmitButton() throws Throwable {
        HomePage.clickSignInButton();
    }

    @Then("^I see message \"([^\"]*)\"$")
    public void iSeeMessageSomething(String message) throws Throwable {
        HomePage.iSeeAnyTextOnThePage(message);
    }





}