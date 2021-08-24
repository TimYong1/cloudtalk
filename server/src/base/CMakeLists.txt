PROJECT(Base)

AUX_SOURCE_DIRECTORY(${CMAKE_CURRENT_SOURCE_DIR} SRC_LIST1)
AUX_SOURCE_DIRECTORY(${CMAKE_CURRENT_SOURCE_DIR}/pb/protocol SRC_LIST1)

foreach(iter ${CMAKE_CURRENT_SOURCE_DIR}/jsoncpp)
    AUX_SOURCE_DIRECTORY(${iter} SRC_LIST1)
endforeach()



SET(LIBRARY_OUTPUT_PATH ${CMAKE_CURRENT_SOURCE_DIR})
ADD_LIBRARY(base STATIC ${SRC_LIST1})

target_include_directories(
        base PRIVATE
        ${CURL_INCLUDE_DIR}
        ${PROTOBUF_INCLUDE_DIR}

)
target_link_directories(
        base PRIVATE
        ${CURL_LIB_DIR}
        ${PROTOBUF_LIB_DIR}
)
TARGET_LINK_LIBRARIES(base pthread curl crypto)
