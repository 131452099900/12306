package me.xgwd.ticket.sservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.xgwd.bean.doain.RouteDTO;
import me.xgwd.ticket.handler.purchase.bean.TrainStationDO;
import me.xgwd.ticket.mapper.TrainStationMapper;
import me.xgwd.ticket.util.StationCalculateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/23/20:19
 * @Description:
 */
@Component
public class TrainStationService {
    @Autowired
    private TrainStationMapper trainStationMapper;
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        // 列车的所有站点
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        // 列车的所有出发站点 北京 德州 - 南京 嘉兴 海宁 杭州
        List<String> trainStationAllList = trainStationDOList.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());

        // 计算出发站和终点站中间的站点（包含出发站和终点站）
        // 得到15个 各个站点到各个站点 如北京-德州 北京-南京。。。 德州-南京
        List<RouteDTO> routeDTOS = StationCalculateUtil.throughStation(trainStationAllList, departure, arrival);
        return routeDTOS;
    }

    /**
     * 获取需列车站点扣减路线关系
     * 获取开始站点和目的站点、中间站点以及关联站点信息
     *
     * @param trainId   列车 ID
     * @param departure 出发站
     * @param arrival   到达站
     * @return 需扣减列车站点路线关系信息
     */
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);
        // 把列车的出发站点的所有结果查出来
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        List<String> trainStationAllList = trainStationDOList.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());
        return StationCalculateUtil.takeoutStation(trainStationAllList, departure, arrival);
    }
}
