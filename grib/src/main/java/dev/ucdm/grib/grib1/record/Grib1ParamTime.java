/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.record;

import dev.ucdm.grib.common.GribStatType;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.grib1.table.Grib1Customizer;

import dev.ucdm.array.Immutable;

/**
 * Time coordinate from the PDS.
 * Process information from GRIB-1 Table 4: "Forecast time unit"
 * Process information from GRIB-1 Table 5: "Time range indicator"
 *
 * Handles standard (WMO), Grib1Customizer must override / augment
 */
@Immutable
public record Grib1ParamTime(Grib1Customizer cust, int timeRangeIndicator, boolean isInterval, int start,
                             int end, int forecastTime) {

  /**
   * Handles GRIB-1 code table 5 : "Time range indicator".
   *
   * @param cust customizer
   * @param pds the Grib1SectionProductDefinition
   */
  public static Grib1ParamTime factory(Grib1Customizer cust, Grib1SectionProductDefinition pds) {

    int p1 = pds.getTimeValue1(); // octet 19
    int p2 = pds.getTimeValue2(); // octet 20
    int timeRangeIndicatorLocal = pds.getTimeRangeIndicator(); // octet 21
    int n = pds.getNincluded();

    int start = 0;
    int end = 0;
    int forecastTime = 0;
    boolean isInterval = false;

    switch (timeRangeIndicatorLocal) {
      /*
       * Forecast product valid for reference time + P1 (P1 > 0), or
       * Uninitialized analysis product for reference time (P1 = 0), or
       * Image product for reference time (P1 = 0)
       */
      case 0:
        forecastTime = p1;
        break;

      // Initialized analysis product for reference time (P1 = 0)
      case 1:
        // accept defaults
        break;

      case 2: // Product with a valid time ranging between reference time + P1 and reference time + P2
      case 3: // Average (reference time + P1 to reference time + P2)
      case 4: // Accumulation (reference time + P1 to reference time + P2) product considered valid at reference time +
        // P2
      case 5: // Difference (reference time + P2 minus reference time + P1) product considered valid at reference time +
        // P2
        start = p1;
        end = p2;
        isInterval = true;
        break;

      // Average (reference time - P1 to reference time - P2)
      case 6:
        start = -p1;
        end = -p2;
        isInterval = true;
        break;

      // Average (reference time - P1 to reference time + P2)
      case 7:
        start = -p1;
        end = p2;
        isInterval = true;
        break;

      // P1 occupies octets 19 and 20; product valid at reference time + P1
      case 10:
        forecastTime = GribNumbers.int2(p1, p2);
        break;


      /*
       * Climatological mean value: multiple year averages of quantities which are themselves
       * means over some period of time (P2) less than a year. The reference time (R) indicates the
       * date and time of the start of a period of time, given by R to R + P2, over which a mean is
       * formed; N indicates the number of such period-means that are averaged together to form
       * the climatological value, assuming that the N period-mean fields are separated by one
       * year. The reference time indicates the start of the N-year climatology. If P1 = 0 then the
       * data averaged in the basic interval P2 are assumed to be continuous, i.e. all available data
       * are simply averaged together. If P1 = 1 (the unit of time – octet 18, Code table 4 – is not
       * relevant here) then the data averaged together in the basic interval P2 are valid only at the
       * time (hour, minute) given in the reference time, for all the days included in the P2 period.
       * The units of P2 are given by the contents of octet 18 and Code table 4
       */
      case 51: // LOOK we dont really know if this is right
        forecastTime = p2;
        break;

      /*
       * 113: Average of N forecasts (or initialized analyses); each product has forecast period of P1
       * (P1 = 0 for initialized analyses); products have reference times at intervals of P2, beginning
       * at the given reference time.
       *
       * 114: Accumulation of N forecasts (or initialized analyses); each product has forecast period of
       * P1 (P1 = 0 for initialized analyses); products have reference times at intervals of P2,
       * beginning at the given reference time
       *
       * 115: Average of N forecasts, all with the same reference time; the first has a forecast period of
       * P1, the remaining forecasts follow at intervals of P2
       *
       * 116: Accumulation of N forecasts, all with the same reference time; the first has a forecast
       * period of P1, the remaining forecasts follow at intervals of P2
       */
      case 113:
      case 114:
      case 115:
      case 116:
        forecastTime = p1;
        start = p1;
        end = (n > 0) ? p1 + (n - 1) * p2 : p1; // switch to n-1 on 3/13/2015
        isInterval = (n > 0);
        break;

      /*
       * Average of N forecasts; the first has a forecast period of P1, the subsequent ones have
       * forecast periods reduced from the previous one by an interval of P2; the reference time for
       * the first is given in octets 13 to 17, the subsequent ones have reference times increased
       * from the previous one by an interval of P2. Thus all the forecasts have the same valid time,
       * given by the initial reference time + P1
       */
      case 117:
        end = p1;
        isInterval = true;
        break;

      /*
       * Temporal variance, or covariance, of N initialized analyses; each product has forecast
       * period of P1 = 0; products have reference times at intervals of P2, beginning at the given
       * reference time
       */
      case 118:
        end = n * p2;
        isInterval = true;
        break;

      /*
       * Standard deviation of N forecasts, all with the same reference time with respect to the time
       * average of forecasts; the first forecast has a forecast period of P1, the remaining forecasts
       * follow at intervals of P2
       */
      case 119:
        start = p1;
        end = p1 + n * p2;
        isInterval = true;
        break;

      // ECMWF "Average of N Forecast" added 11/21/2014
      // see "http://emoslib.sourcearchive.com/documentation/000370.dfsg.2/grchk1_8F-source.html"
      // C Add Time range indicator = 120 Average of N Forecast. Each product
      // C is an accumulation from forecast lenght P1 to forecast
      // C lenght P2, with reference times at intervals P2-P1
      case 120:
        start = p1;
        end = p2;
        isInterval = true;
        break;

      // Average of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 123:
        end = n * p2;
        isInterval = true;
        break;

      // Accumulation of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 124:
        end = n * p2;
        isInterval = true;
        break;

      /*
       * Standard deviation of N forecasts, all with the same reference time with respect to time
       * average of the time tendency of forecasts; the first forecast has a forecast period of P1,
       * the remaining forecasts follow at intervals of P2
       */
      case 125:
        end = p1 + n * p2;
        isInterval = true;
        break;

      default:
        throw new IllegalArgumentException("PDS: Unknown Time Range Indicator " + timeRangeIndicatorLocal);
    }

    return new Grib1ParamTime(cust, timeRangeIndicatorLocal, isInterval, start, end, forecastTime);
  }

  // code table 5 - 2010 edition of WMO manual on codes
  public static String getTimeTypeName(int timeRangeIndicator) {
    String timeRange = switch (timeRangeIndicator) {
      /*
       * Forecast product valid for reference time + P1 (P1 > 0), or
       * Uninitialized analysis product for reference time (P1 = 0), or
       * Image product for reference time (P1 = 0)
       */
      case 0 -> "Uninitialized analysis / image product / forecast product valid for RT + P1";

      // Initialized analysis product for reference time (P1 = 0)
      case 1 -> "Initialized analysis product for reference time";

      // Product with a valid time ranging between reference time + P1 and reference time + P2
      case 2 -> "product valid, interval = (RT + P1) to (RT + P2)";

      // Average (reference time + P1 to reference time + P2)
      case 3 -> "Average, interval = (RT + P1) to (RT + P2)";

      /*
       * Accumulation (reference time + P1 to reference time + P2) product considered valid at
       * reference time + P2
       */
      case 4 -> "Accumulation, interval = (RT + P1) to (RT + P2)";

      /*
       * Difference (reference time + P2 minus reference time + P1) product considered valid at
       * reference time + P2
       */
      case 5 -> "Difference, interval = (RT + P2) - (RT + P1)";

      // Average (reference time - P1 to reference time - P2)
      case 6 -> "Average, interval = (RT - P1) to (RT - P2)";

      // Average (reference time - P1 to reference time + P2)
      case 7 -> "Average, interval = (RT - P1) to (RT + P2)";

      // P1 occupies octets 19 and 20; product valid at reference time + P1
      case 10 -> "product valid at RT + P1";

      /*
       * Climatological mean value: multiple year averages of quantities which are themselves
       * means over some period of time (P2) less than a year. The reference time (R) indicates the
       * date and time of the start of a period of time, given by R to R + P2, over which a mean is
       * formed; N indicates the number of such period-means that are averaged together to form
       * the climatological value, assuming that the N period-mean fields are separated by one
       * year. The reference time indicates the start of the N-year climatology.
       *
       * If P1 = 0 then the data averaged in the basic interval P2 are assumed to be continuous, i.e. all available data
       * are simply averaged together.
       *
       * If P1 = 1 (the unit of time octet 18, Code table 4 is not
       * relevant here) then the data averaged together in the basic interval P2 are valid only at the
       * time (hour, minute) given in the reference time, for all the days included in the P2 period.
       * The units of P2 are given by the contents of octet 18 and Code table 4
       */
      case 51 -> "Climatological mean values from RT to (RT + P2)";
      // if (p1 == 0) timeRange += " continuous";
      /*
       * Average of N forecasts (or initialized analyses); each product has forecast period of P1
       * (P1 = 0 for initialized analyses); products have reference times at intervals of P2, beginning
       * at the given reference time
       */
      case 113 -> "Average of N forecasts, intervals = (refTime + i * P2, refTime + i * P2 + P1)";

      /*
       * Accumulation of N forecasts (or initialized analyses); each product has forecast period of
       * P1 (P1 = 0 for initialized analyses); products have reference times at intervals of P2,
       * beginning at the given reference time
       */
      case 114 -> "Accumulation of N forecasts, intervals = (refTime + i * P2, refTime + i * P2 + P1)";

      /*
       * Average of N forecasts, all with the same reference time; the first has a forecast period of
       * P1, the remaining forecasts follow at intervals of P2
       */
      case 115 -> "Average of N forecasts, intervals = (refTime, refTime + P1 + i * P2)";

      /*
       * Accumulation of N forecasts, all with the same reference time; the first has a forecast
       * period of P1, the remaining forecasts follow at intervals of P2
       */
      case 116 -> "Accumulation of N forecasts, intervals = (refTime, refTime + P1 + i * P2)";

      /*
       * Average of N forecasts; the first has a forecast period of P1, the subsequent ones have
       * forecast periods reduced from the previous one by an interval of P2; the reference time for
       * the first is given in octets 13 to 17, the subsequent ones have reference times increased
       * from the previous one by an interval of P2. Thus all the forecasts have the same valid time,
       * given by the initial reference time + P1
       */
      case 117 -> "Average of N forecasts, intervals = (refTime + i * P2, refTime + P1)";

      /*
       * Temporal variance, or covariance, of N initialized analyses; each product has forecast
       * period of P1 = 0; products have reference times at intervals of P2, beginning at the given
       * reference time
       */
      case 118 -> "Temporal variance or covariance of N initialized analyses, timeCoord = (refTime + i * P2)";

      /*
       * Standard deviation of N forecasts, all with the same reference time with respect to the time
       * average of forecasts; the first forecast has a forecast period of P1, the remaining forecasts
       * follow at intervals of P2
       */
      case 119 -> "Standard Deviation of N forecasts, timeCoord = (refTime + P1 + i * P2)";

      // ECMWF "Average of N Forecast" added 11/21/2014. pretend its WMO standard. maybe should move to ecmwf ??
      // see "http://emoslib.sourcearchive.com/documentation/000370.dfsg.2/grchk1_8F-source.html"
      // C Add Time range indicator = 120 Average of N Forecast. Each product
      // C is an accumulation from forecast lenght P1 to forecast
      // C lenght P2, with reference times at intervals P2-P1
      case 120 ->
              "Average of N Forecasts (ECMWF), accumulation from forecast P1 to P2, with reference times at intervals P2-P1";

      // Average of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 123 -> "Average of N uninitialized analyses, intervals = (refTime, refTime + i * P2)";

      // Accumulation of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 124 -> "Accumulation of N uninitialized analyses, intervals = (refTime, refTime + i * P2)";

      /*
       * Standard deviation of N forecasts, all with the same reference time with respect to time
       * average of the time tendency of forecasts; the first forecast has a forecast period of P1,
       * the remaining forecasts follow at intervals of P2
       */
      case 125 -> "Standard deviation of N forecasts, intervals = (refTime, refTime + P1 + i * P2)";
      default -> "Unknown Time Range Indicator " + timeRangeIndicator;
    };

    return timeRange;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get interval [start, end] since reference time in units of timeUnit, must be an interval.
   * 
   * @return interval [start, end]
   */
  public int[] getInterval() {
    return new int[] {start, end};
  }

  /**
   * Get interval size (end - start) in units of timeUnit, only if an interval.
   * 
   * @return interval size
   */
  public int getIntervalSize() {
    return isInterval ? end - start : 0;
  }

  /**
   * Forecast time since reference time in units of timeUnit, only if not an interval.
   * 
   * @return Forecast time
   */
  public int getForecastTime() {
    return forecastTime;
  }

  /**
   * The time unit name (code table 5)
   * 
   * @return time unit name
   */
  public String getTimeTypeName() {
    return cust.getTimeTypeName(timeRangeIndicator);
  }

  /**
   * The time unit statistical type, derived from code table 5)
   * 
   * @return time unit statistical type
   */
  public GribStatType getStatType() {
    return cust.getStatType(timeRangeIndicator);
  }

  /**
   * A string representation of the time coordinate, whether its an interval or not.
   * 
   * @return string representation of the time coordinate
   */
  public String getTimeCoord() {
    if (isInterval()) {
      int[] intv = getInterval();
      return intv[0] + "-" + intv[1];
    }
    return Integer.toString(forecastTime);
  }
}
