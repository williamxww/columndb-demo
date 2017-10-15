package edu.caltech.nanodb.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;

/**
 * 数字工具:主要用于double ,BigDecimal的操作,集合的求和及平均值等操作
 * 
 * @author wwxiang
 * @since 2016/9/18.
 */
public class MathUtil {

	/**
	 * 默认精度
	 */
	private static final int DEF_DECIMAL_SCALE = 5;

	/**
	 * 在指定范围内[min,max)取随机整数
	 * 
	 * @param min 下界
	 * @param max 上界
	 * @return 随机数
	 */
	public static int getRandomInt(int min, int max) {
		int gap = max - min;
		return (int) (Math.random() * gap) + min;
	}

	/**
	 * 对集合求和,集合为null, size==0该方法都返回null 集合中的null的元素会被忽略
	 * 
	 * @param collection collection
	 * @return BigDecimal
	 */
	public static BigDecimal sum(Collection<Number> collection) {
		if (collection == null || collection.size() == 0) {
			return null;
		}
		Iterator<Number> iterator = collection.iterator();
		BigDecimal result = null;
		while (iterator.hasNext()) {
			Number raw = iterator.next();
			if (raw == null) {
				continue;
			}
			BigDecimal next = new BigDecimal(raw.toString());
			if (result == null) {
				result = next;
			} else {
				result = result.add(new BigDecimal(next.toString()));
			}
		}
		return result;
	}

	/**
	 * 对集合求平均值,集合为null, size==0该方法都返回null 集合中的null的元素会被忽略
	 * 
	 * @param collection collection
	 * @return BigDecimal
	 */
	public static BigDecimal average(Collection<Number> collection) {
		if (collection == null || collection.size() == 0) {
			return null;
		}
		Iterator<Number> iterator = collection.iterator();
		BigDecimal sum = null;
		int notNullSize = 0;
		while (iterator.hasNext()) {
			Number raw = iterator.next();
			if (raw == null) {
				continue;
			}
			BigDecimal next = new BigDecimal(raw.toString());
			if (sum == null) {
				sum = next;
			} else {
				sum = sum.add(new BigDecimal(next.toString()));
			}
			notNullSize++;
		}
		if (notNullSize == 0) {
			return null;
		}
		return sum.divide(new BigDecimal(notNullSize));
	}

	/**
	 * 取集合最小值,集合为null, size==0该方法都返回null 集合中的null的元素会被忽略
	 * 
	 * @param collection collection
	 * @return BigDecimal
	 */
	public static BigDecimal min(Collection<Number> collection) {
		if (collection == null || collection.size() == 0) {
			return null;
		}
		Iterator<Number> iterator = collection.iterator();
		BigDecimal result = null;
		while (iterator.hasNext()) {
			Number raw = iterator.next();
			if (raw == null) {
				continue;
			}
			BigDecimal next = new BigDecimal(raw.toString());
			if (result == null || next.compareTo(result) < 0) {
				result = next;
			}
		}
		return result;
	}

	/**
	 * 取集合最大值,集合为null, size==0 该方法都返回null 集合中的null的元素会被忽略
	 * 
	 * @param collection collection
	 * @return BigDecimal
	 */
	public static BigDecimal max(Collection<Number> collection) {
		if (collection == null || collection.size() == 0) {
			return null;
		}
		Iterator<Number> iterator = collection.iterator();
		BigDecimal result = null;
		while (iterator.hasNext()) {
			Number raw = iterator.next();
			if (raw == null) {
				continue;
			}
			BigDecimal next = new BigDecimal(raw.toString());
			if (result == null || next.compareTo(result) > 0) {
				result = next;
			}
		}
		return result;
	}

	/**
	 * double 加法
	 * 
	 * @param v1 v1
	 * @param v2 v2
	 * @return double
	 */
	public static double add(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(new Double(v1).toString());
		BigDecimal b2 = new BigDecimal(new Double(v2).toString());
		return b1.add(b2).doubleValue();
	}

	/**
	 * double 减法
	 * 
	 * @param v1 v1
	 * @param v2 v2
	 * @return double
	 */
	public static double subtract(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(new Double(v1).toString());
		BigDecimal b2 = new BigDecimal(new Double(v2).toString());
		return b1.subtract(b2).doubleValue();
	}

