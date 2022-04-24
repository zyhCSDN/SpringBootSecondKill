package com.debug.kill.server.controller;

import java.sql.*;


/**
 * @author:ZHAOYONGHENG
 * @date:2022/1/18
 * @version:1.0.0
 */
public class DeleteTest {

        public static void main(String[] args) throws ClassNotFoundException, SQLException {
            final String url = "jdbc:mysql://127.0.0.1/bacthdelete?useUnicode=true&characterEncoding=utf8";
            final String name = "com.mysql.jdbc.Driver";
            final String user = "root";
            final String password = "root";
            Connection conn = null;
            Class.forName(name); // 指定连接类型
            conn = DriverManager.getConnection(url, user, password); // 获取连接
            if (conn != null) {
                System.out.println("获取连接成功");
//                insert(conn);
                deleteBatch(conn);
            } else {
                System.out.println("获取连接失败");
            }
        }

        public static void insert(Connection conn) {
            // 一共插入数据
            int totalCount = 10000000;
            // 每次sql插入数据
            int perTimeCount = 10000;
            // 开始时间
            Long begin = System.currentTimeMillis();
            String prefix = "INSERT INTO test_delete (id,sex,name,company,department,position,lastUpdateTime) VALUES ";
//            String prefix = "INSERT INTO test_delete2 (id,sex,name,company,department,position,lastUpdateTime) VALUES ";
            try {
                // 保存sql后缀
                StringBuffer suffix = new StringBuffer();
                // 设置事务为非自动提交
                conn.setAutoCommit(false);
                // 比起st，pst会更好些
                PreparedStatement pst = (PreparedStatement) conn.prepareStatement(""); // 准备执行语句
                // 外层循环，总提交事务次数
                for (int i = 1; i <= totalCount; i++) {
                    //suffix = new StringBuffer();
                    // 第j次提交步长
                    // 构建SQL后缀
                    suffix.append("('" + i + "','1'" + ",'我是名字" + i + "'" + ",'np公司名'" + ",'np部门'" + ",'np职位',"+i+"),");
                    if (i % perTimeCount == 0) {
                        // 构建完整SQL
                        String sql = prefix + suffix.substring(0, suffix.length() - 1);
                        // 添加执行SQL
                        pst.addBatch(sql);
                        // 执行操作
                        pst.executeBatch();
                        // 提交事务
                        conn.commit();
                        // 清空上一次添加的数据
                        suffix = new StringBuffer();
                    }
                }
                // 头等连接
                pst.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // 结束时间
            Long end =System.currentTimeMillis();
            // 耗时
            System.out.println(totalCount + "条数据插入花费时间 : " + (end - begin) / 1000 + " s");
            System.out.println("插入完成");
        }



    //批量删除 千万条数据

    public static void deleteBatch(Connection conn) throws SQLException {
//数据中需要删除的数据量

        Long expiredCount = 0L;

//已经删除数据量

        Long totalDeleted = 0L;

//要删除表的名字


//要删除的条件

        String positions = "np职位";

// 开始时间

        Long begin = System.currentTimeMillis();

//带有占位符的sql

        String sql = "delete from test_delete where positions = ? limit 100000 ";

        PreparedStatement pstmt = conn.prepareStatement(sql);

//循环批量删除

        do {

            pstmt.setString(1, positions);

// 返回值代表收到影响的行数

            int result = pstmt.executeUpdate();

//已经删除条数

            totalDeleted += result;

//还有条数

            expiredCount = queryCount( positions, conn);

        } while (expiredCount > 0);

// 结束时间

        Long end = System.currentTimeMillis();

// 耗时

        System.out.println("千万条数据删除花费时间 : " + (end - begin) / 1000 + " s");

        System.out.println("删除完成");

    }
//查询过期记录数量

    private static long queryCount( String schoolName, Connection conn) throws SQLException {
        String sql = "SELECT COUNT (*) as cnt FROM test_delete where positions = ? ";

        PreparedStatement pstmt = conn.prepareStatement(sql);


        pstmt.setString(1, schoolName);

        ResultSet rs = pstmt.executeQuery(sql);

        while (rs.next()) {
            long count = rs.getInt("cnt");

            return count;

        }

        return 0L;

    }

}
