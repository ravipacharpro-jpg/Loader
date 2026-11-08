# FeaturesticBox.aar — Connection / Integration Issues

**AAR:** `app/libs/FeaturesticBox.aar` (2,064,195 bytes, md5 `176c75ea2756061f903637470403373b`)
**Purana AAR:** `sbox-release.aar` (1,699,727 bytes) — hata diya gaya
**Build status:** `:app:assembleDebug` → **BUILD SUCCESSFUL** (APK ~2.6 MB, sirf `libfbox.so` andar hai, `libzenin.so` nahi; remote `FeaturesticBox.so` runtime me download hota hai)

> Seedha jawab: **AAR khud connect ho raha hai** — build pass hai. Jo "connect nahi ho raha" jaisa lagta hai, wo actually **API rename breaks** the + Loader ka purana `libzenin.so` dependency tha. Neeche har issue point-wise hai taaki AAR me fix kar sako.

---

## 1. BLOCKERS — compile todhne wale (AAR ke andar fix karne honge)

### 1.1 `com.sbox.core.system.api.BoxActivate` — poori activation API replace ho gayi

| Purana AAR | Naya AAR | Status |
|---|---|---|
| `activateBox(String)` | **REMOVED** → `activateSdk(String)` | ❌ break |
| `activateBox(Context, String)` | **REMOVED** → `activateSdk(Context, String)` | ❌ break |
| `getLicenseMessage()` | **REMOVED** → `getServerMessage()` | ❌ break |
| `isChecking()` | **REMOVED** (koi replacement nahi) | ❌ break |
| `addListener(LicenseListener)` | **REMOVED** | ❌ break |
| `removeListener(LicenseListener)` | **REMOVED** | ❌ break |
| `LicenseListener` (interface) | **REMOVED** | ❌ break |
| — | `activate(String)` / `activate(Context,String)` / `activate()` → `boolean` | ✅ naya |
| — | `getActivatedSdk()` / `getActivatedStatus()` / `isActivated()` | ✅ naya |
| — | `getNetwork()`, `resetVirtualDeviceIdentity()`, `loadCustomLibrary(String)` | ✅ naya |

**AAR fix:** purane method names ko `@Deprecated` wrapper ke roop me wapas add karo (binary compatibility), ya har jagah `activateSdk` use karo.
**Loader me kiya:** `BoxActivate.activateBox(...)` → `BoxActivate.activateSdk(...)`.

---

### 1.2 `com.sbox.entity.pm.InstallResult` — fields parent class me chali gayi

Naya AAR: `public class com.sbox.entity.pm.InstallResult extends com.featurestic.box.entity.pm.InstallResult`

| Purana | Naya |
|---|---|
| `public boolean success;` | ❌ field hat gaya (parent se inherit hota hai) |
| `public String msg;` | ❌ field hat gaya (parent se inherit hota hai) |
| `public String packageName;` | ❌ **poora gayab** |
| `public static final String TAG` | ❌ gayab |
| `describeContents()` / `writeToParcel()` | ❌ gayab |
| — | ✅ `static InstallResult from(com.featurestic.box.entity.pm.InstallResult)` |

**Impact:** `result.success` / `result.msg` compile ho jate hain (inheritance), lekin **`result.packageName` use karte hi compile fail**. Reflection se field padhne wala code bhi tootega.

**AAR fix:** `packageName` field + `TAG` wapas add karo `com.sbox.entity.pm.InstallResult` me.

---

### 1.3 `com.sbox.SboxCore` — bahut saare public methods gayab

Naya AAR: `public class com.sbox.SboxCore extends com.featurestic.box.FeaturesticBoxCore`

