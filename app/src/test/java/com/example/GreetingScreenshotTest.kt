package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.EmergencyReport
import com.example.ui.ReportItemCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun report_card_screenshot() {
    val mockReport = EmergencyReport(
      id = 1,
      alertType = "Terremoto / Sismo",
      timestamp = 1719750000000L, // Static timestamp for consistent screenshot
      latitude = 4.7110,
      longitude = -74.0721,
      severityColor = "RED",
      country = "Colombia",
      city = "Bogotá",
      lastUpdate = 1719750000000L
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ReportItemCard(report = mockReport, onClick = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

