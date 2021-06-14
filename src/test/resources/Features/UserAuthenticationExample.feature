# Cucumber uses Gherkin language syntax to make the code easy to non-programmers read and understand the flow.

# Three sections - FEATURE | BACKGROUND | SCENARIO
@test
Feature: User Authentication

  Scenario: User authentication verification
    Given I go to home page
    When I enter "gus@gmail.com" and "1q2w3e4r5t"
    And I click on submit button
    Then I see message "Authentication"
    And I see message "Authentication failed"




