###### 测试类
    org.springframework.context.support.ClassPathXmlApplicationContextTests

###### ClassPathXmlApplicationContext初始化步骤 (refresh()执行步骤)：
    1.初始化前的准备工作，例如对系统属性或者环境变量进行准备及验证
    2.初始化BeanFactory，并进行XML文件的读取；在这一步中，会复用BeanFactory中的配置文件读取解析及其他功能。
    这一步之后，ClassPathXMLApplicationContext实际上就已经包含了BeanFactory所提供的功能，也就可以进行Bean的提取等基础操作了
    3.对BeanFactory进行各种功能填充,@Qualifier与@Autowired这2个注解就是在这一步中增加的支持
    4.子类覆盖方法做额外的处理
    5.激活各种BeanFactory处理器
    6.注册拦截Bean创建的bean处理器，这里只是注册，真正的调用是在getBean的时候
    7.为上下文初始化Message源，即对不同语言的消息体进行国际化处理
    8.初始化应用消息广播器，并放入“ApplicationEventMulticaster” bean中
    9.留给子类来初始化其他的bean
    10.在所有注册的bean中查找listener bean，注册到消息广播器中
    11.初始化剩下的单实例（非惰性的）
    12.完成刷新过程，通知生命周期处理器lifecycleProcessor刷新过程，同时发出ContextRefreshEvent通知别人