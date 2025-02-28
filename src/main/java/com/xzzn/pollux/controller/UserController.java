package com.xzzn.pollux.controller;

import com.github.dockerjava.api.command.ListNetworksCmd;
import com.xzzn.pollux.common.ListResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.common.enums.VerificationTypeEnum;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.mapper.ModelInfoMapper;
import com.xzzn.pollux.mapper.TaskReviewMapper;
import com.xzzn.pollux.model.dto.UserListPageDTO;
import com.xzzn.pollux.model.vo.request.user.*;
import com.xzzn.pollux.model.vo.response.user.UserDataResponse;
import com.xzzn.pollux.model.vo.response.user.UserLoginResponse;
import com.xzzn.pollux.model.vo.response.user.UserInfo;
import com.xzzn.pollux.service.ReviewService;
import com.xzzn.pollux.service.SMSService;
import com.xzzn.pollux.service.impl.ModelInfoServiceImpl;
import com.xzzn.pollux.service.impl.QATaskFilesServiceImpl;
import com.xzzn.pollux.service.impl.TaskReviewServiceImpl;
import com.xzzn.pollux.service.impl.UserServiceImpl;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.xzzn.pollux.entity.*;
import springfox.documentation.swagger2.mappers.ModelMapper;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author xzzn
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private SMSService smsService;

    @Resource
    private QATaskFilesServiceImpl qaTaskFilesService;

    @Resource
    private ModelInfoServiceImpl modelInfoService;

    @Resource
    private TaskReviewMapper taskReviewMapper;

    @Resource
    private ModelInfoMapper modelInfoMapper;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return token
     */
    @PostMapping("/register")
    public ResultResponse<String> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        return ResultUtils.success(userService.userRegister(userRegisterRequest));
    }

    /**
     * 密码登陆
     *
     * @param userLoginRequest 用户登陆请求
     * @return token
     */
    @PostMapping("/login")
    public UserLoginResponse userLogin(@Valid @RequestBody UserLoginRequest userLoginRequest) {
        return new UserLoginResponse(200, userService.userLogin(userLoginRequest), "success");
    }

    /**
     * 验证码登陆
     *
     * @param userLoginByCaptchaRequest 用户验证码登陆请求
     * @return token
     */
    @PostMapping("/login/code")
    public UserLoginResponse userLoginByCode(@Valid @RequestBody UserLoginByCaptchaRequest userLoginByCaptchaRequest) {
        return new UserLoginResponse(200, userService.userLoginByCaptcha(userLoginByCaptchaRequest), "success");
    }

    /**
     * 发送登陆验证码
     *
     * @param userSendLoginCodeRequest 用户发送登录验证码请求
     * @return 成功或失败
     */
    @PostMapping("/login/send-code")
    public ResultResponse<Boolean> sendLoginCode(@Valid @RequestBody UserSendLoginCodeRequest userSendLoginCodeRequest) {
        return ResultUtils.success(smsService.sendVerificationCode(userSendLoginCodeRequest.getAccount(), VerificationTypeEnum.LOGIN));
    }

    /**
     * 发送注册验证码
     *
     * @param userSendRegisterCodeRequest 用户发送注册验证码请求
     * @return 成功或失败
     */
    @PostMapping("/register/send-code")
    public ResultResponse<Boolean> sendRegisterCode(@Valid @RequestBody UserSendRegisterCodeRequest userSendRegisterCodeRequest) {
        return ResultUtils.success(smsService.sendVerificationCode(userSendRegisterCodeRequest.getAccount(), VerificationTypeEnum.REGISTER));
    }

    @GetMapping
    public ResultResponse<UserInfo> getUser(
            @RequestHeader("ACCESS-KEY") String accessKey
    ) {
        return ResultUtils.success(userService.getUserInfoByToken(accessKey));
    }

    @GetMapping("/all")
    public ResultResponse<ArrayList<User>> getAllUsers(
            @RequestHeader("ACCESS-KEY") String accessKey
    ) {
        ArrayList<User> userList = (ArrayList<User>) userService.list();
        return ResultUtils.success(userList);
    }

    @GetMapping("/list")
    public ListResponse<ArrayList<User>> getPagedUsers(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "sortAttribute", required = false, defaultValue = "user_name") String sortAttribute,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection
    ) {
        UserListPageDTO userList = userService.getAllUserByPage(page, size, sortAttribute, sortDirection);
        return new ListResponse<>(200, (ArrayList<User>) userList.getUserlist(), userList.getTotalNum(), "success");
    }

    @GetMapping("/get")
    public ResultResponse<UserDataResponse> getUserData(
            @RequestHeader("ACCESS-KEY") String accessKey
    ){
        String userId = userService.getUser(accessKey).getId();
        int qaTotalNum = qaTaskFilesService.lambdaQuery()
                .select(QATaskFiles::getQaCount) // 只选择 qaCount 字段
                .list()
                .stream()
                .mapToInt(QATaskFiles::getQaCount)
                .sum();

        Integer totalReviewNum = taskReviewMapper.getTotalReviewNumByUserId(userId);
        int reviewTotalNum = totalReviewNum != null ? totalReviewNum : 0; // 处理可能的 null 值

        int modelTotalNum = modelInfoService.lambdaQuery().count().intValue();

        Integer OnlineTotalNum = modelInfoMapper.selectTotalOnlineCount();
        int modelOnlineTotalNum = OnlineTotalNum != null ? OnlineTotalNum : 0;

        return ResultUtils.success(UserDataResponse.builder()
                .qaTotalNum(qaTotalNum)
                .reviewTotalNum(reviewTotalNum)
                .modelTotalNum(modelTotalNum)
                .modelOnlineTotalNum(modelOnlineTotalNum)
                .build());
    }
}
