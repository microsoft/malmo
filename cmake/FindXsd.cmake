# Locate Xsd from code synthesis include paths and binary
# Xsd can be found at http://codesynthesis.com/products/xsd/
# Written by Frederic Heem, frederic.heem _at_ telsey.it

# This module defines
# XSD_INCLUDE_DIR, where to find elements.hxx, etc.
# XSD_EXECUTABLE, where is the xsd compiler
# XSD_LIBRARIES, the libraries to include in the link
# XSD_FOUND, If false, don't try to use xsd

set(_XSD_SEARCHES)

# Search XSD_ROOT first if it is set.
if(XSD_ROOT)
  set(_XSD_SEARCH_ROOT PATHS ${XSD_ROOT} NO_DEFAULT_PATH)
  list(APPEND _XSD_SEARCHES _XSD_SEARCH_ROOT)
endif()

# Normal search.
set(_XSD_SEARCH_NORMAL
  PATHS "$ENV{PROGRAMFILES}/CodeSynthesis XSD 4.0"
  )
list(APPEND _XSD_SEARCHES _XSD_SEARCH_NORMAL)

set(XSD_NAMES xsdcxx xsd xerces-c xerces-c_3)
set(XSD_NAMES_DEBUG xerces-cD xerces-c_3D)

# Malmo-specific thing: on some platforms we need to manually install CodeSynthesis XSD and so the standard
# exectable name is 'xsd' instead of 'xsdcxx'. On others, 'xsd' matches an executable in Mono
# and so we have to avoid that.
# First get the system name - this code is the same as in the Malmo root CMakeLists.txt
if( APPLE )
  set( SYSTEM_NAME "Mac" )
elseif( UNIX )
  set( SYSTEM_NAME "Linux" )
  execute_process(COMMAND lsb_release -is OUTPUT_VARIABLE LSB_ID)
  execute_process(COMMAND lsb_release -rs OUTPUT_VARIABLE LSB_RELEASE)
  if( LSB_ID AND LSB_RELEASE )
    string(STRIP "${LSB_ID}" LSB_ID)
    string(STRIP "${LSB_RELEASE}" LSB_RELEASE)
    set( SYSTEM_NAME "${SYSTEM_NAME}-${LSB_ID}-${LSB_RELEASE}")
  elseif( EXISTS "/etc/debian_version")
    file( READ /etc/debian_version DEBIAN_VERSION )
    set( SYSTEM_NAME "${SYSTEM_NAME}-${DEBIAN_VERSION}")
  elseif( EXISTS "/etc/redhat-release")
    set( SYSTEM_NAME "${SYSTEM_NAME}-Redhat")
  endif()
elseif( WIN32)
  set( SYSTEM_NAME "Windows" )
endif()
if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
  set( SYSTEM_NAME "${SYSTEM_NAME}-64bit" )
else()
  set( SYSTEM_NAME "${SYSTEM_NAME}-32bit" )
endif()

if( ( ${SYSTEM_NAME} MATCHES "^Linux-Ubuntu-14.04.*$" ) OR ( ${SYSTEM_NAME} MATCHES "^Linux-Debian-7\\..*$" ) OR ( ${SYSTEM_NAME} STREQUAL "Mac-64bit" ) )
  set( EXTRA_XSD_EXECUTABLE_NAMES "xsd" )
endif()

# Try each search configuration.
foreach(search ${_XSD_SEARCHES})
  find_path(XSD_INCLUDE_DIR NAMES xercesc ${${search}} PATH_SUFFIXES include)
  FIND_PROGRAM(XSD_EXECUTABLE NAMES ${EXTRA_XSD_EXECUTABLE_NAMES} xsdcxx xsd.exe ${${search}} PATH_SUFFIXES bin)
endforeach()

# Allow XSD_LIBRARY to be set manually, as the location of the xsd library
if(NOT XSD_LIBRARY)
  if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
    set( _XSD_LIB_DIR lib64 )
  else()
    set( _XSD_LIB_DIR lib )
  endif()
  
  foreach(search ${_XSD_SEARCHES})
    find_library(XSD_LIBRARY_RELEASE NAMES ${XSD_NAMES} ${${search}} PATH_SUFFIXES ${_XSD_LIB_DIR}/vc-12.0)
    find_library(XSD_LIBRARY_DEBUG NAMES ${XSD_NAMES_DEBUG} ${${search}} PATH_SUFFIXES ${_XSD_LIB_DIR}/vc-12.0)
  endforeach()

  include(SelectLibraryConfigurations)
  select_library_configurations(XSD)
endif()

unset(XSD_NAMES)
unset(XSD_NAMES_DEBUG)

MARK_AS_ADVANCED(
  XSD_LIBRARY
  XSD_INCLUDE_DIR
  XSD_EXECUTABLE
) 

# if the include and the program are found then we have it
IF(XSD_INCLUDE_DIR)
  IF(XSD_EXECUTABLE)
    SET( XSD_FOUND "YES" )
  ENDIF(XSD_EXECUTABLE)

    set(XSD_INCLUDE_DIRS ${XSD_INCLUDE_DIR})

    if(NOT XSD_LIBRARIES)
      set(XSD_LIBRARIES ${XSD_LIBRARY})
    endif()

    if(NOT TARGET XSD::XSD)
      add_library(XSD::XSD UNKNOWN IMPORTED)
      set_target_properties(XSD::XSD PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES "${XSD_INCLUDE_DIRS}")

		if(XSD_LIBRARY_RELEASE)
		set_property(TARGET XSD::XSD APPEND PROPERTY
			IMPORTED_CONFIGURATIONS RELEASE)
		set_target_properties(XSD::XSD PROPERTIES
			IMPORTED_LOCATION_RELEASE "${XSD_LIBRARY_RELEASE}")
		endif()

		if(XSD_LIBRARY_DEBUG)
		set_property(TARGET XSD::XSD APPEND PROPERTY
			IMPORTED_CONFIGURATIONS DEBUG)
		set_target_properties(XSD::XSD PROPERTIES
			IMPORTED_LOCATION_DEBUG "${XSD_LIBRARY_DEBUG}")
		endif()
		
		if(NOT XSD_LIBRARY_RELEASE AND NOT XSD_LIBRARY_DEBUG)
        set_property(TARGET XSD::XSD APPEND PROPERTY
          IMPORTED_LOCATION "${XSD_LIBRARY}")
      endif()
	ENDIF()
ENDIF(XSD_INCLUDE_DIR)

