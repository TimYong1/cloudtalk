include(ExternalProject)



set(CURL_ROOT          ${CMAKE_CURRENT_BINARY_DIR}/3rd_party/curl-7.78.0)

ExternalProject_Add(curl-7.78.0
        PREFIX ${CURL_ROOT}
        #--Download step--------------
        URL ${PROJECT_SOURCE_DIR}/cmake/dep/curl-7.78.0.tar.gz
        #URL_HASH SHA1=7fdb90a2d45085feb8b76167cae419ad4c211d6b
        #--Configure step-------------
        CONFIGURE_COMMAND cd ${CURL_ROOT}/src/curl-7.78.0  &&  cmake -D CMAKE_INSTALL_PREFIX=${CURL_ROOT} .
        #--Build step-----------------
        BUILD_COMMAND cd ${CURL_ROOT}/src/curl-7.78.0 && make
        #--Install step---------------
        UPDATE_COMMAND "" # Skip annoying updates for every build
        INSTALL_COMMAND cd ${CURL_ROOT}/src/curl-7.78.0 && make install
        )

SET(CURL_INCLUDE_DIR ${CURL_ROOT}/include)
SET(CURL_LIB_DIR  ${CURL_ROOT}/lib)
