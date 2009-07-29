// 2 # 1 "com/donhatchsw/util/Arrays.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/Arrays.prejava"
/*
* Copyright (c) 2005,2006 Don Hatch Software
*/

//
// Arrays.prejava
//
// Author: Don Hatch (hatch@plunk.org)
// This code may be used for any purpose as long as it is good and not evil.
//

package com.donhatchsw.util;

// 19 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 23 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 25 # 15 "com/donhatchsw/util/Arrays.prejava" 2

/**
 * Some common operations on multidimensional arrays:
 * copying, appending, concatenation, flattening, sub-arrays, slicing, indexing,
 * shuffling, and conversion to and from string representations.
 * <p>
 * Reflection and recursion are used to obtain fully general functions
 * that work for ragged arrays of any number of dimensions.
 */

/* not in javadoc for now */
/* <p>
 * XXX should be made to throw appropriate exceptions on illegal arguments (currently some are asserts and some are not checked)
 * <p>
 * XXX wish list for more:
 * <ul>
 *      <li> array compare to given depth using equals </li>
 *      <li> array compare to given depth using == </li>
 *      <li> array toString with depth (then PRINTVEC and PRINTMAT can have more control) </li>
 *      <li> get() taking additional integer argument specifying dimension of array being indexed into </li>
 *      <li> delete an element at given index </li>
 *      <li> linear search using equals </li>
 *      <li> linear search using == </li>
 *      <li> subarray allowing permutation or general lut into original dims
 *      <li> arbitrary slicing </li>
 *      <li> arbitrary index shuffling, flattening, diagonalizing </li>
 * </ul>
 */
public final class Arrays
{
    private Arrays() {} // uninstantiatable

