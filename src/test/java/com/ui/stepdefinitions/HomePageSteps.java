package com.ui.stepdefinitions;

import com.ui.pages.HomePage;
import com.ui.pages.SignInPage;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;

public class HomePageSteps {
    @Given("I go to home page")
    public void i_go_to_home_page() {
         HomePage.getHomePage();
    }

    @When("I enter username {string} and password {string}")
    public void i_enter_username_and_password(String string, String string2) {

    }

    @Then("I can login")
    public void i_can_login() {

    }

    @When("the user clicks the Login button")
    public void the_user_clicks_the_Login_button() {

    }



    @Then("I see error message {string}")
    public void i_see_error_message(String string) {

    }



    @When("^the user inputs the (incorrect|correct) personal information$")
    public void theUserInputsInformation(String correctOrIncorrect, DataTable dataTable) {
        Map<String,String> data = dataTable.asMap(String.class,String.class);
        String firstName = data.get("First name");
        String lastName = data.get("Last name");
        String email = data.get("email");
        String password = data.get("password");
        SignInPage.enterLoginCredentials(email,password);
    }


    @Then("the user should be authenticated")
    public void the_user_should_be_authenticated() {

    }


    @Then("the user should be redirected to their dashboard")
    public void the_user_should_be_redirected_to_their_dashboard() {

    }



    @Then("the user should be presented with a successful message")
    public void the_user_should_be_presented_with_a_successful_message() {

    }

}
