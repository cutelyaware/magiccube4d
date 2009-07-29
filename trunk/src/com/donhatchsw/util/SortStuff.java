// 2 # 1 "com/donhatchsw/util/SortStuff.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/SortStuff.prejava"
package com.donhatchsw.util;

/**
* Functions for qsorting and binary searching, suitable
* for use in any version of java.
* The reason for this is that Comparator and sorting stuff
* didn't come until Java 1.2.
*/
public class SortStuff
{
    private SortStuff() {} // uninstantiatable

    /** Caller-supplied Object comparison function to be used for sorting and searching. */
    public interface Comparator
    {
        /** Should return an integer less than, equal to, or greater than zero if the first argument is considered to be respectively less than, equal to, or greater than the second. */
        int compare(Object a, Object b);
    }
    /** Caller-supplied int comparison function to be used for sorting and searching. */
    public static class IntComparator
    {
        /** Should return an integer less than, equal to, or greater than zero if the first argument is considered to be respectively less than, equal to, or greater than the second.  Default implementation is the standard order. */
        public int compare(int i, int j)
        {
            return i < j ? -1 :
                   i > j ? 1 : 0;
        }
    }
    /** Caller-supplied double comparison function to be used for sorting and searching. */
    public static class DoubleComparator
    {
        /** Should return an integer less than, equal to, or greater than zero if the first argument is considered to be respectively less than, equal to, or greater than the second. Default implementation is the standard order. */
        public int compare(double x, double y)
        {
            return x < y ? -1 :
                   x > y ? 1 : 0;
        }
    }

    /** Sorts the array of Objects using the given Comparator. */
    public static void sort(Object list[], Comparator comparator)
    {
        sortRange(list, 0, list.length-1, comparator);
    }
    /** Sorts the array of doubles into increasing order. */
    public static void sort(double list[])
    {
        sort(list, new DoubleComparator());
    }
    /** Sorts the array of ints into increasing order. */
    public static void sort(int list[])
    {
        sort(list, new IntComparator());
    }
    /** Sorts the array of doubles using the given DoubleComparator. */
    public static void sort(double list[], final DoubleComparator comparator)
    {
        double ptrlist[][] = new double[list.length][1];
        int i;
        for (i = 0; i < list.length; ++i)
            ptrlist[i][0] = list[i];
        sort(ptrlist, new Comparator() {
            public int compare(Object a, Object b)
            {
                return comparator.compare(((double[])a)[0],((double[])b)[0]);
            }
        });
        for (i = 0; i < list.length; ++i)
            list[i] = ptrlist[i][0];
    }
    /** Sorts the array of ints using the given IntComparator. */
    public static void sort(int list[], final IntComparator comparator)
    {
        int ptrlist[][] = new int[list.length][1];
        int i;
        for (i = 0; i < list.length; ++i)
            ptrlist[i][0] = list[i];
        sort(ptrlist, new Comparator() {
            public int compare(Object a, Object b)
            {
                return comparator.compare(((int[])a)[0],((int[])b)[0]);
            }
        });
        for (i = 0; i < list.length; ++i)
            list[i] = ptrlist[i][0];
    }


    //
    // The actual recursive algorithm...
    // XXX actually it's messed up that this is public, because the interpretation of j is surprising-- it's the last index rather than the length
    //
        public static void sortRange(Object list[], int i, int j, Comparator comparator)
        {
            if (j <= i)
                return;
            int k = (i + j) / 2;
            swap(list, k, j);
            int l = partitionRange(list, i - 1, j, list[j], comparator);
            swap(list, l, j);
            if(l - i > 1)
                sortRange(list, i, l - 1, comparator);
            if(j - l > 1)
                sortRange(list, l + 1, j, comparator);
        }

        private static void swap(Object list[], int i, int j)
        {
            Object temp = list[j];
            list[j] = list[i];
            list[i] = temp;
        }

        private static int partitionRange(Object list[], int i, int j, Object object, Comparator comparator)
        {
            do
            {
                while (comparator.compare(list[++i], object) < 0)
                    ;
                while (j > 0 && comparator.compare(list[--j], object) > 0)
                    ;
                swap(list, i, j);
            } while(i < j);
            swap(list, i, j);
            return i;
        }


    /**
    *  Binary search, using comparator.
    */
    public static int bsearch(Object list[], Object item, Comparator comparator)
    {
        int lo = 0; // lowest possible
        int hi = list.length-1; // highest possible
        while (lo <= hi)
        {
            int mid = (lo+hi)/2;
            int disposition = comparator.compare(list[mid], item);
            if (disposition < 0) // list[mid] is too low
                lo = mid+1;
            else if (disposition > 0) // list[mid] is too high
                hi = mid-1;
            else
                return mid; // list[mid] is just right
        }
        return -1; // failed
    } // bsearch

    /**
    *  Remove consecutive duplicate items,
    *  as determined by comparator.  Only operates on the first oldN
    *  elements.  Returns the number of resulting elements.
    */
    public static int nodup(Object list[], int oldN, Comparator comparator)
    {
        int newN = 0;
        for (int oldI = 0; oldI < oldN; oldI++)
        {
            if (newN == 0
             || comparator.compare(list[oldI], list[newN-1]) != 0)
                list[newN++] = list[oldI];
        }
        return newN;
    } // nodup
    /**
    *  Remove consecutive duplicate items,
    *  as determined by comparator.  Returns the number of resulting elements.
    */
    public static int nodup(Object list[], Comparator comparator)
    {
        return nodup(list, list.length, comparator);
    } // nodup

} // SortStuff
