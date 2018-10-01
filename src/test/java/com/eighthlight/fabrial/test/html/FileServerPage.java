package com.eighthlight.fabrial.test.html;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class FileServerPage {
  private final WebDriver driver;
  private final String baseURL;

  public FileServerPage(WebDriver driver, String baseURL) {
    this.driver = driver;
    this.baseURL = baseURL;
  }

  public FileServerPage goToPageForDirectory(String path) {
    driver.navigate().to(Paths.get(baseURL, path).toString());
    return this;
  }

  public boolean listContainsFile(String filename) {
    return fileInListWithName(filename).isPresent();
  }

  public FileServerPage clickElementForFile(String filename) {
    fileInListWithName(filename)
        .orElseThrow(NoSuchElementException::new)
        .click();
    return this;
  }

  public List<WebElement> fileList() {
    return driver.findElements(By.cssSelector("ul > li > a"));
  }

  public String currentUrl() {;
    return driver.getCurrentUrl();
  }

  public String bodyText() {
    return driver.findElement(By.tagName("body")).getText();
  }

  private Optional<WebElement> fileInListWithName(String filename) {
    return fileList().stream().filter(e -> e.getText().equals(filename)).findFirst();
  }
}
