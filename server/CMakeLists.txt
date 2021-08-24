cmake_minimum_required(VERSION 3.14.0)
project(TeamTalk)


set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -I/usr/local/include -std=c++14 \
     -Wall  -fPIC")
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OPTION_CXX_FLAGS}")
set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake)
include(curl)
include(spdlog)
include(protobuf)
add_subdirectory(src)