package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.DailyCheckin;
import com.example.entity.DietRecord;
import com.example.entity.ExerciseRecord;
import com.example.entity.HealthRecord;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.DietRecordMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.service.DataExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {

    private final HealthRecordMapper healthRecordMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final DietRecordMapper dietRecordMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void exportAllDataCSV(Long userId, HttpServletResponse response) throws IOException {
        String filename = "health-data-" + java.time.LocalDate.now() + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        response.setCharacterEncoding("UTF-8");
        // BOM for Excel compatibility
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            // Sheet 1: 健康档案
            pw.println("===== 健康档案 =====");
            pw.println("日期,身高(cm),体重(kg),BMI,基础代谢(kcal),日常热量(kcal),目标,疾病史,过敏史,运动习惯,饮食习惯");
            List<HealthRecord> healthRecords = getHealthRecords(userId);
            for (HealthRecord r : healthRecords) {
                pw.printf("%s,%d,%d,%s,%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        r.getCreateTime() != null ? DATE_FMT.format(r.getCreateTime()) : "",
                        r.getHeight() != null ? r.getHeight() : 0,
                        r.getWeight() != null ? r.getWeight() : 0,
                        r.getBmi() != null ? r.getBmi().toString() : "",
                        r.getBmr() != null ? r.getBmr() : "",
                        r.getDailyCalorie() != null ? r.getDailyCalorie() : "",
                        nvl(r.getGoal()), nvl(r.getDiseaseHistory()), nvl(r.getAllergyHistory()),
                        nvl(r.getExerciseHabit()), nvl(r.getDietHabit()));
            }

            // Sheet 2: 打卡记录
            pw.println("\n===== 打卡记录 =====");
            pw.println("日期,运动状态,饮食状态,体重(kg),心情,备注");
            List<DailyCheckin> checkins = getCheckins(userId);
            for (DailyCheckin c : checkins) {
                String exStatus = c.getExerciseStatus() == 2 ? "全部完成" : c.getExerciseStatus() == 1 ? "部分完成" : "未完成";
                String dietStatus = c.getDietStatus() == 2 ? "全部完成" : c.getDietStatus() == 1 ? "部分完成" : "未完成";
                pw.printf("%s,%s,%s,%s,\"%s\",\"%s\"\n",
                        c.getCheckDate() != null ? c.getCheckDate().format(DATE_ONLY) : "",
                        exStatus, dietStatus,
                        c.getCurrentWeight() != null ? c.getCurrentWeight() : "",
                        nvl(c.getMood()), nvl(c.getNote()));
            }

            // Sheet 3: 运动记录
            pw.println("\n===== 运动记录 =====");
            pw.println("时间,运动类型,时长(分钟),消耗热量(kcal),备注");
            List<ExerciseRecord> exercises = getExercises(userId);
            for (ExerciseRecord e : exercises) {
                pw.printf("%s,%s,%d,%d,\"%s\"\n",
                        e.getCreateTime() != null ? DATE_FMT.format(e.getCreateTime()) : "",
                        nvl(e.getExerciseType()),
                        e.getDurationMinutes() != null ? e.getDurationMinutes() : 0,
                        e.getCaloriesBurned() != null ? e.getCaloriesBurned() : 0,
                        nvl(e.getNote()));
            }

            // Sheet 4: 饮食记录
            pw.println("\n===== 饮食记录 =====");
            pw.println("时间,食物名称,分类,摄入热量(kcal),蛋白质(g),脂肪(g),碳水(g),备注");
            List<DietRecord> diets = getDiets(userId);
            for (DietRecord d : diets) {
                pw.printf("%s,\"%s\",%s,%d,%s,%s,%s,\"%s\"\n",
                        d.getCreateTime() != null ? DATE_FMT.format(d.getCreateTime()) : "",
                        nvl(d.getFoodName()), nvl(d.getCategory()),
                        d.getCaloriesConsumed() != null ? d.getCaloriesConsumed() : 0,
                        d.getProtein() != null ? d.getProtein().toString() : "",
                        d.getFat() != null ? d.getFat().toString() : "",
                        d.getCarbs() != null ? d.getCarbs().toString() : "",
                        nvl(d.getNote()));
            }
        }
    }

    @Override
    public void exportAllDataExcel(Long userId, HttpServletResponse response) throws IOException {
        String filename = "health-data-" + java.time.LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try (Workbook wb = new XSSFWorkbook()) {
            // Sheet 1: 健康档案
            Sheet sheet1 = wb.createSheet("健康档案");
            Row header1 = sheet1.createRow(0);
            String[] cols1 = {"日期","身高(cm)","体重(kg)","BMI","基础代谢","日常热量","目标","疾病史","过敏史","运动习惯","饮食习惯"};
            for (int i = 0; i < cols1.length; i++) header1.createCell(i).setCellValue(cols1[i]);
            List<HealthRecord> healthRecords = getHealthRecords(userId);
            for (int i = 0; i < healthRecords.size(); i++) {
                Row row = sheet1.createRow(i + 1);
                HealthRecord r = healthRecords.get(i);
                row.createCell(0).setCellValue(r.getCreateTime() != null ? DATE_FMT.format(r.getCreateTime()) : "");
                row.createCell(1).setCellValue(r.getHeight() != null ? r.getHeight() : 0);
                row.createCell(2).setCellValue(r.getWeight() != null ? r.getWeight() : 0);
                row.createCell(3).setCellValue(r.getBmi() != null ? r.getBmi().doubleValue() : 0);
                row.createCell(4).setCellValue(r.getBmr() != null ? r.getBmr() : 0);
                row.createCell(5).setCellValue(r.getDailyCalorie() != null ? r.getDailyCalorie() : 0);
                row.createCell(6).setCellValue(nvl(r.getGoal()));
                row.createCell(7).setCellValue(nvl(r.getDiseaseHistory()));
                row.createCell(8).setCellValue(nvl(r.getAllergyHistory()));
                row.createCell(9).setCellValue(nvl(r.getExerciseHabit()));
                row.createCell(10).setCellValue(nvl(r.getDietHabit()));
            }

            // Sheet 2: 打卡记录
            Sheet sheet2 = wb.createSheet("打卡记录");
            Row header2 = sheet2.createRow(0);
            String[] cols2 = {"日期","运动状态","饮食状态","体重","心情","备注"};
            for (int i = 0; i < cols2.length; i++) header2.createCell(i).setCellValue(cols2[i]);
            List<DailyCheckin> checkins = getCheckins(userId);
            for (int i = 0; i < checkins.size(); i++) {
                Row row = sheet2.createRow(i + 1);
                DailyCheckin c = checkins.get(i);
                row.createCell(0).setCellValue(c.getCheckDate() != null ? c.getCheckDate().format(DATE_ONLY) : "");
                String ex = c.getExerciseStatus() == 2 ? "全部完成" : c.getExerciseStatus() == 1 ? "部分完成" : "未完成";
                String diet = c.getDietStatus() == 2 ? "全部完成" : c.getDietStatus() == 1 ? "部分完成" : "未完成";
                row.createCell(1).setCellValue(ex);
                row.createCell(2).setCellValue(diet);
                row.createCell(3).setCellValue(c.getCurrentWeight() != null ? String.valueOf(c.getCurrentWeight()) : "");
                row.createCell(4).setCellValue(nvl(c.getMood()));
                row.createCell(5).setCellValue(nvl(c.getNote()));
            }

            // Sheet 3: 运动记录
            Sheet sheet3 = wb.createSheet("运动记录");
            Row header3 = sheet3.createRow(0);
            String[] cols3 = {"时间","运动类型","时长(分钟)","消耗热量(kcal)","备注"};
            for (int i = 0; i < cols3.length; i++) header3.createCell(i).setCellValue(cols3[i]);
            List<ExerciseRecord> exercises = getExercises(userId);
            for (int i = 0; i < exercises.size(); i++) {
                Row row = sheet3.createRow(i + 1);
                ExerciseRecord e = exercises.get(i);
                row.createCell(0).setCellValue(e.getCreateTime() != null ? DATE_FMT.format(e.getCreateTime()) : "");
                row.createCell(1).setCellValue(nvl(e.getExerciseType()));
                row.createCell(2).setCellValue(e.getDurationMinutes() != null ? e.getDurationMinutes() : 0);
                row.createCell(3).setCellValue(e.getCaloriesBurned() != null ? e.getCaloriesBurned() : 0);
                row.createCell(4).setCellValue(nvl(e.getNote()));
            }

            // Sheet 4: 饮食记录
            Sheet sheet4 = wb.createSheet("饮食记录");
            Row header4 = sheet4.createRow(0);
            String[] cols4 = {"时间","食物名称","分类","摄入热量(kcal)","蛋白质(g)","脂肪(g)","碳水(g)","备注"};
            for (int i = 0; i < cols4.length; i++) header4.createCell(i).setCellValue(cols4[i]);
            List<DietRecord> diets = getDiets(userId);
            for (int i = 0; i < diets.size(); i++) {
                Row row = sheet4.createRow(i + 1);
                DietRecord d = diets.get(i);
                row.createCell(0).setCellValue(d.getCreateTime() != null ? DATE_FMT.format(d.getCreateTime()) : "");
                row.createCell(1).setCellValue(nvl(d.getFoodName()));
                row.createCell(2).setCellValue(nvl(d.getCategory()));
                row.createCell(3).setCellValue(d.getCaloriesConsumed() != null ? d.getCaloriesConsumed() : 0);
                row.createCell(4).setCellValue(d.getProtein() != null ? d.getProtein().doubleValue() : 0);
                row.createCell(5).setCellValue(d.getFat() != null ? d.getFat().doubleValue() : 0);
                row.createCell(6).setCellValue(d.getCarbs() != null ? d.getCarbs().doubleValue() : 0);
                row.createCell(7).setCellValue(nvl(d.getNote()));
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                Sheet sheet = wb.getSheetAt(i);
                for (int col = 0; col < sheet.getRow(0).getLastCellNum(); col++) {
                    sheet.autoSizeColumn(col);
                }
            }

            wb.write(response.getOutputStream());
        }
    }

    private List<HealthRecord> getHealthRecords(Long userId) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getCreateTime);
        return healthRecordMapper.selectList(wrapper);
    }

    private List<DailyCheckin> getCheckins(Long userId) {
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .orderByDesc(DailyCheckin::getCheckDate);
        return dailyCheckinMapper.selectList(wrapper);
    }

    private List<ExerciseRecord> getExercises(Long userId) {
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId)
                .orderByDesc(ExerciseRecord::getCreateTime);
        return exerciseRecordMapper.selectList(wrapper);
    }

    private List<DietRecord> getDiets(Long userId) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .orderByDesc(DietRecord::getCreateTime);
        return dietRecordMapper.selectList(wrapper);
    }

    private String nvl(String s) { return s == null ? "" : s.replace("\"", "'"); }
}