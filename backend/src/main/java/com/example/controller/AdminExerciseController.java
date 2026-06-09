package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.ExerciseItemCreateDTO;
import com.example.dto.ExerciseItemUpdateDTO;
import com.example.service.AuditLogService;
import com.example.service.ExerciseService;
import com.example.vo.ExerciseItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员运动字典管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/exercise")
public class AdminExerciseController {

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private AuditLogService auditLogService;

    @Operation(summary = "查询所有运动项目")
    @GetMapping("/items")
    public Result<List<ExerciseItemVO>> listAllItems() {
        return Result.success(exerciseService.listAllItems());
    }

    @NoRepeatSubmit
    @Operation(summary = "新增运动项目")
    @PostMapping("/item")
    public Result<Void> create(@Validated @RequestBody ExerciseItemCreateDTO dto,
                               @RequestAttribute("userId") Long userId,
                               HttpServletRequest request) {
        exerciseService.createExerciseItem(dto);
        auditLogService.log(userId, null, "CREATE", "exercise_item", null,
                "新增运动项目: " + dto.getName(), request.getRemoteAddr());
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "修改运动项目")
    @PutMapping("/item")
    public Result<Void> update(@Validated @RequestBody ExerciseItemUpdateDTO dto,
                               @RequestAttribute("userId") Long userId,
                               HttpServletRequest request) {
        exerciseService.updateExerciseItem(dto);
        auditLogService.log(userId, null, "UPDATE", "exercise_item", dto.getId(),
                "修改运动项目: " + dto.getName(), request.getRemoteAddr());
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "删除运动项目")
    @DeleteMapping("/item/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId,
                               HttpServletRequest request) {
        exerciseService.deleteExerciseItem(id);
        auditLogService.log(userId, null, "DELETE", "exercise_item", id,
                "删除运动项目", request.getRemoteAddr());
        return Result.success();
    }
}