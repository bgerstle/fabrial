package com.eighthlight.fabrial.test.html;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class WebDriverFixture implements AutoCloseable {
  public final WebDriver driver;

  public static WebDriverFixture chrome() {
    return new WebDriverFixture(new ChromeDriver());
  }

  public WebDriverFixture(WebDriver driver) {
    this.driver = driver;
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }
}
