# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("llama-android")

#load local llama.cpp
add_subdirectory(../../../../../../dev/llama.cpp-master build-llama)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
add_library(${CMAKE_PROJECT_NAME} SHARED
        llama-android.cpp
)

# Aggiungi questo blocco per dire al tuo file .cpp dove trovare gli header di llama.cpp
target_include_directories(${CMAKE_PROJECT_NAME} PUBLIC
        # Path alla cartella principale di llama.cpp (per common.h, etc.)
        "../../../../../../dev/llama.cpp-master"

        # --- LA RIGA DECISIVA È QUESTA ---
        # Path alla cartella 'src' (per llama-sampling.h e altri header interni)
        "../../../../../../dev/llama.cpp-master/src"
)

# Specifies libraries CMake should link to your target library.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        llama
        common
        android
        log
)