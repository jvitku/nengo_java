/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ca_nengo_util_impl_NEFGPUInterface */

#ifndef _Included_ca_nengo_util_impl_NEFGPUInterface
#define _Included_ca_nengo_util_impl_NEFGPUInterface
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ca_nengo_util_impl_NEFGPUInterface
 * Method:    nativeGetNumDevices
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ca_nengo_util_impl_NEFGPUInterface_nativeGetNumDevices
  (JNIEnv *, jclass);
 
/*
 * Class:     ca_nengo_util_impl_NEFGPUInterface
 * Method:    nativeSetupRun
 * Signature: ([[[[F[[I[[F[[[F[[[[F[[F[[I[[I[IF)V
 */
JNIEXPORT void JNICALL Java_ca_nengo_util_impl_NEFGPUInterface_nativeSetupRun
  (JNIEnv *, jclass, jobjectArray, jobjectArray, jobjectArray, jobjectArray, jobjectArray, jobjectArray, jobjectArray, jobjectArray, jobjectArray, jintArray, jfloat, jintArray, jint);

/*
 * Class:     ca_nengo_util_impl_NEFGPUInterface
 * Method:    nativeStep
 * Signature: ([[[F[[[F[[FFF)V
 */
JNIEXPORT void JNICALL Java_ca_nengo_util_impl_NEFGPUInterface_nativeStep
  (JNIEnv *, jclass, jobjectArray, jobjectArray, jobjectArray, jfloat, jfloat);

/*
 * Class:     ca_nengo_util_impl_NEFGPUInterface
 * Method:    nativeKill
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ca_nengo_util_impl_NEFGPUInterface_nativeKill
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
