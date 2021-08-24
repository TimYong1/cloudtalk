include(ExternalProject)
ExternalProject_Add(libevent PREFIX libevent
        URL ${CMAKE_CURRENT_SOURCE_DIR}/cmake/dep/libevent-2.1.12-stable.tar.gz
        URL_MD5 b5333f021f880fe76490d8a799cd79f4
        CONFIGURE_COMMAND ./configure --prefix=${CMAKE_CURRENT_SOURCE_DIR}/third_party/libevent --disable-shared --disable-openssl
        BUILD_COMMAND make "CPPFLAGS=-I${OPENSSL_INCLUDE_DIRECTORIES}" "LDFLAGS=-L${OPENSSL_LINK_DIRECTORIES} -ldl"
        BUILD_IN_SOURCE 1)

ExternalProject_Get_Property(libevent INSTALL_DIR)
file(MAKE_DIRECTORY ${INSTALL_DIR}/include)

add_library(libevent-static STATIC IMPORTED GLOBAL)
set_property(TARGET libevent-static PROPERTY IMPORTED_LOCATION ${INSTALL_DIR}/lib/libevent.a)
set_property(TARGET libevent-static PROPERTY IMPORTED_INTERFACE_LINK_LIBRARIES pthread crypto)
set_property(TARGET libevent-static PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${INSTALL_DIR}/include)

add_library(libevent-static-openssl STATIC IMPORTED GLOBAL)
set_property(TARGET libevent-static-openssl PROPERTY IMPORTED_LOCATION ${INSTALL_DIR}/lib/libevent_openssl.a)
set_property(TARGET libevent-static-openssl PROPERTY IMPORTED_INTERFACE_LINK_LIBRARIES pthread crypto)
set_property(TARGET libevent-static-openssl PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${INSTALL_DIR}/include)

add_library(libevent-static-pthreads STATIC IMPORTED GLOBAL)
set_property(TARGET libevent-static-pthreads PROPERTY IMPORTED_LOCATION ${INSTALL_DIR}/lib/libevent_pthreads.a)
set_property(TARGET libevent-static-pthreads PROPERTY IMPORTED_INTERFACE_LINK_LIBRARIES pthread crypto)
set_property(TARGET libevent-static-pthreads PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${INSTALL_DIR}/include)
