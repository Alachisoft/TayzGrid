:: This file is reponsible for compiling the protobuf for Java Client. 
:: Usage details can be found in provided readme
echo off
@SET OUTPUT_PATH=y:\Protobuf_build\JavaProto\build\
@SET COMPILE_PATH=y:\Protobuf_build\protoc-2.3.0-win32\
@SET PROTO_FILE_PATH=y:\Protobuf_build\JavaProto\

cd %PROTO_FILE_PATH%


FOR /f %%A IN ('dir %PROTO_FILE_PATH% /b *.proto')  DO FOR %%B IN (%%A) DO %COMPILE_PATH%protoc --proto_path=%PROTO_FILE_PATH% --java_out=%OUTPUT_PATH% %PROTO_FILE_PATH%%%B


