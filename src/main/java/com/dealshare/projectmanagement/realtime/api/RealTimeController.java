package com.dealshare.projectmanagement.realtime.api;

import com.dealshare.projectmanagement.realtime.application.RealTimeReplayService;
import com.dealshare.projectmanagement.realtime.application.RealTimePresenceService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class RealTimeController {

    private final RealTimeReplayService replayService;
    private final RealTimePresenceService presenceService;

    public RealTimeController(RealTimeReplayService replayService, RealTimePresenceService presenceService) {
        this.replayService = replayService;
        this.presenceService = presenceService;
    }

    @MessageMapping("/realtime/replay")
    @SendToUser("/queue/replay")
    ReplayResponse replay(@Payload ReplayRequest request) {
        return replayService.replay(request);
    }

    @MessageMapping("/presence/board")
    @SendToUser("/queue/presence")
    PresenceResponse joinBoard(@Payload PresenceRequest request, SimpMessageHeaderAccessor headers) {
        return presenceService.joinBoard(headers.getSessionId(), request);
    }

    @MessageMapping("/presence/issue")
    @SendToUser("/queue/presence")
    PresenceResponse joinIssue(@Payload PresenceRequest request, SimpMessageHeaderAccessor headers) {
        return presenceService.joinIssue(headers.getSessionId(), request);
    }

    @MessageMapping("/presence/leave")
    @SendToUser("/queue/presence")
    PresenceResponse leave(@Payload PresenceRequest request, SimpMessageHeaderAccessor headers) {
        return presenceService.leave(headers.getSessionId(), request);
    }
}
