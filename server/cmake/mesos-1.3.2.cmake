include(ExternalProject)

set(MESOS_ROOT          ${CMAKE_SOURCE_DIR}/thirdparty/mesos-1.3.2)
set(MESOS_LIB_DIR       ${MESOS_ROOT}/lib)
set(MESOS_INCLUDE_DIR   ${MESOS_ROOT}/include)

set(MESOS_URL           https://github.com/apache/mesos/archive/1.3.2.zip)
set(MESOS_CONFIGURE     cd ${MESOS_ROOT}/src/mesos-1.3.2 && ./autogen.sh && ./configure --prefix=${MESOS_ROOT})
set(MESOS_MAKE          cd ${MESOS_ROOT}/src/mesos-1.3.2 && make)
set(MESOS_INSTALL       cd ${MESOS_ROOT}/src/mesos-1.3.2 && make install)
ExternalProject_Add(mesos-1.3.2
        URL                  ${MESOS_URL}
        DOWNLOAD_NAME        mesos-1.3.2.zip
        PREFIX               ${MESOS_ROOT}
        CONFIGURE_COMMAND    ${MESOS_CONFIGURE}
        BUILD_COMMAND        ${MESOS_MAKE}
        INSTALL_COMMAND      ${MESOS_INSTALL}
)