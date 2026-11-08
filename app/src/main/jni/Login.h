#include "oxorany.h"
#include <openssl/sha.h>
#include "StrEnc.h"
#include <jni.h>
#include <string>
#include <android/log.h>
#include <curl/curl.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/err.h>
#include <openssl/md5.h>
#include <openssl/aes.h>
#include <json.hpp>

using json = nlohmann::ordered_json;
time_t rng = 0;
std::string Enc;
static char ZENINOP[64];
static std::string exdate = oxorany ("NULL");

std::string g_Token, g_Auth;

bool xConnected = false, xServerConnection = false, memek = false;




const char *GetAndroidID(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getContentResolverMethod = env->GetMethodID(contextClass,"getContentResolver","()Landroid/content/ContentResolver;");
    jclass settingSecureClass = env->FindClass("android/provider/Settings$Secure");
    jmethodID getStringMethod = env->GetStaticMethodID(settingSecureClass,"getString", "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;");

    auto obj = env->CallObjectMethod(context, getContentResolverMethod);
    auto str = (jstring) env->CallStaticObjectMethod(settingSecureClass, getStringMethod, obj,env->NewStringUTF("android_id"));
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceModel(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "MODEL","Ljava/lang/String;");

    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceBrand(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "BRAND","Ljava/lang/String;");

    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetPackageName(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getPackageNameId = env->GetMethodID(contextClass, "getPackageName","()Ljava/lang/String;");

    auto str = (jstring) env->CallObjectMethod(context, getPackageNameId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceUniqueIdentifier(JNIEnv *env, const char *uuid) {
    jclass uuidClass = env->FindClass("java/util/UUID");

    auto len = strlen(uuid);

    jbyteArray myJByteArray = env->NewByteArray(len);
    env->SetByteArrayRegion(myJByteArray, 0, len, (jbyte *) uuid);

    jmethodID nameUUIDFromBytesMethod = env->GetStaticMethodID(uuidClass,"nameUUIDFromBytes","([B)Ljava/util/UUID;");
    jmethodID toStringMethod = env->GetMethodID(uuidClass, "toString","()Ljava/lang/String;");

    auto obj = env->CallStaticObjectMethod(uuidClass, nameUUIDFromBytesMethod, myJByteArray);
    auto str = (jstring) env->CallObjectMethod(obj, toStringMethod);
    return env->GetStringUTFChars(str, 0);
}

struct MemoryStruct {
    char *memory;
    size_t size;
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *) userp;

    mem->memory = (char *) realloc(mem->memory, mem->size + realsize + 1);
    if (mem->memory == NULL) {
        return 0;
    }

    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}
std::string CalcMD5(std::string s) {
    std::string result;

    unsigned char hash[MD5_DIGEST_LENGTH];
    char tmp[4];

    MD5_CTX md5;
    MD5_Init(&md5);
    MD5_Update(&md5, s.c_str(), s.length());
    MD5_Final(hash, &md5);
    for (unsigned char i : hash) {
        sprintf(tmp, "%02x", i);
        result += tmp;
    }
    return result;
}

std::string CalcSHA256(std::string s) {
    std::string result;

    unsigned char hash[SHA256_DIGEST_LENGTH];
    char tmp[4];

    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, s.c_str(), s.length());
    SHA256_Final(hash, &sha256);
    for (unsigned char i : hash) {
        sprintf(tmp, "%02x", i);
        result += tmp;
    }
    return result;
}
std::string base64_decodeLuffy(const std::string &encodedText) {
    BIO *bio, *b64;
    FILE* stream;
    int encodedSize = static_cast<int>(encodedText.length());
    char *buffer = static_cast<char*>(malloc(encodedSize));
    memcpy(buffer, encodedText.c_str(), encodedSize);

    b64 = BIO_new(BIO_f_base64());
    bio = BIO_new_mem_buf(buffer, encodedSize);
    bio = BIO_push(b64, bio);

    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);

    int decodedSize = BIO_read(bio, buffer, encodedSize);

    BIO_free_all(bio);

    std::string result(buffer, decodedSize);
    free(buffer);

    return result;
}

std::string actualEncryptionKey = oxorany("b591nKHFOnXpwu0W"); 
std::string actualIV = oxorany("zcN3ZOg4uhjw6H9S");

