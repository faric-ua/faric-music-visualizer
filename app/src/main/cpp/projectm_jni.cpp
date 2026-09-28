#include <jni.h>
#include <android/log.h>
#include <GLES2/gl2.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
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

static GLuint g_foreground_program = 0;
static GLuint g_foreground_vbo = 0;
static GLint g_attr_position = -1;
static GLint g_u_resolution = -1;
static GLint g_u_time = -1;
static GLint g_u_amplitude = -1;
static GLint g_u_bass = -1;
static GLint g_u_mid = -1;
static GLint g_u_high = -1;
static GLint g_u_beat = -1;
static GLint g_u_mode = -1;
static std::atomic<int> g_foreground_sample{0};

static int g_width = 1;
static int g_height = 1;

static float g_target_amplitude = 0.0f;
static float g_target_bass = 0.0f;
static float g_target_mid = 0.0f;
static float g_target_high = 0.0f;
static float g_target_beat = 0.0f;

static float g_amplitude = 0.0f;
static float g_bass = 0.0f;
static float g_mid = 0.0f;
static float g_high = 0.0f;
static float g_beat = 0.0f;

static auto g_started_at = std::chrono::steady_clock::now();
static auto g_last_frame_at = g_started_at;

static const char* kForegroundVertexShader = R"(
attribute vec2 aPosition;
varying vec2 vUv;

void main() {
    vUv = aPosition * 0.5 + 0.5;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
)";

static const char* kForegroundFragmentShader = R"(
precision mediump float;

varying vec2 vUv;

uniform vec2 uResolution;
uniform float uTime;
uniform float uAmplitude;
uniform float uBass;
uniform float uMid;
uniform float uHigh;
uniform float uBeat;
uniform float uMode;

