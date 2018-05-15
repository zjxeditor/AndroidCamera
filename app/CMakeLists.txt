cmake_minimum_required(VERSION 3.4.1)

# Add system libraries
find_library(log-lib log)

# Add third-party libraries
add_library( csrt SHARED IMPORTED )
set_target_properties(csrt
PROPERTIES IMPORTED_LOCATION
${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libcsrt.so)
include_directories( src/main/jniLibs/include )


add_library(nativetracker SHARED
src/main/cpp/nativetracker.cpp )
add_library(nativedisplay SHARED
src/main/cpp/nativedisplay.cpp)

include_directories(src/main/cpp)

target_link_libraries(nativetracker
                       csrt
                       ${log-lib})
target_link_libraries(nativedisplay
                        -landroid
                       ${log-lib})