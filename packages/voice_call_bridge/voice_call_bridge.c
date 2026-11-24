#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dlfcn.h>
#include <android/log.h>
#include <sys/system_properties.h>

#define TAG "voice_call_bridge"

typedef void (*String8_ctor_t)(void*, const char*);
typedef void (*String8_dtor_t)(void*);
typedef int (*AudioSystem_setParameters_t)(const void*);
typedef int (*AudioSystem_setVoiceVolume_t)(float);

static String8_ctor_t s_ctor = NULL;
static String8_dtor_t s_dtor = NULL;
static AudioSystem_setParameters_t s_setParams = NULL;
static AudioSystem_setVoiceVolume_t s_setVoiceVolume = NULL;

static int init_audio_symbols(void) {
    if (s_ctor && s_dtor && s_setParams) return 0;

    void* hUtils = dlopen("/system/lib64/libutils.so", RTLD_NOW);
    if (!hUtils) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to dlopen libutils.so: %s", dlerror());
        return -1;
    }

    void* hAudio = dlopen("/system/lib64/libaudiobase.so", RTLD_NOW);
    if (!hAudio) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to dlopen libaudiobase.so: %s", dlerror());
        return -1;
    }

    s_ctor = (String8_ctor_t)dlsym(hUtils, "_ZN7android7String8C1EPKc");
    s_dtor = (String8_dtor_t)dlsym(hUtils, "_ZN7android7String8D1Ev");
    s_setParams = (AudioSystem_setParameters_t)dlsym(hAudio, "_ZN7android11AudioSystem13setParametersERKNS_7String8E");
    s_setVoiceVolume = (AudioSystem_setVoiceVolume_t)dlsym(hAudio, "_ZN7android11AudioSystem14setVoiceVolumeEf");

    if (!s_ctor || !s_dtor || !s_setParams) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to resolve AudioSystem symbols");
        return -1;
    }
    return 0;
}

static void send_param(const char* kv) {
    if (init_audio_symbols() != 0) return;
    char str8_buf[64] = {0};
    s_ctor(str8_buf, kv);
    int ret = s_setParams(str8_buf);
    s_dtor(str8_buf);
    __android_log_print(ANDROID_LOG_INFO, TAG, "setParameters(%s) -> %d", kv, ret);
}

static int has_second_sim(void) {
    char prop[PROP_VALUE_MAX] = {0};
    __system_property_get("gsm.sim.state", prop);
    if (strchr(prop, ',') != NULL) {
        return 1;
    }
    return 0;
}

void activate_call_audio(void) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "Activating voice call audio (call_state=2)");
    send_param("vsid=297816064;call_state=2");
    if (has_second_sim()) {
        send_param("vsid=299651072;call_state=2");
    }
    if (s_setVoiceVolume) {
        s_setVoiceVolume(1.0f);
    }
}

void deactivate_call_audio(void) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "Deactivating voice call audio (call_state=1)");
    send_param("vsid=297816064;call_state=1");
    if (has_second_sim()) {
        send_param("vsid=299651072;call_state=1");
    }
}

int main(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: %s [activate|2|deactivate|1|daemon]\n", argv[0]);
        return 1;
    }

    const char* cmd = argv[1];
    if (strcmp(cmd, "2") == 0 || strcmp(cmd, "activate") == 0 || strcmp(cmd, "on") == 0) {
        activate_call_audio();
        return 0;
    } else if (strcmp(cmd, "1") == 0 || strcmp(cmd, "deactivate") == 0 || strcmp(cmd, "off") == 0 || strcmp(cmd, "idle") == 0) {
        deactivate_call_audio();
        return 0;
    } else if (strcmp(cmd, "daemon") == 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Starting voice call bridge daemon");
        int last_state = -1;
        char prop_val[PROP_VALUE_MAX] = {0};

        while (1) {
            __system_property_get("sys.oplus.call_exist", prop_val);
            int current_state = (prop_val[0] == '1' || prop_val[0] == '2') ? 1 : 0;
            if (current_state != last_state) {
                if (current_state == 1) {
                    activate_call_audio();
                } else if (last_state != -1) {
                    deactivate_call_audio();
                }
                last_state = current_state;
            }
            usleep(150000); // 150 ms check
        }
    }

    printf("Unknown argument: %s\n", cmd);
    return 1;
}
