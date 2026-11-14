package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.nativecore.SecurityClassification
import com.example.nativecore.VectraNativeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vectra", appName)
    }

    @Test
    fun `main activity handles incoming http view intent`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org"))
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        val activity = controller.get()
        assertTrue(!activity.isFinishing)
    }

    @Test
    fun `url normalization and security analysis work`() {
        val normalized = VectraNativeEngine.normalizeInput("wikipedia.org", 0, true)
        assertEquals("https://wikipedia.org", normalized.finalUrl)
        assertEquals(SecurityClassification.SECURE_HTTPS, VectraNativeEngine.analyzeSecurity(normalized.finalUrl))
    }
}