const float PI = 3.14159265358979323846;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    vec2 p = vUv * 2.0 - 1.0;
    p.x *= uResolution.x / max(uResolution.y, 1.0);

    float radius = length(p);
    float angle = atan(p.y, p.x);

    float pulseRadius =
        0.175
        + uAmplitude * 0.025
        + uBass * 0.115
        + uMid * 0.026
        + uBeat * 0.085;

    float ringWidth =
        0.010
        + uHigh * 0.016
        + uBass * 0.006
        + uBeat * 0.020;

    float ring = 1.0 - smoothstep(
        ringWidth,
        ringWidth + 0.012,
        abs(radius - pulseRadius)
    );

    float sectors = 84.0;
    float spokePhase = fract((angle + PI) / (2.0 * PI) * sectors);

    float spokeLine = 1.0 - smoothstep(
        0.015,
        0.105 + uHigh * 0.035,
        abs(spokePhase - 0.5)
    );

    float angularEnergy =
        0.50
        + 0.30 * sin(
            angle * 3.0
            + uTime * (1.15 + uMid * 2.8 + uBass * 1.4)
        )
        + 0.18 * sin(
            angle * 7.0
            - uTime * (1.45 + uHigh * 3.0 + uBeat * 1.6)
        );

    float spokeLength =
        0.285
        + uAmplitude * 0.175
        + uBass * 0.285
        + uMid * 0.115
        + uHigh * 0.085
        + uBeat * 0.185
        + angularEnergy * 0.075;

    float innerMask = smoothstep(
        pulseRadius + 0.012,
        pulseRadius + 0.045,
        radius
    );

    float outerMask = 1.0 - smoothstep(
        spokeLength,
        spokeLength + 0.055,
        radius
    );

    float spokes = spokeLine * innerMask * outerMask;

    float coreGlow =
        exp(-radius * (8.0 - uBass * 1.8))
        * (0.22 + uAmplitude * 0.35 + uBeat * 0.25);

    vec2 sparkGrid = floor(
        (p + vec2(uTime * 0.025, -uTime * 0.035))
        * vec2(42.0, 74.0)
    );

    float sparkRnd = hash21(sparkGrid);
    float sparkGate = step(0.965 - uHigh * 0.025, sparkRnd);
    float sparkTwinkle =
        0.5 + 0.5 * sin(uTime * 7.0 + sparkRnd * 30.0);

    float sparks =
        sparkGate
        * sparkTwinkle
        * (0.08 + uHigh * 0.42);

    vec3 orange = vec3(1.0, 0.34, 0.055);
    vec3 cyan = vec3(0.08, 0.82, 1.0);

    float colorMix =
        0.5 + 0.5 * sin(angle * 2.0 + uTime * 0.35);

    vec3 accent = mix(orange, cyan, colorMix);

    float pulseRays =
        ring * (0.72 + uBeat * 0.45)
        + spokes * (0.28 + uAmplitude * 0.56)
        + coreGlow
        + sparks;

    float orbit1 = 1.0 - smoothstep(
        0.010 + uHigh * 0.010,
        0.030 + uHigh * 0.012,
        abs(
            radius
            - (
                0.16
                + 0.070 * sin(uTime * (2.0 + uBass * 2.8))
                + uBass * 0.090
                + uBeat * 0.055
            )
        )
    );

    float orbit2 = 1.0 - smoothstep(
        0.010,
        0.026,
        abs(
            radius
            - (
                0.31
                + 0.060 * sin(
                    uTime * (2.45 + uMid * 2.2)
                    + angle * 3.0
                )
                + uMid * 0.075
                + uBeat * 0.045
            )
        )
    );

    float orbitRings =
        (orbit1 + orbit2)
        * (0.42 + uAmplitude * 0.55 + uBass * 0.30 + uBeat * 0.52)
        + coreGlow * 0.62;

    float haloPhase = fract(
        (
            angle
            + PI
            + uTime * (0.35 + uHigh * 0.75 + uBass * 0.20)
        )
        / (2.0 * PI)
        * 48.0
    );
    float haloBars = 1.0 - smoothstep(
        0.08,
        0.22,
        abs(haloPhase - 0.5)
    );
    float haloBand = 1.0 - smoothstep(
        0.018,
        0.055,
        abs(
            radius
            - (
                0.23
                + uBass * 0.145
                + uAmplitude * 0.035
                + uBeat * 0.080
            )
        )
    );

    float spectrumHalo =
        haloBars
        * haloBand
        * (
            0.50
            + uMid * 0.62
            + uHigh * 0.58
            + uBass * 0.26
            + uBeat * 0.42
        )
        + sparks * 0.95
        + coreGlow * 0.42;

    vec2 edgeUv = abs(vUv * 2.0 - 1.0);
    float edgeDist = max(edgeUv.x, edgeUv.y);
    float edgeCoord =
        edgeUv.x > edgeUv.y
        ? vUv.y
        : vUv.x;

    float flareReach =
        0.075
        + uBass * 0.160
        + uAmplitude * 0.045
        + uBeat * 0.125;

    float flareBody = 1.0 - smoothstep(
        flareReach,
        flareReach + 0.085,
        1.0 - edgeDist
    );

    float flareWave =
        0.5
        + 0.5 * sin(
            edgeCoord * 52.0
            + uTime * (3.0 + uBass * 5.0)
            + sin(edgeCoord * 17.0 - uTime * 2.2) * 2.5
        );

    float flareFine =
        0.5
        + 0.5 * sin(
            edgeCoord * 121.0
            - uTime * (5.0 + uHigh * 8.0)
        );

    float flareTongues =
        pow(max(flareWave, 0.0), 3.0)
        * (0.55 + flareFine * 0.45);

    float flareMode =
        uMode < 0.5
        ? 0.82
        : (
            uMode < 1.5
            ? 0.56
            : 1.10
        );

    float solarFlares =
        flareBody
        * flareTongues
        * (
            0.12
            + uBass * 0.58
            + uBeat * 0.78
            + uHigh * 0.24
        )
        * flareMode;

    vec3 flareHot = vec3(1.0, 0.28, 0.025);
    vec3 flareCore = vec3(1.0, 0.92, 0.52);
    vec3 flareColor = mix(
        flareHot,
        flareCore,
        clamp(flareFine + uBeat * 0.25, 0.0, 1.0)
    );

    float intensity = pulseRays;
    if (uMode > 0.5 && uMode < 1.5) {
        intensity = orbitRings;
    } else if (uMode >= 1.5) {
        intensity = spectrumHalo;
    }

    float alpha = clamp(
        intensity + solarFlares,
        0.0,
        1.0
    );

    vec3 color =
        accent * intensity
        + flareColor * solarFlares;

    gl_FragColor = vec4(color, alpha);
}
)";

static GLuint compile_shader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);

    if (!shader) {
        LOGE("glCreateShader failed");
        return 0;
    }

    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint compiled = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);

    if (compiled != GL_TRUE) {
        GLint logLength = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLength);

        std::string log;

        if (logLength > 1) {
            log.resize(static_cast<size_t>(logLength));
            glGetShaderInfoLog(shader, logLength, nullptr, log.data());
        }

        LOGE("foreground shader compile failed: %s", log.c_str());
        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

