package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixture;
import com.eighthlight.fabrial.test.html.FileServerPage;
import com.eighthlight.fabrial.test.html.WebDriverFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

@Tag("acceptance")
public class GetDirContentsAcceptanceTest {
  @Test
  void clickingOnFilePresentsContents() throws Exception {
    try (var baseDirFixture = new TempDirectoryFixture();
        var fileFixture = new TempFileFixture(baseDirFixture.tempDirPath);
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            baseDirFixture.tempDirPath));
        var driverFixture = WebDriverFixture.chrome()) {
      var fileContents = "foo";
      fileFixture.write(new ByteArrayInputStream(fileContents.getBytes()));

      serverFixture.server.start();
      driverFixture.driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
      var page = new FileServerPage(driverFixture.driver,
                                    "http://localhost:" + ServerConfig.DEFAULT_PORT);

      assertThat(
          page.goToPageForDirectory("/")
              .clickElementForFile(fileFixture.tempFilePath.getFileName().toString())
              .bodyText(),
          is(fileContents));
    }
  }

  @Test
  void navigatingToDirectoryDisplaysContents() throws Exception {
    try (var baseDirFixture = new TempDirectoryFixture();
        var fileFixture1 = new TempFileFixture(baseDirFixture.tempDirPath);
        var childDirFixture = new TempDirectoryFixture(baseDirFixture.tempDirPath);
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            baseDirFixture.tempDirPath));
        var driverFixture = WebDriverFixture.chrome()) {
      serverFixture.server.start();
      driverFixture.driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
      var page = new FileServerPage(driverFixture.driver,
                                    "http://localhost:" + ServerConfig.DEFAULT_PORT);

      var rootFiles = page.goToPageForDirectory("/").fileList();

      var rootFileElementTexts = rootFiles.stream()
                                          .map(WebElement::getText)
                                          .collect(Collectors.toList());
      assertThat(
          rootFileElementTexts,
          containsInAnyOrder(
              fileFixture1.tempFilePath.getFileName().toString(),
              childDirFixture.tempDirPath.getFileName().toString()));

      var rootFileElementTags =
          rootFiles.stream()
                   .map(WebElement::getTagName)
                   .distinct()
                   .collect(Collectors.toList());
      assertThat(rootFileElementTags, is(List.of("a")));
    }
  }

  @Test
  void nestedFileNavigationAndRetrieval() throws Exception {
    int testPort = 8082;
    try (var baseDirFixture = new TempDirectoryFixture();
        var childDirFixture = new TempDirectoryFixture(baseDirFixture.tempDirPath);
        var nestedFileFixture = new TempFileFixture(childDirFixture.tempDirPath);
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            baseDirFixture.tempDirPath));
        var driverFixture = WebDriverFixture.chrome()) {
      var fileContents = "bar";
      nestedFileFixture.write(new ByteArrayInputStream(fileContents.getBytes()));

      serverFixture.server.start();
      driverFixture.driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
      var page = new FileServerPage(driverFixture.driver,
                                    "http://localhost:" + ServerConfig.DEFAULT_PORT);

      assertThat(
          page.goToPageForDirectory("/")
              .clickElementForFile(childDirFixture.tempDirPath.getFileName().toString())
              .clickElementForFile(nestedFileFixture.tempFilePath.getFileName().toString())
              .bodyText(),
          is(fileContents));
    }
  }
}