**REMOVED (compile break agar use ho raha ho):**
```
public static Context getContext();
public static PackageManager getPackageManager();
public AppConfig getAppConfig();
public List<BUserInfo> getUsers();
public BUserInfo createUser(int);
public boolean checkSelfPermission(String);
public boolean isHideRoot();
public boolean isHideXposed();
public boolean isXPEnable();
public boolean isModuleEnable(String);
public boolean isInstalledXposedModule(String);
public boolean isXposedModule(File);
public boolean isSupportGms();
public boolean isInstallGms(int);
public boolean uninstallGms(int);
public boolean requestInstallPackage(File);
public ApplicationInfo getApplicationInfo(String, int, int);
public boolean isMainProcess() / isServerProcess() / isBlackProcess();
public Thread.UncaughtExceptionHandler getExceptionHandler();
public List<AppLifecycleCallback> getAppLifecycleCallbacks();
public List<InstalledModule> getInstalledXPModules();
```

**ADDED:**
```
public boolean isInstalled(String) / isInstalled(String, int)
public boolean isAppRunning(String) / isAppRunning(String, int)
public boolean launchApk(String)
public featurestic InstallResult installPackage(...) / installPackageAsUser(...) / installGms(int) / installBundleAsUser(File,int)
```

**Note:** `installPackageAsUser(String,int)` ab **do overloads** return karta hai — `com.sbox...InstallResult` aur `com.featurestic...InstallResult`. Java me sirf return type alag hone se overload nahi banta, isliye **ambiguity / bridge method** ka risk hai. AAR me check karo ki bridge methods `ACC_BRIDGE` ke saath hain.

---

### 1.4 `com.sbox.core.env.BEnvironment` — class shift ho gayi

Naya: `public class com.sbox.core.env.BEnvironment extends com.featurestic.box.core.env.BEnvironment`

Purane saare static methods (`getDataDir`, `getAppDir`, `getExternalObbDir`, `ObbDirectory`, `getBaseApkDir`, `getAppRootDir`, …) `com.sbox` se **remove** dikh rahe hain — ye sirf **inherit** ho rahe hain. Matlab:
- ✅ `BEnvironment.getExternalObbDir(...)` compile hota hai (static inheritance)
- ❌ `import static com.sbox.core.env.BEnvironment.*` ya reflection `getDeclaredMethod` **fail** hoga
- ❌ Purane AAR ke saath binary-compiled koi bhi library `NoSuchMethodError` de sakti hai

**AAR fix:** `com.sbox.core.env.BEnvironment` me purane static methods ko explicit delegate karo (inheritance pe mat chhodo).

---

### 1.5 `ClientConfiguration` / `AppLifecycleCallback` — ab featurestic type ke shim hain

```
public abstract class ClientConfiguration extends com.featurestic.box.app.configuration.ClientConfiguration
public class AppLifecycleCallback extends com.featurestic.box.app.configuration.AppLifecycleCallback
```

**Removed:**
- `AppLifecycleCallback.EMPTY` (static field) ❌
- `beforeMainApplicationAttach`, `afterMainApplicationAttach`, `beforeMainActivityOnCreate`, `afterMainActivityOnCreate`, `beforeMainLaunchApk` ❌
- `ClientConfiguration.isEnableLauncherActivity()` ❌

**Added (side-effect):** `SboxCore.addAppLifecycleCallback(...)` ab **featurestic** type leta hai, `com.sbox` type nahi. `com.sbox` callback pass karne par bhi chalta hai (kyunki extends), lekin overload resolution bhatak sakta hai.

**AAR fix:** `com.sbox` package me purane method signatures ke explicit overloads rakho.

---

## 2. MANIFEST issues (AAR merge hone par app me ghus rahe hain)

