package com.debug.kill.server.utils;

import com.debug.kill.server.dto.PersonDto;
import com.google.common.collect.Lists;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: Zhaoyongheng
 * @date: 2019/9/26
 */
public class Java8Util {


    private static List<PersonDto> list;
    //初始化对象集合
    static {
        list= Lists.newLinkedList();
        PersonDto dto1=new PersonDto(1,21,"赵六");
        PersonDto dto2=new PersonDto(2,20,"张三");
        PersonDto dto3=new PersonDto(3,22,"李四");
        PersonDto dto4=new PersonDto(4,20,"王五");

        list.add(dto1);
        list.add(dto2);
        list.add(dto3);
        list.add(dto4);
    }

    //筛选功能:filter 结合 collect方法 使用
    public static void method1(){
        //筛选年龄大于20的小伙子
        if (list!=null && !list.isEmpty()){
            System.out.println("\n--filter筛选功能1，结果：");

            List<PersonDto> resList=list.stream().filter(p -> p.getAge() > 20).collect(Collectors.toList());
            System.out.println(resList);
        }

        //筛选名字为 王五 的小伙子
        if (list!=null && !list.isEmpty()){
            System.out.println("\n--filter筛选功能2，结果：");

            List<PersonDto> resList=list.stream().filter(p -> "王五".equals(p.getName())).collect(Collectors.toList());
            System.out.println(resList);
        }
    }


    //转换map~按照指定的字段/元素属性进行转换：结合 collect 方法使用
    public static void method2(){
        if (list!=null && !list.isEmpty()){
            System.out.println("\n--转换map~按照指定的字段/元素属性进行转换，结果：");

            //SET打印无序
            Set<String> nameSet=list.stream().map(PersonDto::getName).collect(Collectors.toSet());
            //list打印有序
            List<String> nameList=list.stream().map(PersonDto::getName).collect(Collectors.toList());
//            System.out.println(nameSet);
            System.out.println(nameList);

//            list.forEach(System.out::println);

        }
    }

    //去重distinct ~ 配合对象的 equals() 和 hashCode()方法

    public static void method3(){
        //TODO：对象去重 - 对象需要实现equals hashCode方法
        list.add(new PersonDto(4,20,"王五"));
        System.out.println("去重以前："+list);
        List<PersonDto> resList=list.stream().distinct().collect(Collectors.toList());
        System.out.println("去重以后："+resList);
    }



    //排序sorted
    public static void method4(){
        //按照年龄排序、再按照id排序
        list.add(new PersonDto(0,20,"郑六"));
        if (list!=null && !list.isEmpty()){
            List<PersonDto> resList=list.stream()
                    .sorted(Comparator.comparingInt(PersonDto::getAge).thenComparing(Comparator.comparing(PersonDto::getId)))
                    .collect(Collectors.toList());
            System.out.println("按照年龄排序、再按照id排序-顺序：\n"+resList);
        }

        //按照年龄排序、再按照id排序 ~ 也可以将最终结果倒序来
        if (list!=null && !list.isEmpty()){
            List<PersonDto> resList=list.stream()
                    .sorted(Comparator.comparingInt(PersonDto::getAge).thenComparing(Comparator.comparing(PersonDto::getId)).reversed())
                    .collect(Collectors.toList());
            System.out.println("\n按照年龄排序、再按照id排序-最终倒序：\n"+resList);
        }

    }

    //最小min、最大max
    public static void method5(){
        if (list!=null && !list.isEmpty()){
            PersonDto p=list.stream()
                    .min(Comparator.comparingInt(PersonDto::getId))
                    .get();
            System.out.println("\nid最小："+p);
        }

        if (list!=null && !list.isEmpty()){
            PersonDto p=list.stream()
                    .max(Comparator.comparingInt(PersonDto::getAge))
                    .get();
            System.out.println("\n年龄最大："+p);
        }
    }


    public static void main(String[] args) {
       // method1();
        method2();
        //method3();
        //method4();
        //method5();
    }

}
