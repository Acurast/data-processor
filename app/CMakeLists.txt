#
# CMakeLists.txt: abandon all hope, etc etc etc
#
# SPDX-License-Identifier: BSD-3-Clause
#
# This file is part of Sweet B, a safe, compact, embeddable library for
# elliptic curve cryptography.
#
# https://github.com/westerndigitalcorporation/sweet-b
#
# Copyright (c) 2020-2021 Western Digital Corporation or its affiliates.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice,
# this list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice,
# this list of conditions and the following disclaimer in the documentation
# and/or other materials provided with the distribution.
#
# 3. Neither the name of the copyright holder nor the names of its contributors
# may be used to endorse or promote products derived from this software without
# specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#

cmake_minimum_required(VERSION 3.5)

project(sweet_b)

set(CMAKE_C_STANDARD 11)

include_directories(src/main/cpp/include)

# START ARM Adaptions
set(CMAKE_C_FLAGS "-Os -mthumb" CACHE STRING "" FORCE)
set(CMAKE_EXE_LINKER_FLAGS "-static" CACHE STRING "" FORCE)
set(SB_TEST_WORD_SIZE "4" CACHE STRING "")
set(SB_TEST_VERIFY_QR "0" CACHE STRING "")
set(SB_LIBRARY_DEFINES "SB_UNROLL=3" CACHE STRING "")
set(SB_FE_ASM "1" CACHE STRING "")
set(SB_ASM_SOURCES "src/main/cpp/sb_fe_armv7.s" CACHE STRING "")
# END ARM Adaptions

set(SB_PUBLIC_HEADERS
        src/main/cpp/include/sb_sw_lib.h
        src/main/cpp/include/sb_hmac_drbg.h
        src/main/cpp/include/sb_hmac_sha256.h
        src/main/cpp/include/sb_hkdf.h
        src/main/cpp/include/sb_sha256.h
        src/main/cpp/include/sb_sw_context.h
        src/main/cpp/include/sb_types.h
        src/main/cpp/include/sb_all.h)

set(SB_PRIVATE_HEADERS
        src/main/cpp/sb_error.h
        src/main/cpp/sb_fe.h
        src/main/cpp/sb_fe_tests.c.h
        src/main/cpp/sb_sw_curves.h
        src/main/cpp/sb_sw_lib_tests.c.h
        src/main/cpp/sb_test.h
        src/main/cpp/sb_test_cavp.h
        src/main/cpp/sb_test_list.h)

set(SB_SOURCES
        ${SB_PUBLIC_HEADERS}
        ${SB_PRIVATE_HEADERS}
        ${SB_ASM_SOURCES}

        src/main/cpp/sweet_b_signer.c
        src/main/cpp/sb_sha256.c
        src/main/cpp/sb_hmac_sha256.c
        src/main/cpp/sb_hmac_drbg.c
        src/main/cpp/sb_hkdf.c

        src/main/cpp/sb_fe.c
        src/main/cpp/sb_sw_lib.c)

add_library(sweet_b SHARED ${SB_SOURCES})

target_link_libraries(
        sweet_b
        android
        log
)