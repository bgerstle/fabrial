package com.eighthlight.fabrial.test.html;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class WebDriverFixture implements AutoCloseable {
  public final WebDriver driver;

  public static WebDriverFixture chrome() {

    var opts = new ChromeOptions();

    opts.addArguments("--disable-gpu",
                      "--headless");

    if (System.getenv("CI") != null) {
      opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
    }

    return new WebDriverFixture(new ChromeDriver(opts));
  }

  public WebDriverFixture(WebDriver driver) {
    this.driver = driver;
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }
}
