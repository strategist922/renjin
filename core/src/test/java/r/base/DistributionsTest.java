/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package r.base;

import org.apache.commons.math.MathException;
import org.junit.Test;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests the distribution functions.
 * Since most of the heavy lifting is done by the Apache Commons library,
 * we just want to make sure that we've lined the dist parameters up correctly.
 */
public class DistributionsTest {

  private static final double ERROR = 0.00001;

  @Test
  public void norm() throws MathException {
    assertThat( Distributions.dnorm(0, 0, 1, /* log */ false), closeTo(0.3989423, ERROR));
    assertThat( Distributions.dnorm(0, 0, 1, /* log */ true), closeTo( -0.9189385, ERROR));

    assertThat( Distributions.pnorm(0.25, 0, 1, /* lower.tail */ true, /* log.p */ false), closeTo( 0.5987063, ERROR));
    assertThat( Distributions.pnorm(0.25, 0, 1, /* lower.tail */ false, /* log.p */ false), closeTo( 0.4012937, ERROR));
    assertThat( Distributions.pnorm(0.25, 0, 1, /* lower.tail */ true, /* log.p */ true), closeTo(  -0.5129841, ERROR));
    assertThat( Distributions.pnorm(0.25, 0, 1, /* lower.tail */ false, /* log.p */ true), closeTo(  -0.9130618, ERROR));

    assertThat( Distributions.qnorm(0.25, 0, 1, /* lower.tail */ true, /* log.p */ false), closeTo( -0.6744898, ERROR));
    assertThat( Distributions.qnorm(0.25, 0, 1, /* lower.tail */ false, /* log.p */ false), closeTo( 0.6744898, ERROR));
    assertThat( Distributions.qnorm(0.99, 0, 1, /* lower.tail */ false, /* log.p */ false), closeTo( -2.326348, ERROR));
    assertThat( Distributions.qnorm(0.99, 0, 1, /* lower.tail */ false, /* log.p */ true), equalTo(Double.NaN));
    assertThat( Distributions.qnorm(0, 0, 1, /* lower.tail */ true, /* log.p */ false), equalTo(Double.NEGATIVE_INFINITY));

  }

  @Test
  public void beta() throws MathException {
    assertThat( Distributions.dbeta(0.4, 5, 1, false), closeTo(0.128, ERROR));
  }

  @Test
  public void binom() throws MathException {
    assertThat( Distributions.dbinom(3, 5, 0.25, false), closeTo( 0.08789063, ERROR));
  }

  @Test
  public void exp() throws MathException {
    assertThat( Distributions.dexp(0.5, 0.25, false), closeTo(0.5413411, ERROR));
  }

  @Test
  public void hyper() throws MathException {
    assertThat( Distributions.dhyper(3, 5, 2, 3, false), closeTo(0.2857143, ERROR));
  }
}