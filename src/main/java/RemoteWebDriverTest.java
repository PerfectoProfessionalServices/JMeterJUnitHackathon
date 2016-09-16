import org.apache.jmeter.protocol.java.sampler.JUnitSampler;
import org.apache.jorphan.logging.LoggingManager;
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

    private final static org.apache.log.Logger log = LoggingManager.getLoggerForClass();

    private static final String siteUnderTest = "http://nxc.co.il/demoaut/index.php";
    private static ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();
    private static DesiredCapabilities capabilities;

    private static void setInitialCapabilities(String user, String password) {
        capabilities = new DesiredCapabilities("MobileOS", "", Platform.ANY);
        capabilities.setCapability("user", user);
        capabilities.setCapability("password", password);
        capabilities.setCapability(WindTunnelUtils.WIND_TUNNEL_PERSONA_CAPABILITY, WindTunnelUtils.EMPTY);
        capabilities.setCapability("scriptName", "HackathonDemoTest");
    }

    private static void addUserDefinedCapabilities(String caps) {
        if (caps == null || caps.indexOf("=") < 0)
            return;
        for (String capKeyValue : caps.split(","))
            if (capKeyValue != null && capKeyValue.length() > 3 && capKeyValue.indexOf("=") > 0)
                capabilities.setCapability(capKeyValue.split("=")[0], capKeyValue.split("=")[1]);
    }

    @BeforeClass
    public static void oneTimeSetUp(  ) throws Exception {
        // do your one-time setup here!
        log.info("Getting Driver...");

        JUnitSampler sampler = new JUnitSampler();
        String host = sampler.getThreadContext().getVariables().get("perfectoHost");
        String user = sampler.getThreadContext().getVariables().get("perfectoUser");
        String password = sampler.getThreadContext().getVariables().get("perfectoPassword");
        String caps = sampler.getThreadContext().getVariables().get("desiredCapabilities");

        setInitialCapabilities(user, password);
        addUserDefinedCapabilities(caps);

        // Call this method if you want the script to share the devices with the Perfecto Lab plugin.
        try {
            PerfectoLabUtils.setExecutionIdCapability(capabilities, host);
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(capabilities.toString());
        driver.set(new RemoteWebDriver(new URL("https://" + host + "/nexperience/perfectomobile/wd/hub"), capabilities));
        driver.get().manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        log.info(driver.get().getCapabilities().toString());
    }

    @AfterClass
    public static void oneTimeTearDown(  ) {
        // do your one-time tear down here!
        try {
            // Retrieve the URL of the Wind Tunnel Report, can be saved to your execution summary and used to download the report at a later point
            String reportURL = (String) (driver.get().getCapabilities().getCapability(WindTunnelUtils.WIND_TUNNEL_REPORT_URL_CAPABILITY));
            log.info("WindTunnel Report URL:\n\t" + reportURL);
            driver.get().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        driver.get().quit();
        log.info("Run ended, Driver closed/quit");
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
