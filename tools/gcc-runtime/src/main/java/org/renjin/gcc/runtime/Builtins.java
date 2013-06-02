package org.renjin.gcc.runtime;

public class Builtins {

	public static double __builtin_powi__(double base, int exponent) {
		if(exponent == 1) {
			return base;
		} else if(exponent == 2) {
			return base * base;
		} else {
			return Math.pow(base, (double)exponent);
		}
	}
  
  public static double __builtin_copysign__(double magnitude, double sign) {
    return Math.copySign(magnitude, sign);
  }

	public static int __fpclassifyd(double x) {
		// TODO: lookup the exact behavior of this function
		return Double.isNaN(x) ? 0 : 1;
	}
	
	public static int _gfortran_pow_i4_i4__(int base, int exponent) {
		if(exponent < 0) {
			throw new IllegalArgumentException("exponent must be > 0: " + exponent);
		}
		int result = 1;
		for(int i=0;i<exponent;++i) {
			result *= base;
		}
		return result;
	}

  public static int __isnan(double x) {
    return Double.isNaN(x) ? 1: 0;
  }


  public static boolean unordered(double x, double y) {
    return Double.isNaN(x) || Double.isNaN(y);
  }

  public static void _gfortran_set_args__(int argc, ObjectPtr argv) {
    // TODO
  }

  public static void _gfortran_set_options__(int x, IntPtr y) {
    // TODO
  }

  public static void _gfortran_stop_string__(int x, int y) {
    // TODO
  }

}
