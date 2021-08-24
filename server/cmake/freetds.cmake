include(ExternalProject)

set(FREETDS_ROOT            ${CMAKE_BINARY_DIR}/dep/freetds)
set(FREETDS_LIB_DIR         ${FREETDS_ROOT}/lib)
set(FREETDS_INCLUDE_DIR     ${FREETDS_ROOT}/include)

set(FREETDS_URL             ${CMAKE_SOURCE_DIR}/dep/freetds-1.1.12.tar.gz)
set(FREETDS_CONFIGURE       cd ${FREETDS_ROOT}/src/freetds-1.1.12 && ./configure --prefix=${FREETDS_ROOT})
set(FREETDS_MAKE            cd ${FREETDS_ROOT}/src/freetds-1.1.12 && make)
set(FREETDS_INSTALL         cd ${FREETDS_ROOT}/src/freetds-1.1.12 && make install)


ExternalProject_Add(freetds-1.1.12
        URL                  ${FREETDS_URL}
        DOWNLOAD_NAME        freetds-1.1.12.tar.gz
        PREFIX               ${FREETDS_ROOT}
        CONFIGURE_COMMAND    ${FREETDS_CONFIGURE}
        BUILD_COMMAND        ${FREETDS_MAKE}
        INSTALL_COMMAND      ${FREETDS_INSTALL}
)

list(APPEND FREETDS_LIBRARY ${FREETDS_LIB_DIR}/libsybdb.a )
list(APPEND FREETDS_LIBRARY ${FREETDS_LIB_DIR}/libct.a )