	/**
	 * double 乘法
	 * 
	 * @param v1 v1
	 * @param v2 v2
	 * @return double
	 */
	public static double multiply(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(new Double(v1).toString());
		BigDecimal b2 = new BigDecimal(new Double(v2).toString());
		return b1.multiply(b2).doubleValue();
	}

	/**
	 * double 除法，默认2位有效数字
	 * 
	 * @param v1 v1
	 * @param v2 v2
	 * @return double
	 */
	public static double divide(double v1, double v2) {
		return divide(v1, v2, DEF_DECIMAL_SCALE);
	}

	/**
	 * double 除法,允许设置精度
	 * 
	 * @param v1 v1
	 * @param v2 v2
	 * @param scale 精度
	 * @return double
	 */
	public static double divide(double v1, double v2, int scale) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		return divide(v1, v2, scale, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * double 除法,允许设置精度，取近似值方式
	 * 
	 * @param v1 v1
	 * @param v2 v2
	 * @param scale scale
	 * @param roundingMode roundingMode
	 * @see BigDecimal#ROUND_HALF_UP etc.
	 * @return double
	 */
	public static double divide(double v1, double v2, int scale, int roundingMode) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		BigDecimal b1 = new BigDecimal(new Double(v1).toString());
		BigDecimal b2 = new BigDecimal(new Double(v2).toString());
		return divide(b1, b2, scale, roundingMode).doubleValue();
	}

	/**
	 * double 取近似值
	 * 
	 * @param v v
	 * @param scale 精度
	 * @return double
	 */
	public static double round(double v, int scale) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		BigDecimal b = new BigDecimal(v);
		return round(b, scale).doubleValue();
	}

	/**
	 * 将double进行格式化,pattern可以传入null
	 * 
	 * @see #formatDecimal(BigDecimal, String)
	 * @param input 输入参数
	 * @param pattern 格式
	 * @return 格式化后的字符串
	 */
	public static String formatDouble(double input, String pattern) {
		if (StringUtil.isEmpty(pattern)) {
			pattern = "###0.##";
		}
		return new DecimalFormat(pattern).format(input);
	}

	/**
	 * BigDecimal除法，可设置值的精度（四舍五入）
	 * 
	 * @param b1 b1
	 * @param b2 b2
	 * @param scale 精度
	 * @return BigDecimal
	 */
	public static BigDecimal divide(BigDecimal b1, BigDecimal b2, int scale) {
		return divide(b1, b2, scale, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * BigDecimal除法，可设置值的精度及取近似值的方式
	 * 
	 * @param b1 b1
	 * @param b2 b2
	 * @param scale 精度
	 * @param roundingMode 取近似值的方式
	 * @return BigDecimal
	 */
	public static BigDecimal divide(BigDecimal b1, BigDecimal b2, int scale, int roundingMode) {
		if (b1 == null || b2 == null) {
			return null;
		}

		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		return b1.divide(b2, scale, roundingMode);
	}

	/**
	 * BigDecimal 四舍五入
	 * 
	 * @param decimal 小数
	 * @param scale 小数精度
	 * @return BigDecimal
	 */
	public static BigDecimal round(BigDecimal decimal, int scale) {
		if (decimal == null) {
			return null;
		}
		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		return round(decimal, scale, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * BigDecimal 取近似值
	 * 
	 * @param decimal 小数
	 * @param scale 小数精度
	 * @param roundingMode 取近似值方式
	 * @return BigDecimal
	 */
	public static BigDecimal round(BigDecimal decimal, int scale, int roundingMode) {
		if (decimal == null) {
			return null;
		}
		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		return decimal.divide(BigDecimal.ONE, scale, roundingMode);
	}

	/**
	 * 格式化为字符串 当#在最前端或是最后端，且其对应的数字为0，则0不显示;如果用0做pattern则其对应的数字一定显示，该位没有值也会显示0
	 * 00123.12 用 #,##0.0 显示为 123.1 用 0000.0 显示为 0123.1 用 #0.000 显示为 123.120
	 * 
	 * @param input input
	 * @param pattern pattern
	 * @return String
	 */
	public static String formatDecimal(BigDecimal input, String pattern) {
		if (input == null) {
			return null;
		}
		if (StringUtil.isEmpty(pattern)) {
			pattern = "#,##0.##";
		}
		return new DecimalFormat(pattern).format(input);
	}
}
