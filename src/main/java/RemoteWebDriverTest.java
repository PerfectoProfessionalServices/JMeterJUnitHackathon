import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResultFactory;
import org.apache.commons.lang.time.StopWatch;
import org.apache.jmeter.protocol.java.sampler.JUnitSampler;
import org.apache.jmeter.threads.JMeterVariables;
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
    private static ThreadLocal<ReportiumClient> reportiumClient = new ThreadLocal<>();
    static boolean runVitals = true;
    static boolean runReportium = true;
    static int timerThreshold = 30000;

    private static void setInitialCapabilities(String user, String password) {
        DesiredCapabilities dcaps = new DesiredCapabilities("MobileOS", "", Platform.ANY);
        dcaps.setCapability("user", user);
        dcaps.setCapability("password", password);
        dcaps.setCapability(WindTunnelUtils.WIND_TUNNEL_PERSONA_CAPABILITY, WindTunnelUtils.EMPTY);
        //dcaps.setCapability(WindTunnelUtils.DEVICE_NETWORK_CAPABILITY, "4g_lte_advanced_good");
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

    private static ReportiumClient createRemoteReportiumClient(RemoteWebDriver driver, DesiredCapabilities capabilities) {
        if (!runReportium) return new ReportiumClientFactory().createLoggerClient();

        PerfectoExecutionContext perfectoExecutionContext = new PerfectoExecutionContext.PerfectoExecutionContextBuilder()
                .withProject(new Project(capabilities.getCapability("scriptName") + "", "Hackathon Demo Version 0.1"))
                .withWebDriver(driver)
                .build();
        return new ReportiumClientFactory().createPerfectoReportiumClient(perfectoExecutionContext);
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
        try {
            timerThreshold = Integer.parseInt(sampler.getThreadContext().getVariables().get("timerThreshold"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        runVitals = !"false".equalsIgnoreCase(sampler.getThreadContext().getVariables().get("runVitals"));
        runReportium = !"false".equalsIgnoreCase(sampler.getThreadContext().getVariables().get("runReportium"));

        setInitialCapabilities(user, password);
        addUserDefinedCapabilities(caps);

        // Call this method if you want the script to share the devices with the Perfecto Lab plugin.
        try {
            PerfectoLabUtils.setExecutionIdCapability(capabilities.get(), host);
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(capabilities.get().toString());
        boolean driverStarted;
        StopWatch timer = new StopWatch();
        timer.start();
        do {
            driver.set(new RemoteWebDriver(new URL("https://" + host + "/nexperience/perfectomobile/wd/hub"), capabilities.get()));
            driverStarted = true;
        } while (!driverStarted && timer.getTime() < timerThreshold);
        reportiumClient.set(createRemoteReportiumClient(driver.get(),capabilities.get()));
        reportiumClient.get().testStart(capabilities.get().getCapability("scriptName") +"", new TestContext("Hackathon", "Jeremy", "Mitch", "Evy", "Rick", "JMeter"));

        driver.get().manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        log.info(driver.get().getCapabilities().toString());
        Library lib = new Library(driver.get(), runVitals);
        lib.vitalsStart();
    }

    @AfterClass
    public static void oneTimeTearDown(  ) {
        // do your one-time tear down here!
        JUnitSampler sampler = new JUnitSampler();
        String reportURL = "WindTunnel Report NOT Found!";
        if (driver.get() == null) return;
        try {

        	Library lib = new Library(driver.get(), runVitals);
            lib.vitalsStop();
            // Retrieve the URL of the Wind Tunnel Report, can be saved to your execution summary and used to download the report at a later point
            reportURL = (String) (driver.get().getCapabilities().getCapability(WindTunnelUtils.WIND_TUNNEL_REPORT_URL_CAPABILITY));
            log.info("WindTunnel Report URL:\n\t" + reportURL);
            sampler.setSuccess(reportURL);
            JMeterVariables vars = sampler.getThreadContext().getVariables();
            vars.put("reportURL", reportURL);
            sampler.getThreadContext().setVariables(vars);
            reportiumClient.get().testStop(TestResultFactory.createSuccess());
            driver.get().close();
        } catch (Exception e) {
            sampler.setFailure(reportURL);
            if (reportiumClient.get() != null)
                reportiumClient.get().testStop(TestResultFactory.createFailure("Test stop failure.", e));
            e.printStackTrace();
        }
        driver.get().quit();
        log.info("Run ended, Driver closed/quit");
    }

    public RemoteWebDriverTest() {}

    @Test
    public void testGetWebSite() {
        reportiumClient.get().testStep("Get Device Driver");
    	Library lib = new Library(driver.get());
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
        reportiumClient.get().testStep("Login and Verify Welcome");
    	Library lib = new Library(driver.get());
        //Login to web application - set user and password
        driver.get().findElement(By.name("username")).sendKeys("John");
        driver.get().findElement(By.name("password")).sendKeys("Perfecto1");
        driver.get().findElement(By.cssSelector(".login>div>button")).click();
        long uxTimer1 = lib.step(30000, "signInBtn", "timer1");
        lib.findText("\"Welcome back John\"");
        long uxTimer2 = lib.step(30000, "welcomeText", "timer2");

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
        params3.put("timeout", timerThreshold / 1000 + "");
        Assert.assertTrue("true".equals(driver.get().executeScript("mobile:checkpoint:text", params3) + ""));

    }
}
