include(ExternalProject)

set(HWLOC_ROOT         ${CMAKE_BINARY_DIR}/thirdparty/hwloc-1.11)
set(HWLOC_LIB_DIR      ${HWLOC_ROOT}/lib)
set(HWLOC_INCLUDE_DIR  ${HWLOC_ROOT}/include)

set(HWLOC_URL          https://github.com/open-mpi/hwloc/archive/v1.11.zip)
set(HWLOC_CONFIGURE    cd ${HWLOC_ROOT}/src/hwloc-1.11 && ./autogen.sh && ./configure --prefix=${HWLOC_ROOT})
set(HWLOC_MAKE         cd ${HWLOC_ROOT}/src/hwloc-1.11 && make)
set(HWLOC_INSTALL      cd ${HWLOC_ROOT}/src/hwloc-1.11 && make install)


ExternalProject_Add(hwloc-1.11
        URL                  ${HWLOC_URL}
        DOWNLOAD_NAME        hwloc-1.11.zip
        PREFIX               ${HWLOC_ROOT}
        CONFIGURE_COMMAND    ${HWLOC_CONFIGURE}
        BUILD_COMMAND        ${HWLOC_MAKE}
        INSTALL_COMMAND      ${HWLOC_INSTALL}
)
