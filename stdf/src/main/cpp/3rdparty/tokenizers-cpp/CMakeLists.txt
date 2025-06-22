cmake_minimum_required(VERSION 3.19...3.30)
project(tokenizers_cpp C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(FetchContent)

set(TOKENIZERS_CPP_RUST_FLAGS "")
set(TOKENIZERS_CPP_CARGO_TARGET "")
set(TOKENIZERS_CPP_LINK_LIBS "")
set(TOKENIZERS_C_LINK_LIBS "")
set(CARGO_EXTRA_ENVS "")

# --- INIZIO BLOCCO DEFINITIVO V3 ---

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    list(APPEND TOKENIZERS_C_LINK_LIBS ${CMAKE_DL_LIBS})

elseif (CMAKE_SYSTEM_NAME STREQUAL "Darwin" OR CMAKE_SYSTEM_NAME STREQUAL "iOS")
    if (CMAKE_SYSTEM_NAME STREQUAL "iOS")
        if (CMAKE_OSX_SYSROOT MATCHES ".*iPhoneSimulator\\.platform.*")
            if(CMAKE_OSX_ARCHITECTURES MATCHES "x86_64")
                set(TOKENIZERS_CPP_CARGO_TARGET x86_64-apple-ios)
            else ()
                set(TOKENIZERS_CPP_CARGO_TARGET aarch64-apple-ios-sim)
            endif()
        else ()
            set(TOKENIZERS_CPP_CARGO_TARGET aarch64-apple-ios)
        endif()
    else() # Darwin (macOS)
        if (CMAKE_SYSTEM_PROCESSOR STREQUAL "arm64")
            set(TOKENIZERS_CPP_CARGO_TARGET aarch64-apple-darwin)
        endif()
    endif()
    find_library(FOUNDATION_LIB Foundation)
    find_library(SECURITY_LIB Security)
    list(APPEND TOKENIZERS_C_LINK_LIBS ${FOUNDATION_LIB} ${SECURITY_LIB})
    if (CMAKE_OSX_DEPLOYMENT_TARGET)
        set(CARGO_EXTRA_ENVS
                MACOSX_DEPLOYMENT_TARGET=${CMAKE_OSX_DEPLOYMENT_TARGET}
        )
    endif()

elseif (CMAKE_SYSTEM_NAME STREQUAL "Android")
    if (ANDROID_ABI STREQUAL "arm64-v8a")
        set(TOKENIZERS_CPP_CARGO_TARGET aarch64-linux-android)
    else()
        message(FATAL_ERROR "ABI Android non supportato: ${ANDROID_ABI}.")
    endif()

    # Logica robusta per rilevare l'host Windows
    if(CMAKE_HOST_WIN32)
        set(HOST_TAG "windows-x86_64")
        set(EXE_SUFFIX ".cmd")
    else() # Assume Linux/macOS
        set(HOST_TAG "linux-x86_64")
        set(EXE_SUFFIX "")
    endif()

    set(ANDROID_NDK_TOOLCHAIN_ROOT "${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/${HOST_TAG}")

    set(CARGO_EXTRA_ENVS
            AR_${TOKENIZERS_CPP_CARGO_TARGET}=${ANDROID_NDK_TOOLCHAIN_ROOT}/bin/llvm-ar${EXE_SUFFIX}
            CC_${TOKENIZERS_CPP_CARGO_TARGET}=${ANDROID_NDK_TOOLCHAIN_ROOT}/bin/${TOKENIZERS_CPP_CARGO_TARGET}${ANDROID_NATIVE_API_LEVEL}-clang${EXE_SUFFIX}
            CXX_${TOKENIZERS_CPP_CARGO_TARGET}=${ANDROID_NDK_TOOLCHAIN_ROOT}/bin/${TOKENIZERS_CPP_CARGO_TARGET}${ANDROID_NATIVE_API_LEVEL}-clang++${EXE_SUFFIX}
    )

elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    set(TOKENIZERS_CPP_CARGO_TARGET x86_64-pc-windows-msvc)
    list(APPEND TOKENIZERS_C_LINK_LIBS ntdll wsock32 ws2_32 Bcrypt iphlpapi userenv psapi)

endif()

# --- FINE BLOCCO DEFINITIVO ---

set(TOKENIZERS_CPP_CARGO_FLAGS "")
set(TOKENIZERS_CPP_CARGO_TARGET_DIR ${CMAKE_CURRENT_BINARY_DIR})
set(TOKENIZERS_CPP_CARGO_BINARY_DIR ${CMAKE_CURRENT_BINARY_DIR})

if (NOT TOKENIZERS_CPP_CARGO_TARGET STREQUAL "")
    list(APPEND TOKENIZERS_CPP_CARGO_FLAGS --target ${TOKENIZERS_CPP_CARGO_TARGET})
    set(TOKENIZERS_CPP_CARGO_BINARY_DIR
            "${TOKENIZERS_CPP_CARGO_BINARY_DIR}/${TOKENIZERS_CPP_CARGO_TARGET}")
endif()

if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(TOKENIZERS_CPP_CARGO_BINARY_DIR "${TOKENIZERS_CPP_CARGO_BINARY_DIR}/debug")
else ()
    list(APPEND TOKENIZERS_CPP_CARGO_FLAGS --release)
    set(TOKENIZERS_CPP_CARGO_BINARY_DIR "${TOKENIZERS_CPP_CARGO_BINARY_DIR}/release")
endif ()

get_filename_component(TOKENIZERS_CPP_ROOT ${CMAKE_CURRENT_LIST_FILE} DIRECTORY)
set(TOKENIZERS_CPP_CARGO_SOURCE_PATH ${TOKENIZERS_CPP_ROOT}/rust)

option(MSGPACK_USE_BOOST "Use Boost libraried" OFF)
add_subdirectory(msgpack)

option(MLC_ENABLE_SENTENCEPIECE_TOKENIZER "Enable SentencePiece tokenizer" ON)

if(MSVC)
    set(TOKENIZERS_RUST_LIB "${TOKENIZERS_CPP_CARGO_BINARY_DIR}/tokenizers_c.lib")
else()
    set(TOKENIZERS_RUST_LIB "${TOKENIZERS_CPP_CARGO_BINARY_DIR}/libtokenizers_c.a")
endif()
set(TOKENIZERS_CPP_INCLUDE ${TOKENIZERS_CPP_ROOT}/include)

add_custom_command(
        OUTPUT ${TOKENIZERS_RUST_LIB}
        COMMAND
        ${CMAKE_COMMAND} -E env
        CARGO_TARGET_DIR=${TOKENIZERS_CPP_CARGO_TARGET_DIR}
        ${CARGO_EXTRA_ENVS}
        RUSTFLAGS="${TOKENIZERS_CPP_RUST_FLAGS}"
        cargo build ${TOKENIZERS_CPP_CARGO_FLAGS}
        WORKING_DIRECTORY ${TOKENIZERS_CPP_CARGO_SOURCE_PATH}
        POST_BUILD COMMAND
        ${CMAKE_COMMAND} -E copy
        ${TOKENIZERS_RUST_LIB} "${CMAKE_CURRENT_BINARY_DIR}"
)

set(
        TOKENIZER_CPP_SRCS
        src/sentencepiece_tokenizer.cc
        src/huggingface_tokenizer.cc
        src/rwkv_world_tokenizer.cc
)
add_library(tokenizer_cpp_objs OBJECT ${TOKENIZER_CPP_SRCS})
target_include_directories(tokenizer_cpp_objs PRIVATE sentencepiece/src)
target_include_directories(tokenizer_cpp_objs PRIVATE msgpack/include)
target_include_directories(tokenizer_cpp_objs PUBLIC ${TOKENIZERS_CPP_INCLUDE})
if (MLC_ENABLE_SENTENCEPIECE_TOKENIZER STREQUAL "ON")
    target_compile_definitions(tokenizer_cpp_objs PUBLIC MLC_ENABLE_SENTENCEPIECE_TOKENIZER)
endif ()
target_link_libraries(tokenizer_cpp_objs PRIVATE msgpack-cxx)

option(SPM_ENABLE_SHARED "override sentence piece config" OFF)
option(SPM_ENABLE_TCMALLOC "" OFF)

if (CMAKE_SYSTEM_NAME STREQUAL "iOS")
    macro (set_xcode_property TARGET XCODE_PROPERTY XCODE_VALUE)
        set_property (TARGET ${TARGET} PROPERTY
                XCODE_ATTRIBUTE_${XCODE_PROPERTY} ${XCODE_VALUE})
    endmacro (set_xcode_property)
endif()
add_subdirectory(sentencepiece sentencepiece EXCLUDE_FROM_ALL)

add_library(tokenizers_c INTERFACE ${TOKENIZERS_RUST_LIB})
target_link_libraries(tokenizers_c INTERFACE ${TOKENIZERS_RUST_LIB} ${TOKENIZERS_C_LINK_LIBS})

add_library(tokenizers_cpp STATIC $<TARGET_OBJECTS:tokenizer_cpp_objs>)
target_link_libraries(tokenizers_cpp PRIVATE tokenizers_c sentencepiece-static ${TOKENIZERS_CPP_LINK_LIBS})
target_include_directories(tokenizers_cpp PUBLIC ${TOKENIZERS_CPP_INCLUDE})
