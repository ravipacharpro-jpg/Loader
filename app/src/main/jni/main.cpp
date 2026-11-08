
//#include <obfuscate.h>
#include "oxorany.h"
#include <openssl/sha.h>
#include "StrEnc.h"
#include <zlib.h>  // For CRC32
#include <pthread.h>
#include <jni.h>
#include <string>
#include "ESP.h"
#include "Hacks.h"

ESP espOverlay;
int type = 1, utype = 2;



extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_Overlay_DrawOn(JNIEnv *env, jclass , jobject espView, jobject canvas) {
    espOverlay = ESP(env, espView, canvas);
    if (espOverlay.isValid()){
        DrawESP(espOverlay, espOverlay.getWidth(), espOverlay.getHeight());
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_MAct_pp(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(exdate.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_bgmi_MAct_ZENINOP(JNIEnv *env, jobject activityObject) {
    return env->NewStringUTF(ZENINOP);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_LogAct_GetKey(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(oxorany("https://t.me/")); // Link Channel
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_utils_Downtwo_Version(JNIEnv *env, jclass clazz) {
    // return URL to version file
    const char *versionUrl = (oxorany("https://github.com/BGMILITEOP558/Op098/releases/download/Op773/version.txt"));// can put GitHub too
    return env->NewStringUTF(versionUrl);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_utils_Downtwo_Link(JNIEnv *env, jclass clazz) {
    // return URL to your update ZIP file
    const char *downloadUrl = (oxorany("https://github.com/ravipacharpro-jpg/Fbox/releases/download/Floader/FeaturesticBox.so"));// can put GitHub too
    return env->NewStringUTF(downloadUrl);
}


extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_Overlay_Close(JNIEnv *, jobject) {
    Close();
    options.openState = -1;
    options.aimBullet = -1;
    options.aimT = -1;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_bgmi_floating_Overlay_getReady(JNIEnv *, jobject thiz) {
    int sockCheck = 1;

    if (!Create()) {
        perror("Creation failed");
        return false;
    }
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &sockCheck, sizeof(int));
    if (!Bind()) {
        perror("Bind failed");
        return false;
    }

    if (!Listen()) {
        perror("Listen failed");
        return false;
    }
    if (Accept()) {
        return true;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_ToggleAim_ToggleAim(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.openState = 0;
    else
        options.openState = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_ToggleBullet_ToggleBullet(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimBullet = 0;
    else
        options.aimBullet = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_ToggleSimulation_ToggleSimulation(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimT = 0;
    else
        options.aimT = -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SettingValue(JNIEnv *, jobject, jint code, jboolean jboolean1) {

    switch ((int) code) {
        case 2:
            isPlayerLine = jboolean1;
            break;
        case 3:
            isPlayerBox = jboolean1;
            break;
        case 4:
            isSkeleton = jboolean1;
            break;
        case 5:
            isPlayerDistance = jboolean1;
            break;
        case 6:
            isPlayerHealth = jboolean1;
            break;
        case 7:
            isPlayerName = jboolean1;
            break;
        case 8:
            isPlayerHead = jboolean1;
            break;
        case 9:
            is360Alertv2 = jboolean1;
            break;
        case 10:
            isPlayerWeapon = jboolean1;
            break;
        case 11:
            isGrenadeWarning = jboolean1;
            break;
        case 12:
          //  isVehicles = jboolean1;
            break;
        case 13:
          //  isItems = jboolean1;
            break;
        case 14:
           // isLootBox = jboolean1;
            break;
        case 15:
            options.ignoreAi = jboolean1;
            break;
        case 16:
            isPlayerWeaponIcon = jboolean1;
            break;
        
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SettingAim(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            options.openState = -1;
            break;
        case 2:
            options.aimBullet = -1;
            break;
        case 3:
            options.pour = value;
            break;
        case 4:
            options.ignoreBot = value;
            break;
        case 5:
            options.InputInversion = value;
            break;
        case 6:
            options.tracingStatus = value;
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SettingMemory(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.LessRecoil = value;
            break;
        case 2:
            otherFeature.SmallCrosshair = value;
            break;
        case 3:
            otherFeature.Aimbot = value;
            break;
        case 4:
            otherFeature.WideView = value;
            break;
          
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Range(JNIEnv *, jobject, jint range) {
    options.aimingRange = 1 + range;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_distances(JNIEnv *, jobject, jint distances) {
    options.aimingDist = distances;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_recoil(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_recoil2(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe1 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_recoil3(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe2 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Bulletspeed(JNIEnv *env, jobject thiz, jint bulletspeed) {
    options.aimingSpeed = bulletspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_AimingSpeed(JNIEnv *env, jobject thiz, jint aimingspeed) {
    options.touchSpeed = aimingspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Smoothness(JNIEnv *env, jobject thiz, jint smoothness) {
    options.Smoothing = smoothness;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_TouchSize(JNIEnv *env, jobject thiz, jint touchsize) {
    options.touchSize = touchsize;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_TouchPosX(JNIEnv *env, jobject thiz, jint touchposx) {
    options.touchX = touchposx;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_TouchPosY(JNIEnv *env, jobject thiz, jint touchposy) {
    options.touchY = touchposy;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_WideView(JNIEnv *env, jobject thiz, jint wideview) {
    otherFeature.WideView = wideview;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Target(JNIEnv *, jobject, jint target) {
    options.aimbotmode = target;
}
extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_AimWhen(JNIEnv *, jobject, jint state) {
    options.aimingState = state;
}
extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_AimBy(JNIEnv *, jobject, jint aimby) {
    options.priority = aimby;
}
extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_RadarSize(JNIEnv *, jobject, jint size) {
    request.radarSize = size;
}





extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SkinHack(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:  otherFeature.clothes = 1; break;   // Blood Raven
        case 2:  otherFeature.clothes = 2; break;   // Golden Pharaoh
        case 3:  otherFeature.clothes = 3; break;   // Avalanche
        case 4:  otherFeature.clothes = 4; break;   // Poseidon
        case 5:  otherFeature.clothes = 5; break;   // Arcane Jester
        case 6:  otherFeature.clothes = 6; break;   // Silvanus
        case 7:  otherFeature.clothes = 7; break;   // Marmoris
        case 8:  otherFeature.clothes = 8; break;   // Fiore
        case 9:  otherFeature.clothes = 9; break;   // Ignis
        case 10: otherFeature.clothes = 10; break;  // White Mummy (no)
        case 11: otherFeature.clothes = 11; break;  // Galadria
        case 12: otherFeature.clothes = 12; break;  // Flamewraith Set
        case 13: otherFeature.clothes = 13; break;  // Majestic Cavalry Set
        case 14: otherFeature.clothes = 14; break;  // Bramble Overlord Set
        case 15: otherFeature.clothes = 15; break;  // Nether Visage Set
        case 16: otherFeature.clothes = 16; break;  // Spectral Swan Set
        case 17: otherFeature.clothes = 17; break;  // Untamed Celestial
        case 18: otherFeature.clothes = 18; break;  // Snowstar Sweetheart
        case 19: otherFeature.clothes = 19; break;  // Arctic Conqueror
        case 20: otherFeature.clothes = 20; break;  // Feral Ravager
        case 21: otherFeature.clothes = 21; break;  // Vampyra Countess
        case 22: otherFeature.clothes = 22; break;  // Serene Lumina
        case 23: otherFeature.clothes = 23; break;  // Mercury Soldier
        case 24: otherFeature.clothes = 24; break;  // Luminous Muse
        case 25: otherFeature.clothes = 25; break;  // Origin Lumen
        case 26: otherFeature.clothes = 26; break;  // Serpengleam
        case 27: otherFeature.clothes = 27; break;  // Shinobi Spirit
        case 28: otherFeature.clothes = 28; break;  // Foxy Flare
        case 29: otherFeature.clothes = 29; break;  // Glacial Bride
        case 30: otherFeature.clothes = 30; break;  // Boxerbolt
        case 31: otherFeature.clothes = 31; break;  // Dandy Groovster
        case 32: otherFeature.clothes = 32; break;  // Wrathful Neptune
        case 33: otherFeature.clothes = 33; break;  // Noctum Sunder
        case 34: otherFeature.clothes = 34; break;  // Crimson Ephialtes
        default:
            otherFeature.clothes = 0; // reset or no selection
            break;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Skinbag(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.bag = 1; // Poseidon
            break;
        case 2:
            otherFeature.bag = 2; // Mystique Splendor
            break;
        case 3:
            otherFeature.bag = 3; // Ancient Civilization
            break;
        case 4:
            otherFeature.bag = 4; // Galadria
            break;
        case 5:
            otherFeature.bag = 5; // Alfheim Wonder
            break;
        
        
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Skinhelmet(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.helmet = 1; // Inferno Rider
            break;
        case 2:
            otherFeature.helmet = 2; // Auric Sentinel
            break;
        case 3:
            otherFeature.helmet = 3; // Galadria
            break;
        case 4:
            otherFeature.helmet = 4; // Shining Eagle
            break;
        case 5:
            otherFeature.helmet = 5; // Eternal Kingdom
            break;
        
    }
}











