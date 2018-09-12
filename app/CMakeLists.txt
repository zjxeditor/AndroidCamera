cmake_minimum_required(VERSION 3.4.1)

# Add system libraries
find_library(log-lib log)

add_library(nativedisplay SHARED
src/main/cpp/nativedisplay.cpp)

include_directories(src/main/cpp)

target_link_libraries(nativedisplay
                        -landroid
                       ${log-lib})