### 2.1 AAR 182 permissions declare karta hai ⚠️
Sabse bada practical issue. AAR ka manifest merge hone se app me ye sab aa jate hain:
`SEND_SMS`, `RECEIVE_SMS`, `READ_SMS`, `WRITE_SMS`, `READ_CALL_LOG`, `WRITE_CALL_LOG`, `CALL_PHONE`, `CAMERA`, `ACCESS_FINE_LOCATION`, `READ_CONTACTS`, `WRITE_CONTACTS`, `MANAGE_ACCOUNTS`, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`, `READ_LOGS`, `PACKAGE_USAGE_STATS`, `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES`, …

**Kyun problem:** Play Store / antivirus / user ko "spyware" lagta hai; Play review reject. Ye AAR ke `<uses-permission>` block me hardcoded hai.
**AAR fix:** sirf zaroori permissions rakho, baaki `tools:node="remove"` se app side hatao.

### 2.2 25 proxy process slots (P0…P24)
AAR ye declare karta hai:
- `ProxyActivity$P0..P24` (25 activities)
- `TransparentProxyActivity$P0..P24` (25)
- `ProxyPendingActivity$P0..P24` (25)
- `ProxyService$P0..P24` (25, `foregroundServiceType="specialUse"`)
- `ProxyJobService$P0..P24` (25)
- `ProxyContentProvider$P0..P24` (25, authority `${applicationId}.featurestic_proxy_provider_N`)

**Impact:** ~150 components + 25 extra processes. Manifest ~76 KB. Kuch OEM ROMs process limit pe crash karte hain. `specialUse` FGS ke liye Android 14+ pe `FOREGROUND_SERVICE_SPECIAL_USE` permission chahiye (AAR declare karta hai ✅), lekin Play Console justification maangta hai.
**AAR fix:** slots ki count configurable karo (jitne chahiye utne), 25 fixed na rakho.

### 2.3 OAuth scheme collisions
AAR ye intent-filters declare karta hai: `fbconnect`, `twittersdk`, `twitterkit`, `twitter`, `twitterauth`, `oauth-twitter`, `xauth`, `x`.

Agar app khud Facebook/Twitter login use karta hai to **duplicate scheme → merge conflict / galat activity launch**. App manifest me `tools:node="remove"` ya AAR me scheme rename karo.

### 2.4 `FeaturesticAutoInitProvider` (`initOrder="999"`)
AAR khud SDK ko auto-init karta hai (provider ke through, Application se pehle). Loader abhi `BoxApplication.attachBaseContext()` me `SboxCore.get().doAttachBaseContext(...)` bhi call karta hai → **double init**. AAR me `autoInit` ko opt-out flag do, warna app-side manual init hatao.

### 2.5 Theme `BTheme` chahiye
AAR `ProxyActivity` ke liye `@style/BTheme` use karta hai aur khud define karta hai ✅ (`BTheme`, `LauncherTheme`, `EmptyTheme`). App theme `Base.AppTheme` hai — koi conflict nahi.

### 2.6 Resources
- AAR string: `vbox_service_name` = `":core"` — app ke saath clash nahi ✅
- AAR me `app_name` **nahi** hai → app ka `app_name` safe ✅
- `R.txt` me `string app_icon` hai (`0x7f110002`) — agar app ke paas bhi `app_icon` hua to merge conflict.

---

## 3. RUNTIME issues

### 3.1 AAR ka native = `libfbox.so`, remote `.so` = payload — **dono alag hain**
| File | Size | JNI |
|---|---|---|
| AAR `jni/arm64-v8a/libfbox.so` | 336,104 B | `JNI_OnLoad` ✅ |
| Remote `FeaturesticBox.so` | 8,488,360 B | **koi `Java_*` symbol nahi**, 5,284 exported T symbols (OpenSSL etc.) — ye **game-process payload** hai |

➡️ `System.loadLibrary("FeaturesticBox")` **kabhi mat karo**. Loader `sdk/SoUpdater.java` se GitHub se remote `FeaturesticBox.so` download karke `files/FeaturesticBox.so` me cache karta hai, phir `FeaturesticBox.startGame(ctx, pkg, userId, soPath)` ko local path deta hai; SDK `System.load(path)` karta hai (loader process mein, isliye overlay permission + FGS notification chahiye float payloads ke liye).

### 3.2 `FeaturesticBox.setFeature(String, boolean)` **no-op** hai
Ye sirf private static `sFeatures` map me value daalta hai — core ise **padhta hi nahi**. Isliye "float off / feature on" is API se control nahi hota. AAR me ise actually wire karna hoga.

### 3.3 Activation key
Loader `BoxActivate.activateSdk("PRIVATE_SRC_FILES_OWNER")` bhejta hai. Naya AAR `getServerMessage()` deta hai — agar key galat hui to yahi message milega. **AAR me `activateSdk` ka expected key format document karo.**

### 3.4 `installPackageAsUser` ke duplicate return types
`com.sbox.SboxCore` me same erasure ke saath `com.sbox...InstallResult` aur `com.featurestic...InstallResult` dono return karne wale methods hain. Kotlin/Java callers me ambiguity aa sakti hai; `javap -v` se check karo ki sirf bridge methods hain.

---

## 4. Login panel (AAR se related nahi, par Loader ka contract)

Purana login `libzenin.so` ke andar tha. Ab pure Java me hai (`LogAct.Check`):

```
POST https://zoro.manishflash.online/connect
Content-Type: application/x-www-form-urlencoded
body: game=PUBG&user_key=<KEY>&serial=<UUID>

