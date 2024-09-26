package me.xgwd.ticket.sservice.strage.bitmap;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/18:08
 * @Description:
 */

import cn.hutool.core.lang.Singleton;
import me.xgwd.ticket.sservice.strage.bitmap.train.TrainBusinessCheckSeat;
import me.xgwd.ticket.sservice.strage.bitmap.train.TrainFirstCheckSeat;
import me.xgwd.ticket.sservice.strage.bitmap.train.TrainSecondCheckSeat;

/**
 * 座位通过 BitMap 检测抽象工厂
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
public abstract class BitMapCheckSeatStatusFactory {

    public static final String TRAIN_BUSINESS = "TRAIN_BUSINESS";
    public static final String TRAIN_FIRST = "TRAIN_FIRST";
    public static final String TRAIN_SECOND = "TRAIN_SECOND";

    /**
     * 获取座位检查方法实例
     *
     * @param mark 座位标识
     * @return 座位检查类
     */
    public static BitMapCheckSeat getInstance(String mark) {
        BitMapCheckSeat instance = null;
        switch (mark) {
            case TRAIN_BUSINESS -> {
                instance = Singleton.get(TRAIN_BUSINESS);
                if (instance == null) {
                    instance = new TrainBusinessCheckSeat();
                    Singleton.put(TRAIN_BUSINESS, instance);
                }
            }
            case TRAIN_FIRST -> {
                instance = Singleton.get(TRAIN_FIRST);
                if (instance == null) {
                    instance = new TrainFirstCheckSeat();
                    Singleton.put(TRAIN_FIRST, instance);
                }
            }
            case TRAIN_SECOND -> {
                instance = Singleton.get(TRAIN_SECOND);
                if (instance == null) {
                    instance = new TrainSecondCheckSeat();
                    Singleton.put(TRAIN_SECOND, instance);
                }
            }
        }
        return instance;
    }
}