    public static Object subarray(Object array, int starts[], int ns[])
    {
        return _subarray(array, starts, ns, 0);
    } // subarray
        private static Object _subarray(Object array, int starts[], int ns[],
                                        int iDim)
        {
            int nDims = ns.length;
            if (nDims == 0)
                return array;
            Object subarray = java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), ns[0]);
            if (nDims == 1)
                System.arraycopy(array, starts==null?0:starts[0], // from here
                                 subarray, 0, // to here
                                 ns[0]);
            else
            {
                int start = starts==null ? 0 : starts[iDim];
                int n = ns[iDim];
                for (int i = 0; (i) < (n); ++i)
                {
                    Object item = java.lang.reflect.Array.get(array, start+i);
                    Object subItem = _subarray(array, starts, ns, iDim+1);
                    java.lang.reflect.Array.set(subarray, i, subItem);
                }
            }
            return subarray;
        } // subarray
    public static Object subarray(Object array, int start, int n)
    {
        if (true) // set to true to test; a bit heavyweight though
            return subarray(array, new int[]{start}, new int[]{n});
        else
        {
            Object subarray = java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), n);
            System.arraycopy(array, start, subarray, 0, n);
            return subarray;
        }
    } // subarray
    public static Object[] subarray(Object arrayOfArrays[],
                                    int startRow, int nRows,
                                    int startCol, int nCols)
    {
        return (Object[])subarray(arrayOfArrays,
                                  new int[] {startRow, startCol},
                                  new int[] {nRows, nCols});
    } // subarray 2-d
    public static Object[][] subarray(Object arrayOfArraysOfArrays[][],
                                      int startLayer, int nLayers,
                                      int startRow, int nRows,
                                      int startCol, int nCols)
    {
        return (Object[][])subarray(arrayOfArraysOfArrays,
                                    new int[] {startLayer,startRow,startCol},
                                    new int[] {nLayers,nRows,nCols});
    } // subarray 3-d

    public static Object subarray(Object array, int start, int n, boolean which[])
    {
        int subarrayLength = subarrayLength(array, start, n, 1, which);
        Object subarray = java.lang.reflect.Array.newInstance(
                                array.getClass().getComponentType(), subarrayLength);
        int iNew = 0;
        for (int iOld = 0; (iOld) < (n); ++iOld)
        {
            if (which == null
             || which[start+iOld])
            java.lang.reflect.Array.set(subarray, iNew++,
                java.lang.reflect.Array.get(array, start+iOld));
        }
        do { if (!(iNew == subarrayLength)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+119 +"): " + "iNew == subarrayLength" + ""); } while (false);
        return subarray;
    } // subarray with which

    public static Object subarray(Object array, boolean which[])
    {
        if (which == null)
            return array;
        return subarray(array, 0, java.lang.reflect.Array.getLength(array), which);
    } // full subarray with which


    // could just do append(v, new Double(last))
    // but that would be an unnecessary allocation
    public static double[] append(double v[], double last)
    {
        int i, n = v.length;
        double result[] = new double[n+1];
        System.arraycopy(v, 0, result, 0, n);
        result[n] = last;
        return result;
    } // append double
    public static int[] append(int v[], int last)
    {
        int i, n = v.length;
        int result[] = new int[n+1];
        System.arraycopy(v, 0, result, 0, n);
        result[n] = last;
        return result;
    } // append int

    /**
     * Returns a newly allocated array
     * consisting of the old array with the "last" object appended.
     * <br>
     * array can have scalar components,
     * in which case last should be of the corresponding wrapper type.
     */
    public static Object append(Object array,
                                Object last)
    {
        int n = java.lang.reflect.Array.getLength(array);
        Object result = java.lang.reflect.Array.newInstance(
                                array.getClass().getComponentType(), n+1);
        System.arraycopy(array, 0, result, 0, n);
        java.lang.reflect.Array.set(result, n, last);
        return result;
    } // append

    public static Object append2(Object array,
                                 Object secondToLast,
                                 Object last)
    {
        return append(append(array, secondToLast),last); // XXX not efficient
    } // append2

    /**
     * Returns a newly allocated array
     * that is the concatenation of the given arrays.
     */
    public static Object concat(Object array0,
                                Object array1)
    {
        if (false) // can set this to true to test flatten (a bit heavyweight)
            return flatten(new Object[]{array0,array1}, 0, 2);
        else
        {
            int n0 = java.lang.reflect.Array.getLength(array0);
            int n1 = java.lang.reflect.Array.getLength(array1);
            Object newArray = java.lang.reflect.Array.newInstance(
                                        array0.getClass().getComponentType(),
                                        n0 + n1);
            System.arraycopy(array0, 0, newArray, 0, n0);
            System.arraycopy(array1, 0, newArray, n0, n1);
            return newArray;
        }
    } // concat

    public static Object concat3(Object a0, Object a1, Object a2)
    {
        if (false) // can set this to true to test flatten (a bit heavyweight)
            return flatten(new Object[]{a0,a1,a2}, 0, 2);
        else
            return concat(concat(a0,a1),a2);
    }

    public static Object concat4(Object a0, Object a1, Object a2, Object a3)
    {
        if (false) // can set this to true to test flatten (a bit heavyweight)
            return flatten(new Object[]{a0,a1,a2,a3}, 0, 2);
        else
            return concat(concat(a0,a1),concat(a2,a3));
    }

    public static void copy(Object to, Object from, int depth)
    {
        do { if (!(depth >= 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+215 +"): " + "depth >= 1" + ""); } while (false); // XXX should be param check throwing exception
        {
            int n = java.lang.reflect.Array.getLength(from);
            do { if (!(n == java.lang.reflect.Array.getLength(to))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+218 +"): " + "n == java.lang.reflect.Array.getLength(to)" + ""); } while (false);
            if (depth == 1)
                System.arraycopy(from, 0,
                                 to, 0,
                                 n);
            else
                for (int i = 0; (i) < (n); ++i)
                    copy(java.lang.reflect.Array.get(to, i),
                         java.lang.reflect.Array.get(from, i),
                         depth-1);
        }
    } // copy
    public static Object copy(Object from, int depth)
    {
        if (depth == 0)
            return from;
        do { if (!(depth >= 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+234 +"): " + "depth >= 1" + ""); } while (false); // XXX should be param check throwing exception
        {
            int n = java.lang.reflect.Array.getLength(from);
            Object to = java.lang.reflect.Array.newInstance(
                                from.getClass().getComponentType(),
                                n);
            if (depth == 1)
                System.arraycopy(from, 0,
                                 to, 0,
                                 n);
            else
                for (int i = 0; (i) < (n); ++i)
                    java.lang.reflect.Array.set(to, i,
                                copy(java.lang.reflect.Array.get(from,i),
                                     depth-1));
            return to;
        }
    } // copy

    public static boolean sizesMatch(Object a, Object b, int depth)
    {
        // XXX should the null checks come before or after the depth==0 check?  I think this is right but I'm not sure
        if (a==null && b==null)
            return true;
        if (a==null || b==null)
            return false;
        if (depth == 0)
            return true;
        int aLen = java.lang.reflect.Array.getLength(a);
        int bLen = java.lang.reflect.Array.getLength(b);
        if (aLen != bLen)
            return false;
        for (int i = 0; (i) < (aLen); ++i)
            if (!sizesMatch(java.lang.reflect.Array.get(a, i),
                            java.lang.reflect.Array.get(b, i),
                            depth-1))
                return false;
        return true;
    } // sizesMatch

    /**
     * Index into the given array, which is treated as one-dimensional.
     * inds should be a multidimensional array of int (or a single Int).
     * The returned array will have the same dimension and size as inds.
     * <p>
     * XXX should make an enhanced version that lets array
     *     be multidimensional, and allows specifying its dimension,
     *     in which case each 1-dimensional component of indices
     *     should have length equal to that dimension, and is used to 
     *     index into it.
     */
    public static Object getMany(Object array, Object indices)
    {
        Class indicesClass = indices.getClass();

        // Not necessary, but optimize for most common case...
        if (true)
        {
            if (indicesClass == int[].class)
            {
                int ints[] = (int[])indices;
                int n = ints.length;
                Object result = java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), n);
                for (int i = 0; (i) < (n); ++i)
                    java.lang.reflect.Array.set(result, i,
                            java.lang.reflect.Array.get(array, ints[i]));
                return result;
            }
        }

        if (indicesClass == Integer.class)
            return java.lang.reflect.Array.get(array, ((Integer)indices).intValue());
        int n = java.lang.reflect.Array.getLength(indices);
        Object result = java.lang.reflect.Array.newInstance(
                getClassOfArrayOf(array.getClass().getComponentType(), getDim(indices)-1),
                n);
        for (int i = 0; (i) < (n); ++i)
        {
            java.lang.reflect.Array.set(result, i,
                getMany(array, java.lang.reflect.Array.get(indices, i)));
        }
        return result;
    } // getMany

    /**
     * Here is part of what I wanted, above...
     * just need to combine the two.
     */
    public static Object getOne(Object array, int index[])
    {
        int n = index.length;
        for (int i = 0; (i) < (n); ++i)
            array = java.lang.reflect.Array.get(array, index[i]);
        return array;
    } // getOne


    // XXX naming stinks! make this understandible!
    // XXX needs to be differentiated from the more common version
    // XXX (familiar to matlab users I think) getMany,
    // XXX since the prototype is the same
    public static Object getManyOnes(Object array, Object indices)
    {
        if (getDim(indices) == 1)
            return getOne(array, (int[])indices);

        int n = java.lang.reflect.Array.getLength(indices);
        Object[] result = new Object[n]; // XXX cop out! should get the type right! but it's really fuckin confusing me right now
        for (int i = 0; (i) < (n); ++i)
            java.lang.reflect.Array.set(result,i,
                          getManyOnes(array,
                                      java.lang.reflect.Array.get(indices,i)));
        return result;
    } // getMany

    /**
     * remove consecutive duplicate items,
     * as determined by ==.
     * XXX could have more efficient versions for Object[] and all
     * XXX arrays of primitive types, but whatever
     */
    public static int nodupUsingEqualsSymbol(Object newArray,
                                             Object oldArray, int oldN)
    {
        int newN = 0;
        for (int oldI = 0; (oldI) < (oldN); ++oldI)
            if (oldI == 0 || !subEqualsSymbol(oldArray,oldI, oldArray,oldI-1))
                java.lang.reflect.Array.set(newArray, newN++,
                        java.lang.reflect.Array.get(oldArray, oldI));
        return newN;
    } // nodupUsingEqualsSymbol

    public static Object nodupUsingEqualsSymbol(Object oldArray)
    {
        int oldN = java.lang.reflect.Array.getLength(oldArray);
        Object newArray = java.lang.reflect.Array.newInstance(
                                oldArray.getClass().getComponentType(),
                                oldN);
        int newN = nodupUsingEqualsSymbol(newArray, oldArray, oldN);
        return subarray(newArray, 0, newN);
    } // noDupUsingEqualsSymbol


    /**
     * note, array expansions are by a factor of 3/2.
     * <br>
     * XXX maybe shouldn't hard code that?
     */
    public static Object[] insertAtNullOrAppend(Object[] array,
                                                Object item)
    {
        {
            int ind = indexOfUsingEqualsSymbol(array, null);
            if (ind != -1)
            {
                array[ind] = item;
                return array;
            }
        }
        // array is full; expand it!
        {
            int n = array.length;
            int newN = ((n+1)>=(n*3/2)?(n+1):(n*3/2));
            Object newArray[] = (Object[])java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), newN);
            System.arraycopy(array, 0, newArray, 0, n);
            newArray[n] = item;
            return newArray;
        }
    } // insertAtNullOrAppend

    public static int numNonNulls(Object[] array)
    {
        int numNonNulls = 0;
        int i, n = array.length;
        for (i = 0; (i) < (n); ++i)
            if (array[i] != null)
                numNonNulls++;
        return numNonNulls;
    } // numNonNulls


    /**
     * Return the total number of items in array,
     * viewed as a "depth"-dimensional array.
     * <br>
     * depth must be <= the true depth of the array.
     * <dl><dd>
     *     depth=0 means return 1.
     *     <br>
     *     depth=1 means return array.length.
     *     <br>
     *     depth=2 means return array[0].length + ... + array[n-1].length
     * </dd></dl>
     *     etc.
     * XXX JAVADOC GROUP
     */
    public static int arrayLength(Object array,
                                  int depth)
    {
        return arrayLength(array, depth, null);
    } // arrayLength with depth

    public static int arrayLength(Object array, int depth, boolean which[])
    {
        if (depth == 0)
            return 1;
        int n = java.lang.reflect.Array.getLength(array);
        return subarrayLength(array, 0, n, depth, which);
    } // arrayLength with depth and which

    public static int subarrayLength(Object array,
                                     int start, int n,
                                     int depth,
                                     boolean which[]) // XXX should take an arbitrary array, use if it's the right level, otherwise index into it when recursing
    {
        do { if (!(depth >= 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+450 +"): " + "depth >= 1" + ""); } while (false); // XXX should be param check throwing exception
        {
            if (depth == 1)
            {
                if (which == null)
                {
                    // short-cut... not necessary, but quicker
                    // than looping through the whole array incrementing sum by
                    // 1 each time.
                    return n;
                }
                else
                {
                    int sum = 0;
                    for (int i = 0; (i) < (n); ++i)
                        if (which[start+i])
                            sum++;
                    return sum;
                }
            }
        }
        int sum = 0;
        for (int i = 0; (i) < (n); ++i)
        {
            if (which == null
             || which[start+i])
            sum += arrayLength(((Object[])array)[start+i], depth-1);
        }
        return sum;
    } // arrayLength with depth


    public static Object arrayLengths(Object array,
                                      int startDepth,
                                      int furtherDepth)
    {
        // XXX this is of questionable merit
        if (startDepth == 0)
            return new Integer(arrayLength(array, furtherDepth));

        int n = java.lang.reflect.Array.getLength(array);
        if (startDepth == 1)
        {
            int result[] = new int[n];
            for (int i = 0; (i) < (n); ++i)
                result[i] = arrayLength(java.lang.reflect.Array.get(array,i),
                                        furtherDepth);
            return result;
        }

        // XXX this temp stuff is kind of yucky, should figure out the right type to begin with. Also I'm not sure it does the right thing if furtherDepth==0 and/or the component type is int; have to think about it
        Object temp[] = new Object[n];
        for (int i = 0; (i) < (n); ++i)
            temp[i] = arrayLengths(java.lang.reflect.Array.get(array,i),
                                   startDepth-1,
                                   furtherDepth);
        Object result = java.lang.reflect.Array.newInstance(
                                                  temp[0].getClass(), n);
        System.arraycopy(temp, 0, result, 0, n);
        return result;
    } // arrayLengths


    /** XXX maybe should take a class instead of an object? */
    public static int getDim(Object array)
    {
        int dim = 0;
        Class c = array.getClass();
        while ((c = c.getComponentType()) != null)
            dim++;
        return dim;
    } // getDim
    private static Class getClassOfArrayOf(Class clazz, int dim)
    {
        for (int i = 0; (i) < (dim); ++i)
            clazz = java.lang.reflect.Array.newInstance(clazz,0).getClass();
        return clazz;
    } // getClassOfArrayOf

    // XXX should handle negative depth?
    private static Class getComponentType(Class clazz, int depth)
    {
        for (int i = 0; (i) < (depth); ++i)
            clazz = clazz.getComponentType();
        return clazz;
    } // getComponentType

    // Safer than the above, if the array was declared as, for example:
    //    double a = {0,0};
    //    double b = {1,1};
    //    Object array[] = new Object[]{a0,a1,a2,a3};
    // XXX should handle negative depth?
    private static Class getComponentType(Object array, int depth)
    {
        // Try the other way first...
        {
            Class c = getComponentType(array.getClass(), depth);
            if (c != null)
                return c;
        }

        do { if (!(depth > 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+551 +"): " + "depth > 0" + ""); } while (false);
        int n = java.lang.reflect.Array.getLength(array);
        for (int i = 0; (i) < (n); ++i)
        {
            Class c = getComponentType(java.lang.reflect.Array.get(array, i), depth-1);
            if (c != null)
                return c;
        }
        return Object.class; // wild guess
    } // getComponentType

    /**
     * Get the maximum depth that actually occurs in the array,
     * i.e. the "extent".
     * This is in contrast to getDim() which returns the "intent".
     */
    public static int getDimExtent(Object array)
    {
        if (array == null)
            return 0; // XXX maybe
        Class c = array.getClass();
        if (!c.isArray())
            return 0;
        int n = java.lang.reflect.Array.getLength(array);
        int maxComponentDim = 0;
        for (int i = 0; (i) < (n); ++i)
        {
            int componentDim = getDimExtent(java.lang.reflect.Array.get(array,i));
            maxComponentDim = ((maxComponentDim)>=(componentDim)?(maxComponentDim):(componentDim));
        }
        return maxComponentDim + 1;
    } // getDimExtent




    /**
     * Make an array of indices into the given array, at given depth.
     * <p>
     * For example, if depth == 2,
     * <br>
     * then inds[i][j][0] == i
     * <br>
     *  and inds[i][j][1] == j.
     */
    public static Object makeArrayOfIndices(Object array, int depth)
    {
        int currentIndex[] = new int[depth];
        return _makeArrayOfIndices(array, depth, currentIndex);
    } // makeArrayOfIndices

        private static Object _makeArrayOfIndices(Object array, int depth, int currentIndex[])
        {
            if (depth == 0)
                return (int[])Arrays.copy(currentIndex, 1); // ?

            int arrayLength = java.lang.reflect.Array.getLength(array);
            int fullDepth = currentIndex.length;
            int indexIntoCurrentIndex = currentIndex.length - depth;

            if (depth == 1)
            {
                int inds[][] = new int[arrayLength][fullDepth];
                for (int i = 0; (i) < (arrayLength); ++i)
                {
                    currentIndex[indexIntoCurrentIndex] = i;
                    System.arraycopy(currentIndex, 0,
                                     inds[i], 0,
                                     fullDepth);
                }
                return inds;
            }
            else
            {
                do { if (!(depth > 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+625 +"): " + "depth > 1" + ""); } while (false); // XXX should be param check throwing exception
                Object inds[] = (Object[])java.lang.reflect.Array.newInstance(
                                    Arrays.getClassOfArrayOf(int[].class, depth-1),
                                    arrayLength);
                do { if (!(getDim(inds) == depth+1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+629 +"): " + "getDim(inds) == depth+1" + ""); } while (false);
                for (int i = 0; (i) < (arrayLength); ++i)
                {
                    currentIndex[indexIntoCurrentIndex] = i;
                    inds[i] = _makeArrayOfIndices(java.lang.reflect.Array.get(array, i), depth-1, currentIndex);
                }
                return inds;
            }
        } // _makeArrayOfIndices

    /** XXX this isn't thought out very well */
    public static Object removeNulls(Object oldArray[])
    {
        int nOld = oldArray.length;
        int nNew = 0; // and counting
        for (int iOld = 0; (iOld) < (nOld); ++iOld)
            if (oldArray[iOld] != null)
                nNew++;
        Object newArray[] = (Object[])java.lang.reflect.Array.newInstance(oldArray.getClass().getComponentType(), nNew);
        int iNew = 0;
        for (int iOld = 0; (iOld) < (nOld); ++iOld)
        {
            Object oldItem = oldArray[iOld];
            if (oldItem != null)
                newArray[iNew++] = oldItem;
        }
        return newArray;
    } // removeNulls


    /**
     * flatten furtherDepth levels into one level.
     * <p>
     * furtherDepth can be 0, in which case it means expand 0 levels
     * into 1 level by replacing each element x at depth startDepth
     * by the singleton array {x}.
     */
    public static Object flatten(Object array,
                                 int startDepth,
                                 int furtherDepth)
    {
        if (furtherDepth == 1)
            return array; // XXX assumes immutable/sharable, maybe should be a param?
        int nFlat = arrayLength(array, (startDepth==0 ? furtherDepth : 1));

        // if furtherdepth == 2, result component type is array's
        // component type squashed by 1, etc.
        Class componentType = getComponentType(getUnwrappedClass(array), furtherDepth);
        // that might have failed (returned null),
        // if somewhere between the top level
        // and depth is something of type Object[] instead of array
        // of the actual type of the components.
        // In this case, make a greater effort and actually traverse contents...
        if (componentType == null)
            componentType = getComponentType(array, furtherDepth);

        Object flatArray = java.lang.reflect.Array.newInstance(
                       componentType,
                       nFlat);
        if (startDepth == 0)
        {
            int pos = _flatten(flatArray, 0, array, furtherDepth);
            do { if (!(pos == nFlat)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+691 +"): " + "pos == nFlat" + ""); } while (false);
        }
        else
        {
            do { if (!(startDepth >= 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+695 +"): " + "startDepth >= 1" + ""); } while (false); // XXX should be params check
            for (int i = 0; (i) < (nFlat); ++i)
                java.lang.reflect.Array.set(flatArray,i,
                            flatten(java.lang.reflect.Array.get(array,i),
                                    startDepth-1,
                                    furtherDepth));
        }
        return flatArray;
    } // flatten


        private static int _flatten(Object dest,
                                    int pos,
                                    Object array,
                                    int nDimsToFlattenIntoOne)
        {
            if (nDimsToFlattenIntoOne == 0)
            {
                java.lang.reflect.Array.set(dest, pos, array);
                pos++;
            }
            else if (nDimsToFlattenIntoOne == 1)
            {
                int n = java.lang.reflect.Array.getLength(array);
                System.arraycopy(array, 0,
                                 dest, pos,
                                 n);
                pos += n;
            }
            else
            {
                do { if (!(nDimsToFlattenIntoOne >= 2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+726 +"): " + "nDimsToFlattenIntoOne >= 2" + ""); } while (false); // XXX should be params check
                int n = java.lang.reflect.Array.getLength(array);
                for (int i = 0; (i) < (n); ++i)
                    pos = _flatten(dest, pos,
                                   java.lang.reflect.Array.get(array,i),
                                   nDimsToFlattenIntoOne-1);
            }
            return pos;
        } // _flatten

    public static Object flatten(Object array,
                                 int startDepth,
                                 int furtherDepth,
                                 boolean which[])
    {
        return flatten(subarray(array,which), startDepth, furtherDepth);
    } // flatten with which



    /** fill an existing array with a constant */
    public static void fill(Object array, Object item)
    {
        int n = java.lang.reflect.Array.getLength(array);
        for (int i = 0; (i) < (n); ++i)
            java.lang.reflect.Array.set(array, i, item);
    }
    /** allocate a new array and fill it with a constant */
    public static Object fill(int n, Object item)
    {
        Object result = java.lang.reflect.Array.newInstance(
                                            getUnwrappedClass(item), n);
        fill(result, item);
        return result;
    } // fill
    public static int[] fill(int n, int item)
    {
        return (int[])fill(n, new Integer(item));
    }

    public static void reverse(int to[], int from[])
    {
        int n = from.length;
        do { if (!(n == to.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+769 +"): " + "n == to.length" + ""); } while (false);
        if (to == from)
        {
            int temp;
            int halfN = n/2; // rounded down
            for (int i = 0; (i) < (halfN); ++i)
                {temp=(to[i]);to[i]=(to[n-1-i]);to[n-1-i]=(temp);};
        }
        else
        {
            for (int i = 0; (i) < (n); ++i)
                to[i] = from[n-1-i];
        }
    } // reverse, int
    public static void reverse(Object to, Object from)
    {
        int n = java.lang.reflect.Array.getLength(from);
        do { if (!(n == java.lang.reflect.Array.getLength(to))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+786 +"): " + "n == java.lang.reflect.Array.getLength(to)" + ""); } while (false);
        if (to == from)
        {
            int halfN = n/2; // rounded down
            for (int i = 0; (i) < (halfN); ++i)
                swap(to, i, to, n-1-i);
        }
        else
        {
            for (int i = 0; (i) < (n); ++i)
                java.lang.reflect.Array.set(to, i,
                                    java.lang.reflect.Array.get(from, n-1-i));
        }
    } // reverse, general
    public static Object reverse(Object from)
    {
        Object result = java.lang.reflect.Array.newInstance(
                                from.getClass().getComponentType(),
                                java.lang.reflect.Array.getLength(from));
        reverse(result, from);
        return result;
    }

    /**
     * Swaps array0[i0] with array1[i1].
     */
    public static void swap(Object array0, int i0,
                            Object array1, int i1)
    {
        Object temp = java.lang.reflect.Array.get(array0, i0);
        java.lang.reflect.Array.set(array0, i0,
                                    java.lang.reflect.Array.get(array1, i1));
        java.lang.reflect.Array.set(array1, i1, temp);
    }


    /**
     * Return a new Array of length 1,
     * whose single element is the given item
     * (or the corresponding scalar, if item is of a wrapper type).
     */
    public static Object singleton(Object item)
    {
        return fill(1, item);
    } // singleton


    public static int indexOf(boolean Array[], boolean item)
    {
        int i, n = Array.length;
        for (i = 0; (i) < (n); ++i)
            if (Array[i] == item)
                return i;
        return -1;
    }
    public static int indexOf(int Array[], int item)
    {
        int i, n = Array.length;
        for (i = 0; (i) < (n); ++i)
            if (Array[i] == item)
                return i;
        return -1;
    }
    public static int indexOfUsingEqualsSymbol(Object Array[], Object item)
    {
        int i, n = Array.length;
        for (i = 0; (i) < (n); ++i)
            if (Array[i] == item)
                return i;
        return -1;
    }

    /**
     * XXX should take a class instead of an item, right?
     * XXX then need to figure out the isAssignableFrom() thing or whatever.
     */
    public static Class getUnwrappedClass(Object item)
    {
        return item instanceof Number ?
                   item instanceof Byte ? byte.class :
                   item instanceof Double ? double.class :
                   item instanceof Float ? float.class :
                   item instanceof Integer ? int.class :
                   item instanceof Long ? long.class :
                   item instanceof Short ? short.class :
                   item.getClass() :
               item instanceof Boolean ? boolean.class :
               item instanceof Character ? char.class :
               item.getClass();
    } // getUnwrappedClass
    public static Class getWrapperClass(Class c)
    {
        if (!c.isPrimitive())
            return c;
        return c == byte.class ? Byte.class :
               c == double.class ? Double.class :
               c == float.class ? Float.class :
               c == int.class ? Integer.class :
               c == long.class ? Long.class :
               c == short.class ? Short.class :
               c == boolean.class ? Boolean.class :
               c == char.class ? Character.class :
               c; // this should never happen, assuming I have covered all prim types here
    } // getWrapperClass

    // Return whether array0[i0] == array1[i1], using == (not equals()).
    public static boolean subEqualsSymbol(Object array0, int i0,
                                          Object array1, int i1)
    {
        if (false)
        {
            // Test wider types first, so we do widening conversions
            // but not narrowing ones.
            // XXX ARGH-- need to to the following to get proper comparisons:
            // XXX         float/long -> double
            // XXX         float/int -> double
            // XXX oy, this sucks!  should we just convert every number to double?
            // XXX         
            // (XXX not sure about char and byte--
            //  XXX it may be that either is convertible
            //  XXX to the other, so again it doesn't matter --
            //  XXX but then again char is wchar these days isn't it?
            //  XXX so putting that first... haven't tested much of this)


            Class c0 = array0.getClass().getComponentType();
            Class c1 = array1.getClass().getComponentType();

            return c0 == double.class || c1 == double.class ?
                                   java.lang.reflect.Array.getDouble(array0,i0)
                                == java.lang.reflect.Array.getDouble(array1,i1) :
                   c0 == float.class || c1 == float.class ?
                                   java.lang.reflect.Array.getFloat(array0,i0)
                                == java.lang.reflect.Array.getFloat(array1,i1) :
                   c0 == long.class || c1 == long.class ?
                                   java.lang.reflect.Array.getLong(array0,i0)
                                == java.lang.reflect.Array.getLong(array1,i1) :
                   c0 == int.class || c1 == int.class ?
                                   java.lang.reflect.Array.getInt(array0,i0)
                                == java.lang.reflect.Array.getInt(array1,i1) :
                   c0 == short.class || c1 == short.class ?
                                   java.lang.reflect.Array.getShort(array0,i0)
                                == java.lang.reflect.Array.getShort(array1,i1) :
                   c0 == char.class || c1 == char.class ?
                                   java.lang.reflect.Array.getChar(array0,i0)
                                == java.lang.reflect.Array.getChar(array1,i1) :
                   c0 == byte.class || c1 == byte.class ?
                                   java.lang.reflect.Array.getByte(array0,i0)
                                == java.lang.reflect.Array.getByte(array1,i1) :
                   c0 == boolean.class || c1 == boolean.class ?
                                   java.lang.reflect.Array.getBoolean(array0,i0)
                                == java.lang.reflect.Array.getBoolean(array1,i1) :
                   ((Object[])array0)[i0] == ((Object[])array1)[i1];
        }
        else
        {
            do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+942 +"): " + "false" + ""); } while (false); // XXX implement me better! need to convert all numbers to double, I think.   this really sucks
            return false;
        }
    } // subEqualsSymbol

    /**
     * Java 1.2 has java.util.Random.nextInt(n); this works for 1.1.
     * This is basically the code from nextInt(n), from the man page.
     */
    public static int randomIndex(int n, java.util.Random rng)
    {
        if (n<=0)
            throw new IllegalArgumentException("n must be positive");

        if ((n & -n) == n) // i.e., n is a power of 2
            //return (int)((n * (long)rng.next(31)) >> 31);
            return (int)((n * (long)(rng.nextInt()&~(1<<31))) >> 31);

        int bits, val;
        do {
            //bits = next(31);
            bits = rng.nextInt()&~(1<<31);
            val = bits % n;
        } while(bits - val + (n-1) < 0);
        return val;
    } // randomIndex

    /**
     * XXX should probably make optimized versions for int[], double[],
     *     and Object[]
     */
    public static void shuffle(Object array, java.util.Random rng)
    {
        int n = java.lang.reflect.Array.getLength(array);
        for (int i = 1; i < n; ++i)
        {
            int j = randomIndex(i+1, rng);
            if (j != i)
                swap(array, i, array, j);
        }
    } // shuffle

    /** Allowing a different pre,sep,suf for each dimension... */
    public static String toString(Object array,
                                  String preSepSuf[][/*3*/],
                                  int indexIntoPSS)
    {
        if (array == null)
            return "null";
        else if (array.getClass() == String.class)
            return "\""+array.toString()+"\""; // XXX should escapify
        else if (array.getClass() == Character.class)
            return "'"+array.toString()+"."; // XXX should escapify




        if (!array.getClass().isArray())
            return array.toString(); // XXX should double-quote Strings and single-quote Characters, see magiccubend solver which has a decent version... it also does decent stuff with java.util.ArrayList and java.util.Map... and it also deals with the string buffer gracefully instead of creating zillions of them... in short, it's awesome

        String prefix = preSepSuf[indexIntoPSS][0];
        String separator = preSepSuf[indexIntoPSS][1];
        String suffix = preSepSuf[indexIntoPSS][2];

        int n = java.lang.reflect.Array.getLength(array);
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        for (int i = 0; (i) < (n); ++i)
        {
            sb.append(toString(java.lang.reflect.Array.get(array, i),
                               preSepSuf,
                               indexIntoPSS+1));
            if (i+1 < n)
                sb.append(separator);
        }
        sb.append(suffix);
        return sb.toString();
    } // toString

    public static String toString(Object array,
                                  String prefix,
                                  String separator,
                                  String suffix)
    {
        if (array == null)
            return "null";
        return toString(array,
                        (String[][/*3*/])fill(
                                              getDimExtent(array), // getDim intent not safe for this
                                              new String[]{prefix,
                                                           separator,
                                                           suffix}),
                        0);
    }

    public static String toStringCompact(Object array)
    {
        return toString(array, "{", ",", "}");
    } // toStringCompact

    /** XXX might I want even the individual elements on separate lines? */
    public static String toStringNonCompact(Object array,
                                            String indentString,
                                            String indentIncr)
    {
        String newline = System.getProperty("line.separator"); // XXX grumble
        int nDims = getDimExtent(array); // getDim intent not safe for this
        String pss[][] = new String[nDims][/*3*/];
        String levelIndentString = indentString;
        for (int iDim = 0; (iDim) < (nDims); ++iDim)
        {
            String pre, sep, suf;
            if (nDims-iDim == 1)
            {
                pre = levelIndentString + "{";
                sep = ",";
                suf = "}";
            }
            else
            {
                pre = levelIndentString + "{" + newline;
                sep = "," + newline;
                suf = newline + levelIndentString + "}";
            }
            pss[iDim] = new String[] {pre,sep,suf};
            levelIndentString += indentIncr; // for next iteration
        }
        return toString(array, pss, 0);
    } // toStringNonCompact

    public static Object fromString(String s)
        throws java.text.ParseException
    {
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        Object obj = new ArrayFormat().parseObjectSucceedOrThrowAnException(s, pos, s.length());
        return obj;
    } // fromString





    /**
     * Text format for printing and parsing arrays.
     * This is rather awkward to use by itself;
     * the most convenient way to print or parse a single array
     * is by using Arrays.toString(Object array) for printing,
     * or Arrays.fromString(String s) for parsing.
     */
    public static class ArrayFormat
        extends java.text.Format
    {
        //
        // Required by java.text.Format...
        // XXX JAVADOC GROUP
        //
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos)
            {
                toAppendTo.append(toStringCompact(obj));
                // XXX I don't understand FieldPosition
                return toAppendTo;
            } // format
            // On failure, returns null and leaves pos unchanged.
            public Object parseObject(String s, java.text.ParsePosition pos)
            {
                int startPos = pos.getIndex();
                try
                {
                    return parseObjectSucceedOrThrowAnException(s, pos, s.length());
                }
                catch (java.text.ParseException e)
                {
                    pos.setIndex(startPos);
                    return null;
                }
            } // parseObject

        //
        // ArrayFormat-specific...
        // XXX note these can throw an exception,
        // XXX whereas the above one can't. bleah. what a mess.
        // XXX JAVADOC GROUP
        //
            public Object parseObjectSucceedOrThrowAnException(String s, java.text.ParsePosition pos, int end)
                throws java.text.ParseException
            {
                return parseObject(s, pos, end,
                                   guessClass(s, pos.getIndex(), s.length()));
            } // parseObjectSucceedOrThrowAnException

            private String removeComments(String in)
            {
                StringBuffer sb = new StringBuffer();
                boolean inSlashSlashComment = false;
                int nIn = in.length();
                for (int iIn = 0; (iIn) < (nIn); ++iIn)
                {
                    char c = in.charAt(iIn);
                    if (c == '/'
                     && iIn+1 < nIn
                     && in.charAt(iIn+1) == '/')
                        inSlashSlashComment = true;
                    else if (c == '\n')
                        inSlashSlashComment = false;

                    if (!inSlashSlashComment)
                        sb.append(c);
                }
                return sb.toString();
            } // removeComments

            public Object parseObject(String s,
                                      java.text.ParsePosition pos,
                                      int end,
                                      Class clazz)
                throws java.text.ParseException
            {
                s = removeComments(s); // XXX eek, should only do this at top level
                Object obj = null;
                if (clazz.isArray())
                {

                    int i = pos.getIndex();
                    do {while ((i) < (end) && Character.isWhitespace(s.charAt(i))) i++;} while (false);
                    if (end-i >= 4
                     && s.startsWith("null", i))
                    {
                        i += 4;
                        obj = null;
                    }
                    else if (end-i >= 1 && s.charAt(i) == '{')
                    {
                        i++;
                        do {while ((i) < (end) && Character.isWhitespace(s.charAt(i))) i++;} while (false);
                        if (end-i >= 1 && s.charAt(i) == '}')
                        {
                            i++;
                            obj = java.lang.reflect.Array.newInstance(clazz.getComponentType(),0);
                        }
                        else
                        {
                            java.util.Vector v = new java.util.Vector();
                            while (true)
                            {
                                pos.setIndex(i);
                                v.addElement(parseObject(s, pos,
                                                         end,
                                                         clazz.getComponentType()));
                                i = pos.getIndex();

                                do {while ((i) < (end) && Character.isWhitespace(s.charAt(i))) i++;} while (false);

                                // XXX this seems way too convoluted; should think about more streamlined logic
                                if (end-i >= 1 && s.charAt(i) == ',')
                                {
                                    i++;
                                    do {while ((i) < (end) && Character.isWhitespace(s.charAt(i))) i++;} while (false);
                                    if (end-i >= 1 && s.charAt(i) == '}')
                                    {
                                        i++;
                                        break;
                                    }
                                }
                                else if (end-i >= 1 && s.charAt(i) == '}')
                                {
                                    i++;
                                    break;
                                }
                                else
                                {
                                    if (end-i >= 1)
                                        throw new java.text.ParseException("unexpected char '"+s.charAt(i)+"' between array elements", i);
                                    else
                                        throw new java.text.ParseException("unmatched '{'", i);
                                }
                            }
                            obj = java.lang.reflect.Array.newInstance(clazz.getComponentType(),v.size());
                            if (false) // lame, it doesn't do the right thing for wrappers
                            {
                                v.copyInto((Object[])obj);
                            }
                            else
                            {
                                int n = v.size();
                                for (int ii = 0; (ii) < (n); ++ii)
                                    java.lang.reflect.Array.set(obj,ii,
                                                                v.elementAt(ii));
                            }
                        }
                    }
                    else
                        throw new java.text.ParseException("can't parse array", i);
                    pos.setIndex(i);
                }
                else if (clazz == boolean.class)
                {
                    int i = pos.getIndex();
                    if (end-i >= 4
                     && s.startsWith("true", i))
                    {
                        i += 4;
                        obj = new Boolean(true);
                    }
                    else if (end-i >= 5
                          && s.startsWith("false", i))
                    {
                        i += 5;
                        obj = new Boolean(false);
                    }
                    else
                    {
                        throw new java.text.ParseException("Unmatched '{'", i);
                    }
                    pos.setIndex(i);
                }
                else if (clazz == char.class)
                {
                    // XXX should really parse it! why is there no Character.valueOf? (I only checked 1.1; maybe they added one afterwards)
                    int i = pos.getIndex();
                    do { if (!(end-i >= 3 && s.charAt(i+0) == '\'' && s.charAt(i+2) == '\'')) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+1263 +"): " + "end-i >= 3 && s.charAt(i+0) == '\\'' && s.charAt(i+2) == '\\''" + ""); } while (false);


                    obj = new Character(s.charAt(i+1));
                    i += 3;
                    pos.setIndex(i);
                }
                else
                {
                    Class wrapperClass = getWrapperClass(clazz);
                    if (!Number.class.isAssignableFrom(wrapperClass)) // if Number is not a superclass of wrapperClass
                        throw new java.text.ParseException("Don't know how to parse class "+clazz.getName()+"",pos.getIndex());

                    java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance();
                    // XXX should set parseIntegerOnly if clazz is an integral type, probably
                    int startPos = pos.getIndex();
                    Number number = parseTakingParsePositionAndThrowingParseException(numberFormat, s, pos);
                    if (pos.getIndex() > end)
                    {
                        // Argh, ran off the end.
                        // make a copy of the string region
                        // and try again.
                        pos.setIndex(startPos);
                        s = s.substring(0, end);
                        number = parseTakingParsePositionAndThrowingParseException(numberFormat, s, pos);
                    }

                    // XXX AAAAAAARGH!!!!!!!!! integer parsing eats up commas!!!
                    // XXX this workaround is INCREDIBLY inefficient!
                    {
                        int commaInd = s.indexOf(',', startPos);
                        if (commaInd != -1
                         && commaInd < end
                         && pos.getIndex() > commaInd)
                        {
                            pos.setIndex(startPos);
                            s = s.substring(0, commaInd);
                            number = parseTakingParsePositionAndThrowingParseException(numberFormat, s, pos);
                        }
                    }

                    if (number.getClass() == wrapperClass)
                        obj = number;
                    else
                    {
                        if (wrapperClass == Byte.class)
                            obj = new Byte(number.byteValue());
                        else if (wrapperClass == Double.class)
                            obj = new Double(number.doubleValue());
                        else if (wrapperClass == Float.class)
                            obj = new Float(number.floatValue());
                        else if (wrapperClass == Integer.class)
                            obj = new Integer(number.intValue());
                        else if (wrapperClass == Long.class)
                            obj = new Long(number.longValue());
                        else if (wrapperClass == Short.class)
                            obj = new Short(number.shortValue());
                        else
                            throw new java.text.ParseException("Don't know how to parse class "+clazz.getName()+"",startPos);
                    }
                }

                return obj;
            } // parseObject

        //
        // Private utilities...
        // XXX JAVADOC GROUP
        //

            private static Class guessClass(String s, int start, int end)
                throws java.text.ParseException
            {
                /**
                 * Guess the primitive type.
                 * If all arrays are ultimately empty, int will be used.
                 */
                Class primType = (s.indexOf("true") != -1 ? boolean.class : // XXX should ignore case
                                  s.indexOf("false") != -1 ? boolean.class : // XXX should ignore case
                                  s.indexOf(".") != -1 ? double.class :
                                  s.indexOf("E") != -1 ? double.class :
                                  s.indexOf("e") != -1 ? double.class :
                                  s.indexOf("f") != -1 ? float.class :
                                  s.indexOf("F") != -1 ? float.class :




                                  s.indexOf("'") != -1 ? char.class : int.class);
                int nDims = braceDepth(s, start, end);

                /**
                 * Guess the dimension.
                 * If all arrays are ultimately empty,
                 * the maximum brace depth will be used.
                 */
                Class clazz;
                {
                    // XXX is there a better way to do this?
                    clazz = primType;
                    for (int iDim = 0; (iDim) < (nDims); ++iDim)
                        clazz = java.lang.reflect.Array.newInstance(clazz,0).getClass();
                }
                return clazz;
            } // guessClass
            private static int braceDepth(String s, int start, int end)
                throws java.text.ParseException
            {
                if (end-start == 0)
                    return 0; // parse will probably fail

                do {while ((start) < (end) && Character.isWhitespace(s.charAt(start))) start++;} while (false);

                if (end-start >= 4
                 && s.startsWith("null", start))
                    return 1;

                if (start < end
                 && s.charAt(start) == '{')
                {
                    int level = 0, maxLevel = 0;
                    for (int i = start; i < end; ++i)
                    for (; i < end; ++i)
                    {
                        char c = s.charAt(i);
                        if (c == '{')
                        {
                            level++;
                            maxLevel = ((level)>=(maxLevel)?(level):(maxLevel));
                        }
                        else if (c == '}')
                        {
                            level--;
                            if (level == 0)
                                break;
                        }
                        else if (end-i >= 4
                              && s.startsWith("null", i))
                            maxLevel = ((level+1)>=(maxLevel)?(level+1):(maxLevel));
                    }
                    if (level != 0)
                        throw new java.text.ParseException("Unmatched '{'", start);
                    return maxLevel;
                }
                return 0;
            } // braceDepth

            /**
             * Format.parseObject can either take a ParsePosition
             * or throw a ParseException, but not both--
             * this is SO FUCKED UP!!
             * (I am regretting using the java.text stuff at all.)
             * Anyway here's a version that does both.
             */
            private static Object parseObjectTakingParsePositionAndThrowingParseException(java.text.Format format, String s, java.text.ParsePosition pos)
                throws java.text.ParseException
            {
                Object obj = format.parseObject(s, pos);
                if (obj != null)
                    return obj;
                // failed; call the version that throws the exception
                int startPos = pos.getIndex();
                String subS = s.substring(startPos);
                try
                {
                    obj = format.parseObject(subS);
                }
                catch (java.text.ParseException e)
                {
                    // re-throw with corrected error offset.
                    throw new java.text.ParseException(e.getMessage(),
                                                       startPos + e.getErrorOffset());
                }
                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+1434 +"): " + "false" + ""); } while (false); // should never get here
                return null;
            } // parseObjectTakingParsePositionAndThrowingParseException
            private static Number parseTakingParsePositionAndThrowingParseException(java.text.NumberFormat numberFormat, String s, java.text.ParsePosition pos)
                throws java.text.ParseException
            {
                return (Number)parseObjectTakingParsePositionAndThrowingParseException(numberFormat, s, pos);
            } // parseTakingParsePositionAndThrowingParseException


    } // class ArrayFormat

    /*
     * XXX is there a different kind of ParseException that is a subclass of RuntimeException? java.lang.NumberFormatException? should use that instead? drat, it doesn't give a position, but could just encode that in the string.
     * <br>
     * XXX this is a retarded place to put this comment
     */
    /**
     * Runs a few sanity tests on various Arrays class functions.
     */
    public static void main(String args[])
    {
        if (true)
        {
            Integer thisZero = new Integer(0);
            Integer thatZero = new Integer(0);
            Integer thisOne = new Integer(1);
            Integer thatOne = new Integer(1);
            System.out.println("thisZero == thatZero" + " = " + (thisZero == thatZero));
            System.out.println("thisZero == thatOne" + " = " + (thisZero == thatOne));
            System.out.println("thisZero.equals(thatZero)" + " = " + (thisZero.equals(thatZero)));
            System.out.println("thisZero.equals(thatOne)" + " = " + (thisZero.equals(thatOne)));
            System.out.println("5.f == (long)5" + " = " + (5.f == (long)5));
            System.out.println(".5f == (long)5" + " = " + (.5f == (long)5));
            System.out.println("((long)1<<(long)34) == ((float)((float)(1<<17)*(float)(1<<17)))" + " = " + (((long)1<<(long)34) == ((float)((float)(1<<17)*(float)(1<<17)))));

            System.out.println("(((long)1<<(long)34) + (long)1) == (float)((float)((float)(1<<17)*(float)(1<<17)) + (float)1)" + " = " + ((((long)1<<(long)34) + (long)1) == (float)((float)((float)(1<<17)*(float)(1<<17)) + (float)1)));

            // ooh, yuck! that returned true but it shouldn't have!
            // I think the long is getting converted to float!

            {
                // From  http://www.jchq.net/discus/messages/1/51247.html?1060615561
                long ll=Long.MAX_VALUE;
                float ff;

                System.out.println("Before ll is "+ll);
                ff = ll;
                System.out.println("ff is "+ff);
                ll = (long) ff;
                System.out.println("After ll is "+ll);
                ff = Float.intBitsToFloat(Float.floatToIntBits(ff));
                System.out.println("ff is "+ff);
                ff = Float.intBitsToFloat(Float.floatToIntBits(ff)+1);
                System.out.println("ff+ is "+ff);
            }
            {
                // From  http://www.jchq.net/discus/messages/1/51247.html?1060615561
                long ll=Long.MAX_VALUE/2;
                float ff;

                System.out.println("Before ll is "+ll);
                ff = ll;
                System.out.println("ff is "+ff);
                ll = (long) ff;
                System.out.println("After ll is "+ll);
            }
            /*
            {
                Here is an explanation:
                when ll is converted to float, it loses precision
                and turns into 9223372036854775808 (= 2^63 exactly).
                This value is bigger than any long,
                so when converted back to long, it turns into
                Long.MAX_VALUE = 9223372036854775807 again.

                If you change your code to say Long.MAX_VALUE/2
                instead of Long.MAX_VALUE, it will demonstrate
                the loss of precision like you expect.
            }
            */
            return;
        }

        if (true)
        {
            double A[] = {0,0};
            double B[] = {1,1};
            double C[] = {2,2};
            double D[] = {3,3};
            System.out.println("concat(A,B)" + " = " + Arrays.toStringCompact(concat(A,B)));
            System.out.println("concat3(A,B,C)" + " = " + Arrays.toStringCompact(concat3(A,B,C)));
            System.out.println("concat4(A,B,C,D)" + " = " + Arrays.toStringCompact(concat4(A,B,C,D)));
        }


        if (true)
        {
            double a[][][] = {
                {
                    {0,1},
                },
                {
                    {6,5,4,3},
                },
                {
                },
                {
                    {6,5,4,3},
                    {0},
                    {},
                },
            };
            System.out.println("a" + " = " + Arrays.toStringCompact(a));
            System.out.println("makeArrayOfIndices(a,0)" + " = " + Arrays.toStringCompact(makeArrayOfIndices(a,0)));
            System.out.println("makeArrayOfIndices(a,1)" + " = " + Arrays.toStringCompact(makeArrayOfIndices(a,1)));
            System.out.println("makeArrayOfIndices(a,2)" + " = " + Arrays.toStringCompact(makeArrayOfIndices(a,2)));
            System.out.println("makeArrayOfIndices(a,3)" + " = " + Arrays.toStringCompact(makeArrayOfIndices(a,3)));
            System.out.println("flatten(a,0,0)" + " = " + Arrays.toStringCompact(flatten(a,0,0)));
            System.out.println("flatten(a,0,1)" + " = " + Arrays.toStringCompact(flatten(a,0,1)));
            System.out.println("flatten(a,0,2)" + " = " + Arrays.toStringCompact(flatten(a,0,2)));
            System.out.println("flatten(a,0,3)" + " = " + Arrays.toStringCompact(flatten(a,0,3)));
            System.out.println("flatten(a,1,0)" + " = " + Arrays.toStringCompact(flatten(a,1,0)));
            System.out.println("flatten(a,1,1)" + " = " + Arrays.toStringCompact(flatten(a,1,1)));
            System.out.println("flatten(a,1,2)" + " = " + Arrays.toStringCompact(flatten(a,1,2)));
            System.out.println("flatten(a,2,0)" + " = " + Arrays.toStringCompact(flatten(a,2,0)));
            System.out.println("flatten(a,2,1)" + " = " + Arrays.toStringCompact(flatten(a,2,1)));
            System.out.println("flatten(a,3,0)" + " = " + Arrays.toStringCompact(flatten(a,3,0)));
        }

        if (true)
        {
            String s = "  {  {  {  1  ,  2  }  ,  null  }  }  ";
            System.out.println("s" + " = " + (s));
            java.text.ParsePosition pos = new java.text.ParsePosition(0);
            try
            {
                Object b = new ArrayFormat().parseObjectSucceedOrThrowAnException(s, pos, s.length());
                System.out.println("b" + " = " + Arrays.toStringCompact(b));
            }
            catch (java.text.ParseException e)
            {
                System.err.println("Oh no! (at position "+e.getErrorOffset()+")");
                e.printStackTrace();
            }
        }


        if (true)
        {
            int a[][][] = {
                {
                    {1,2,3},
                    {4,5,6},
                },
                null,
                {
                    null,
                    {7,8,9},
                    null,
                    null,
                    {},
                    null,
                },
                {},
            };
            String s = toStringCompact(a);
            System.out.println("s" + " = " + (s));
            java.text.ParsePosition pos = new java.text.ParsePosition(0);
            try
            {
                Object b = new ArrayFormat().parseObjectSucceedOrThrowAnException(s, pos, s.length());
                System.out.println("b" + " = " + Arrays.toStringCompact(b));
            }
            catch (java.text.ParseException e)
            {
                System.err.println("Oh no! (at position "+e.getErrorOffset()+")");
                e.printStackTrace();
            }
        }

        if (true)
        {
            java.util.Random rng = new java.util.Random(0);
            for (int i = 0; (i) < (100); ++i)
            {
                if (i == 0)
                    continue;
                System.out.println(i+":");
                for (int j = 0; (j) < (100); ++j)
                {
                    int n = randomIndex(i, rng);
                    System.out.print(" " + randomIndex(i, rng));
                    do { if (!(((0 <=(n))&&((n)< i)))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrays.prejava"+"("+1627 +"): " + "INRANGE(0 <=, n, < i)" + ""); } while (false);
                }
                System.out.println();
            }
        }
    } // main

} // class Arrays
