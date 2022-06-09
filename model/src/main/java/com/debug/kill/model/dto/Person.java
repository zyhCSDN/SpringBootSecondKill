package com.debug.kill.model.dto;

/**
 * @author: Zhaoyongheng
 * @date: 2019/9/25
 */

public class Person {
    String name = "AAA",
            gender = "女",
            gender5 = "女";

    String name1 = "aaaa";
    String gender1 = "男";

    int age = 22;

    @Override
    public String toString() {
        return "[name=" + name + ", age=" + age + ", gender=" + gender + "]";
    }

    public static void main(String[] args) {

        Person p = new Person();
        System.out.println(p); // 隐式调用toString(0)方法
        System.out.println(p.toString()); // 显式调用toString(0)方法
    }

}