###### JDBC连接数据库的流程及其原理
    1.在开发环境中加载指定数据库的驱动程序
    2.在java程序中加载驱动程序；通过Class.forName("指定数据库的驱动程序")
    3.创建数据连接对象。通过DriverManager类创建数据库连接对象Connection
    4.创建Statement对象。Statement类主要是用于执行静态sql语句并返回它所生成结果的对象
    5.调用Statement对象的相关方法执行相对应的SQL语句
    6.关闭数据库连接
