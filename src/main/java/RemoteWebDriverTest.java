import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * For programming samples and updated templates refer to the Perfecto GitHub at: https://github.com/PerfectoCode
 */
public class RemoteWebDriverTest {

    // TODO Replace <YOUR_CLOUD> with your Perfecto Cloud url. Ex. "partners.perfectomobile.com"
    private static final String host = <YOUR_CLOUD>;
    private static final String siteUnderTest = "http://nxc.co.il/demoaut/index.php";
    private static ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();
    private static DesiredCapabilities capabilities;

    private static void setInitialCapabilities() {
        capabilities = new DesiredCapabilities("MobileOS", "", Platform.ANY);
        // TODO Replace <YOUR_USER> with your Perfecto Cloud username (email) "String"
        capabilities.setCapability("user", <YOUR_USER>);
        // TODO Replace <YOUR_PASSWORD> with your Perfecto Cloud password "String"
        capabilities.setCapability("password", <YOUR_PASSWORD>);
        capabilities.setCapability("os", "Android|iOS");
        capabilities.setCapability(WindTunnelUtils.WIND_TUNNEL_PERSONA_CAPABILITY, WindTunnelUtils.EMPTY);
        capabilities.setCapability("scriptName", "HackathonDemoTest");
    }

    @BeforeClass
    public static void oneTimeSetUp(  ) throws Exception {
        // do your one-time setup here!
        System.out.println("Getting Driver...");

        setInitialCapabilities();
        // Call this method if you want the script to share the devices with the Perfecto Lab plugin.
        try {
            PerfectoLabUtils.setExecutionIdCapability(capabilities, host);
        } catch (IOException e) {
            e.printStackTrace();
        }
        driver.set(new RemoteWebDriver(new URL("https://" + host + "/nexperience/perfectomobile/wd/hub"), capabilities));
        driver.get().manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void oneTimeTearDown(  ) {
        // do your one-time tear down here!
        try {
            // Retrieve the URL of the Wind Tunnel Report, can be saved to your execution summary and used to download the report at a later point
            String reportURL = (String) (driver.get().getCapabilities().getCapability(WindTunnelUtils.WIND_TUNNEL_REPORT_URL_CAPABILITY));
            System.out.println(reportURL);
            driver.get().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        driver.get().quit();
        System.out.println("Run ended");
    }

    public RemoteWebDriverTest() {}

    @Test
    public void testGetWebSite() {
        driver.get().get(siteUnderTest);
        WebElement seat = driver.get().findElement(By.id("seat"));
        Assert.assertNotNull(seat);
    }

    @Test
    public void testGotoSelectSeatPageAndCountSeats() {
        driver.get().findElement(By.id("seat")).click();
        List<WebElement> seats = driver.get().findElements(By.xpath(".//span[contains(@class,'seat ')]"));
        Assert.assertEquals(seats.size(), 165);
    }

    @Test
    public void testSelectRandomSeat(){
        //Find all available seats and select random one
        //Make sure selected seat number appears on screen
        List<WebElement> availableSeats = driver.get().findElements(By.xpath(".//span[contains(@class,'available')]"));
        WebElement selectedSeat = availableSeats.get(new Random().nextInt(availableSeats.size() - 1));
        String seatName = selectedSeat.getAttribute("id");
        selectedSeat.click();
        String seatNumber = driver.get().findElement(By.className("paxseatlabel")).getText();
        Assert.assertEquals(seatName, seatNumber);
    }

    @Test
    public void testClickBackToLogin() {
        driver.get().findElement(By.xpath("//*[text()='Back']")).click();
        Assert.assertNotNull(driver.get().findElement(By.name("username")));
    }

    @Test
    public void testLoginToSiteVerifyWelcome() {
        //Login to web application - set user and password
        driver.get().findElement(By.name("username")).sendKeys("John");
        driver.get().findElement(By.name("password")).sendKeys("Perfecto1");
        driver.get().findElement(By.cssSelector(".login>div>button")).click();

        //Text checkpoint on "Welcome back John"
        (new WebDriverWait(driver.get(), 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.findElement(By.cssSelector("#welcome>h3")) != null;
            }
        });

        String welcomeText = driver.get().findElement(By.cssSelector("#welcome>h3")).getText();
        Assert.assertTrue(welcomeText.contains("Welcome back John"));
    }
}
