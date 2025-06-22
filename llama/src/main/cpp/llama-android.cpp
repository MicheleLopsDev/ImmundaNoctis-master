#include <android/log.h>
#include <jni.h>
#include <iomanip>
#include <cmath>
#include <string>
#include <unistd.h>

#include "llama.h"
#include "common.h"
#include "llama-sampling.h" // <-- L'include necessario

#define TAG "llama-android.cpp"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

jclass la_int_var;
jmethodID la_int_var_value;
jmethodID la_int_var_inc;

std::string cached_token_chars;

bool is_valid_utf8(const char * string) {
    if (!string) return true;
    const unsigned char * bytes = (const unsigned char *)string;
    int num;
    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) num = 1;
        else if ((*bytes & 0xE0) == 0xC0) num = 2;
        else if ((*bytes & 0xF0) == 0xE0) num = 3;
        else if ((*bytes & 0xF8) == 0xF0) num = 4;
        else return false;
        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) return false;
            bytes += 1;
        }
    }
    return true;
}

static void log_callback(ggml_log_level level, const char * fmt, void * data) {
    if (level == GGML_LOG_LEVEL_ERROR)     __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_INFO) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_WARN) __android_log_print(ANDROID_LOG_WARN, TAG, fmt, data);
    else __android_log_print(ANDROID_LOG_DEFAULT, TAG, fmt, data);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_load_1model(JNIEnv *env, jobject, jstring filename) {
    llama_model_params model_params = llama_model_default_params();
    auto path_to_model = env->GetStringUTFChars(filename, 0);
    LOGi("Loading model from %s", path_to_model);
    auto model = llama_model_load_from_file(path_to_model, model_params);
    env->ReleaseStringUTFChars(filename, path_to_model);
    if (!model) {
        LOGe("load_model() failed");
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "load_model() failed");
        return 0;
    }
    return reinterpret_cast<jlong>(model);
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1model(JNIEnv *, jobject, jlong model) {
    llama_model_free(reinterpret_cast<llama_model *>(model));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_new_1context(JNIEnv *env, jobject, jlong jmodel) {
    auto model = reinterpret_cast<llama_model *>(jmodel);
    if (!model) {
        LOGe("new_context(): model cannot be null");
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Model cannot be null");
        return 0;
    }
    int n_threads = std::max(1, std::min(8, (int) sysconf(_SC_NPROCESSORS_ONLN) - 2));
    LOGi("Using %d threads", n_threads);
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    llama_context * context = llama_new_context_with_model(model, ctx_params);
    if (!context) {
        LOGe("llama_new_context_with_model() returned null)");
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "llama_new_context_with_model() returned null)");
        return 0;
    }
    return reinterpret_cast<jlong>(context);
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1context(JNIEnv *, jobject, jlong context) {
    llama_free(reinterpret_cast<llama_context *>(context));
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_backend_1free(JNIEnv *, jobject) {
    llama_backend_free();
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_log_1to_1android(JNIEnv *, jobject) {
    llama_log_set(log_callback, NULL);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_new_1batch(JNIEnv *, jobject, jint n_tokens, jint embd, jint n_seq_max) {
    return reinterpret_cast<jlong>(new llama_batch(llama_batch_init(n_tokens, embd, n_seq_max)));
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1batch(JNIEnv *, jobject, jlong batch_pointer) {
    delete reinterpret_cast<llama_batch *>(batch_pointer);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_new_1sampler(
        JNIEnv *env,
        jobject thiz,
        jfloat temperature,
        jfloat repeat_penalty,
        jint top_k,
        jfloat top_p
) {
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * sampler_chain = llama_sampler_chain_init(sparams);

    LOGi("Creating final sampler chain with temp=%.2f, repeat_penalty=%.2f, top_k=%d, top_p=%.2f",
         temperature, repeat_penalty, top_k, top_p);

    // 1. Repetition Penalty
    llama_sampler_chain_add(sampler_chain, llama_sampler_init_penalties(
            64,
            repeat_penalty,
            0.0f,
            0.0f
    ));

    // 2. Top-K
    llama_sampler_chain_add(sampler_chain, llama_sampler_init_top_k(top_k));

    // 3. Top-P
    llama_sampler_chain_add(sampler_chain, llama_sampler_init_top_p(top_p, 1));

    // 4. Temperature
    llama_sampler_chain_add(sampler_chain, llama_sampler_init_temp(temperature));

    // Aggiungiamo un campionatore di default alla fine per fare la scelta finale
    llama_sampler_chain_add(sampler_chain, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    return reinterpret_cast<jlong>(sampler_chain);
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1sampler(JNIEnv *, jobject, jlong sampler_pointer) {
    llama_sampler_free(reinterpret_cast<llama_sampler *>(sampler_pointer));
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_backend_1init(JNIEnv *, jobject, jboolean numa) {
    llama_backend_init();
    llama_numa_init(numa ? GGML_NUMA_STRATEGY_DISTRIBUTE : GGML_NUMA_STRATEGY_DISABLED);
}


extern "C"
JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_system_1info(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

extern "C"
JNIEXPORT jint JNICALL
Java_android_llama_cpp_LLamaAndroid_completion_1init(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jstring jtext,
        jboolean format_chat,
        jint n_len
) {
    cached_token_chars.clear();
    const auto text = env->GetStringUTFChars(jtext, 0);
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    auto batch = reinterpret_cast<llama_batch *>(batch_pointer);
    bool parse_special = (format_chat == JNI_TRUE);
    const auto tokens_list = common_tokenize(context, text, true, parse_special);
    auto n_ctx = llama_n_ctx(context);
    auto n_kv_req = tokens_list.size() + n_len;
    LOGi("n_len = %d, n_ctx = %d, n_kv_req = %d", n_len, n_ctx, n_kv_req);
    if (n_kv_req > n_ctx) {
        LOGe("error: n_kv_req > n_ctx, the required KV cache size is not big enough");
    }
    for (auto id : tokens_list) {
        LOGi("token: `%s`-> %d ", common_token_to_piece(context, id).c_str(), id);
    }
    common_batch_clear(*batch);
    for (size_t i = 0; i < tokens_list.size(); i++) {
        common_batch_add(*batch, tokens_list[i], i, { 0 }, false);
    }
    batch->logits[batch->n_tokens - 1] = true;
    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() failed");
    }
    env->ReleaseStringUTFChars(jtext, text);
    return batch->n_tokens;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_completion_1loop(
        JNIEnv * env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jlong sampler_pointer,
        jint n_len,
        jobject intvar_ncur
) {
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    auto batch   = reinterpret_cast<llama_batch   *>(batch_pointer);
    auto sampler = reinterpret_cast<llama_sampler *>(sampler_pointer);
    const auto model = llama_get_model(context);
    const auto vocab = llama_model_get_vocab(model);

    if (!la_int_var) la_int_var = env->GetObjectClass(intvar_ncur);
    if (!la_int_var_value) la_int_var_value = env->GetMethodID(la_int_var, "getValue", "()I");
    if (!la_int_var_inc) la_int_var_inc = env->GetMethodID(la_int_var, "inc", "()V");

    const auto new_token_id = llama_sampler_sample(sampler, context, -1);
    llama_sampler_accept(sampler, new_token_id);

    const auto n_cur = env->CallIntMethod(intvar_ncur, la_int_var_value);
    if (llama_vocab_is_eog(vocab, new_token_id) || n_cur >= n_len) {
        return nullptr;
    }

    auto new_token_chars = common_token_to_piece(context, new_token_id);
    cached_token_chars += new_token_chars;

    jstring new_token = nullptr;
    if (is_valid_utf8(cached_token_chars.c_str())) {
        new_token = env->NewStringUTF(cached_token_chars.c_str());
        LOGi("cached: %s, new_token_chars: `%s`, id: %d", cached_token_chars.c_str(), new_token_chars.c_str(), new_token_id);
        cached_token_chars.clear();
    } else {
        new_token = env->NewStringUTF("");
    }
    common_batch_clear(*batch);
    common_batch_add(*batch, new_token_id, n_cur, { 0 }, true);
    env->CallVoidMethod(intvar_ncur, la_int_var_inc);
    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() returned null");
    }
    return new_token;
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_kv_1cache_1clear(JNIEnv *, jobject, jlong context) {
    llama_memory_clear(llama_get_memory(reinterpret_cast<llama_context *>(context)), true);
}