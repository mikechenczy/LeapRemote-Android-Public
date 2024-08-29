package org.mj.leapremote.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    private int userId;
    private String username;
    private String nickname;
    private String password;
    private String email;
    private String phoneNum;
    private int age;
    private long createdTime;
    private long vip;
}