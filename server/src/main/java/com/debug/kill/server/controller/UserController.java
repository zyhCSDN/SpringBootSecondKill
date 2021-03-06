package com.debug.kill.server.controller;/**
 * Created by Administrator on 2019/7/2.
 */

import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户controller
 *
 * @Author:debug (SteadyJack)
 * @Date: 2019/7/2 17:45
 **/
@Controller
@Api(tags = "用户模块")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private Environment env;

    /**
     * 跳到登录页
     *
     * @return
     */
    @RequestMapping(value = {"/to/login"})
    public String toLogin() {
        return "login";
    }

    /**
     * 登录认证
     *
     * @param userName
     * @param password
     * @param modelMap
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(@RequestParam String userName, @RequestParam String password, ModelMap modelMap) {
        String errorMsg = "";
        try {
            if (!SecurityUtils.getSubject().isAuthenticated()) {
                String newPsd = new Md5Hash(password, env.getProperty("shiro.encrypt.password.salt")).toString();
                UsernamePasswordToken token = new UsernamePasswordToken(userName, newPsd);
                SecurityUtils.getSubject().login(token);
            }
        } catch (UnknownAccountException e) {
            errorMsg = e.getMessage();
            modelMap.addAttribute("userName", userName);
        } catch (DisabledAccountException e) {
            errorMsg = e.getMessage();
            modelMap.addAttribute("userName", userName);
        } catch (IncorrectCredentialsException e) {
            errorMsg = e.getMessage();
            modelMap.addAttribute("userName", userName);
        } catch (Exception e) {
            errorMsg = "用户登录异常，请联系管理员!";
            e.printStackTrace();
        }
        if (StringUtils.isBlank(errorMsg)) {
            return "redirect:/index";
        } else {
            modelMap.addAttribute("errorMsg", errorMsg);
            return "login";
        }
    }

    /**
     * 退出登录
     *
     * @return
     */
    @RequestMapping(value = "/logout")
    public String logout() {
        SecurityUtils.getSubject().logout();
        Session session = SecurityUtils.getSubject().getSession();
        //清除session
        session.stop();
        return "login";
    }


    public static final String md5(String password, String salt) {
        //加密方式
        String hashAlgorithmName = "MD5";
        //盐：为了即使相同的密码不同的盐加密后的结果也不同
        ByteSource byteSalt = ByteSource.Util.bytes(salt);
        //密码
        Object source = password;
        //加密次数
        int hashIterations = 1024;
        SimpleHash result = new SimpleHash(hashAlgorithmName, source, byteSalt, hashIterations);
        return result.toString();
    }

    public static void main(String[] args) {
        String password = new Md5Hash("123456", "11299c42bf954c0abb373efbae3f6b26").toString();
        System.out.println(password);
        //加密后的结果

    }

}



































