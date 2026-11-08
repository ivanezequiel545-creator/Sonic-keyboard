package app.velocity.keyboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NativeKeyboardTest {
    private String shell(String command) throws Exception {
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
             FileInputStream input = new FileInputStream(fd.getFileDescriptor());
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048]; int count;
            while((count=input.read(buffer)) != -1) bytes.write(buffer,0,count);
            return bytes.toString("UTF-8").trim();
        }
    }
    @Test public void serviceUsesAndroidInputMethodPermission() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ServiceInfo service = target.getPackageManager().getServiceInfo(new ComponentName(target,VelocityInputMethodService.class),PackageManager.GET_META_DATA | PackageManager.MATCH_DISABLED_COMPONENTS);
        assertEquals("android.permission.BIND_INPUT_METHOD",service.permission);
        assertTrue(service.exported);
        assertEquals(BuildConfig.PREVIEW_STAGE >= 3,service.enabled);
    }
    @Test public void typesAndDeletesInAnotherAppAndAdaptsToNumericAndPasswordFields() throws Exception {
        assertTrue(BuildConfig.PREVIEW_STAGE >= 3);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String component = "app.velocity.keyboard/.VelocityInputMethodService";
        String previous = shell("settings get secure default_input_method");
        String testPackage = InstrumentationRegistry.getInstrumentation().getContext().getPackageName();
        try {
            shell("settings put secure show_ime_with_hard_keyboard 1");
            shell("ime enable " + component);
            shell("ime set " + component);
            shell("am start -W -n " + testPackage + "/app.velocity.keyboard.InputProbeActivity");
            UiObject2 field = device.wait(Until.findObject(By.desc("external-probe-text")),10000); assertNotNull(field); field.click();
            assertTrue(device.wait(Until.hasObject(By.desc("Espacio")),10000));
            for(String key : new String[]{"h","o","l","a"}) { UiObject2 button=device.wait(Until.findObject(By.text(key)),5000); assertNotNull(button); button.click(); }
            assertTrue(device.wait(Until.hasObject(By.text("hola").desc("external-probe-text")),5000));
            device.findObject(By.desc("Borrar carácter")).click();
            assertTrue(device.wait(Until.hasObject(By.text("hol").desc("external-probe-text")),5000));
            device.findObject(By.desc("Emojis o letras")).click();
            device.wait(Until.findObject(By.text("😊")),5000).click();
            device.findObject(By.desc("Emojis o letras")).click();
            device.findObject(By.desc("Borrar carácter")).click();
            assertTrue(device.wait(Until.hasObject(By.text("hol").desc("external-probe-text")),5000));
            device.findObject(By.desc("external-probe-number")).click();
            device.wait(Until.findObject(By.text("1")),5000).click();
            device.findObject(By.text("2")).click();
            assertTrue(device.wait(Until.hasObject(By.text("12").desc("external-probe-number")),5000));
            assertFalse(device.hasObject(By.text("q")));
            device.findObject(By.desc("external-probe-password")).click();
            assertTrue(device.wait(Until.hasObject(By.text("Velocity · campo privado")),5000));
            assertFalse(device.hasObject(By.desc("Emojis o letras")));
        } finally {
            if(previous.contains("/")) shell("ime set " + previous);
            shell("ime disable " + component);
            device.pressHome();
        }
    }
}
