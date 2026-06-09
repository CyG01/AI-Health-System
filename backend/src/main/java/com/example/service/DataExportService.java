package com.example.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户全量数据导出服务
 */
public interface DataExportService {

    /** 导出用户全部健康数据为CSV文件 */
    void exportAllDataCSV(Long userId, HttpServletResponse response) throws IOException;

    /** 导出用户全部健康数据为Excel文件 */
    void exportAllDataExcel(Long userId, HttpServletResponse response) throws IOException;
}