@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title AI 健康管理系统 - 一键启动
color 0F

set "PROJECT_DIR=%~dp0"
set "BACKEND_DIR=%PROJECT_DIR%backend"
set "FRONTEND_DIR=%PROJECT_DIR%frontend"

set "MYSQL_PORT=3306"
set "REDIS_PORT=6379"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"

echo.
echo  ============================================================
echo     AI Health System - 一键启动脚本
echo  ============================================================
echo.

rem ============================
rem  1. 环境检查
rem ============================
echo  [1/5] 检查开发环境...
echo.

set "ENV_OK=1"

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo    [X] Java 未安装，请安装 JDK 17+ 并配置 JAVA_HOME
    set "ENV_OK=0"
) else (
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        echo    [OK] Java %%~v
    )
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo    [X] Maven 未安装，请安装 Maven 3.8+ 并添加到 PATH
    set "ENV_OK=0"
) else (
    for /f "tokens=3" %%v in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        echo    [OK] Maven %%v
    )
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo    [X] Node.js 未安装，请安装 Node 18+ 并添加到 PATH
    set "ENV_OK=0"
) else (
    for /f "tokens=1" %%v in ('node -v 2^>^&1') do (
        echo    [OK] Node.js %%v
    )
)

if "%ENV_OK%"=="0" (
    echo.
    echo  [ERROR] 环境检查未通过，请先安装缺失的依赖。
    goto :end
)

echo.

rem ============================
rem  2. 基础服务检查 (MySQL / Redis)
rem ============================
echo  [2/5] 检查基础服务...
echo.

set "INFRA_OK=1"

rem -- 检查 MySQL --
netstat -an | findstr ":%MYSQL_PORT% " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo    [OK] MySQL 已运行 (端口 %MYSQL_PORT%^)
) else (
    echo    [!!] MySQL 未在端口 %MYSQL_PORT% 运行
    set "INFRA_OK=0"
)

rem -- 检查 Redis --
netstat -an | findstr ":%REDIS_PORT% " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo    [OK] Redis 已运行 (端口 %REDIS_PORT%^)
) else (
    echo    [!!] Redis 未在端口 %REDIS_PORT% 运行
    set "INFRA_OK=0"
)

if "%INFRA_OK%"=="0" (
    echo.
    echo    尝试通过 Docker 启动 MySQL 和 Redis...
    echo.
    where docker >nul 2>&1
    if !errorlevel! neq 0 (
        echo    [X] Docker 未安装，无法自动启动基础服务。
        echo        请手动启动 MySQL 和 Redis，或安装 Docker Desktop。
        goto :end
    )

    rem 检查 Docker Desktop 是否运行
    docker info >nul 2>&1
    if !errorlevel! neq 0 (
        echo    [X] Docker Desktop 未运行，请先启动 Docker Desktop。
        goto :end
    )

    echo    [..] 正在执行 docker-compose up -d mysql redis ...
    pushd "%PROJECT_DIR%"
    docker-compose up -d mysql redis
    if !errorlevel! neq 0 (
        echo    [X] Docker Compose 启动失败，请检查 docker-compose.yml
        popd
        goto :end
    )
    popd

    echo    [..] 等待 MySQL 就绪...
    set "WAIT_COUNT=0"
    :wait_mysql
    timeout /t 2 /nobreak >nul
    netstat -an | findstr ":%MYSQL_PORT% " | findstr "LISTENING" >nul 2>&1
    if !errorlevel! equ 0 goto :mysql_ready
    set /a WAIT_COUNT+=1
    if !WAIT_COUNT! lss 15 goto :wait_mysql
    echo    [X] MySQL 启动超时（30秒），请检查 Docker 容器日志
    goto :end
    :mysql_ready
    echo    [OK] MySQL 已就绪

    set "WAIT_COUNT=0"
    :wait_redis
    timeout /t 1 /nobreak >nul
    netstat -an | findstr ":%REDIS_PORT% " | findstr "LISTENING" >nul 2>&1
    if !errorlevel! equ 0 goto :redis_ready
    set /a WAIT_COUNT+=1
    if !WAIT_COUNT! lss 20 goto :wait_redis
    echo    [X] Redis 启动超时（20秒），请检查 Docker 容器日志
    goto :end
    :redis_ready
    echo    [OK] Redis 已就绪
)

echo.

rem ============================
rem  3. 前端依赖检查
rem ============================
echo  [3/5] 检查前端依赖...
echo.

if not exist "%FRONTEND_DIR%\node_modules\" (
    echo    [..] node_modules 不存在，正在安装前端依赖...
    pushd "%FRONTEND_DIR%"
    call npm install
    if !errorlevel! neq 0 (
        echo    [X] npm install 失败
        popd
        goto :end
    )
    popd
    echo    [OK] 前端依赖安装完成
) else (
    echo    [OK] node_modules 已存在
)

echo.

rem ============================
rem  4. 清理旧进程（如果端口被占用）
rem ============================
echo  [4/5] 检查端口占用...
echo.

