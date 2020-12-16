package org.springframework.tests.my;

import org.junit.Test;
import org.springframework.tests.sample.beans.CustomEnum;
import org.springframework.tests.sample.beans.GenericBean;

/**
 * @Description: 测试bean的创建和获取
 * @author: sxk
 * @CreateDate: 2020/12/10 11:23
 * @Version: 1.0
 */
public class CreateBeanTest {

	@Test
	public void test() {
		GenericBean testBean = new GenericBean();
		testBean.setCustomEnum(CustomEnum.VALUE_1);
		System.out.println(testBean);
	}

}
