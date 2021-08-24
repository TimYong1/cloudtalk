include(ExternalProject)

set(LOG4CPLUS_ROOT            ${CMAKE_BINARY_DIR}/dep/log4cplus)
set(LOG4CPLUS_LIB_DIR         ${LOG4CPLUS_ROOT}/lib)
set(LOG4CPLUS_INCLUDE_DIR     ${LOG4CPLUS_ROOT}/include)

set(LOG4CPLUS_URL             ${CMAKE_SOURCE_DIR}/dep/log4cplus-2.0.5.tar.gz)
set(LOG4CPLUS_CONFIGURE       cd ${LOG4CPLUS_ROOT}/src/log4cplus-2.0.5 && ./configure --prefix=${LOG4CPLUS_ROOT})
set(LOG4CPLUS_MAKE            cd ${LOG4CPLUS_ROOT}/src/log4cplus-2.0.5 && make)
set(LOG4CPLUS_INSTALL         cd ${LOG4CPLUS_ROOT}/src/log4cplus-2.0.5 && make install)


ExternalProject_Add(log4cplus-2.0.5
        URL                  ${LOG4CPLUS_URL}
        DOWNLOAD_NAME        log4cplus-2.0.5.tar.gz
        PREFIX               ${LOG4CPLUS_ROOT}
        CONFIGURE_COMMAND    ${LOG4CPLUS_CONFIGURE}
        BUILD_COMMAND        ${LOG4CPLUS_MAKE}
        INSTALL_COMMAND      ${LOG4CPLUS_INSTALL}
        )

list(APPEND LOG4CPLUS_LIBRARY ${LOG4CPLUS_LIB_DIR}/liblog4cplus.so )







