package com.example.service;

import com.example.dto.CheckinSubmitDTO;
import com.example.dto.CheckinSupplementDTO;
import com.example.vo.CheckinStatsVO;
import com.example.vo.CheckinVO;

import java.util.List;

public interface CheckinService {

    CheckinVO submitCheckin(Long userId, CheckinSubmitDTO dto);

    CheckinVO supplementCheckin(Long userId, CheckinSupplementDTO dto);

    List<CheckinVO> getCheckinList(Long userId);

    CheckinStatsVO getStats(Long userId);
}
