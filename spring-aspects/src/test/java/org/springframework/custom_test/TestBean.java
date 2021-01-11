package org.springframework.custom_test;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/5 9:28
 * @Version: 1.0
 */
public class TestBean {

	private String testStr = "testStr";

	public String getTestStr() {
		return testStr;
	}

	public void setTestStr(String testStr) {
		this.testStr = testStr;
	}

	public void test() {
		System.out.println("test==========");
	}
}
