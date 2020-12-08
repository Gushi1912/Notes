package com.gushi.swagger.controller;

import com.gushi.swagger.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

/**
 * @author Gushi1912 | gushiyang@sheca.com
 * @version 0.0.1
 * 2020/12/8
 */
@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口", hidden = false)
public class UserController {

    @ApiOperation("添加用户")
    @PostMapping("/add")
    public String add(@RequestBody User user) {
        StringBuilder builder = new StringBuilder();
        return builder.append("username:").append(user.getUsername()).append(",")
                .append("age: ").append(user.getAge()).append(",")
                .append("isFemale: ").append(user.getIsFemale()).append(",")
                .append("height: ").append(user.getHeight()).toString();
    }

    @ApiOperation("删除用户")
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable @ApiParam(name = "id",value = "用户id") Integer id) {
        return "the user is deleted and it's id is " + id;
    }

    @ApiOperation("更新用户")
    @PostMapping("/update")
    public String update(@RequestBody User user) {
        return "用户更新成功";
    }

    @ApiOperation("查找用户")
    @GetMapping("/find/{id}")
    public String find(@PathVariable @ApiParam(name = "id",value = "用户id") Integer id) {
        return "未能查找到id为：" + id + "的用户";
    }
}
