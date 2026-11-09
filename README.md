# Featurestic Loader — Apple Cupertino Edition (Pre-Connected with `FeaturesticBox-release.aar`)

Yeh ek **100% Clean, Scratch-built Android Studio Loader Project** hai jo **Apple Human Interface Guidelines (Cupertino Glassmorphism)** par design kiya gaya hai aur `app/libs/FeaturesticBox-release.aar` ke saath pre-connected hai.

---

## Screen Flow & Features

1. **Screen 1 — PHP Panel Login (`LoginActivity`)**:
   - Frosted Glassmorphism UI + Apple spring physics animations.
   - Standard PHP Panels (`POST game=...&user_key=...&serial=...`) se key verify karta hai.
   - Testing ke liye `FEATURESTIC-VIP` (ya jab tak `AppConfig.PHP_PANEL_URL` placeholder hai tab tak koi bhi 4+ char ki key) se turant login ho jata hai.
2. **Screen 2 — Virtual Workspace & Smart Single Button (`MainActivity`)**:
   - **Step 1 (`Setup Game`)**: Pehli baar aane par button **`Setup Game`** dikhata hai. Click karte hi yeh background me Game APK ko Virtual Box me clone karta hai aur OBB files (`main` + `patch` `.obb`) ko copy/mount karke 120 FPS + Clean Device ID environment set karta hai.
   - **Step 2 (`Start Game`)**: Setup pura hote hi **wahi button smooth spring animation ke saath `Start Game` (Green)** me convert ho jata hai!
   - **Step 3 (Launch)**: `Start Game` par click karte hi `FeaturesticBox.startGame(...)` call hota hai aur game Virtual Box ke andar start ho jata hai.

---

## Sirf 1 File Edit Karni Hai (`AppConfig.java`)

Path: `app/src/main/java/com/featurestic/loader/AppConfig.java`

```java
public static final String PHP_PANEL_URL = "https://your-panel-domain.com/connect";
public static final String PHP_GAME_TAG = "PUBG";
public static final String PLUGIN_SO_URL_OR_PATH = ""; // Optional .so URL or local path
```
