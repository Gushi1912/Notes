package com.gushi.swagger.model;

import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Gushi1912 | gushiyang@sheca.com
 * @version 0.0.1
 * 2020/12/8
 */
@Data
@ApiModel("用户信息")
public class User {
    @ApiModelProperty(name = "username",value = "用户名")
    private String username;
    @ApiModelProperty(name = "age",value = "用户年龄")
    private Integer age;
    @ApiModelProperty(name = "isFemale",value = "性别")
    private Boolean isFemale;
    @ApiModelProperty(name = "height",value = "身高")
    private Double height;

//    public String getUsername() {
//        return username;
//    }
//
//    public void setUsername(String username) {
//        this.username = username;
//    }
//
//    public Integer getAge() {
//        return age;
//    }
//
//    public void setAge(Integer age) {
//        this.age = age;
//    }
//
//    public Boolean getFemale() {
//        return isFemale;
//    }
//
//    public void setIsFemale(Boolean female) {
//        this.isFemale = female;
//    }
//
//    public Double getHeight() {
//        return height;
//    }
//
//    public void setHeight(Double height) {
//        this.height = height;
//    }
}