rem -- 检查后端端口 --
netstat -an | findstr ":%BACKEND_PORT% " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo    [!!] 端口 %BACKEND_PORT% 已被占用（后端可能正在运行^）
    set /p KILL_BACKEND="    是否终止旧进程并重启后端？(Y/n): "
    if /i "!KILL_BACKEND!"=="" set "KILL_BACKEND=Y"
    if /i "!KILL_BACKEND!"=="Y" (
        for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%BACKEND_PORT% " ^| findstr "LISTENING"') do (
            taskkill /PID %%p /F >nul 2>&1
            echo    [OK] 已终止旧后端进程 (PID: %%p^)
        )
        timeout /t 1 /nobreak >nul
    ) else (
        echo    [--] 跳过重启后端
        set "SKIP_BACKEND=1"
    )
) else (
    set "SKIP_BACKEND=0"
)

rem -- 检查前端端口 --
netstat -an | findstr ":%FRONTEND_PORT% " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo    [!!] 端口 %FRONTEND_PORT% 已被占用（前端可能正在运行^）
    set /p KILL_FRONTEND="    是否终止旧进程并重启前端？(Y/n): "
    if /i "!KILL_FRONTEND!"=="" set "KILL_FRONTEND=Y"
    if /i "!KILL_FRONTEND!"=="Y" (
        for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%FRONTEND_PORT% " ^| findstr "LISTENING"') do (
            taskkill /PID %%p /F >nul 2>&1
            echo    [OK] 已终止旧前端进程 (PID: %%p^)
        )
        timeout /t 1 /nobreak >nul
    ) else (
        echo    [--] 跳过重启前端
        set "SKIP_FRONTEND=1"
    )
) else (
    set "SKIP_FRONTEND=0"
)

echo.

rem ============================
rem  5. 启动服务
rem ============================
echo  [5/5] 启动应用服务...
echo.

if not defined SKIP_BACKEND set "SKIP_BACKEND=0"
if not defined SKIP_FRONTEND set "SKIP_FRONTEND=0"

rem -- 启动后端 --
if "%SKIP_BACKEND%"=="0" (
    start "Backend - Spring Boot (8080)" cmd /k ^
        "title Backend - Spring Boot (8080) ^&^& ^
         color 0A ^&^& ^
         cd /d "%BACKEND_DIR%" ^&^& ^
         set DB_PASSWORD=GCYgcygcygcy123/ ^&^& ^
         set SPRING_PROFILES_ACTIVE=local ^&^& ^
         echo [INFO] Starting Spring Boot backend on port %BACKEND_PORT% (profile: local^)... ^&^& ^
         echo. ^&^& ^
         mvn spring-boot:run"
    echo    [OK] 后端已在新窗口启动
) else (
    echo    [--] 后端保持现有进程
)

rem -- 启动前端 --
if "%SKIP_FRONTEND%"=="0" (
    start "Frontend - Vite (5173)" cmd /k ^
        "title Frontend - Vite (5173) ^&^& ^
         color 0B ^&^& ^
         cd /d "%FRONTEND_DIR%" ^&^& ^
         echo [INFO] Starting Vite dev server on port %FRONTEND_PORT%... ^&^& ^
         echo. ^&^& ^
         npm run dev"
    echo    [OK] 前端已在新窗口启动
) else (
    echo    [--] 前端保持现有进程
)

echo.

rem ============================
rem  等待服务就绪并显示信息
rem ============================
if "%SKIP_BACKEND%"=="0" (
    echo    [..] 等待后端启动（约 30-60 秒^）...
    set "WAIT_COUNT=0"
    :wait_backend
    timeout /t 3 /nobreak >nul
    powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%BACKEND_PORT%/actuator/health' -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop; exit 0 } catch { exit 1 }" >nul 2>&1
    if !errorlevel! equ 0 goto :backend_ready
    set /a WAIT_COUNT+=1
    if !WAIT_COUNT! lss 30 goto :wait_backend
    echo    [!!] 后端启动较慢，请稍后手动检查 http://localhost:%BACKEND_PORT%
    goto :show_urls
    :backend_ready
    echo    [OK] 后端已就绪
)

if "%SKIP_FRONTEND%"=="0" (
    echo    [..] 等待前端启动...
    set "WAIT_COUNT=0"
    :wait_frontend
    timeout /t 2 /nobreak >nul
    netstat -an | findstr ":%FRONTEND_PORT% " | findstr "LISTENING" >nul 2>&1
    if !errorlevel! equ 0 goto :frontend_ready
    set /a WAIT_COUNT+=1
    if !WAIT_COUNT! lss 15 goto :wait_frontend
    echo    [!!] 前端启动较慢，请稍后手动检查 http://localhost:%FRONTEND_PORT%
    goto :show_urls
    :frontend_ready
    echo    [OK] 前端已就绪
)

:show_urls
echo.
echo  ============================================================
echo     启动完成！访问地址：
echo  ============================================================
echo.
echo    前端页面:   http://localhost:%FRONTEND_PORT%
echo    后端 API:   http://localhost:%BACKEND_PORT%
echo    API 文档:   http://localhost:%BACKEND_PORT%/swagger-ui.html
echo    健康检查:   http://localhost:%BACKEND_PORT%/actuator/health
echo.
echo  ============================================================
echo     按任意键关闭此窗口（服务在各自的窗口中继续运行^）
echo  ============================================================

:end
echo.
pause >nul
endlocal
