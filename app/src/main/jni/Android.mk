LOCAL_PATH := $(call my-dir)

THIS_PATH := $(LOCAL_PATH)
LIBWEBP_PATH := $(THIS_PATH)/libwebp

CFLAGS := -Wall \
          -Werror \
          -O2 \
          -DANDROID \
          -DWEBP_SWAP_16BIT_CSP \


################################################################################
# libwebp-opt - compiles a static archive of libwebp with optimizations

include $(LIBWEBP_PATH)/Android.mk
include $(CLEAR_VARS)

LOCAL_PATH := $(LIBWEBP_PATH)

LOCAL_MODULE := webp-opt

LOCAL_SRC_FILES := \
    $(dec_srcs) \
    $(dsp_dec_srcs) \
    $(enc_srcs) \
    $(dsp_enc_srcs) \
    $(mux_srcs) \
    $(utils_dec_srcs) \
    $(utils_enc_srcs) \

LOCAL_CFLAGS := $(CFLAGS)
LOCAL_C_INCLUDES += $(LIBWEBP_PATH)/src

# prefer arm over thumb mode for performance gains
LOCAL_ARM_MODE := arm

include $(BUILD_STATIC_LIBRARY)

################################################################################
# libwebp_encoder - compiles webp_encoder with libwebp static library

include $(CLEAR_VARS)

LOCAL_PATH := $(THIS_PATH)
LOCAL_SRC_FILES := $(THIS_PATH)/webp_encoder.cpp
LOCAL_C_INCLUDES := $(LIBWEBP_PATH)/src

LOCAL_CFLAGS := $(CFLAGS)
LOCAL_CPPFLAGS := -std=c++11

# Debug release contains logging
ifneq ($(APP_OPTIM),release)
    LOCAL_LDLIBS := -llog
    LOCAL_CFLAGS += "-DDEBUG"
endif

# prefer arm over thumb mode for performance gains
LOCAL_ARM_MODE := arm

LOCAL_WHOLE_STATIC_LIBRARIES := webp-opt

LOCAL_MODULE := webp_encoder

include $(BUILD_SHARED_LIBRARY)
