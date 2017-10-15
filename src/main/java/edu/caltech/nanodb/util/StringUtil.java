package edu.caltech.nanodb.util;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具
 * 
 * @author jyliu
 * @author wwxiang
 * @since 2016/9/12.
 */
public class StringUtil {

	/**
	 * length=0 or all elements are null or ""
	 * 
	 * @param objs objs
	 * @return boolean
	 */
	public static boolean isEmpty(Object... objs) {
		if (objs == null) {
			return true;
		} else {
			for (Object obj : objs) {
				if (obj != null && !"".equals(obj)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * length > 0 and all elements are not null or ""
	 * 
	 * @param objs objs
	 * @return boolean
	 */
	public static boolean isNotEmpty(Object... objs) {
		if (objs == null) {
			return false;
		} else {
			for (Object obj : objs) {
				if (obj == null || "".equals(obj)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * null, length=0, whiteSpace
	 * 
	 * @param charSequence string
	 * @return is Blank
	 */
	public static boolean isBlank(CharSequence charSequence) {
		return StringUtils.isBlank(charSequence);
	}

	/**
	 * 字符串中是否所有的字符都为数字, 注意小数点不是数字 StringUtils.isNumeric("123") = true
	 * StringUtils.isNumeric("12.3") = false StringUtils.isNumeric("-123") =
	 * false
	 * 
	 * @param cs the CharSequence to check
	 * @return is Numeric
	 */
	public static boolean isNumeric(CharSequence cs) {
		return StringUtils.isNumeric(cs);
	}

	/**
	 * Finds the first index within a CharSequence
	 * 
	 * @param seq the CharSequence to check, may be null
	 * @param searchSeq the CharSequence to find, may be null
	 * @return first index
	 */
	public static int indexOf(CharSequence seq, CharSequence searchSeq) {
		return StringUtils.indexOf(seq, searchSeq);
	}

	/**
	 * Check if a CharSequence ends with a specified suffix.
	 * 
	 * @param str the CharSequence to check
	 * @param suffix 后缀
	 * @return boolean
	 */
	public static boolean endsWith(CharSequence str, CharSequence suffix) {
		return StringUtils.endsWith(str, suffix);
	}

	/**
	 * Check if a CharSequence starts with a specified prefix
	 * 
	 * @param str the CharSequence to check
	 * @param prefix the prefix to find
	 * @return boolean
	 */
	public static boolean startsWith(CharSequence str, CharSequence prefix) {
		return StringUtils.startsWith(str, prefix);
	}

	/**
	 * Splits the provided text into an array, separators specified.
	 * 
	 * @param str the String to parse, may be null
	 * @param separatorChars delimiters
	 * @return an array of parsed Strings
	 */
	public static String[] split(String str, String separatorChars) {
		return StringUtils.split(str, separatorChars);
	}

	/**
	 * Joins the elements of the provided array into a single String containing
	 * the provided list of elements.
	 * 
	 * @param array the array of values to join together, may be null
	 * @param separator separator
	 * @return the joined String
	 */
	public static String join(Object[] array, String separator) {
		return StringUtils.join(array, separator);
	}

	/**
	 * 将集合连接成字符串
	 * 
	 * @param iterable List set
	 * @param separator separator
	 * @return the joined String
	 */
	public static String join(Iterable<?> iterable, String separator) {
		return StringUtils.join(iterable, separator);
	}

	/**
	 * build sql string like 'a','b','c'
	 * 
	 * @param iterable collection
	 * @return String
	 */
	public static String joinForSql(Iterable<?> iterable) {
		if (iterable == null) {
			return "";
		}
		Iterator<?> iterator = iterable.iterator();
		if (iterator == null || !iterator.hasNext()) {
			return "";
		}

		final Object first = iterator.next();
		final StringBuilder buf = new StringBuilder();
		if (first != null) {
			buf.append("'" + first + "'");
		}
		while (iterator.hasNext()) {
			buf.append(",");
			final Object obj = iterator.next();
			if (obj != null) {
				buf.append("'" + obj + "'");
			}
		}
		return buf.toString();
	}

	/**
	 * Trim removes start and end characters &lt;= 32.
	 * 
	 * @param str the String to be trimmed
	 * @return the trimmed String, or an empty String if {@code null} input
	 */
	public static String trimToEmpty(String str) {
		return StringUtils.trimToEmpty(str);
	}

	/**
	 * Removes control characters (char &lt;= 32) from both ends of this String,
	 * handling {@code null} by returning {@code null}.
	 * 
	 * @param str the String to be trimmed
	 * @return the trimmed string
	 */
	public static String trim(String str) {
		return StringUtils.trim(str);
	}

	/**
	 * 去除str头尾里stripChars包含的字符 str 为null 返回null stripChars为null 则返回str原值
	 *
	 * @param str the String to remove characters from
	 * @param stripChars the characters to remove, null treated as whitespace
	 * @return the stripped String
	 */
	public static String strip(String str, String stripChars) {
		return StringUtils.strip(str, stripChars);
	}

	/**
	 * 将text中所有searchString换成replacement
	 * 
	 * @param text text to search and replace in
	 * @param searchString the String to search for
	 * @param replacement the String to replace it with
	 * @return the text with any replacements processed
	 */
	public static String replaceAll(final String text, final String searchString, final String replacement) {
		return StringUtils.replace(text, searchString, replacement, -1);
	}

	/**
	 * 替换掉正则表达式匹配的字符串
	 * 
	 * @param source source
	 * @param regex regex
	 * @param replacement replacement
	 * @return 替换后的string
	 */
	public static String replacePattern(final String source, final String regex, final String replacement) {
		return Pattern.compile(regex, Pattern.DOTALL).matcher(source).replaceAll(replacement);
	}

	/**
	 * 从text中找searchString，找到后用replacement替换，只替换一次
	 * 
	 * @param text text
	 * @param searchString searchString
	 * @param replacement replacement
	 * @return 替换后的string
	 */
	public static String replaceFirst(final String text, final String searchString,
			final String replacement) {
		return StringUtils.replace(text, searchString, replacement, 1);
	}

	/**
	 * Left pad a String with a specified String.
	 * 
	 * @param str the String to pad out, may be null
	 * @param size the size to pad to
	 * @param padChar the character to pad with
	 * @return left padded String
	 */
	public static String leftPad(final String str, final int size, final char padChar) {
		return StringUtils.leftPad(str, size, padChar);
	}

	/**
	 * Right pad a String with a specified String.
	 * 
	 * @param str the String to pad out, may be null
	 * @param size the size to pad to
	 * @param padChar the character to pad with
	 * @return right padded String
	 */
	public static String rightPad(final String str, final int size, final char padChar) {
		return StringUtils.rightPad(str, size, padChar);
	}


	/**
	 * 字符串转int，转换异常时返回0
	 * 
	 * @param str str
	 * @return int
	 */
	public static int toInt(final String str) {
		return toInt(str, 0);
	}

	/**
	 * 字符串转int，转换异常时返回默认值
	 * 
	 * @param str str
	 * @param defaultValue defaultValue
	 * @return int
	 */
	public static int toInt(final String str, final int defaultValue) {
		if (str == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(str);
		} catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}

	/**
	 * 字符串转long
	 * 
	 * @param str str
	 * @return long
	 */
	public static long toLong(final String str) {
		return toLong(str, 0L);
	}

	/**
	 * 字符串转long
	 * 
	 * @param str str
	 * @param defaultValue 默认值
	 * @return long
	 */
	public static long toLong(final String str, final long defaultValue) {
		if (str == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(str);
		} catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}

	/**
	 * 字符串转double
	 * 
	 * @param str str
	 * @return double
	 */
	public static double toDouble(final String str) {
		return toDouble(str, 0.0d);
	}

	/**
	 * 字符串转double
	 * 
	 * @param str str
	 * @param defaultValue defaultValue
	 * @return double
	 */
	public static double toDouble(final String str, final double defaultValue) {
		if (str == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(str);
		} catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}

}
