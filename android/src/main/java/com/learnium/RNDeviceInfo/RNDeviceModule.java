package com.learnium.RNDeviceInfo;

import android.Manifest;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.provider.Settings.Secure;
import android.webkit.WebSettings;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.app.ActivityManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.lang.Runtime;

import javax.annotation.Nullable;

public class RNDeviceModule extends ReactContextBaseJavaModule {

  ReactApplicationContext reactContext;

  WifiInfo wifiInfo;

  DeviceType deviceType;
  public RNDeviceModule(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    this.deviceType = getDeviceType(reactContext);
  }

  @Override
  public String getName() {
    return "RNDeviceInfo";
  }

  private WifiInfo getWifiInfo() {
    if ( this.wifiInfo == null ) {
      WifiManager manager = (WifiManager) reactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      this.wifiInfo = manager.getConnectionInfo();
    }
    return this.wifiInfo;
  }

  private String getCurrentLanguage() {
    Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return current.toLanguageTag();
    } else {
      StringBuilder builder = new StringBuilder();
      builder.append(current.getLanguage());
      if (current.getCountry() != null) {
        builder.append("-");
        builder.append(current.getCountry());
      }
      return builder.toString();
    }
  }

  private String getCurrentCountry() {
    Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
    return current.getCountry();
  }

  private Boolean isEmulator() {
    return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.toLowerCase().contains("droid4x")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.HARDWARE.contains("vbox86")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("sdk_x86")
            || Build.PRODUCT.contains("vbox86p")
            || Build.PRODUCT.contains("emulator")
            || Build.PRODUCT.contains("simulator")
            || Build.BOARD.toLowerCase().contains("nox")
            || Build.BOOTLOADER.toLowerCase().contains("nox")
            || Build.HARDWARE.toLowerCase().contains("nox")
            || Build.PRODUCT.toLowerCase().contains("nox")
            || Build.SERIAL.toLowerCase().contains("nox")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"));
  }

  private Boolean isTablet() {
    return deviceType == DeviceType.TABLET;
  }

  private Boolean is24Hour() {
    return android.text.format.DateFormat.is24HourFormat(this.reactContext.getApplicationContext());
  }

  private static DeviceType getDeviceType(ReactApplicationContext reactContext) {
    DeviceType deviceTypeFromConfig = getDeviceTypeFromResourceConfiguration(reactContext);

    if (deviceTypeFromConfig != null && deviceTypeFromConfig != DeviceType.UNKNOWN) {
      return deviceTypeFromConfig;
    }

    return  getDeviceTypeFromPhysicalSize(reactContext);
  }

  // Use `smallestScreenWidthDp` to determine the screen size
  // https://android-developers.googleblog.com/2011/07/new-tools-for-managing-screen-sizes.html
  private  static  DeviceType getDeviceTypeFromResourceConfiguration(ReactApplicationContext reactContext) {
    int smallestScreenWidthDp = reactContext.getResources().getConfiguration().smallestScreenWidthDp;

    if (smallestScreenWidthDp == Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
      return  DeviceType.UNKNOWN;
    }

    return  smallestScreenWidthDp >= 600 ? DeviceType.TABLET : DeviceType.HANDSET;
  }

  private static DeviceType getDeviceTypeFromPhysicalSize(ReactApplicationContext reactContext) {
    // Find the current window manager, if none is found we can't measure the device physical size.
    WindowManager windowManager = (WindowManager) reactContext.getSystemService(Context.WINDOW_SERVICE);

    if (windowManager == null) {
      return DeviceType.UNKNOWN;
    }

    // Get display metrics to see if we can differentiate handsets and tablets.
    // NOTE: for API level 16 the metrics will exclude window decor.
    DisplayMetrics metrics = new DisplayMetrics();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      windowManager.getDefaultDisplay().getRealMetrics(metrics);
    } else {
      windowManager.getDefaultDisplay().getMetrics(metrics);
    }

    // Calculate physical size.
    double widthInches = metrics.widthPixels / (double) metrics.xdpi;
    double heightInches = metrics.heightPixels / (double) metrics.ydpi;
    double diagonalSizeInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));

    if (diagonalSizeInches >= 3.0 && diagonalSizeInches <= 6.9) {
      // Devices in a sane range for phones are considered to be Handsets.
      return DeviceType.HANDSET;
    } else if (diagonalSizeInches > 6.9 && diagonalSizeInches <= 18.0) {
      // Devices larger than handset and in a sane range for tablets are tablets.
      return DeviceType.TABLET;
    } else {
      // Otherwise, we don't know what device type we're on/
      return DeviceType.UNKNOWN;
    }
  }

  @ReactMethod
  public void isPinOrFingerprintSet(Callback callback) {
    KeyguardManager keyguardManager = (KeyguardManager) this.reactContext.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); //api 16+
    callback.invoke(keyguardManager.isKeyguardSecure());
  }

  @ReactMethod
  public void getIpAddress(Promise p) {
    String ipAddress = Formatter.formatIpAddress(getWifiInfo().getIpAddress());
    p.resolve(ipAddress);
  }

  @ReactMethod
  public void getMacAddress(Promise p) {
    String macAddress = getWifiInfo().getMacAddress();
    p.resolve(macAddress);
  }

  @ReactMethod
  public String getCarrier() {
    TelephonyManager telMgr = (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
    return telMgr.getNetworkOperatorName();
  }

  @Override
  public @Nullable Map<String, Object> getConstants() {
    HashMap<String, Object> constants = new HashMap<String, Object>();

    PackageManager packageManager = this.reactContext.getPackageManager();
    String packageName = this.reactContext.getPackageName();

    constants.put("appVersion", "not available");
    constants.put("appName", "not available");
    constants.put("buildVersion", "not available");
    constants.put("buildNumber", 0);

    try {
      PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      String applicationName = this.reactContext.getApplicationInfo().loadLabel(this.reactContext.getPackageManager()).toString();
      constants.put("appVersion", info.versionName);
      constants.put("buildNumber", info.versionCode);
      constants.put("firstInstallTime", info.firstInstallTime);
      constants.put("lastUpdateTime", info.lastUpdateTime);
      constants.put("appName", applicationName);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    String deviceName = "Unknown";

    String permission = "android.permission.BLUETOOTH";
    int res = this.reactContext.checkCallingOrSelfPermission(permission);
    if (res == PackageManager.PERMISSION_GRANTED) {
      try {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        if (myDevice != null) {
          deviceName = myDevice.getName();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }



    try {
      if (Class.forName("com.google.android.gms.iid.InstanceID") != null) {
        constants.put("instanceId", com.google.android.gms.iid.InstanceID.getInstance(this.reactContext).getId());
      }
    } catch (ClassNotFoundException e) {
      constants.put("instanceId", "N/A: Add com.google.android.gms:play-services-gcm to your project.");
    }
    constants.put("serialNumber", Build.SERIAL);
    constants.put("deviceName", deviceName);
    constants.put("systemName", "Android");
    constants.put("systemVersion", Build.VERSION.RELEASE);
    constants.put("model", Build.MODEL);
    constants.put("brand", Build.BRAND);
    constants.put("deviceId", Build.BOARD);
    constants.put("apiLevel", Build.VERSION.SDK_INT);
    constants.put("deviceLocale", this.getCurrentLanguage());
    constants.put("deviceCountry", this.getCurrentCountry());
    constants.put("uniqueId", Secure.getString(this.reactContext.getContentResolver(), Secure.ANDROID_ID));
    constants.put("systemManufacturer", Build.MANUFACTURER);
    constants.put("bundleId", packageName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      try {
        constants.put("userAgent", WebSettings.getDefaultUserAgent(this.reactContext));
      } catch (RuntimeException e) {
        constants.put("userAgent", System.getProperty("http.agent"));
      }
    }
    constants.put("timezone", TimeZone.getDefault().getID());
    constants.put("isEmulator", this.isEmulator());
    constants.put("isTablet", this.isTablet());
    constants.put("is24Hour", this.is24Hour());
    if (getCurrentActivity() != null &&
            (getCurrentActivity().checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                    getCurrentActivity().checkCallingOrSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED ||
                    getCurrentActivity().checkCallingOrSelfPermission("android.permission.READ_PHONE_NUMBERS") == PackageManager.PERMISSION_GRANTED)) {
      TelephonyManager telMgr = (TelephonyManager) this.reactContext.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
      constants.put("phoneNumber", telMgr.getLine1Number());
    }
    constants.put("carrier", this.getCarrier());

    Runtime rt = Runtime.getRuntime();
    constants.put("maxMemory", rt.maxMemory());
    ActivityManager actMgr = (ActivityManager) this.reactContext.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    actMgr.getMemoryInfo(memInfo);
    constants.put("totalMemory", memInfo.totalMem);

    return constants;
  }
}
