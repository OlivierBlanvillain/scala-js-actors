package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class Integration extends BrowserSpecification {

  "Two browsers" should {

    "see each other" in new WithTwoBrowsers(WebDriverFactory(FIREFOX), WebDriverFactory(FIREFOX)) {
      browser1 goTo "/"
      browser2 goTo "/"
      browser2 waitUntil browser1.pageSource.contains("lol")
    }

  }
}
