include(ExternalProject)
ExternalProject_Add(SpdLog
        PREFIX
            spdlog
        URL
            ${CMAKE_CURRENT_SOURCE_DIR}/cmake/dep/spdlog-1.8.5.tar.gz
        URL_MD5
            8755cdbc857794730a022722a66d431a
        CMAKE_CACHE_ARGS
            -DCMAKE_INSTALL_PREFIX:PATH=${CMAKE_CURRENT_SOURCE_DIR}/third_party/spdlog
        BUILD_IN_SOURCE 1)













