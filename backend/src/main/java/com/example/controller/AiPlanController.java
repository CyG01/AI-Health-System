package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.PlanGenerateDTO;
import com.example.service.AiPlanService;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "AI智能计划生成")
@RestController
@RequestMapping("/api/ai-plan")
public class AiPlanController {

    @Autowired
    private AiPlanService aiPlanService;

    @RateLimit(time = 60, count = 1)
    @NoRepeatSubmit
    @Operation(summary = "生成AI计划")
    @PostMapping("/generate")
    public Result<AiPlanDetailVO> generate(@Validated @RequestBody PlanGenerateDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success(aiPlanService.generatePlan(dto, userId));
    }

    @RateLimit(time = 60, count = 1)
    @NoRepeatSubmit
    @Operation(summary = "生成AI计划（SSE流式）")
    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Validated @RequestBody PlanGenerateDTO dto,
                                     @RequestAttribute("userId") Long userId) {
        SseEmitter emitter = new SseEmitter(60000L);
        aiPlanService.generatePlanStream(dto, userId, emitter);
        return emitter;
    }

    @Operation(summary = "查询计划列表")
    @GetMapping("/list")
    public Result<Page<AiPlanVO>> list(@RequestAttribute("userId") Long userId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(aiPlanService.getPlanList(userId, page, size, keyword));
    }

    @Operation(summary = "计划详情")
    @GetMapping("/{id}")
    public Result<AiPlanDetailVO> detail(@PathVariable Long id,
                                         @RequestAttribute("userId") Long userId) {
        return Result.success(aiPlanService.getPlanDetail(id, userId));
    }

    @NoRepeatSubmit
    @Operation(summary = "切换当前生效计划")
    @PutMapping("/{id}/active")
    public Result<Void> active(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId) {
        aiPlanService.activePlan(id, userId);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "删除计划")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId) {
        aiPlanService.deletePlan(id, userId);
        return Result.success();
    }
}
