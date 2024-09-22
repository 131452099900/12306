package me.xgwd.api.user;

import me.xgwd.bean.dto.PassengerActualRespDTO;
import me.xgwd.bean.dto.PassengerRemoveReqDTO;
import me.xgwd.bean.dto.PassengerReqDTO;
import me.xgwd.bean.dto.PassengerRespDTO;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/22/14:10
 * @Description:
 */
public interface PassengerService {
    void updatePassenger(PassengerReqDTO requestParam);

    List<PassengerRespDTO>  listPassengerQueryByUsername(String username);

    List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids);

    void savePassenger(PassengerReqDTO requestParam, String username);

    void removePassenger(PassengerRemoveReqDTO requestParam);

}
