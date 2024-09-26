package me.xgwd.ticket.util;

import me.xgwd.bean.doain.RouteDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/23/20:21
 * @Description:
 */
public final class StationCalculateUtil {

    /**
     * 计算出发站和终点站中间的站点（包含出发站和终点站）
     *
     * @param stations     所有站点数据
     * @param startStation 出发站
     * @param endStation   终点站
     * @return 出发站和终点站中间的站点（包含出发站和终点站）
     */
    public static List<RouteDTO> throughStation(List<String> stations, String startStation, String endStation) {
        List<RouteDTO> routesToDeduct = new ArrayList<>();
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return routesToDeduct;
        }
        for (int i = startIndex; i < endIndex; i++) {
            for (int j = i + 1; j <= endIndex; j++) {
                String currentStation = stations.get(i);
                String nextStation = stations.get(j);
                RouteDTO routeDTO = new RouteDTO(currentStation, nextStation);
                routesToDeduct.add(routeDTO);
            }
        }
        return routesToDeduct;
    }

    /**
     * 计算出发站和终点站需要扣减余票的站点（包含出发站和终点站）
     *
     * @param stations     所有站点数据
     * @param startStation 出发站
     * @param endStation   终点站
     * @return 出发站和终点站需要扣减余票的站点（包含出发站和终点站）
     */
    public static List<RouteDTO> takeoutStation(List<String> stations, String startStation, String endStation) {
        List<RouteDTO> takeoutStationList = new ArrayList<>();
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return takeoutStationList;
        }

        // 北京 德州 南京 嘉兴 海宁 杭州
        // 如果是德州-海宁 那么北京-南京 北京-嘉兴 北京-海宁 也需要扣减
        if (startIndex != 0) {
            for (int i = 0; i < startIndex; i++) {
                for (int j = 1; j < stations.size() - startIndex; j++) {
                    takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(startIndex + j)));
                }
            }
        }
        for (int i = startIndex; i <= endIndex; i++) {
            for (int j = i + 1; j < stations.size() && i < endIndex; j++) {
                takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(j)));
            }
        }
        return takeoutStationList;
    }

    public static void main(String[] args) {
        List<String> stations = Arrays.asList("北京南", "济南西", "南京南", "杭州东", "宁波");
        String startStation = "北京南";
        String endStation = "南京南";
        StationCalculateUtil.takeoutStation(stations, startStation, endStation).forEach(System.out::println);
    }
}

