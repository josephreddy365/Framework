Feature: This is example feature
@logo
  Scenario: This is Example scenario

  Given I go to home page
  When I enter login credentials
    | username | password |
    | test     | test     |
    Then I can login
    #Given,When , Then , And , But
    # Datatables
   # Cucumber Annotations @Before @After