std::string decryptAES(const std::string &encryptedText, const std::string &key, const std::string &iv) {
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) {
        return std::string("");
    }

    int len;
    int plaintext_len;
    std::string decryptedText;

    std::vector<char> plaintext(encryptedText.size() + AES_BLOCK_SIZE);

    if (EVP_DecryptInit_ex(ctx, EVP_aes_128_cbc(), NULL, (const unsigned char *)key.c_str(), (const unsigned char *)iv.c_str()) != 1) {
        EVP_CIPHER_CTX_free(ctx);
        return std::string("");
    }

    if (EVP_DecryptUpdate(ctx, (unsigned char *)plaintext.data(), &len, (const unsigned char *)encryptedText.c_str(), encryptedText.size()) != 1) {
        EVP_CIPHER_CTX_free(ctx);
        return std::string("");
    }
    plaintext_len = len;

    if (EVP_DecryptFinal_ex(ctx, (unsigned char *)plaintext.data() + len, &len) != 1) {
        EVP_CIPHER_CTX_free(ctx);
        return std::string("");
    }
    plaintext_len += len;

    decryptedText.assign(plaintext.data(), plaintext_len);
    EVP_CIPHER_CTX_free(ctx);

    return decryptedText;
}

std::string decode_and_decrypt(const std::string& encodedData, const std::string& key, const std::string& iv) {
    try {
        std::string base64Decoded = base64_decodeLuffy(encodedData);
        std::string decryptedData = decryptAES(base64Decoded, key, iv);
        std::string finalDecodedData = base64_decodeLuffy(decryptedData);

        return finalDecodedData;
    } catch (const std::exception& e) {
        return "";
    }
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_LogAct_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    auto user_key = env->GetStringUTFChars(mUserKey, 0);
    std::string hwid = user_key;
    hwid += GetAndroidID(env, mContext);
    hwid += GetDeviceModel(env);
    hwid += GetDeviceBrand(env);
    std::string UUID = GetDeviceUniqueIdentifier(env, hwid.c_str());
    std::string errMsg;
    struct MemoryStruct chunk{};
    chunk.memory = (char *) malloc(1);
    chunk.size = 0;


    CURL *curl;
    CURLcode res;
    curl = curl_easy_init();
    if (curl) {
        char lol[1000];
        sprintf(lol,oxorany("https://zoro.manishflash.online/connect"));
        // SSL pin removed: previous host pin does not match zoro.manishflash.online
        curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, oxorany("POST"));
        curl_easy_setopt(curl, CURLOPT_URL, lol);+
                curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1);
        curl_easy_setopt(curl, CURLOPT_DEFAULT_PROTOCOL, oxorany("https"));
        struct curl_slist *headers = NULL;
        headers = curl_slist_append(headers, oxorany("Accept: application/json"));
        headers = curl_slist_append(headers, oxorany("Content-Type: application/x-www-form-urlencoded"));
        headers = curl_slist_append(headers, oxorany("Charset: UTF-8"));
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        char data[4096];
        sprintf(data, oxorany("game=PUBG&user_key=%s&serial=%s"), user_key, UUID.c_str());
        curl_easy_setopt(curl, CURLOPT_POST, 1);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, data);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &chunk);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYSTATUS, 0);
        curl_easy_setopt(curl, CURLOPT_USERAGENT, oxorany(""));
        res = curl_easy_perform(curl);
        
        
        if (res == CURLE_OK) {
            try {
                json result = json::parse(chunk.memory);
                
          /*      
                if (res == CURLE_OK) {
            std::string finalResponse = decode_and_decrypt(chunk.memory, actualEncryptionKey, actualIV);
        try {
                json result = json::parse(finalResponse);
                */
                auto STATUS = std::string{"status"};
                if (result[STATUS] == true) {
                    auto DATA = std::string{"data"};
                    auto TOKEN = std::string{"token"};
                    auto RNG = std::string{"rng"};
                    exdate = result["data"]["EXP"].get<std::string>();
                    std::string token = result[DATA][TOKEN].get<std::string>();
                    time_t rng = result[DATA][RNG].get<time_t>();
                    if (rng + 30 > time(0)) {
                        std::string auth = oxorany("PUBG");
                        auth += "-";
                        auth += user_key;
                        auth += "-";
                        auth += UUID;
                        auth += "-";
                        std::string license = oxorany("Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E");
                        auth += license.c_str();
                        std::string outputAuth = CalcMD5(auth);
                        g_Token = token;
                        g_Auth = outputAuth;
                        xConnected = g_Token == g_Auth;
                        xServerConnection = true;
                        memek = true;
                        if (xConnected && xServerConnection) {
                            printf(oxorany("Login Success \n"));
                        }
                    }
                } else {
                    auto REASON = std::string{"reason"};
                    errMsg = result[REASON].get<std::string>();
                }
            } catch (json::exception &e) {
                errMsg = e.what();
            }
        } else {
            errMsg = curl_easy_strerror(res);
        }
    }
    curl_easy_cleanup(curl);
    return xConnected ? env->NewStringUTF(StrEnc("8q", "\x77\x3A", 2).c_str()) : env->NewStringUTF(errMsg.c_str());
}
