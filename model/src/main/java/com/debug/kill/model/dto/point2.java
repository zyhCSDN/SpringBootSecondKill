package com.debug.kill.model.dto;

/**
 * @author: Zhaoyongheng
 * @date: 2019/9/25
 */
class point2{
    String x,y;
    point2(){
    }
    point2(String a,String b){//构造函数；含参的构造函数；构造方法必须要和类名一致
//且没有返回值
        x=a;
        y=b;
    }
    void output(){
        System.out.println(x);
        System.out.println(y);
    }
    public static void main(String args[]){
        point2 pt=new point2();//实例化对象，这时候x,y没有赋值，默认初始值为0；
        pt.output();
        pt.x="66";pt.y="666";
        pt.output();
    }
}