static void destroy_foreground_locked() {
    if (g_foreground_vbo) {
        glDeleteBuffers(1, &g_foreground_vbo);
        g_foreground_vbo = 0;
    }

    if (g_foreground_program) {
        glDeleteProgram(g_foreground_program);
        g_foreground_program = 0;
    }

    g_attr_position = -1;
    g_u_resolution = -1;
    g_u_time = -1;
    g_u_amplitude = -1;
    g_u_bass = -1;
    g_u_mid = -1;
    g_u_high = -1;
    g_u_beat = -1;
    g_u_mode = -1;
}

static bool create_foreground_locked() {
    destroy_foreground_locked();

    GLuint vertex = compile_shader(
        GL_VERTEX_SHADER,
        kForegroundVertexShader
    );

    if (!vertex) return false;

    GLuint fragment = compile_shader(
        GL_FRAGMENT_SHADER,
        kForegroundFragmentShader
    );

    if (!fragment) {
        glDeleteShader(vertex);
        return false;
    }

    g_foreground_program = glCreateProgram();
    glAttachShader(g_foreground_program, vertex);
    glAttachShader(g_foreground_program, fragment);
    glLinkProgram(g_foreground_program);

    glDeleteShader(vertex);
    glDeleteShader(fragment);

    GLint linked = GL_FALSE;
    glGetProgramiv(
        g_foreground_program,
        GL_LINK_STATUS,
        &linked
    );

    if (linked != GL_TRUE) {
        GLint logLength = 0;
        glGetProgramiv(
            g_foreground_program,
            GL_INFO_LOG_LENGTH,
            &logLength
        );

        std::string log;

        if (logLength > 1) {
            log.resize(static_cast<size_t>(logLength));
            glGetProgramInfoLog(
                g_foreground_program,
                logLength,
                nullptr,
                log.data()
            );
        }

        LOGE(
            "foreground program link failed: %s",
            log.c_str()
        );

        destroy_foreground_locked();
        return false;
    }

    g_attr_position = glGetAttribLocation(
        g_foreground_program,
        "aPosition"
    );

    g_u_resolution = glGetUniformLocation(
        g_foreground_program,
        "uResolution"
    );

    g_u_time = glGetUniformLocation(
        g_foreground_program,
        "uTime"
    );

    g_u_amplitude = glGetUniformLocation(
        g_foreground_program,
        "uAmplitude"
    );

    g_u_bass = glGetUniformLocation(
        g_foreground_program,
        "uBass"
    );

    g_u_mid = glGetUniformLocation(
        g_foreground_program,
        "uMid"
    );

    g_u_high = glGetUniformLocation(
        g_foreground_program,
        "uHigh"
    );

    g_u_beat = glGetUniformLocation(
        g_foreground_program,
        "uBeat"
    );

    g_u_mode = glGetUniformLocation(
        g_foreground_program,
        "uMode"
    );

    const GLfloat vertices[] = {
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f,
    };

    glGenBuffers(1, &g_foreground_vbo);
    glBindBuffer(GL_ARRAY_BUFFER, g_foreground_vbo);

    glBufferData(
        GL_ARRAY_BUFFER,
        sizeof(vertices),
        vertices,
        GL_STATIC_DRAW
    );

    glBindBuffer(GL_ARRAY_BUFFER, 0);

    LOGI("FARIC foreground compositor ready");
    return true;
}

static float follow(
        float current,
        float target,
        float dt,
        float attackHz,
        float releaseHz) {
    const float rate =
        target > current ? attackHz : releaseHz;

    const float factor =
        std::clamp(dt * rate, 0.0f, 1.0f);

    return current + (target - current) * factor;
}

