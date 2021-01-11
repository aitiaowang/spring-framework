package org.springframework.custom_test.instrument;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/8 10:21
 * @Version: 1.0
 */
public class TestMain {

	public static void main(String[] args) {
		TestMain testMain = new TestMain();
		testMain.test();
	}

	public void test() {
		System.out.println("Hello Word");
	}
}
