@echo off
REM ============================================================
REM AI Health System - 数据库自动备份脚本
REM 使用方法：
REM   1. 修改下方 MYSQL_PATH、DB_NAME、DB_USER、DB_PASS
REM   2. 在Windows任务计划程序中配置每日凌晨3:00执行
REM   3. 备份保留策略：本地保留30天
REM ============================================================

set BACKUP_DIR=E:\backup\ai_health_system
set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin
set DB_NAME=ai_health_system
set DB_USER=root
set DB_PASS=你的数据库密码
set DATE=%date:~0,4%%date:~5,2%%date:~8,2%
set TIME=%time:~0,2%%time:~3,2%%time:~6,2%
set TIME=%TIME: =0%

REM 创建日期目录
if not exist "%BACKUP_DIR%\%DATE%" mkdir "%BACKUP_DIR%\%DATE%"

REM 执行全量备份
echo [%date% %time%] 开始备份数据库 %DB_NAME% ...
"%MYSQL_PATH%\mysqldump.exe" -u%DB_USER% -p%DB_PASS% --single-transaction --routines --triggers --default-character-set=utf8mb4 %DB_NAME% > "%BACKUP_DIR%\%DATE%\%DB_NAME%_%DATE%_%TIME%.sql"

if %errorlevel% equ 0 (
    echo [%date% %time%] 备份成功: %BACKUP_DIR%\%DATE%\%DB_NAME%_%DATE%_%TIME%.sql
) else (
    echo [%date% %time%] 备份失败！错误代码: %errorlevel%
)

REM 清理30天前的备份
forfiles /p "%BACKUP_DIR%" /s /m *.sql /d -30 /c "cmd /c del @path" 2>nul
echo [%date% %time%] 已清理30天前的旧备份

REM 统计备份文件大小
dir "%BACKUP_DIR%\%DATE%" /s 2>nul | find "个文件" 
echo [%date% %time%] 备份任务完成