static void draw_foreground_locked() {
    if (!g_foreground_program || !g_foreground_vbo) {
        return;
    }

    const auto now = std::chrono::steady_clock::now();

    const float dt = std::clamp(
        std::chrono::duration<float>(
            now - g_last_frame_at
        ).count(),
        0.001f,
        0.050f
    );

    g_last_frame_at = now;

    g_amplitude = follow(
        g_amplitude,
        g_target_amplitude,
        dt,
        58.0f,
        14.0f
    );

    g_bass = follow(
        g_bass,
        g_target_bass,
        dt,
        72.0f,
        16.0f
    );

    g_mid = follow(
        g_mid,
        g_target_mid,
        dt,
        54.0f,
        14.0f
    );

    g_high = follow(
        g_high,
        g_target_high,
        dt,
        62.0f,
        15.0f
    );

    if (g_target_beat > g_beat) {
        g_beat = g_target_beat;
    } else {
        g_beat = std::max(
            0.0f,
            g_beat - dt * 3.2f
        );
    }

    g_target_beat = std::max(
        0.0f,
        g_target_beat - dt * 4.2f
    );

    const float timeSeconds =
        std::chrono::duration<float>(
            now - g_started_at
        ).count();

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, g_width, g_height);

    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE);

    glUseProgram(g_foreground_program);

    glUniform2f(
        g_u_resolution,
        static_cast<float>(g_width),
        static_cast<float>(g_height)
    );

    glUniform1f(g_u_time, timeSeconds);
    glUniform1f(g_u_amplitude, g_amplitude);
    glUniform1f(g_u_bass, g_bass);
    glUniform1f(g_u_mid, g_mid);
    glUniform1f(g_u_high, g_high);
    glUniform1f(g_u_beat, g_beat);
    glUniform1f(
        g_u_mode,
        static_cast<float>(g_foreground_sample.load(std::memory_order_relaxed))
    );

    glBindBuffer(
        GL_ARRAY_BUFFER,
        g_foreground_vbo
    );

    glEnableVertexAttribArray(
        static_cast<GLuint>(g_attr_position)
    );

    glVertexAttribPointer(
        static_cast<GLuint>(g_attr_position),
        2,
        GL_FLOAT,
        GL_FALSE,
        2 * sizeof(GLfloat),
        reinterpret_cast<void*>(0)
    );

    glDrawArrays(
        GL_TRIANGLE_STRIP,
        0,
        4
    );

    glDisableVertexAttribArray(
        static_cast<GLuint>(g_attr_position)
    );

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glUseProgram(0);
    glDisable(GL_BLEND);
}

static void reset_signal_locked() {
    g_target_amplitude = 0.0f;
    g_target_bass = 0.0f;
    g_target_mid = 0.0f;
    g_target_high = 0.0f;
    g_target_beat = 0.0f;

    g_amplitude = 0.0f;
    g_bass = 0.0f;
    g_mid = 0.0f;
    g_high = 0.0f;
    g_beat = 0.0f;
}

