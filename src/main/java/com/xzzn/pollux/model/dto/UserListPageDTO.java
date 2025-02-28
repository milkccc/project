package com.xzzn.pollux.model.dto;

import com.xzzn.pollux.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserListPageDTO implements Serializable {
    private long totalNum;
    private List<User> userlist;
}
