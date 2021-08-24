include(ExternalProject)

### OpenSSL 1.1.0h


ExternalProject_Add(OpenSSL PREFIX openssl
        URL ${CMAKE_CURRENT_SOURCE_DIR}/cmake/dep/openssl-1.1.0h.tar.gz
        URL_MD5 5271477e4d93f4ea032b665ef095ff24
        CONFIGURE_COMMAND ./Configure linux-x86_64 --prefix=${CMAKE_CURRENT_SOURCE_DIR}/third_party/openssl --openssldir=${CMAKE_CURRENT_SOURCE_DIR}/third_party/openssl/lib/ssl
        no-weak-ssl-ciphers enable-ec_nistp_64_gcc_128 no-shared
        BUILD_IN_SOURCE 1)

ExternalProject_Get_Property(OpenSSL INSTALL_DIR)
set(OPENSSL_INCLUDE_DIRECTORIES ${INSTALL_DIR}/include)
set(OPENSSL_LINK_DIRECTORIES ${INSTALL_DIR}/lib)
file(MAKE_DIRECTORY ${INSTALL_DIR}/include)

add_library(openssl-crypto-static STATIC IMPORTED GLOBAL)
set_property(TARGET openssl-crypto-static PROPERTY IMPORTED_LOCATION ${INSTALL_DIR}/lib/libcrypto.a)
set_property(TARGET openssl-crypto-static PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${INSTALL_DIR}/include)

add_library(openssl-ssl-static STATIC IMPORTED GLOBAL)
set_property(TARGET openssl-ssl-static PROPERTY IMPORTED_LOCATION ${INSTALL_DIR}/lib/libssl.a)
set_property(TARGET openssl-ssl-static PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${INSTALL_DIR}/include)
