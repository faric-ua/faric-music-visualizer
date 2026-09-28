#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cstdint>
#include <mutex>
#include <string>

#include <projectM-4/projectM.h>
#include <projectM-4/playlist.h>

#define LOG_TAG "FARIC-projectM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static projectm_handle g_projectm = nullptr;
static projectm_playlist_handle g_playlist = nullptr;
static std::mutex g_mutex;

static void destroy_locked() {
    if (g_playlist) {
        projectm_playlist_destroy(g_playlist);
        g_playlist = nullptr;
    }
    if (g_projectm) {
        projectm_destroy(g_projectm);
        g_projectm = nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeCreate(
        JNIEnv* env,
        jclass,
        jint width,
        jint height,
        jstring presetPath,
        jstring texturePath,
        jint meshX,
        jint meshY,
        jint targetFps,
        jdouble softCutSeconds) {
    std::lock_guard<std::mutex> lock(g_mutex);
    destroy_locked();

    const char* pathChars = env->GetStringUTFChars(presetPath, nullptr);
    std::string path = pathChars ? pathChars : "";
    if (pathChars) {
        env->ReleaseStringUTFChars(presetPath, pathChars);
    }

    const char* textureChars = env->GetStringUTFChars(texturePath, nullptr);
    std::string texture = textureChars ? textureChars : "";
    if (textureChars) {
        env->ReleaseStringUTFChars(texturePath, textureChars);
    }

    g_projectm = projectm_create();
    if (!g_projectm) {
        LOGE("projectm_create failed");
        return;
    }

    projectm_set_window_size(g_projectm, width, height);
    projectm_set_aspect_correction(g_projectm, true);
    projectm_set_mesh_size(g_projectm, meshX, meshY);
    projectm_set_fps(g_projectm, targetFps);
    projectm_set_preset_duration(g_projectm, 18);
    projectm_set_soft_cut_duration(g_projectm, softCutSeconds);
    projectm_set_hard_cut_enabled(g_projectm, true);
    projectm_set_hard_cut_duration(g_projectm, 18);
    projectm_set_hard_cut_sensitivity(g_projectm, 1.0);
    projectm_set_beat_sensitivity(g_projectm, 1.15);

    if (!texture.empty()) {
        const char* texturePaths[] = { texture.c_str() };
        projectm_set_texture_search_paths(g_projectm, texturePaths, 1);
    }

    g_playlist = projectm_playlist_create(g_projectm);
    if (!g_playlist) {
        LOGE("projectm_playlist_create failed");
        return;
    }

    const auto added = projectm_playlist_add_path(g_playlist, path.c_str(), true, false);
    LOGI("preset path=%s presets=%u", path.c_str(), added);

    if (added > 0) {
        projectm_playlist_set_shuffle(g_playlist, true);
        projectm_playlist_set_position(g_playlist, 0, true);
    } else {
        projectm_load_preset_file(g_projectm, "idle://", false);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeResize(
        JNIEnv*,
        jclass,
        jint width,
        jint height) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_projectm) {
        projectm_set_window_size(g_projectm, width, height);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeRender(
        JNIEnv*,
        jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_projectm) {
        projectm_opengl_render_frame(g_projectm);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeAddPcm(
        JNIEnv* env,
        jclass,
        jshortArray pcmData,
        jint frameCount) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_projectm || !pcmData) return;

    const jsize length = env->GetArrayLength(pcmData);
    if (length <= 0) return;

    jshort* data = env->GetShortArrayElements(pcmData, nullptr);
    if (!data) return;

    const int frames = std::min<int>(length, frameCount);
    projectm_pcm_add_int16(
        g_projectm,
        reinterpret_cast<const int16_t*>(data),
        frames,
        PROJECTM_MONO
    );

    env->ReleaseShortArrayElements(pcmData, data, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeNextPreset(
        JNIEnv*,
        jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_playlist && projectm_playlist_size(g_playlist) > 0) {
        projectm_playlist_play_next(g_playlist, true);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeDestroy(
        JNIEnv*,
        jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    destroy_locked();
}
