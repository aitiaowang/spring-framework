package org.springframework.custom_test.converter;

import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Date;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/4 11:30
 * @Version: 1.0
 */
public class StringToDateConverterTest {

	@Test
	public void test() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new StringToDateConverter());
		String str = "2021-01-04";
		Date date = conversionService.convert(str, Date.class);
		System.out.println(date);
	}
}
