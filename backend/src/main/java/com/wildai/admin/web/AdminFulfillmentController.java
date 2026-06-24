package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.AddNoteRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.AssignRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.MarkFailedRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.MarkSuccessRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.WaitUserRequest;
import com.wildai.fulfillment.dto.FulfillmentTaskDetailDto;
import com.wildai.fulfillment.dto.FulfillmentTaskSummaryDto;
import com.wildai.fulfillment.service.AdminFulfillmentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api/fulfillment-tasks")
public class AdminFulfillmentController {

    private final AdminFulfillmentService fulfillmentService;

    public AdminFulfillmentController(AdminFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public ApiResponse<PageResult<FulfillmentTaskSummaryDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assigneeAdminId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(fulfillmentService.listTasks(status, assigneeAdminId, orderNo, pageNo, pageSize));
    }

    @GetMapping("/{taskNo}")
    public ApiResponse<FulfillmentTaskDetailDto> detail(@PathVariable String taskNo) {
        return ApiResponse.ok(fulfillmentService.getTaskDetail(taskNo));
    }

    @PostMapping("/{taskNo}/assign")
    public ApiResponse<Map<String, String>> assign(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable String taskNo,
                                                   @RequestBody AssignRequest req) {
        fulfillmentService.assign(taskNo, principal.id(), req);
        return ApiResponse.ok(Map.of("message", "分派成功"));
    }

    @PostMapping("/{taskNo}/notes")
    public ApiResponse<Map<String, String>> addNote(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable String taskNo,
                                                    @RequestBody AddNoteRequest req) {
        fulfillmentService.addNote(taskNo, principal.id(), req);
        return ApiResponse.ok(Map.of("message", "备注已保存"));
    }

    @PostMapping("/{taskNo}/wait-user")
    public ApiResponse<Map<String, String>> waitUser(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable String taskNo,
                                                     @RequestBody WaitUserRequest req) {
        fulfillmentService.requestUserAction(taskNo, principal.id(), req);
        return ApiResponse.ok(Map.of("message", "已标记等待用户"));
    }

    @PostMapping("/{taskNo}/success")
    public ApiResponse<Map<String, String>> markSuccess(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable String taskNo,
                                                        @RequestBody MarkSuccessRequest req) {
        fulfillmentService.markSuccess(taskNo, principal.id(), req);
        return ApiResponse.ok(Map.of("message", "已标记履约成功"));
    }

    @PostMapping("/{taskNo}/fail")
    public ApiResponse<Map<String, String>> markFailed(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable String taskNo,
                                                       @RequestBody MarkFailedRequest req) {
        fulfillmentService.markFailed(taskNo, principal.id(), req);
        return ApiResponse.ok(Map.of("message", "已标记履约失败"));
    }
}