static void destroy_locked() {
    destroy_foreground_locked();

    if (g_playlist) {
        projectm_playlist_destroy(g_playlist);
        g_playlist = nullptr;
    }

    if (g_projectm) {
        projectm_destroy(g_projectm);
        g_projectm = nullptr;
    }

    reset_signal_locked();
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

    const char* pathChars =
        env->GetStringUTFChars(
            presetPath,
            nullptr
        );

    std::string path =
        pathChars ? pathChars : "";

    if (pathChars) {
        env->ReleaseStringUTFChars(
            presetPath,
            pathChars
        );
    }

    const char* textureChars =
        env->GetStringUTFChars(
            texturePath,
            nullptr
        );

    std::string texture =
        textureChars ? textureChars : "";

    if (textureChars) {
        env->ReleaseStringUTFChars(
            texturePath,
            textureChars
        );
    }

    g_width =
        std::max(
            1,
            static_cast<int>(width)
        );

    g_height =
        std::max(
            1,
            static_cast<int>(height)
        );

    g_started_at =
        std::chrono::steady_clock::now();

    g_last_frame_at = g_started_at;

    g_projectm = projectm_create();

    if (!g_projectm) {
        LOGE("projectm_create failed");
        return;
    }

    projectm_set_window_size(
        g_projectm,
        g_width,
        g_height
    );

    projectm_set_aspect_correction(
        g_projectm,
        true
    );

    projectm_set_mesh_size(
        g_projectm,
        meshX,
        meshY
    );

    projectm_set_fps(
        g_projectm,
        targetFps
    );

    projectm_set_preset_duration(
        g_projectm,
        18
    );

    projectm_set_soft_cut_duration(
        g_projectm,
        softCutSeconds
    );

    projectm_set_hard_cut_enabled(
        g_projectm,
        true
    );

    projectm_set_hard_cut_duration(
        g_projectm,
        18
    );

    projectm_set_hard_cut_sensitivity(
        g_projectm,
        1.0
    );

    projectm_set_beat_sensitivity(
        g_projectm,
        1.15
    );

    if (!texture.empty()) {
        const char* texturePaths[] = {
            texture.c_str()
        };

        projectm_set_texture_search_paths(
            g_projectm,
            texturePaths,
            1
        );
    }

    const bool directPreset =
        path.size() >= 5
        && path.substr(path.size() - 5) == ".milk";

    if (directPreset) {
        projectm_set_preset_locked(
            g_projectm,
            true
        );

        projectm_load_preset_file(
            g_projectm,
            path.c_str(),
            false
        );

        LOGI(
            "direct preset=%s mesh=%dx%d viewport=%dx%d",
            path.c_str(),
            meshX,
            meshY,
            g_width,
            g_height
        );
    } else {
        g_playlist =
            projectm_playlist_create(
                g_projectm
            );

        if (!g_playlist) {
            LOGE(
                "projectm_playlist_create failed"
            );

            return;
        }

        const auto added =
            projectm_playlist_add_path(
                g_playlist,
                path.c_str(),
                true,
                false
            );

        LOGI(
            "preset path=%s presets=%u mesh=%dx%d viewport=%dx%d",
            path.c_str(),
            added,
            meshX,
            meshY,
            g_width,
            g_height
        );

        if (added > 0) {
            projectm_playlist_set_shuffle(
                g_playlist,
                true
            );

            projectm_set_preset_locked(
                g_projectm,
                false
            );

            projectm_playlist_set_position(
                g_playlist,
                0,
                true
            );
        } else {
            projectm_load_preset_file(
                g_projectm,
                "idle://",
                false
            );
        }
    }

    create_foreground_locked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeResize(
        JNIEnv*,
        jclass,
        jint width,
        jint height) {
    std::lock_guard<std::mutex> lock(g_mutex);

    g_width =
        std::max(
            1,
            static_cast<int>(width)
        );

    g_height =
        std::max(
            1,
            static_cast<int>(height)
        );

    if (g_projectm) {
        projectm_set_window_size(
            g_projectm,
            g_width,
            g_height
        );
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeRender(
        JNIEnv*,
        jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_projectm) return;

    projectm_opengl_render_frame(
        g_projectm
    );

    draw_foreground_locked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeAddPcm(
        JNIEnv* env,
        jclass,
        jshortArray pcmData,
        jint frameCount) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_projectm || !pcmData) {
        return;
    }

    const jsize length =
        env->GetArrayLength(pcmData);

    if (length <= 0) {
        return;
    }

    jshort* data =
        env->GetShortArrayElements(
            pcmData,
            nullptr
        );

    if (!data) {
        return;
    }

    const int frames =
        std::min<int>(
            length,
            frameCount
        );

    projectm_pcm_add_int16(
        g_projectm,
        reinterpret_cast<const int16_t*>(
            data
        ),
        frames,
        PROJECTM_MONO
    );

    env->ReleaseShortArrayElements(
        pcmData,
        data,
        JNI_ABORT
    );
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeSetSignal(
        JNIEnv*,
        jclass,
        jfloat amplitude,
        jfloat bass,
        jfloat mid,
        jfloat high,
        jfloat beat) {
    std::lock_guard<std::mutex> lock(g_mutex);

    g_target_amplitude =
        std::clamp(
            static_cast<float>(amplitude),
            0.0f,
            1.0f
        );

    g_target_bass =
        std::clamp(
            static_cast<float>(bass),
            0.0f,
            1.0f
        );

    g_target_mid =
        std::clamp(
            static_cast<float>(mid),
            0.0f,
            1.0f
        );

    g_target_high =
        std::clamp(
            static_cast<float>(high),
            0.0f,
            1.0f
        );

    g_target_beat =
        std::max(
            g_target_beat,
            std::clamp(
                static_cast<float>(beat),
                0.0f,
                1.0f
            )
        );
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeLoadPreset(
        JNIEnv* env,
        jclass,
        jstring presetPath,
        jboolean smoothTransition) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_projectm || !presetPath) {
        return;
    }

    const char* pathChars =
        env->GetStringUTFChars(
            presetPath,
            nullptr
        );

    if (!pathChars) {
        return;
    }

    projectm_set_preset_locked(
        g_projectm,
        true
    );

    projectm_load_preset_file(
        g_projectm,
        pathChars,
        smoothTransition == JNI_TRUE
    );

    env->ReleaseStringUTFChars(
        presetPath,
        pathChars
    );
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeSetAutoPresetSwitching(
        JNIEnv*,
        jclass,
        jboolean enabled) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_projectm) {
        projectm_set_preset_locked(
            g_projectm,
            enabled != JNI_TRUE
        );
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeSetForegroundSample(
        JNIEnv*,
        jclass,
        jint sampleId) {
    g_foreground_sample.store(
        std::clamp(
            static_cast<int>(sampleId),
            0,
            2
        ),
        std::memory_order_relaxed
    );
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeNextPreset(
        JNIEnv*,
        jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (
        g_playlist
        && projectm_playlist_size(
            g_playlist
        ) > 0
    ) {
        if (g_projectm) {
            projectm_set_preset_locked(
                g_projectm,
                true
            );
        }

        projectm_playlist_play_next(
            g_playlist,
            true
        );
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_saney_musicvisualizer_projectm_ProjectMBridge_nativeDestroy(
        JNIEnv*,
        jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    destroy_locked();
}
