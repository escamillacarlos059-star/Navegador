#include <jni.h>
#include <string>
#include "vectra_engine.hpp"

namespace {

std::string jstring_to_std(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    if (chars == nullptr) return "";
    std::string out(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return out;
}

} // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeNormalizeInput(
    JNIEnv* env,
    jobject /* thiz */,
    jstring rawInput,
    jint searchEngineId,
    jboolean stripTrackingParams
) {
    const std::string input = jstring_to_std(env, rawInput);
    const auto res = vectra::VectraEngine::instance().normalize_input(
        input,
        static_cast<int>(searchEngineId),
        stripTrackingParams == JNI_TRUE
    );
    // Pack fields with '\n' delimiter: finalUrl\ndisplayHost\nisSearch\nstrippedCount
    const std::string packed = res.final_url + "\n" +
        res.display_host + "\n" +
        (res.is_search_query ? "1" : "0") + "\n" +
        std::to_string(res.stripped_params_count);
    return env->NewStringUTF(packed.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeShouldBlockRequest(
    JNIEnv* env,
    jobject /* thiz */,
    jstring requestUrl,
    jstring pageHost
) {
    const std::string req = jstring_to_std(env, requestUrl);
    const std::string host = jstring_to_std(env, pageHost);
    const bool blocked = vectra::VectraEngine::instance().should_block_request(req, host);
    return blocked ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeAnalyzeSecurity(
    JNIEnv* env,
    jobject /* thiz */,
    jstring url
) {
    const std::string u = jstring_to_std(env, url);
    return static_cast<jint>(vectra::VectraEngine::instance().analyze_security(u));
}

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeExtractHost(
    JNIEnv* env,
    jobject /* thiz */,
    jstring url
) {
    const std::string u = jstring_to_std(env, url);
    const std::string host = vectra::VectraEngine::instance().extract_host(u);
    return env->NewStringUTF(host.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeAddCustomBlockedDomain(
    JNIEnv* env,
    jobject /* thiz */,
    jstring domain
) {
    const std::string d = jstring_to_std(env, domain);
    return vectra::VectraEngine::instance().add_custom_blocked_domain(d) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeRemoveCustomBlockedDomain(
    JNIEnv* env,
    jobject /* thiz */,
    jstring domain
) {
    const std::string d = jstring_to_std(env, domain);
    return vectra::VectraEngine::instance().remove_custom_blocked_domain(d) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeGetCustomBlockedDomains(
    JNIEnv* env,
    jobject /* thiz */
) {
    const auto list = vectra::VectraEngine::instance().get_custom_blocked_domains();
    std::string joined;
    for (std::size_t i = 0; i < list.size(); ++i) {
        if (i > 0) joined.push_back('\n');
        joined.append(list[i]);
    }
    return env->NewStringUTF(joined.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeGetDiagnostics(
    JNIEnv* env,
    jobject /* thiz */
) {
    const auto d = vectra::VectraEngine::instance().get_diagnostics();
    const std::string packed =
        d.cpp_standard + "\n" +
        d.compiler_info + "\n" +
        d.target_abi + "\n" +
        std::to_string(d.filter_rules_count) + "\n" +
        std::to_string(d.custom_rules_count) + "\n" +
        std::to_string(d.total_requests_inspected) + "\n" +
        std::to_string(d.total_trackers_blocked) + "\n" +
        std::to_string(d.total_params_stripped) + "\n" +
        std::to_string(d.last_lookup_nanos);
    return env->NewStringUTF(packed.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeResetStats(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    vectra::VectraEngine::instance().reset_stats();
}

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_VectraNativeEngine_nativeInspectPeBytes(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray bytes
) {
    if (bytes == nullptr) {
        return env->NewStringUTF("0\nN/A\nN/A\nN/A\n0\nSin datos");
    }
    const jsize len = env->GetArrayLength(bytes);
    jbyte* raw = env->GetByteArrayElements(bytes, nullptr);
    const auto pe = vectra::VectraEngine::instance().inspect_pe_bytes(
        reinterpret_cast<const std::uint8_t*>(raw),
        static_cast<std::size_t>(len > 0 ? len : 0)
    );
    if (raw != nullptr) {
        env->ReleaseByteArrayElements(bytes, raw, JNI_ABORT);
    }
    const std::string packed =
        std::string(pe.is_valid_pe ? "1" : "0") + "\n" +
        pe.architecture + "\n" +
        pe.pe_format + "\n" +
        pe.subsystem + "\n" +
        std::to_string(pe.sections_count) + "\n" +
        pe.status_detail;
    return env->NewStringUTF(packed.c_str());
}

} // extern "C"
