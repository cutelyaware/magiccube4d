// 2 # 1 "com/donhatchsw/util/FuzzyPointHashTable.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/FuzzyPointHashTable.prejava"
/*
* Copyright (c) 2005,2006 Don Hatch Software
*/

package com.donhatchsw.util;

/**
*  Spatial hash table where the key is a double[], with fuzzy equality.
*  <p>
*  Two doubles a,b are considered equal if |a-b| &le; littleEps,
*  and unequal if |a-b| > bigEps.
*  If a pair of doubles is encountered that are neither equal
*  nor unequal (i.e. their difference is in the forbidden range (littleEps,bigEps] ),
*  then a FuzzyException is thrown.
*  <p>
*  Two double[]s are considered equal if all their coords are equal (using
*  the above fuzzy definition of equal),
*  and unequal if any of their coords are unequal
*  (or if the arrays have different sizes).
*  If a pair of double[]s is encountered that are neither equal
*  nor unequal, then a FuzzyException is thrown.
*  <p>
*  Depends on classes:
*  <ul>
*       <li> VecMath </li>
*       <li> Arrays </li>
*  </ul>
* XXX only depends indirectly on Arrays, and only uses VecMath.toString();
* XXX that dependency could be easily removed.
*/

// Doesn't derived from anything,
// because I only felt like implementing a small number of methods (get and put).
public class FuzzyPointHashTable
{
    double bucketSize;
    double littleEps;
    double bigEps;
    double eps; // (littleEps+bigEps)/2
    double invBucketSize; // 1./bucketSize
    java.util.Hashtable hashtable;

    /**
    * Construct a fuzzy point hash table.
    *
    * The params are required to satisfy:
    * <pre>
    *     0 &le; littleEps &le; bigEps &le; bucketSize/1,000,000</pre>
    * BucketSize should be chosen somewhere around the smallest distance
    * ever expected between two distince points, so that it would be
    * surprising if two or more points ever fell into the same bucket;
    * this is not strictly required but the hash table will not be efficient
    * if too many points end up in a single bucket.
    * However; bucketSize must be much much larger than bigEps.
    *
    * The following are typical values:
    * <pre>
    *     littleEps=1e-12
    *     bigEps=1e-10
    *     bucketSize=1/1024.
    * </pre>
    */
    public FuzzyPointHashTable(double littleEps,
                               double bigEps,
                               double bucketSize)
    {
        // Input restriction: 0 <= littleEps <= bigEps <= bucketSize/1,000,000
        if (!(0 <= littleEps))
            throw new IllegalArgumentException("FuzzyPointHashTable: littleEps = "+littleEps+", must be >= 0");
        if (!(littleEps <= bigEps))
            throw new IllegalArgumentException("FuzzyPointHashTable: littleEps = "+littleEps+", bigEps = "+bigEps+", out of order");
        //if (!(1e6*bigEps <= bucketSize))
        if (!(1e4*bigEps <= bucketSize))
            throw new IllegalArgumentException("FuzzyPointHashTable: bigEps = "+bigEps+", bucketSize = "+bucketSize+", bucketSize is not enough bigger than bigEps");

        this.bucketSize = bucketSize;
        this.littleEps = littleEps;
        this.bigEps = bigEps;

        this.eps = (littleEps+bigEps)*.5;
        this.invBucketSize = 1./bucketSize;

        this.hashtable = new java.util.Hashtable();
    } // ctor

    private class FuzzyPoint
    {
        private double point[];
        public FuzzyPoint(double point[])
        {
            this.point = point;
        }
        public boolean equals(Object that)
        {
            double thatPoint[] = ((FuzzyPoint)that).point;
            if (point.length != thatPoint.length)
                return false;
            boolean someonesBiggerThanLittleEps = false;
            for (int i = 0; i < point.length; ++i)
            {
                double diff = Math.abs(point[i]-thatPoint[i]);
                if (diff > bigEps)
                    return false;
                if (diff > littleEps)
                    someonesBiggerThanLittleEps = true;
            }
            if (someonesBiggerThanLittleEps)
            {
                throw new FuzzyException("FuzzyPoint.equals: "+VecMath.toString(point)+" is neither equal nor unequal to "+VecMath.toString(thatPoint)+", using littleEps="+littleEps+", bigEps="+bigEps+"");
            }
            return true;
        } // equals
        public int hashCode()
        {
            int hash = 47;
            for (int i = 0; i < point.length; ++i)
            {
                //
                // We know coord is now in the interval
                //     [gridLine-bigEps, gridLine+bucketSize-bigEps),
                // give or take a bit or so of floating point roundoff error.
                //
                // Check the fuzziness assumption, which says, additionally,
                // that coord must be in either the closed interval:
                //     [gridLine-littleEps, gridLine+littleEps]
                // or the open interval:
                //     (gridLine+bigEps, gridLine+bucketSize-bigEps).
                // i.e. it can't be in either of the forbidden zones
                //     [gridLine-bigEps, gridLine-littleEps)
                // or  (gridLine+littleEps, gridLine+bigEps].
                //
                // I.e. the real line is partitioned into:
                //     (-infinity, gridLine-bigEps)                  impossible
                //     [gridLine-bigEps, gridLine-littleEps)         illegal
                //     [gridLine-littleEps, gridLine+littleEps]      legal
                //     (gridLine+littleEps, gridLine+bigEps]         illegal
                //     (gridLine+bigEps, gridLine+bucketSize-bigEps) legal
                //     [gridLine+bucketSize-bigEps, infinity)        impossible
                //
                //                 illegal
                //                 |   legal
                //                 |   |   illegal
                //                 |   |   |
                //   impossible )[   )[|](   ](    legal    )[ impossible
                //               |    |||    |               |
                //               |    |||    |               gridLine+bucketSize-bigEps
                //               |    |||    gridLine+bigEps
                //               |    ||gridLine+littleEps
                //               |    |gridLine
                //               |    gridLine-littleEps
                //               gridLine-bigEps
                double coord = point[i];
                int gridIndex = (int)Math.floor((coord+bigEps) * invBucketSize);
                double gridLine = gridIndex * bucketSize;

                double diff = Math.abs(coord - gridLine);
                if (littleEps < diff && diff <= bigEps)
                    throw new FuzzyException("FuzzyPoint.hashCode: coord "+coord+" is neither equal nor unequal to grid line "+gridLine+", using littleEps="+littleEps+", bigEps="+bigEps+", bucketSize="+bucketSize+"");
                hash = hash*3 + gridIndex;
            }
            return hash;
        } // hashCode
    } // private class FuzzyPoint


    private FuzzyPoint scratchForGet = new FuzzyPoint(new double[0]);

    /**
    * Gets the value assocated with key, or null if there is none.
    **/
    public Object get(double key[])
    {
        scratchForGet.point = key; // XXX hey, why can I do this? scratchForGet.point is supposed to be private!!!
        return hashtable.get(scratchForGet);
    }

    /**
    * Puts the value into the table with given key,
    * returning the previous value (or null if there was none)
    */
    public Object put(double key[],
                      Object value)
    {
        return hashtable.put(new FuzzyPoint(key), value);
    }

    /**
    * Gets thrown when the fuzzyiness assumption is violated.
    * The fuzziness assumption is that for any two points a,b,
    * the max-norm-distance between a and b
    * is either &le; littleEps, or > bigEps.
    * I.e. this exception gets thrown if any max-norm
    * distance between two points is in the forbidden interval (littleEps, bigEps].
    */
    public static class FuzzyException extends RuntimeException
    {
        FuzzyException(String description)
        {
            super(description);
        }
    } // FuzzyException

} // class SpatialHashtable
