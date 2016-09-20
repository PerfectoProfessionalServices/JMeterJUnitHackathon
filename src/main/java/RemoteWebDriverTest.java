import org.apache.jmeter.protocol.java.sampler.JUnitSampler;
import org.apache.jorphan.logging.LoggingManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * For programming samples and updated templates refer to the Perfecto GitHub at: https://github.com/PerfectoCode
 */
public class RemoteWebDriverTest {

    private final static org.apache.log.Logger log = LoggingManager.getLoggerForClass();

    private static final String siteUnderTest = "http://nxc.co.il/demoaut/index.php";
    private static ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();
    private static ThreadLocal<DesiredCapabilities> capabilities = new ThreadLocal<>();
    private static Library lib;

    private static void setInitialCapabilities(String user, String password) {
        DesiredCapabilities dcaps = new DesiredCapabilities("MobileOS", "", Platform.ANY);
        dcaps.setCapability("user", user);
        dcaps.setCapability("password", password);
        dcaps.setCapability(WindTunnelUtils.WIND_TUNNEL_PERSONA_CAPABILITY, WindTunnelUtils.EMPTY);
        dcaps.setCapability("scriptName", "HackathonDemoTest");
        capabilities.set(dcaps);
    }

    private static void addUserDefinedCapabilities(String caps) {
        if (caps == null || caps.indexOf("=") < 0)
            return;
        DesiredCapabilities dcaps = capabilities.get();
        for (String capKeyValue : caps.split(","))
            if (capKeyValue != null && capKeyValue.length() > 3 && capKeyValue.indexOf("=") > 0)
                dcaps.setCapability(capKeyValue.split("=")[0], capKeyValue.split("=")[1]);
        capabilities.set(dcaps);
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
            PerfectoLabUtils.setExecutionIdCapability(capabilities.get(), host);
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(capabilities.get().toString());
        driver.set(new RemoteWebDriver(new URL("https://" + host + "/nexperience/perfectomobile/wd/hub"), capabilities.get()));
        driver.get().manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        log.info(driver.get().getCapabilities().toString());
        lib = new Library(driver.get());
        lib.vitalsStart();
        lib.home();
    }

    @AfterClass
    public static void oneTimeTearDown(  ) {
        // do your one-time tear down here!
        JUnitSampler sampler = new JUnitSampler();
        String reportURL = "WindTunnel Report NOT Found!";
        try {
            lib.home();
            lib.vitalsStop();
            // Retrieve the URL of the Wind Tunnel Report, can be saved to your execution summary and used to download the report at a later point
            reportURL = (String) (driver.get().getCapabilities().getCapability(WindTunnelUtils.WIND_TUNNEL_REPORT_URL_CAPABILITY));
            sampler.setSuccess(reportURL);
            log.info("WindTunnel Report URL:\n\t" + reportURL);
            driver.get().close();
        } catch (Exception e) {
            sampler.setFailure(reportURL);
            e.printStackTrace();
        }
        driver.get().quit();
        log.info("Run ended, Driver closed/quit");
    }

    public RemoteWebDriverTest() {}

    @Test
    public void testGetWebSite() {
        driver.get().get(siteUnderTest);
        //WebElement seat = driver.get().findElement(By.id("seat"));
        Assert.assertTrue("true".equals(lib.findText("\"Perfecto Virtual\"") + ""));
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
        long uxTimer1 = lib.step(3000, "signInBtn", "timer1");
        lib.findText("\"Welcome back John\"");
        long uxTimer2 = lib.step(3000, "welcomeText", "timer2");

        //Text checkpoint on "Welcome back John"
        /*(new WebDriverWait(driver.get(), 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.findElement(By.cssSelector("#welcome>h3")) != null;
            }
        });

        String welcomeText = driver.get().findElement(By.cssSelector("#welcome>h3")).getText();
        Assert.assertTrue(welcomeText.contains("Welcome back John"));*/
        Map<String, Object> params3 = new HashMap<>();
        // Check for the text that indicates that the sign in was successful
        params3.put("content", "Welcome back John");
        // allow up-to 30 seconds for the page to display
        params3.put("timeout", "30");
        Assert.assertTrue("true".equals(driver.get().executeScript("mobile:checkpoint:text", params3) + ""));

    }
}
