/**/

#include <string.h>
#include <malloc.h>
#include "sweet_b_signer.h"
#include "sb_sw_lib.h"

/*
 * Is called automatically when library is loaded.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env;
    LOGI("Loaded");

    if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_6) != JNI_OK)
        return -1;

    return JNI_VERSION_1_6;
}

JNIEXPORT jbyteArray JNICALL
Java_com_acurast_attested_executor_native_SweetBSigner_jniCompressPublicKey(
        JNIEnv *env,
        jobject obj,
        jbyteArray publicKey,
        jint curve
) {
    // Prepare public key
    jsize public_key_length = (*env)->GetArrayLength(env, publicKey);
    jbyte *public_key_l = (*env)->GetByteArrayElements(env, publicKey, NULL);
    sb_sw_public_t public_key;
    for (int i = 0; i < public_key_length; i++) {
        public_key.bytes[i] = public_key_l[i];
    }

    // Generate compressed public key
    sb_sw_context_t ct;
    sb_sw_compressed_t compressed_pk;
    _Bool sign;
    sb_error_t result = sb_sw_compress_public_key(
            &ct,
            &compressed_pk,
            &sign,
            &public_key,
            curve,
            SB_DATA_ENDIAN_BIG
    );

    // https://asecuritysite.com/ecc/js_ethereum2
    // y is even (02) or odd (03)
    jbyte yParityByte = sign ? 0x03 : 0x02;

    // Free buffers
    (*env)->ReleaseByteArrayElements(env, publicKey, public_key_l, JNI_ABORT);
    if (result != SB_SUCCESS) return NULL;

    jsize compressed_pk_length = sizeof compressed_pk + 1;
    jbyteArray compressed_pk_bytes = (*env)->NewByteArray(env, compressed_pk_length);
    (*env)->SetByteArrayRegion(env, compressed_pk_bytes, 0, 1, &yParityByte);
    (*env)->SetByteArrayRegion(env, compressed_pk_bytes, 1, compressed_pk_length - 1,
                               &compressed_pk);

    return compressed_pk_bytes;
}

JNIEXPORT jbyteArray JNICALL
Java_com_acurast_attested_executor_native_SweetBSigner_jniGenerateSharedSecret(
        JNIEnv *env,
        jobject obj,
        jbyteArray privateKey,
        jbyteArray publicKey,
        jint curve
) {
    // Prepare private key
    jsize private_key_length = (*env)->GetArrayLength(env, privateKey);
    jbyte *private_key_l = (*env)->GetByteArrayElements(env, privateKey, NULL);
    sb_sw_private_t private_key;
    for (int i = 0; i < private_key_length; i++) {
        private_key.bytes[i] = private_key_l[i];
    }

    // Prepare public key
    jsize public_key_length = (*env)->GetArrayLength(env, publicKey);
    jbyte *public_key_l = (*env)->GetByteArrayElements(env, publicKey, NULL);
    sb_sw_public_t public_key;
    for (int i = 0; i < public_key_length; i++) {
        public_key.bytes[i] = public_key_l[i];
    }

    // Generate shared secret
    sb_sw_context_t ct;
    sb_sw_shared_secret_t secret;
    sb_error_t result = sb_sw_shared_secret(
            &ct,
            &secret,
            &private_key,
            &public_key,
            NULL,
            curve,
            SB_DATA_ENDIAN_BIG
    );

    // Free buffers
    (*env)->ReleaseByteArrayElements(env, privateKey, private_key_l, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, publicKey, public_key_l, JNI_ABORT);
    if (result != SB_SUCCESS) return NULL;

    jsize secret_length = sizeof secret;
    jbyteArray secret_bytes = (*env)->NewByteArray(env, secret_length);
    (*env)->SetByteArrayRegion(env, secret_bytes, 0, secret_length, &secret);

    return secret_bytes;
}

JNIEXPORT jbyteArray JNICALL
Java_com_acurast_attested_executor_native_SweetBSigner_jniSignMessageDigest(
        JNIEnv *env,
        jobject obj,
        jbyteArray privateKey,
        jbyteArray digest,
        jint curve
) {
    // Prepare private key
    jsize private_key_length = (*env)->GetArrayLength(env, privateKey);
    jbyte *private_key_l = (*env)->GetByteArrayElements(env, privateKey, NULL);
    sb_sw_private_t private_key;
    for (int i = 0; i < private_key_length; i++) {
        private_key.bytes[i] = private_key_l[i];
    }
    // Prepare digest message
    jsize digest_length = (*env)->GetArrayLength(env, digest);
    jbyte *digest_l = (*env)->GetByteArrayElements(env, digest, NULL);
    sb_sw_message_digest_t message_digest;
    for (int i = 0; i < digest_length; i++) {
        message_digest.bytes[i] = digest_l[i];
    }

    // Sign payload
    sb_sw_context_t ct;
    sb_sw_signature_t signature;
    sb_error_t result = sb_sw_sign_message_digest(
            &ct,
            &signature,
            &private_key,
            &message_digest,
            NULL,
            curve,
            SB_DATA_ENDIAN_BIG
    );

    // Free buffers
    (*env)->ReleaseByteArrayElements(env, privateKey, private_key_l, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, digest, digest_l, JNI_ABORT);
    if (result != SB_SUCCESS) return NULL;

    jsize signature_length = sizeof signature;
    jbyteArray signature_bytes = (*env)->NewByteArray(env, signature_length);
    (*env)->SetByteArrayRegion(env, signature_bytes, 0, signature_length, &signature);

    return signature_bytes;
}

JNIEXPORT jbyteArray JNICALL
Java_com_acurast_attested_executor_native_SweetBSigner_jniComputePublicKey(
        JNIEnv *env,
        jobject obj,
        jbyteArray data,
        jint curve
) {
    // Prepare private key
    jsize num_bytes = (*env)->GetArrayLength(env, data);
    jbyte *lib = (*env)->GetByteArrayElements(env, data, NULL);
    sb_sw_private_t private_key;
    for (int i = 0; i < num_bytes; i++) {
        private_key.bytes[i] = lib[i];
    }

    // Compute public key
    sb_sw_public_t public_key;
    sb_sw_context_t ct;
    sb_error_t result = sb_sw_compute_public_key(&ct, &public_key, &private_key, NULL, curve,
                                                 SB_DATA_ENDIAN_BIG);

    // Free buffers
    (*env)->ReleaseByteArrayElements(env, data, lib, JNI_ABORT);

    if (result != SB_SUCCESS) return NULL;

    jsize public_key_length = sizeof public_key;
    jbyteArray public_key_bytes = (*env)->NewByteArray(env, public_key_length);
    (*env)->SetByteArrayRegion(env, public_key_bytes, 0, public_key_length, &public_key);

    return public_key_bytes;
}

JNIEXPORT jint JNICALL
Java_com_acurast_attested_executor_native_SweetBSigner_jniVerifyPublicKey(
        JNIEnv *env,
        jobject obj,
        jbyteArray data,
        jint curve
) {
    // Prepare public key
    jsize public_key_length = (*env)->GetArrayLength(env, data);
    sb_byte_t *public_key_buffer = (char *) malloc(public_key_length);
    jbyte *lib = (*env)->GetByteArrayElements(env, data, NULL);
    memcpy(public_key_buffer, lib, public_key_length);
    sb_sw_public_t public_key;
    for (int i = 0; i < public_key_length; i++) {
        public_key.bytes[i] = public_key_buffer[i];
    }

    // Validate public key
    sb_sw_context_t ct;
    sb_error_t result = sb_sw_valid_public_key(&ct, &public_key, curve, SB_DATA_ENDIAN_BIG);

    // Free buffers
    (*env)->ReleaseByteArrayElements(env, data, lib, JNI_ABORT);

    return result;
}
