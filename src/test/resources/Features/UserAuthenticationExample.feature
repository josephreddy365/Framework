# Cucumber uses Gherkin language syntax to make the code easy to non-programmers read and understand the flow.

# Three sections - FEATURE | BACKGROUND | SCENARIO
  @test
Feature: User Authentication
  # Like a title for what you are doing

#  Background:
#    # Some background info; stuff that is already there; Common to all tests in the feature file.
#    Given the user is already registered to the website

  Scenario: User authentication verification
    # Describes what exactly is going to happen

  # @Before {runs before the first step of each scenario}
    Given I go to home page
    When the user enters lgin credentials
      | First name | Gus           |
      | Last name  | Andrade       |
      | Email      | gus@gmail.co  |
      | Password   | 1q2w3e4r5t    |
    When the user inputs the incorrect personal information
      | First name | Gus           |
      | Last name  | Andrade       |
      | Email      | gus@gmail.co  |
      | Password   | 1q2w3e4r5t    |
    And the user clicks the Login button
    Then I see error message "your details are incorrect"
    When the user inputs the correct personal information
      | First name | Gus           |
      | Last name  | Andrade       |
      | Email      | gus@gmail.com |
      | Password   | 1q2w3e4r5t    |
    Then the user should be authenticated
    And the user should be redirected to their dashboard
    And the user should be presented with a successful message

  # @After {Runs after the last step of each scenario}

#  Scenario: User authentication fail
#    @Before{}
#    Given the user is on the login page
#    When the user inputs the personal information
#    But the user inputs the incorrect information
#    And the user clicks the Login button
#    Then the user should be presented with a error message
#    @After{}