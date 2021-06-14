@test
Feature: This is example feature
  Scenario: This is Example scenario

  Scenario: User authentication verification
    Given I go to home page
    When I enter "gus@gmail.com" and "1q2w3e4r5t"
    And I click on submit button
    Then I see message "Authentication"
    And I see message "Authentication failed"