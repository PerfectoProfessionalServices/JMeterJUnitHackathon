import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Library {
	private RemoteWebDriver driver;

	public Library(RemoteWebDriver driver) {
		this.driver = driver;
	}
	
	

	public void launchApplication(String app) {

		Map<String, Object> params1 = new HashMap<>();
		params1.put("name", app);
		Object result1 = driver.executeScript("mobile:application:open",
				params1);

	}
	
	public void vitalsStart()
	{
		Map<String, Object> params1 = new HashMap<>();
		List<String> vitals1 = new ArrayList<>();
		vitals1.add("all");
		params1.put("vitals", vitals1);
		List<String> sources1 = new ArrayList<>();
		sources1.add("device");
		params1.put("sources", sources1);
		Object result1 = driver.executeScript("mobile:monitor:start", params1);
		
		
	}
	
	public void vitalsStop()
	{
		Map<String, Object> params2 = new HashMap<>();
		Object result2 = driver.executeScript("mobile:monitor:stop", params2);
		
		
	}
	
	public long step(long threshold, String description, String name)
	{
		long timer = getUXTimer();
		WindTunnelUtils.reportTimer(driver, timer, threshold, description, name);
		return timer;
		
	}

	public void home() {

		Map<String, Object> params1 = new HashMap<>();
		params1.put("keySequence", "HOME");
		Object result1 = driver.executeScript("mobile:presskey", params1);

	}

	public void waitForElement(By xpath, int timeout) {
		WebElement we = (new WebDriverWait(driver, timeout))
				.until(ExpectedConditions.elementToBeClickable(xpath));
		;
	}

	public Object findText(String text) {
		Map<String, Object> params3 = new HashMap<>();
		params3.put("content", text);
		params3.put("target", "any");
		params3.put("timeout", "40");
		params3.put("threshold", "90");
		Object result = driver.executeScript("mobile:text:find", params3);
		return result;
	}

	public void closeApplication(String app) {
		try {
			Map<String, Object> params4 = new HashMap<>();
			params4.put("name", app);
			Object result4 = driver.executeScript("mobile:application:close",
					params4);
		} catch (Exception ex) {
		}
	}

	public long timerGet(String timerType) {
		String command = "mobile:timer:info";
		Map<String, String> params = new HashMap<String, String>();
		params.put("type", timerType);
		long result = (long) driver.executeScript(command, params);
		return result;
	}

	public long getUXTimer() {
		return timerGet("ux");
	}

	public boolean findByImage(String repoKey) {
		Map<String, Object> params1 = new HashMap<>();
		params1.put("content", repoKey);
		params1.put("source", "camera");
		params1.put("timeout", "30");
		params1.put("measurement", "accurate");
		params1.put("threshold", "90");
		Object result1 = driver.executeScript("mobile:image:find", params1);

		if (result1.equals("true")) {
			return true;
		} else {
			return false;
		}
	}

}
