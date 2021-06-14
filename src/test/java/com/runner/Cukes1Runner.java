import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;


@SuppressWarnings("ALL")
@CucumberOptions(
        plugin = {"pretty",
                "json:target/cucumber-reports/cucumber.json"
        },
        features = {"src/test/resources/Features"},
        glue = {"com.ui.stepdefinitions"},
        tags = {"@test"},
        strict = true
        )
public class Cukes1Runner extends AbstractTestNGCucumberTests {

}
