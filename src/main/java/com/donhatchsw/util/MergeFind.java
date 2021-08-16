// 2 # 1 "com/donhatchsw/util/MergeFind.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/MergeFind.prejava"
package com.donhatchsw.util;
// http://www.cs.cmu.edu/afs/cs.cmu.edu/academic/class/15451-f00/www/lectures/lect1024
public class MergeFind
{
    private int parent[];
    private int rank[]; // rank[i] is number of fair fights i has won
    public MergeFind(int n)
    {
        parent = new int[n];
        rank = new int[n]; // zeros
        for (int i = 0; i < n; ++i)
            parent[i] = i;
    }
    public int find(int i)
    {
        if (parent[i] != parent[parent[i]])
            parent[i] = find(parent[i]);
        return parent[i];
    }
    public void merge(int i, int j)
    {
        i = find(i);
        j = find(j);
        if (rank[i] > rank[j])
            parent[j] = i;
        else if (rank[i] < rank[j])
            parent[i] = j;
        else
        {
            // Pick i as the winner arbitrarily, and increment its rank.
            rank[i]++;
            parent[j] = i;
        }
    }

    public static void main(String args[])
    {
        int n = 10;
        MergeFind mf = new MergeFind(n);
        mf.merge(1,4);
        mf.merge(2,5);
        mf.merge(5,1);
        for (int i = 0; i < n; ++i)
            System.out.println("    "+i+" -> "+mf.find(i));
    } // main
} // class MergeFind
