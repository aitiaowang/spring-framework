package org.springframework.custom_test.converter;

import org.springframework.core.convert.converter.Converter;

import java.util.Date;

/**
 * @Description: 类型转换测试  String => Date
 * @author: sxk
 * @CreateDate: 2021/1/4 11:16
 * @Version: 1.0
 */
public class StringToDateConverter implements Converter<String, Date> {

	@Override
	public Date convert(String source) {
		return DateUtils.parseDate(source);
	}
}
