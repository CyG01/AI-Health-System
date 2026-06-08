@echo off
chcp 65001 >nul 2>&1
title AI 健康管理系统 — 一键启动
echo.
echo ============================================
echo   AI Health System - Starting services...
echo ============================================
echo.
set "PROJECT_DIR=%~dp0"
rem 启动后端
start "Backend - Spring Boot" cmd /k "cd /d "%PROJECT_DIR%backend" && title Backend - Spring Boot && echo [INFO] Starting backend... && mvn spring-boot:run"
rem 启动前端
start "Frontend - Vite" cmd /k "cd /d "%PROJECT_DIR%frontend" && title Frontend - Vite && echo [INFO] Starting frontend... && npm run dev"
echo [OK] Backend and Frontend launched in separate windows.
echo.
pause
