package co.com.ai_finance.api.mapper;

import co.com.ai_finance.api.request.UserRequest;
import co.com.ai_finance.model.users.model.UsersModel;

public class UserMapper {

    public UsersModel toUserModel(UserRequest userRequest){
        return UsersModel.builder()
                .email(userRequest.email())
                .names(userRequest.names())
                .lastnames(userRequest.lastnames())
                .password(userRequest.password())
                .monthlyIncome(userRequest.monthlyIncome())
                .build();
    }
}
