include(ExternalProject)

ExternalProject_Add(Acl
        PREFIX
            acl
        URL
            ${CMAKE_CURRENT_SOURCE_DIR}/cmake/dep/acl-3.5.2-0.tar.gz
        URL_MD5
            78087f58c7686d36be0359a5268674d9
        CMAKE_CACHE_ARGS
            -DCMAKE_INSTALL_PREFIX:PATH=${CMAKE_CURRENT_SOURCE_DIR}/third_party/acl
        CONFIGURE_COMMAND
            ""
        BUILD_IN_SOURCE
            1
)




