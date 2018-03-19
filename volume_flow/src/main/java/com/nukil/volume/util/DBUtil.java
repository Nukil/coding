package com.nukil.volume.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DBUtil {
    private static Logger log = LoggerFactory.getLogger(DBUtil.class);
	private static String url = LoadPropers.getProperties().getProperty("mysql.url").trim();
	private static String username = LoadPropers.getProperties().getProperty("mysql.username", "root").trim();
	private static String password = LoadPropers.getProperties().getProperty("mysql.password", "").trim();
	private static String driver = LoadPropers.getProperties().getProperty("mysql.driver", "com.mysql.jdbc.Driver");
	/**
	 * 静态代码块：
	 */
	static {
		try {
			Class.forName(driver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取连接：
	 * @return
	 */
	public static Connection getConnection() {
		Connection connection = null;
		try {
			if(connection == null){
                connection = DriverManager.getConnection(url, username, password);
			}
		} catch (SQLException e) {
            log.error("创建连接失败！请检查参数：url【" + url + "】,username【" + username + "】,password【" + password + "】");
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * 获取Statement：
	 * @param connection
	 * @return
	 */
	public static Statement getStatement(Connection connection) {
		Statement statement = null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return statement;
	}

	/**
	 * 获取PreparedStatement：
	 * @param connection
	 * @param sql
	 * @return
	 */
	public static PreparedStatement getPreparedStatement(Connection connection, String sql) {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = connection.prepareStatement(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return preparedStatement;
	}

	/**
	 * 关闭连接：
	 * @param connection
	 */
	public static void closeConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			System.out.println("关闭连接异常！");
			e.printStackTrace();
		}
	}

	/**
	 * 关闭Statement：
	 * @param statement
	 */
	public static void closeStatement(Statement statement) {
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			System.out.println("关闭执行器异常！");
			e.printStackTrace();
		}
	}

	/**
	 * 关闭结果集对象资源：
	 * @param resultSet
	 */
	public static void closeResultSet(ResultSet resultSet) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (SQLException e) {
			System.out.println("关闭结果集异常！");
			e.printStackTrace();
		}
	}

	/**
	 * 释放全部资源：
	 * @param connection
	 * @param statement
	 * @param resultSet
	 */
	public static void closeAll(Connection connection, Statement statement, ResultSet resultSet) {
		try {
			closeResultSet(resultSet);
			closeStatement(statement);
			closeConnection(connection);
		} catch (Exception e) {
			System.out.println("释放资源异常!");
			e.printStackTrace();
		}
	}

	/**
	 * 增删改：
	 * @param sql
	 * @return
	 */
	public static int Update(String sql) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = getPreparedStatement(connection, sql);
			return preparedStatement.executeUpdate();
		} catch (Exception e) {
			System.out.println("修改数据失败！" + e.toString());
		} finally {
			closeAll(connection, preparedStatement,null);
		}
		return 0;
	}

	/**
	 * 增删改的重载(需要给SQL语句传参数的增删改)：
	 * @param sql ,list
	 * @return
	 */
	public static int Update(String sql, Object[] objs) {
		//声明连接：
		Connection connection = null;
		//声明执行器：
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = getPreparedStatement(connection, sql);
			//为SQL语句传入参数：
			if (objs != null) {
				for (int i = 0; i < objs.length; i++) {
					preparedStatement.setObject(i + 1, objs[i]);
				}
			}
			return preparedStatement.executeUpdate();
		} catch (Exception e) {
			System.out.println("修改数据失败！" + e.toString());
		} finally {
			// 释放资源
			closeAll(connection, preparedStatement,null);
		}
		return 0;
	}
}
