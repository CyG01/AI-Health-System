package com.example.controller;

import com.example.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "数据导出")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService dataExportService;

    @Operation(summary = "导出CSV（饮食、运动、体重、打卡）")
    @GetMapping("/csv")
    public void exportCSV(@RequestAttribute("userId") Long userId, HttpServletResponse response) throws IOException {
        dataExportService.exportAllDataCSV(userId, response);
    }

    @Operation(summary = "导出Excel（饮食、运动、体重、打卡）")
    @GetMapping("/excel")
    public void exportExcel(@RequestAttribute("userId") Long userId, HttpServletResponse response) throws IOException {
        dataExportService.exportAllDataExcel(userId, response);
    }
}