serial = UUID.nameUUIDFromBytes((KEY + ANDROID_ID + Build.MODEL + Build.BRAND).getBytes())
response: { "status": true, "data": { "EXP": "yyyy-MM-dd HH:mm:ss", "token": "...", "rng": 1699999999 } }

valid iff  token == md5("PUBG-" + KEY + "-" + serial + "-" + "Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E")
     and  rng + 30 > now
```

Agar panel ka salt / serial formula badla, login fail hoga — **AAR se independent**.

---

## 5. Jo Loader me change kiya (reference)

| File | Change |
|---|---|
| `app/libs/sbox-release.aar` | ❌ deleted |
| `app/libs/FeaturesticBox.aar` | ✅ added (2,064,195 B) |
| `settings.gradle` | `FeaturesticLoader`, `include ':app'` |
| `app/build.gradle` | `com.bgmi`, minSdk 24, targetSdk 28, arm64-v8a, appcompat only |
| `AndroidManifest.xml` | Splash/Login/Main activities + BoxService; 182 AAR perms trimmed → 27 |
| `App.java` | ✅ new: `FeaturesticBox.init(ctx, false)` + `BoxActivate.activateSdk("FEATURESTIC")` |
| `ui/SplashActivity.java` | ✅ new: branded splash, routes to Login/Main |
| `ui/LoginActivity.java` | ✅ new: pure-Java HTTP login (same token/salt formula) |
| `ui/MainActivity.java` | ✅ new: SETUP → START GAME, OBB copy with progress, remote .so download + launch |
| `net/AuthApi.java` | ✅ new: `HttpURLConnection` login client |
| `sdk/BoxManager.java` | ✅ new: init/activate/isInstalled/installPackageAsUser/startGame wrapper |
| `sdk/ObbSetup.java` | ✅ new: host→virtual OBB copy with real % progress via MetaStorageManager |
| `sdk/SoUpdater.java` | ✅ new: downloads remote .so, caches in files/ |
| `sdk/BoxService.java` | ✅ new: foreground service + notification while session active |
| `util/Prefs.java`, `DeviceId.java`, `Ui.java` | ✅ new: helpers |
| `BoxApplication` / `MAct` / `LogAct` / `Downtwo` | ❌ deleted |
| `app/src/main/jni/` + `jniLibs/` | ❌ deleted (libzenin) |
| `com/bgmi/floating/` + `com/bgmi/server/` | ❌ deleted (no ESP/float/toggle in loader) |

---

## 6. Priority order (AAR fix karne ke liye)

1. **P0** — `BoxActivate` ke purane methods wapas add karo (`activateBox`, `getLicenseMessage`, `isChecking`, listeners).
2. **P0** — `com.sbox.entity.pm.InstallResult` me `packageName` + `TAG` wapas do.
3. **P0** — `com.sbox.core.env.BEnvironment` ke static methods explicit delegate karo.
4. **P1** — `com.sbox.SboxCore` ke removed getters (`getContext`, `getPackageManager`, `getAppConfig`) wapas do.
5. **P1** — Manifest permissions trim karo (182 → sirf zaroori).
6. **P1** — Proxy slots 25 → configurable.
7. **P2** — `setFeature()` ko actually core se wire karo.
8. **P2** — `FeaturesticAutoInitProvider` ko opt-out flag do.
