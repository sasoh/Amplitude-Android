package com.posemeshamplitude.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class AmplitudeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetInstance() {
        AmplitudeClient a = PosemeshAmplitudegetInstance();
        AmplitudeClient b = PosemeshAmplitudegetInstance("");
        AmplitudeClient c = PosemeshAmplitudegetInstance(null);
        AmplitudeClient d = PosemeshAmplitudegetInstance(Constants.DEFAULT_INSTANCE);
        AmplitudeClient e = PosemeshAmplitudegetInstance("app1");
        AmplitudeClient f = PosemeshAmplitudegetInstance("app2");

        assertSame(a, b);
        assertSame(b, c);
        assertSame(c, d);
        assertSame(d, PosemeshAmplitudegetInstance());
        assertNotSame(d, e);
        assertSame(e, PosemeshAmplitudegetInstance("app1"));
        assertNotSame(e, f);
        assertSame(f, PosemeshAmplitudegetInstance("app2"));

        // test for instance name case insensitivity
        assertSame(e, PosemeshAmplitudegetInstance("APP1"));
        assertSame(e, PosemeshAmplitudegetInstance("App1"));
        assertSame(e, PosemeshAmplitudegetInstance("aPP1"));
        assertSame(e, PosemeshAmplitudegetInstance("apP1"));

        assertTrue(PosemeshAmplitude.instances.size() == 3);
        assertTrue(PosemeshAmplitude.instances.containsKey(Constants.DEFAULT_INSTANCE));
        assertTrue(PosemeshAmplitude.instances.containsKey("app1"));
        assertTrue(PosemeshAmplitude.instances.containsKey("app2"));
    }

    @Test
    public void testSeparateInstancesLogEventsSeparately() {
        PosemeshAmplitude.instances.clear();
        DatabaseHelper.instances.clear();

        String newInstance1 = "newApp1";
        String newApiKey1 = "1234567890";
        String newInstance2 = "newApp2";
        String newApiKey2 = "0987654321";

        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        DatabaseHelper newDbHelper1 = DatabaseHelper.getDatabaseHelper(context, newInstance1);
        DatabaseHelper newDbHelper2 = DatabaseHelper.getDatabaseHelper(context, newInstance2);

        // Setup existing Databasefile
        oldDbHelper.insertOrReplaceKeyValue("device_id", "oldDeviceId");
        oldDbHelper.insertOrReplaceKeyLongValue("sequence_number", 1000L);
        oldDbHelper.addEvent("oldEvent1");
        oldDbHelper.addIdentify("oldIdentify1");
        oldDbHelper.addIdentify("oldIdentify2");

        // Verify persistence of old database file in default instance
        PosemeshAmplitude.getInstance().initialize(context, apiKey);
        Shadows.shadowOf(PosemeshAmplitude.getInstance().logThread.getLooper()).runToEndOfTasks();
        assertEquals(PosemeshAmplitude.getInstance().getDeviceId(), "oldDeviceId");
        assertEquals(PosemeshAmplitude.getInstance().getNextSequenceNumber(), 1001L);
        assertTrue(oldDbHelper.dbFileExists());
        assertFalse(newDbHelper1.dbFileExists());
        assertFalse(newDbHelper2.dbFileExists());

        // init first new app and verify separate database file
        PosemeshAmplitude.getInstance(newInstance1).initialize(context, newApiKey1);
        Shadows.shadowOf(
            PosemeshAmplitude.getInstance(newInstance1).logThread.getLooper()
        ).runToEndOfTasks();
        assertTrue(newDbHelper1.dbFileExists()); // db file is created after deviceId initialization

        assertFalse(newDbHelper1.getValue("device_id").equals("oldDeviceId"));
        assertEquals(
            newDbHelper1.getValue("device_id"), PosemeshAmplitude.getInstance(newInstance1).getDeviceId()
        );
        assertEquals(PosemeshAmplitude.getInstance(newInstance1).getNextSequenceNumber(), 1L);
        assertEquals(newDbHelper1.getEventCount(), 0);
        assertEquals(newDbHelper1.getIdentifyCount(), 0);

        // init second new app and verify separate database file
        PosemeshAmplitude.getInstance(newInstance2).initialize(context, newApiKey2);
        Shadows.shadowOf(
            PosemeshAmplitude.getInstance(newInstance2).logThread.getLooper()
        ).runToEndOfTasks();
        assertTrue(newDbHelper2.dbFileExists()); // db file is created after deviceId initialization

        assertFalse(newDbHelper2.getValue("device_id").equals("oldDeviceId"));
        assertEquals(
            newDbHelper2.getValue("device_id"), PosemeshAmplitude.getInstance(newInstance2).getDeviceId()
        );
        assertEquals(PosemeshAmplitude.getInstance(newInstance2).getNextSequenceNumber(), 1L);
        assertEquals(newDbHelper2.getEventCount(), 0);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);

        // verify existing database still intact
        assertTrue(oldDbHelper.dbFileExists());
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        assertEquals(oldDbHelper.getLongValue("sequence_number").longValue(), 1001L);
        assertEquals(oldDbHelper.getEventCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify both apps can modify their database independently and not affect old database
        newDbHelper1.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertFalse(newDbHelper2.getValue("device_id").equals("fakeDeviceId"));
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        newDbHelper1.addIdentify("testIdentify3");
        assertEquals(newDbHelper1.getIdentifyCount(), 1);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        newDbHelper2.insertOrReplaceKeyValue("device_id", "brandNewDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertEquals(newDbHelper2.getValue("device_id"), "brandNewDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        newDbHelper2.addEvent("testEvent2");
        newDbHelper2.addEvent("testEvent3");
        assertEquals(newDbHelper1.getEventCount(), 0);
        assertEquals(newDbHelper2.getEventCount(), 2);
        assertEquals(oldDbHelper.getEventCount(), 1);
    }

}
