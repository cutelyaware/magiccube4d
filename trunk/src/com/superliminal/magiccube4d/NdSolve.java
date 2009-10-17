package com.superliminal.magiccube4d;
//
// NdSolve.java - solves the d-dimensional analogue of Rubik's cube.
//
// Author: Don Hatch (hatch@plunk.org)
// Last modified: Mon May 15 03:57:35 PDT 2006
//
// This code may be used for any purpose as long as it is good and not evil.
//

/**
 * Solves the d-dimensional analogue of Rubik's cube. <br>
 * Can solve 2<sup>d</sup> and 3<sup>d</sup> puzzles for any number of
 * dimensions d &ge; 3. <br>
 * Simplest programmatic usage:
 * 
 * <pre>
 * String solution = NdSolve
 * 		.solve(&quot;        AAa   AAa   AAa        &quot;
 * 				+ &quot;   bBB c   C c   C c   C Bbb   &quot;
 * 				+ &quot;   BBB C   c C   c C   c bbb   &quot;
 * 				+ &quot;   bBB C   c C   c C   c Bbb   &quot;
 * 				+ &quot;        Aaa   Aaa   Aaa        &quot;);
 * System.out.println(&quot;Solution = \&quot;&quot; + solution + &quot;\&quot;&quot;);
 * </pre>
 * 
 * which prints this:
 * 
 * <pre>
 * Solution = &quot;aBC aBC ABC ABC cBA cBA ABC ABC cBA cBA aCB aCB&quot;
 * </pre>
 * 
 * In your string representation of the puzzle state, you can represent the
 * colors by any 2*d distinct non-space characters. List the stickers in order
 * by the current coordinates of their centers, lexicographically. The format is
 * totally free form; you can add spaces and newlines wherever you feel like it
 * (as above), or omit them altogether:
 * 
 * <pre>
 * &quot;AAaAAaAAabBBcCcCcCBbbBBBCcCcCcbbbbBBCcCcCcBbbAaaAaaAaa&quot;
 * </pre>
 * <p>
 * Each move in the solution is a 90 degree twist of a face, represented by a
 * 3-letter sequence:
 * <ul>
 * <li>the first letter is the color of the face being twisted
 * <li>the second letter is the color of the "from" axis of the twist
 * <li>the third letter is the color of the "to" axis of the twist
 * </ul>
 * In a 3<sup>d</sup> puzzle, the face center stickers never move, and so when
 * we refer to the "color" of a face, we mean the color of its center sticker. <br>
 * In a 2<sup>d</sup> puzzle, the solve algorithm never moves the first cubie,
 * and the color of a face means the respective color on that cubie (or the
 * opposite color in the solved puzzle).
 * <p>
 * That's it!
 * 
 * <hr>
 * 
 * This class has been tested with Java 1.4 and 1.5, using javac and the jikes
 * compiler.
 * <p>
 * Here are all the public static functions:
 * 
 * <pre>
 *     String  newPuzzle(int n, int d)
 *     boolean isSane(String puzzleString)
 *     boolean isSolvable(String puzzleString)
 *               - equivalent to isSolvable(puzzleString, &tilde;0, &tilde;0, null 0)
 *     boolean isSolved(String puzzleString)
 *     String  solve(String puzzleString)
 *               - equivalent to solve(puzzleString, &tilde;0, &tilde;0, null, 0)
 *     String  solve(String puzzleString,
 *                   int whichToPosition,
 *                   int whichToOrient,
 *                   java.io.PrintWriter progressWriter,
 *                   int debugLevel)
 *     String apply(String moves, String puzzleString)
 *     String scramble(String puzzleString,
 *                     int minScrambleChen,
 *                     int maxScrambleChen,
 *                     java.util.Random generator)
 *     int n(String puzzleString)
 *     int d(String puzzleString)
 *     void main(String args[]) - full featured test/demo program
 *     void simple_main(String args[]) - simple test/demo program
 *     void trivial_main(String args[]) - trivial test/demo program
 * 
 * For descriptions of these functions, use javadoc
 * (run &quot;javadoc NdSolve.java&quot;, which will create a bunch of html
 * files in the current directory, including index.html)
 * or see the descriptions above each function
 * (search for the string PUBLIC in the source file).
 * </pre>
 * 
 * 
 * <hr>
 * 
 * Here's a string representation of a pristine 3<sup>3</sup> puzzle:
 * 
 * <pre>
 *        AAA   AAA   AAA
 *   BBB C   c C   c C   c bbb
 *   BBB C   c C   c C   c bbb
 *   BBB C   c C   c C   c bbb
 *        aaa   aaa   aaa
 * </pre>
 * 
 * and after the single twist "ACB" (i.e. twist face A 90 degrees from axis C to
 * axis B):
 * 
 * <pre>
 *        AAA   AAA   AAA
 *   CCC b   B b   B b   B ccc
 *   BBB C   c C   c C   c bbb
 *   BBB C   c C   c C   c bbb
 *        aaa   aaa   aaa
 * </pre>
 * 
 * Note that the twist ACB can also be expressed as ABc or Acb or AbC.
 * <p>
 * Here's a pristine 3<sup>4</sup> puzzle:
 * 
 * <pre>
 *        AAA   AAA   AAA
 *        AAA   AAA   AAA
 *        AAA   AAA   AAA
 *  
 *        BBB   BBB   BBB
 *   CCC D   d D   d D   d ccc
 *   CCC D   d D   d D   d ccc
 *   CCC D   d D   d D   d ccc
 *        bbb   bbb   bbb
 * 
 *        BBB   BBB   BBB
 *   CCC D   d D   d D   d ccc
 *   CCC D   d D   d D   d ccc
 *   CCC D   d D   d D   d ccc
 *        bbb   bbb   bbb
 * 
 *        BBB   BBB   BBB
 *   CCC D   d D   d D   d ccc
 *   CCC D   d D   d D   d ccc
 *   CCC D   d D   d D   d ccc
 *        bbb   bbb   bbb
 *   
 *        aaa   aaa   aaa
 *        aaa   aaa   aaa
 *        aaa   aaa   aaa
 * </pre>
 * 
 * and a pristine 3<sup>5</sup> puzzle (make your window wide):
 * 
 * <pre>
 *                    AAA   AAA   AAA            AAA   AAA   AAA            AAA   AAA   AAA
 *                    AAA   AAA   AAA            AAA   AAA   AAA            AAA   AAA   AAA
 *                    AAA   AAA   AAA            AAA   AAA   AAA            AAA   AAA   AAA
 * 
 *                    BBB   BBB   BBB            BBB   BBB   BBB            BBB   BBB   BBB
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *                    bbb   bbb   bbb            bbb   bbb   bbb            bbb   bbb   bbb
 *                    BBB   BBB   BBB            BBB   BBB   BBB            BBB   BBB   BBB
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *                    bbb   bbb   bbb            bbb   bbb   bbb            bbb   bbb   bbb
 *                    BBB   BBB   BBB            BBB   BBB   BBB            BBB   BBB   BBB
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *  CCC CCC CCC  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  DDD E   e E   e E   e ddd  ccc  ccc  ccc
 *                    bbb   bbb   bbb            bbb   bbb   bbb            bbb   bbb   bbb
 * 
 *                    aaa   aaa   aaa            aaa   aaa   aaa            aaa   aaa   aaa
 *                    aaa   aaa   aaa            aaa   aaa   aaa            aaa   aaa   aaa
 *                    aaa   aaa   aaa            aaa   aaa   aaa            aaa   aaa   aaa
 * 
 * </pre>
 */
// The pristine 3^5 and 3^6 are shown at the end of this program listing.
//

// ==========================================================================

//
// TODO:
// - isSane needs a progressWriter for explanation, maybe
// - see
// http://72.14.207.104/search?q=cache:S66YxvqauqYJ:rhea.redhat.com/doc/core-platform/5.0/engineering-standards/s1-exceptions.html+java+%22is+not+implemented%22+exception&hl=en&gl=us&ct=clnk&cd=7
// " It is a good practice to have one exception per package for signaling an application level error to the caller.  These exceptions can be subclassed for use internally, but the methods that throw exceptions should declare that they throw this package level exception. However, if there is an exception as part of the standard Java API, such as IllegalArgumentException or IOException, it should be used."
// should I make one? Nah...
// - use the standard IllegalArgumentException for illegal arguments to the
// public methods, I think.
// - put in package donhatchsw?
// ARGH, can't do this, since it will create
// a package-level Arrays.class!!!
// oh this SUCKS!!! need to rename Arrays to something else...
// or, better, don't share it, make a little class
// of whatever the Tester needs, probably not much
// - allow comments in the files, using #? Maybe a regexp passed to the
// function?
// - when printing timings of solve
// align the timing printouts? maybe give most of the nMoves columns 4?
// - allow comma-separated lists for positionMask and orientMask
// - change all the INDICES functions to coords so I'm not converting back and
// forth so much
// - expose only what's necessary
//    
// - figure out nice way of printing with optimal whitespace like above
// - make a solver object that can give partial results
// and progress status messages; implement it as an iterator somehow?
// - javadoc
// - <code> variable names? nah, don't bother
// - attempt some sort of line editing in the command shell?
// - make a version of readPuzzle that knows n and d beforehand and therefore
// can't be fooled?

// Assert rather than assert-- these need to always be enabled;
// proper behavior in some cases depends on throwing an Error on assertion
// failure.
// If using the C preprocessor, uncomment the following for better messages,
// and you'll need to comment out the Assert methods in each class.
// #define Assert(expr) { if (!(expr)) throw new
// Error("Assertion failed at "+__FILE__+"("+__LINE__+"): " + #expr + ""); }
// #define assert ... dont use assert, use Assert! assert isn't compiled in by
// default ...
// #define PRINT(x) System.out.println("    " + #x + " = " + Arrays.toString(x))

public class NdSolve {
	// uncomment this if not using the C preprocessor...
	static private void Assert(boolean condition) {
		if (!condition)
			throw new Error("Assertion failed");
	}

	private NdSolve() {
	} // non-instantiatable, only has static member functions

	// =========================================================================
	//
	// Generic utility functions
	// XXX should probably maybe be combined with
	// XXX PuzzleManipulation since PuzzleManipulation is
	// XXX a lame name, I don't know.
	// XXX so far, if it has to do with slices masks,
	// XXX it goes into PuzzleManipulation,
	// XXX however I'm not sure if maybe the index-to-coords stuff should go
	// there too?
	//
	private static class Utils {
		public static int clamp(int x, int a, int b) {
			return x <= a ? a : x >= b ? b : x;
		}

		// assuming all entries of index are in the range 0..n-1,
		// make a histogram.
		public static int[] histogram(int n, int index[]) {
			int hist[] = new int[n]; // all zero
			for (int i = 0; i < index.length; ++i)
				++hist[index[i]];
			return hist;
		}

		// rounds down if the coords fall between cubies,
		// e.g. to find the representative color for faces
		// on even-length puzzles
		public static int[] coordsToIndex(int n, int coords[]) {
			int index[] = new int[coords.length];
			for (int i = 0; i < coords.length; ++i)
				index[i] = (coords[i] + n + 1) / 2;
			return index;
		}

		public static int[] indexToCoords(int n, int index[]) {
			int coords[] = new int[index.length];
			for (int i = 0; i < index.length; ++i)
				coords[i] = 2 * index[i] - (n + 1);
			return coords;
		}

		@SuppressWarnings("unused")
		public static Object coordssToIndexs(int n, Object coords) {
			if (coords.getClass() == int[].class)
				return coordsToIndex(n, (int[]) coords);
			else {
				// XXX really should use reflection? not sure if it's required
				Object[] coordss = (Object[]) coords;
				Object[] indexs = (Object[]) java.lang.reflect.Array
						.newInstance(coordss.getClass().getComponentType(),
								coordss.length);
				for (int i = 0; i < coordss.length; ++i)
					indexs[i] = coordssToIndexs(n, coordss[i]); // recurse
				return indexs;
			}
		}

		public static Object indexsToCoordss(int n, Object index) {
			if (index.getClass() == int[].class)
				return indexToCoords(n, (int[]) index);
			else {
				// XXX really should use reflection? not sure if it's required
				Object[] indexs = (Object[]) index;
				Object[] coordss = (Object[]) java.lang.reflect.Array
						.newInstance(indexs.getClass().getComponentType(),
								indexs.length);
				for (int i = 0; i < indexs.length; ++i)
					coordss[i] = indexsToCoordss(n, indexs[i]); // recurse
				return coordss;
			}
		}

		// A and B can be two sticker coords
		// and/or cubie center coords.
		public static boolean areOnSameCubie(int n, int A[], int B[]) {
			for (int i = 0; i < A.length; ++i)
				if (clamp(A[i], 1, n) != clamp(B[i], 1, n))
					return false;
			return true;
		} // areOnSameCubie

		public static int reverseBits(int x, int nBits) {
			int result = 0;
			for (int iBit = 0; iBit < nBits; ++iBit)
				result |= (((x >> iBit) & 1) << (nBits - 1 - iBit));
			return result;
		} // reverseBits

	} // private static class Utils

	// =========================================================================

	// =========================================================================

	//
	// Puzzle manipulation utility functions.
	// These are functions that could be generally useful
	// even when not solving.
	//
	private static class PuzzleManipulation {
		public static int[] rot90index(int n, int fromAxis, int toAxis,
				int index[]) {
			return Utils.coordsToIndex(n, rot90coords(fromAxis, toAxis, Utils
					.indexToCoords(n, index)));
		}

		//
		// Rotate coords by the 90 degree rotation
		// that takes +fromAxis to +toAxis.
		//
		public static int[] rot90coords(int fromAxis, int toAxis, int coords[]) {
			int result[] = Arrays.copy(coords);
			result[toAxis] = coords[fromAxis];
			result[fromAxis] = -coords[toAxis];
			return result;
		} // rot90coords

		public static int[][] rot90coordss(int fromAxis, int toAxis,
				int coordss[][]) {
			int result[][] = new int[coordss.length][];
			for (int i = 0; i < coordss.length; ++i)
				result[i] = rot90coords(fromAxis, toAxis, coordss[i]);
			return result;
		} // rot90coordss

		public static int[] rot90sCoords(int rots[][], int coords[]) {
			for (int iRot = 0; iRot < rots.length; ++iRot)
				coords = rot90coords(rots[iRot][0], rots[iRot][1], coords);
			return coords;
		} // rot90sCoords

		//
		// Apply a twist90 move to a single coords.
		//
		public static int[] twist90coords(int n, int faceAxis, int faceSign,
				int fromAxis, int toAxis, int slicesMask, int coords[]) {
			int bitIndex = (-faceSign * coords[faceAxis] + n - 1) / 2;
			// XXX this is a bit time consuming... if doing this to many coords
			// at once, consider making an expanded bitmask by duplicating the
			// first and last bit
			bitIndex = Utils.clamp(bitIndex, 0, n - 1); // so stickers move with
														// the slice
			if (((slicesMask >> bitIndex) & 1) == 1)
				return rot90coords(fromAxis, toAxis, coords);
			else
				return coords;
		} // twist90coords

		public static int[] twist90coords(int n, int twist[], int coords[]) {
			return twist90coords(n, twist[0], twist[1], twist[2], twist[3],
					twist[4], coords);
		} // twist90coords

		public static int[] twist90sCoords(int n, int twists[][], int coords[]) {
			for (int i = 0; i < twists.length; ++i)
				coords = twist90coords(n, twists[i], coords);
			return coords;
		} // twist90coords

		public static int[][] twist90sCoordss(int n, int twists[][],
				int coordss[][]) {
			int result[][] = new int[coordss.length][];
			for (int i = 0; i < coordss.length; ++i)
				result[i] = twist90sCoords(n, twists, coordss[i]);
			return result;
			/*
			 * equivalent to go the other way, but no one needed a
			 * twist90coordss in isolation for (int i = 0; i < twists.length;
			 * ++i) coordss = twist90coordss(n, twists[i], coords); return
			 * coordss;
			 */
		} // twist90coords

		//
		// Rotate or twist
		// part or all of the puzzle 90 degrees.
		// Returns a new array without altering puz;
		// the entries of the result will be the (permuted) entries of puz,
		// it doesn't matter what the type of the entries were.

		public static Object twist90(int n, int d, Object puz, int faceAxis,
				int faceSign, int fromAxis, int toAxis, int slicesMask) {
			// System.out.println("in twist90");

			//
			// Canonicalize faceSign to be -1,
			// by reversing the bits in slicesMask if it's +1
			//
			if (faceSign == 1) {
				int newMask = 0;
				for (int i = 0; i < n; ++i)
					newMask |= ((slicesMask >> (n - 1 - i)) & 1) << i;
				slicesMask = newMask;
				faceSign = -1;
			}
			Assert(faceSign == -1);

			//
			// The three axes must be distinct...
			//
			Assert(fromAxis != toAxis);
			Assert(faceAxis != toAxis);
			Assert(faceAxis != fromAxis);

			Object result = Arrays.repeat(null, n + 2, d);

			int fromIndex[] = new int[d];
			int toIndex[] = new int[d];

			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				Arrays.unFlatIndex(fromIndex, iIndex, n + 2, d);
				if (((slicesMask >> Utils.clamp(fromIndex[faceAxis] - 1, 0,
						n - 1)) & 1) == 1) {
					// int toIndex[] = Arrays.copy(fromIndex);
					Arrays.copy(toIndex, fromIndex);
					toIndex[toAxis] = fromIndex[fromAxis];
					toIndex[fromAxis] = n + 2 - 1 - fromIndex[toAxis];
					Arrays.set(result, toIndex, Arrays.get(puz, fromIndex));
				} else {
					Arrays.set(result, fromIndex, Arrays.get(puz, fromIndex)); // XXX
																				// note,
																				// this
																				// is
																				// probably
																				// actually
																				// slower
																				// when
																				// using
																				// an
																				// array
																				// of
																				// char
																				// than
																				// when
																				// using
																				// and
																				// array
																				// of
																				// Object,
																				// because
																				// the
																				// former
																				// has
																				// to
																				// allocate
																				// a
																				// Character
																				// for
																				// each
																				// temporary
				}
			}
			// System.out.println("out twist90");
			return result;
		} // twist90

		public static Object twist90(int n, int d, Object puz,
				int move[/* 5 */]) {
			return twist90(n, d, puz, move[0], move[1], move[2], move[3],
					move[4]);
		} // twist90

		// This is the bottleneck if done naively-- applying the moves
		// was taking hundreds of times more time
		// than computing the sequences.
		// We speed that up by working with flat permutations,
		// expressing move permutations as cycle lists,
		// and caching the cycle lists of previously seen moves.
		public static Object superOptimizedTwist90s(int n, int d, Object puz,
				int moves[][/* 5 */]) {
			int nIndices = Arrays.intpow(n + 2, d);

			int scratchPerm[] = new int[nIndices];
			int scratchIndex[] = new int[d];

			//
			// Flatten...
			//
			Object flatPuz[] = new Object[nIndices];
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				Arrays.unFlatIndex(scratchIndex, iIndex, n + 2, d);
				flatPuz[iIndex] = Arrays.get(puz, scratchIndex);
			}

			//
			// Apply moves to flat puzzle...
			//
			java.util.HashMap<String, int[]> moveToPermutationCycles = new java.util.HashMap<String, int[]>();
			int nHits = 0, nMisses = 0;
			for (int iMove = 0; iMove < moves.length; ++iMove) {
				int move[] = moves[iMove];
				String key = "" + move[0] + " " + move[1] + " " + move[2] + " "
						+ move[3] + " " + move[4];
				int cycles[] = moveToPermutationCycles.get(key);
				if (cycles == null) {
					// Haven't seen this move before.
					// Compute the permutation of the flat array...
					int faceAxis = move[0];
					int faceSign = move[1];
					int fromAxis = move[2];
					int toAxis = move[3];
					int slicesMask = move[4];

					//
					// Canonicalize faceSign to be -1,
					// by reversing the bits in slicesMask if it's +1
					//
					if (faceSign == 1) {
						int newMask = 0;
						for (int i = 0; i < n; ++i)
							newMask |= ((slicesMask >> (n - 1 - i)) & 1) << i;
						slicesMask = newMask;
						faceSign = -1;
					}
					Assert(faceSign == -1);

					for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
						Arrays.unFlatIndex(scratchIndex, iIndex, n + 2, d);
						int hist[] = Utils.histogram(n + 2, scratchIndex);
						if (hist[0] + hist[n + 1] <= 1 // i.e. if not air
								&& ((slicesMask >> Utils.clamp(
										scratchIndex[faceAxis] - 1, 0, n - 1)) & 1) == 1) {
							int temp = scratchIndex[toAxis];
							scratchIndex[toAxis] = scratchIndex[fromAxis];
							scratchIndex[fromAxis] = n + 2 - 1 - temp;
							int jIndex = Arrays.flatIndex(scratchIndex, n + 2,
									d);
							scratchPerm[iIndex] = jIndex; // scratchPerm[iIndex]
															// is where iIndex
															// gets taken by
															// this permutation
						} else {
							scratchPerm[iIndex] = iIndex;
						}
					}
					// PRINT(scratchPerm);
					// And decompose the permutation into cycles...
					// all cycles will have length 4.
					java.util.ArrayList<int[]> cyclesList = new java.util.ArrayList<int[]>();
					for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
						int i0 = scratchPerm[iIndex];
						if (i0 != iIndex) {
							int i1 = scratchPerm[i0];
							int i2 = scratchPerm[i1];
							int i3 = scratchPerm[i2];
							Assert(i0 == scratchPerm[i3]);
							Assert(i0 != i1);
							Assert(i0 != i2);
							Assert(i0 != i3);
							Assert(i1 != i2);
							Assert(i1 != i3);
							Assert(i2 != i3);
							cyclesList.add(new int[] { i0, i1, i2, i3 });
							// so we don't do them again...
							scratchPerm[i0] = i0;
							scratchPerm[i1] = i1;
							scratchPerm[i2] = i2;
							scratchPerm[i3] = i3;
						}
					}
					// Concatenate the 4-cycles into a flat list
					// to save space.
					// cycles = (int[])Arrays.concat(cyclesList.toArray(new
					// int[0][8675309]));
					int[][] intcycles = cyclesList.toArray(new int[0][8675309]);
					cycles = (int[]) Arrays.concat(intcycles);
					// PRINT(cycles);
					// PRINT(cycles.length);
					moveToPermutationCycles.put(key, cycles);
					nMisses++;
				} else
					nHits++;
				for (int iCycle = 0; iCycle < cycles.length;) {
					int i0 = cycles[iCycle++];
					int i1 = cycles[iCycle++];
					int i2 = cycles[iCycle++];
					int i3 = cycles[iCycle++];
					Object o0 = flatPuz[i0];
					Object o1 = flatPuz[i1];
					Object o2 = flatPuz[i2];
					Object o3 = flatPuz[i3];
					flatPuz[i1] = o0;
					flatPuz[i2] = o1;
					flatPuz[i3] = o2;
					flatPuz[i0] = o3;
				}
			}

			//
			// Unflatten...
			//
			Object outPuz = Arrays.repeat(null, n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				Arrays.set(outPuz, index, flatPuz[iIndex]);
			}
			// PRINT(nHits);
			// PRINT(nMisses);
			return outPuz;
		} // superOptimizedTwist90s

		// THIS IS THE BOTTLENECK!
		public static Object twist90s(int n, int d, Object puz,
				int moves[][/* 5 */]) {
			//if (true)
				return superOptimizedTwist90s(n, d, puz, moves);
			//for (int i = 0; i < moves.length; ++i)
			//	puz = twist90(n, d, puz, moves[i]);
			//return puz;
		} // twist90s

		public static Object twist90s(int n, int d, Object puz, java.util.ArrayList<int[]> movesList) {
			return twist90s(n, d, puz, (int[][]) movesList.toArray(new int[0][]));
		} // twist90s

		public static int[] randomTwist90(int n, int d,
				java.util.Random generator) {
			Assert(d >= 3); // otherwise we'll endless loop trying to find 3
							// distinct axes
			int faceAxis, fromAxis, toAxis;
			while (true) {
				faceAxis = generator.nextInt(d);
				fromAxis = generator.nextInt(d);
				toAxis = generator.nextInt(d);
				if (faceAxis != fromAxis && fromAxis != toAxis
						&& toAxis != faceAxis)
					break;
			}
			int faceSign = generator.nextInt(2) * 2 - 1; // -1 or 1
			int slicesMask = 1; // twists just the one face
			if (n >= 2) {
				int nChoices = n / 2; // 2->1 3->1 4->2 5->2 6->3 7->3 etc.
				slicesMask = 1 << generator.nextInt(nChoices);
			}
			return new int[] { faceAxis, faceSign, fromAxis, toAxis, slicesMask };
		} // randomTwist90

		public static int[][] randomTwists90(int nTwists, int n, int d,
				java.util.Random generator) {
			int twists[][] = new int[nTwists][];
			for (int i = 0; i < nTwists; ++i)
				twists[i] = randomTwist90(n, d, generator);
			return twists;
		} // randomTwists90

		//
		// Return a move sequence of some number from minScrambleChen to
		// maxScrambleChen
		// of moves.
		//
		public static int[][] scramble(int n, int d, int minScrambleChen,
				int maxScrambleChen, java.util.Random generator, int debugLevel) {
			// LAME LAME LAME--
			// I'm observing that the very first nextInt(2)
			// that comes out of a java.util.Random
			// has 99% chance of being a 1 !!!!!
			// So call nextInt() once and throw it away, in case
			// we are in that situation.
			// 
			int garbageBit = generator.nextInt(maxScrambleChen + 1
					- minScrambleChen);
			if (debugLevel >= 3)
				System.out.println("    throwing away garbage int "
						+ garbageBit);

			int nScrambleChen = minScrambleChen
					+ generator.nextInt(maxScrambleChen + 1 - minScrambleChen);
			int twists[][] = randomTwists90(nScrambleChen, n, d, generator);
			for (int i = 0; i < nScrambleChen; ++i) {
				if (debugLevel >= 2) {
					int faceAxis = twists[i][0];
					int faceSign = twists[i][1];
					int fromAxis = twists[i][2];
					int toAxis = twists[i][3];
					int slicesMask = twists[i][4];
					System.out.print("    i=" + i);
					System.out.print(" face=" + (faceSign < 0 ? "-" : "+")
							+ faceAxis);
					System.out.print(" from=" + fromAxis);
					System.out.print(" to=" + toAxis);
					System.out.print(" slicesMask=" + slicesMask);
					System.out.println();
				}
			}
			return twists;
		} // scramble

		// a move is:
		// {toAxis,toSign,fromAxis,fromSign,faceAxis,faceSign,slicesMask}
		// its inverse simply reverses the to and from:
		// {fromAxis,fromSign,toAxis,toSign,faceAxis,faceSign,slicesMask}
		//
		public static int[] reverseMove(int move[]) {
			return new int[] { move[0], // same faceAxis
					move[1], // same faceSign
					move[3], // fromAxis = original toAxis
					move[2], // toAxis = original fromAxis
					move[4], // same slicesMask
			};
		}

		public static int[][] reverseMoves(int moves[][]) {
			int reversed[][] = new int[moves.length][];
			for (int i = 0; i < moves.length; ++i)
				reversed[i] = reverseMove(moves[moves.length - 1 - i]);
			return reversed;
		}

		// convenience function that figures out an equivalent
		// twist in which fromSign,toSign are positive
		// and can be omitted.
		// XXX look for places I did this by hand before that would be clearer
		// by using this
		public static int[] makeTwist90(int faceAxis, int faceSign,
				int fromAxis, int fromSign, int toAxis, int toSign,
				int slicesMask) {
			Assert(faceSign == 1 || faceSign == -1);
			Assert(fromSign == 1 || fromSign == -1);
			Assert(toSign == 1 || toSign == -1);
			Assert(faceAxis != fromAxis);
			Assert(fromAxis != toAxis);
			Assert(toAxis != faceAxis);
			if (fromSign == toSign)
				return new int[] { faceAxis, faceSign, fromAxis, toAxis,
						slicesMask };
			else
				return new int[] { faceAxis, faceSign, toAxis, fromAxis,
						slicesMask };
		} // makeTwist90

		private static boolean isStickerIndex(int n, int d, int index[]) {
			int nExtremes = 0;
			for (int iDim = 0; iDim < d; ++iDim)
				if (index[iDim] == 0 || index[iDim] == n + 1)
					nExtremes++;
			return nExtremes == 1;
		} // isStickerIndex

		private static boolean isCubieIndex(int n, int d, int index[]) {
			for (int iDim = 0; iDim < d; ++iDim)
				if (index[iDim] == 0 || index[iDim] == n + 1)
					return false;
			return true;
		} // isCubieIndex

		public static int[][] makeSoDoesntMoveFirstCubie(int n, int d,
				int moves[][], int debugLevel) {
			if (debugLevel >= 1)
				System.out.println("in makeSoDoesntMoveFirstCubie");
			if (debugLevel >= 3)
				System.out.println("    moves = " + Arrays.toString(moves));
			int movesOut[][] = new int[moves.length][];
			int worldAxesInCubie0Space[] = Arrays.identityperm(d);
			int worldSignsInCubie0Space[] = Arrays.repeat(1, d);
			for (int iMove = 0; iMove < moves.length; ++iMove) {
				int move[] = moves[iMove];
				int worldSpaceFaceAxis = move[0];
				int worldSpaceFaceSign = move[1];
				int worldSpaceFromAxis = move[2];
				int worldSpaceFromSign = 1;
				int worldSpaceToAxis = move[3];
				int worldSpaceToSign = 1;
				int slicesMask = move[4];

				// Convert to cubie0 space...
				int cubie0SpaceFaceAxis = worldAxesInCubie0Space[worldSpaceFaceAxis];
				int cubie0SpaceFaceSign = worldSpaceFaceSign
						* worldSignsInCubie0Space[worldSpaceFaceAxis];
				int cubie0SpaceFromAxis = worldAxesInCubie0Space[worldSpaceFromAxis];
				int cubie0SpaceFromSign = worldSpaceFromSign
						* worldSignsInCubie0Space[worldSpaceFromAxis];
				int cubie0SpaceToAxis = worldAxesInCubie0Space[worldSpaceToAxis];
				int cubie0SpaceToSign = worldSpaceToSign
						* worldSignsInCubie0Space[worldSpaceToAxis];

				int bit = cubie0SpaceFaceSign == -1 ? 0 : n - 1;
				if (((slicesMask >> bit) & 1) == 1) // if it includes cubie 0
				{
					if (debugLevel >= 5)
						System.out.println("        YES it included cubie 0");
					// Express as the opposite twist of the opposite face
					slicesMask = Utils.reverseBits(~slicesMask, n);
					cubie0SpaceFaceSign *= -1;
					cubie0SpaceToSign *= -1; // negate just one of them

					// Update world axes in cubie0 space
					// by applying this move.
					{
						int temp = worldAxesInCubie0Space[worldSpaceToAxis];
						worldAxesInCubie0Space[worldSpaceToAxis] = worldAxesInCubie0Space[worldSpaceFromAxis];
						worldAxesInCubie0Space[worldSpaceFromAxis] = temp;
					}
					{
						int temp = worldSignsInCubie0Space[worldSpaceToAxis];
						worldSignsInCubie0Space[worldSpaceToAxis] = worldSignsInCubie0Space[worldSpaceFromAxis];
						worldSignsInCubie0Space[worldSpaceFromAxis] = -temp;
					}
				} else {
					if (debugLevel >= 5)
						System.out
								.println("        NO it did not include cubie 0");
				}
				movesOut[iMove] = makeTwist90(cubie0SpaceFaceAxis,
						cubie0SpaceFaceSign, cubie0SpaceFromAxis,
						cubie0SpaceFromSign, cubie0SpaceToAxis,
						cubie0SpaceToSign, slicesMask);

				if (debugLevel >= 5) // super heavy duty debug with more
										// assertions
				{
					System.out.println("        iMove = " + iMove);
					System.out.println("        moves = "
							+ Arrays.toString(Arrays.subArray(moves, 0,
									iMove + 1)));
					System.out.println("        movesOut = "
							+ Arrays.toString(Arrays.subArray(movesOut, 0,
									iMove + 1)));
					System.out.println("        worldAxesInCubie0Space = "
							+ Arrays.toString(worldAxesInCubie0Space));
					System.out.println("        worldSignsInCubie0Space = "
							+ Arrays.toString(worldSignsInCubie0Space));
					// Make sure we didn't alter the meaning
					// of the move sequence on any coordinate...
					int nIndices = Arrays.intpow(n + 2, d);
					for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
						int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
						int coords[] = Utils.indexToCoords(n, index);
						int twistedCoordsInCubie0Space[] = PuzzleManipulation
								.twist90sCoords(n, Arrays.subArray(movesOut, 0,
										iMove + 1), coords);
						int twistedCoordsInWorldSpace[] = PuzzleManipulation
								.twist90sCoords(n, Arrays.subArray(moves, 0,
										iMove + 1), coords);
						System.out.println("            "
								+ Arrays.toString(coords) + " -> (cubie0)"
								+ Arrays.toString(twistedCoordsInCubie0Space)
								+ ", (world)"
								+ Arrays.toString(twistedCoordsInWorldSpace));

						int twistedCoordsInWorldSpaceMappedToCubie0Space[] = Arrays
								.repeat(-999, d);
						for (int iDim = 0; iDim < d; ++iDim) {
							twistedCoordsInWorldSpaceMappedToCubie0Space[worldAxesInCubie0Space[iDim]] = twistedCoordsInWorldSpace[iDim]
									* worldSignsInCubie0Space[iDim];
						}
						System.out
								.println("                 -> (world to cubie0)"
										+ Arrays
												.toString(twistedCoordsInWorldSpaceMappedToCubie0Space));
						Assert(Arrays.equals(twistedCoordsInCubie0Space,
								twistedCoordsInWorldSpaceMappedToCubie0Space));
					}
				}
			}
			if (debugLevel >= 3)
				System.out.println("    movesOut = "
						+ Arrays.toString(movesOut));
			if (debugLevel >= 1)
				System.out.println("out makeSoDoesntMoveFirstCubie");
			return movesOut;
		} // makeSoDoesntMoveFirstCubie

	} // private static class PuzzleManipulation

	// =========================================================================

	//
	// Internal solve function and building blocks thereof.
	//
	private static class SolveStuff {

		//
		// Solve the puzzle, given puz which is
		// an array of sticker colors (letters).
		// Returns a sequence of moves of the form
		// {faceAxis,faceSign, fromAxis,toAxis, slicesMask}.
		//
		public static int[][/* 5 */] solve(int n, int d, Object puz,
				int whichToPosition, // bit mask
				int whichToOrient, // bit mask
				java.io.PrintWriter progressWriter, int debugLevel) {
			if (debugLevel >= 1)
				System.out.println("in SolveStuff.solve");
			// PRINT(puz);

			// so that we flush after every newline,
			// to guarantee sanity in case interspersed with debugging output...
			// Note, without this, we aren't guaranteed to flush at the end
			// either.
			// XXX since we are going to do this,
			// XXX should we even require progressWriter to be
			// XXX a printWriter to begin with?
			if (progressWriter != null)
				progressWriter = new java.io.PrintWriter(progressWriter, true);

			long time0 = System.currentTimeMillis();
			long time1 = time0;

			// stuff to orient must be a subset of stuff to position...
			// (if the caller has positioned stuff already,
			// it doesn't hurt to position again, it won't produce
			// any more moves)
			// XXX should we just set whichToPosition |= whichToOrient?
			Assert((whichToOrient & ~whichToPosition) == 0);

			//
			// Figure out the indices where the stickers and cubie centers want
			// to be.
			//
			Object puzzleIndices = figureOutWhereIndicesWantToBe(n, d, puz);
			// PRINT(puzzleIndices);

			//
			// We won't modify puz,
			// but we will permute puzzleIndices,
			// gradually turning it into the solved state
			// as we build the solution.
			//

			//
			// Figure out whether the state is even or odd.
			// (It's an odd state iff it's an odd permutation
			// on the 2-sticker cubies.)
			// If it's odd, then start by applying one single arbitrary
			// 90-degree twist.
			//
			java.util.ArrayList<int[]> solution = new java.util.ArrayList<int[]>();
			if (puzzleStateIsOdd(n, d, puzzleIndices)) {
				if (progressWriter != null)
					progressWriter
							.print("    It's odd, applying one twist to fix parity...");
				solution.add(new int[] { 0, +1, 1, 2, 1 }); // XXX this is
															// arbitrary-- maybe
															// should try every
															// possible twist
															// and see which one
															// helps the most?
				// System.out.print("before applying parity-fixing twist: ");
				// PRINT(puzzleIndices);
				puzzleIndices = PuzzleManipulation.twist90s(n, d, puzzleIndices, solution);
				// System.out.print("after  applying parity-fixing twist: ");
				// PRINT(puzzleIndices);
				// System.out.println(PuzzleIO.puzzleToString(n,d,puzzleIndices));
			} else {
				if (progressWriter != null)
					progressWriter.print("    It's even               "); // no
																			// newline
			}
			if (progressWriter != null) {
				long time2 = System.currentTimeMillis();
				progressWriter.println("                 " + (time2 - time0)
						/ 1000. + " secs");
				time1 = time2;
			}

			//
			// position the 2-sticker cubies,
			// orient the 2-sticker cubies,
			// position the 3-sticker cubies,
			// orient the 3-sticker cubies,
			// ...
			// position the d-sticker cubies,
			// orient the d-sticker cubies.
			//
			for (int k = 2; k <= d; ++k) {
				if (n < 3 && k < d)
					continue; // there are no k-sticker cubies
				boolean doPosition = ((whichToPosition & (1 << k)) != 0);
				if (!doPosition) {
					if (progressWriter != null) {
						progressWriter.println("    Positioning " + k
								+ "-sticker cubies... NOT!");
						progressWriter.println("    Orienting " + k
								+ "-sticker cubies... NOT!");
					}
					continue;
				}
				boolean doOrient = ((whichToOrient & (1 << k)) != 0);

				Assert(is_positioned_up_to(whichToPosition, k - 1, n, d,
						puzzleIndices, debugLevel));
				Assert(is_oriented_up_to(whichToOrient, k - 1, n, d,
						puzzleIndices, debugLevel));
				//
				// Position the k-sticker cubies
				//
				{
					if (progressWriter != null) {
						progressWriter.print("    Positioning " + k
								+ "-sticker cubies...");
						progressWriter.flush();
					}
					java.util.ArrayList<int[]> moreMoves = position_ksticker_cubies(k,n, d, puzzleIndices, debugLevel);
					solution.addAll(moreMoves);
					if (progressWriter != null) {
						progressWriter.print("   + " + moreMoves.size() + " = "
								+ solution.size() + " moves");
						long time2 = System.currentTimeMillis();
						progressWriter.print("  + " + (time2 - time1) / 1000.
								+ " = " + (time2 - time0) / 1000. + " secs");
						time1 = time2;
						progressWriter.println();
					}
					if (progressWriter != null) {
						progressWriter.print("        applying...");
						progressWriter.flush();
					}
					puzzleIndices = PuzzleManipulation.twist90s(n, d, puzzleIndices, moreMoves);
					if (progressWriter != null) {
						progressWriter.print("        done.");
						long time2 = System.currentTimeMillis();
						progressWriter.print("                          + "
								+ (time2 - time1) / 1000. + " = "
								+ (time2 - time0) / 1000. + " secs");
						time1 = time2;
						progressWriter.println();
					}
				}
				Assert(is_positioned_up_to(whichToPosition, k, n, d,
						puzzleIndices, debugLevel));
				Assert(is_oriented_up_to(whichToOrient, k - 1, n, d,
						puzzleIndices, debugLevel));
				//
				// Orient the k-sticker cubies
				//
				if (doOrient) {
					if (progressWriter != null) {
						progressWriter.print("    Orienting " + k
								+ "-sticker cubies...");
						progressWriter.flush();
					}
					java.util.ArrayList<int[]> moreMoves = orient_ksticker_cubies(k,
							n, d, puzzleIndices, debugLevel);
					solution.addAll(moreMoves);
					if (progressWriter != null) {
						progressWriter.print("     + " + moreMoves.size()
								+ " = " + solution.size() + " moves");
						long time2 = System.currentTimeMillis();
						progressWriter.print("  + " + (time2 - time1) / 1000.
								+ " = " + (time2 - time0) / 1000. + " secs");
						time1 = time2;
						progressWriter.println();
					}
					if (progressWriter != null) {
						progressWriter.print("        applying...");
						progressWriter.flush();
					}
					puzzleIndices = PuzzleManipulation.twist90s(n, d, puzzleIndices, moreMoves);
					if (progressWriter != null) {
						progressWriter.print("        done.");
						long time2 = System.currentTimeMillis();
						progressWriter.print("                          + "
								+ (time2 - time1) / 1000. + " = "
								+ (time2 - time0) / 1000. + " secs");
						time1 = time2;
						progressWriter.println();
					}
				} else {
					if (progressWriter != null)
						progressWriter.println("    Orienting " + k
								+ "-sticker cubies... NOT!");
				}
				Assert(is_positioned_up_to(whichToPosition, k, n, d,
						puzzleIndices, debugLevel));
				Assert(is_oriented_up_to(whichToOrient, k, n, d, puzzleIndices,
						debugLevel));
			}
			Assert(is_positioned_up_to(whichToPosition, d, n, d, puzzleIndices,
					debugLevel));
			Assert(is_oriented_up_to(whichToOrient, d, n, d, puzzleIndices,
					debugLevel));

			if (debugLevel >= 2)
				System.out
						.println("    solution =" + Arrays.toString(solution));
			if (debugLevel >= 1)
				System.out.println("out SolveStuff.solve");
			return (int[][]) solution.toArray(new int[0][846372622]); // if they
																		// can
																		// be
																		// retarded
																		// then
																		// I can
																		// be
																		// more
																		// retarded
		} // solve

		//
		// Given puz which is sticker letters and spaces,
		// figure out the indices where the stickers and cubie centers
		// want to be. Return a multidimensional array containing,
		// for each index, where the guy currently at that index wants to be.
		//
		private static Object figureOutWhereIndicesWantToBe(int n, int d,
				Object puz) {
			//
			// Get the axis letters.
			//
			char colors[][] = PuzzleIO.getSignedAxisColors(n, d, puz);
			java.util.HashMap<Character, int[]> letterToFaceCenterCubieCoords = new java.util.HashMap<Character, int[]>();
			for (int iDim = 0; iDim < d; ++iDim) {
				int stickerCoords[] = Arrays.repeat(0, d);
				int cubieCoords[] = Arrays.repeat(0, d);
				for (int sign = -1; sign <= 1; sign += 2) {
					stickerCoords[iDim] = sign * (n + 1);
					cubieCoords[iDim] = sign * (n - 1);
					Character letter = new Character(
							colors[iDim][(sign + 1) / 2]);
					Assert(!Character.isWhitespace(letter.charValue()));
					Assert(!letterToFaceCenterCubieCoords.containsKey(letter));
					// letterToFaceCenterCubieCoords.put(letter,
					// Arrays.copy(cubieCoords)); // was doing this, but messes
					// up if n==1
					letterToFaceCenterCubieCoords.put(letter, Arrays.copy(stickerCoords));
				}
			}
			// PRINT(letterToFaceCenterCubieCoords);

			int nIndices = Arrays.intpow(n + 2, d);

			// Start by setting all answer coords of cubie centers
			// to zero; we will accumulate
			// into each cubie center the desired coords of the face center of
			// each sticker
			// that contributes to it.
			Object answer = Arrays.repeat(null, n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				Arrays.set(answer, index, Arrays.repeat(0, d));
			}

			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				Character letter = (Character) Arrays.get(puz, index);
				if (letter.charValue() != ' ') {
					Assert(letterToFaceCenterCubieCoords.containsKey(letter));
					int contribution[] = (int[]) letterToFaceCenterCubieCoords
							.get(letter);
					int cubieCenterIndex[] = Arrays.clamp(index, 1, n);
					int temp[] = (int[]) Arrays.get(answer, cubieCenterIndex);
					temp = Arrays.plus(temp, contribution);
					temp = Arrays.clamp(temp, -(n - 1), n - 1);
					Arrays.set(answer, cubieCenterIndex, temp);
					// System.out.println("        the "+letter+" at index "+Arrays.toString(index)+" contributes "+Arrays.toString(contribution)+" to the cubie center at index "+Arrays.toString(cubieCenterIndex));
				}
			}

			// System.out.println("    coords where cubie centers want to be: "+Arrays.toString(answer));

			// Okay, now we know where each cubie center wants to be (assuming
			// n<=3).
			// To figure out where each sticker wants to be,
			// look at where its cubie wants to be and go 2 units in the
			// direction of the face
			// of the sticker color.
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				Character letter = (Character) Arrays.get(puz, index);
				if (letter.charValue() != ' ') {
					int cubieCenterIndex[] = Arrays.clamp(index, 1, n);
					int whereCubieCenterWantsToBe[] = (int[]) Arrays.get(
							answer, cubieCenterIndex);
					int contribution[] = (int[]) letterToFaceCenterCubieCoords
							.get(letter);
					contribution = Arrays.plus(contribution, contribution); // so
																			// at
																			// least
																			// 2
																			// in
																			// all
																			// nonzero
																			// dirs
					contribution = Arrays.clamp(contribution, -2, 2);

					int whereStickerWantsToBe[] = Arrays.plus(
							whereCubieCenterWantsToBe, contribution);

					// PRINT(index);
					// PRINT(cubieCenterIndex);
					// PRINT(whereCubieCenterWantsToBe);
					// PRINT(contribution);
					// PRINT(whereStickerWantsToBe);
					// System.out.println(" ");

					Arrays.set(answer, index, whereStickerWantsToBe);
				}
			}

			// System.out.println("    coords where cubie centers and stickers want to be: "+Arrays.toString(answer));

			// Convert from coords to indices, and null out the air.
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				int hist[] = Utils.histogram(n + 2, index);
				int coords[] = (int[]) Arrays.get(answer, index);
				if (hist[0] + hist[n + 1] > 1) {
					Assert(Arrays.isAll(coords, 0));
					Arrays.set(answer, index, null);
				} else {
					Arrays.set(answer, index, Utils.coordsToIndex(n, coords));
				}
			}

			// System.out.println("    to indices, with air nulled out: "+Arrays.toString(answer));

			// Make sure the mapping is 1-to-1 and onto.
			{
				Object inverse = Arrays.repeat(null, n + 2, d);
				for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
					int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
					//int hist[] = Utils.histogram(n + 2, index);
					int target[] = (int[]) Arrays.get(answer, index);
					if (target != null) {
						Assert(Arrays.equals(target, Arrays.clamp(target, 0,
								n + 1))); // index in bounds
						int targetHist[] = Utils.histogram(n + 2, target);
						Assert(targetHist[0] + targetHist[n + 1] <= 1); // not
																		// air
						Assert(Arrays.get(inverse, target) == null); // 1-to-1
						Arrays.set(inverse, target, index);
					}
				}
			}

			return answer;
		} // figureOutWhereIndicesWantToBe

		//
		// Find a sequence of moves
		// that properly positions the k-sticker cubies,
		// without messing up the fewer-sticker cubies,
		// but freely messing up the more-sticker cubies.
		// Assumes puzzle state is even.
		//
		private static java.util.ArrayList<int[]> position_ksticker_cubies(int k,
				int n, int d, Object puzzleIndices, int debugLevel) {
			if (debugLevel >= 1)
				System.out.println("    in position_" + k + "sticker_cubies");

			//
			// Decompose the permutation on the k-sticker cubies
			// into cycles, omitting cycles of length 1.
			//
			int cycles[][][] = {}; // XXX bogus O(n^2)
			Object scratch = Arrays.repeat(new Object(), n + 2, d); // set
																	// entries
																	// in
																	// scratch
																	// to null
																	// as we've
																	// seen them
																	// during
																	// position
																	// parity
																	// checking
			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				// It's the center of a k-sticker cubie
				// iff all coords are in [1..n]
				// and it's 1 or n in exactly k axis directions.
				int hist[] = Utils.histogram(n + 2, index);
				boolean isKStickerCubieCenter = (hist[0] == 0
						&& hist[n + 1] == 0 && hist[1] + hist[n] == k);
				if (isKStickerCubieCenter) {
					int cycle[][] = {}; // XXX bogus O(n^2)
					while (Arrays.get(scratch, index) != null) {
						cycle = Arrays.append(cycle, index);
						Arrays.set(scratch, index, null);
						index = (int[]) Arrays.get(puzzleIndices, index);
					}
					if (cycle.length > 1) // omit cycles of length 1
						cycles = Arrays.append(cycles, cycle);
				}
			}
			if (debugLevel >= 2)
				System.out.println("        cycles=" + Arrays.toString(cycles));

			//
			// Further decompose the cyclic decomposition
			// into 3-cycles.
			// This is possible because the puzzle state is even.
			//
			// XXX this code is duplicated below-- should make a common function
			//
			int tricycles[][][] = {}; // XXX bogus O(n^2)
			for (int iCycle = 0; iCycle < cycles.length; ++iCycle) {
				// PRINT(iCycle);
				// (a b c d e f g h) -> (a b c) (a d e) (a f g) (a h)
				int cycle[][] = cycles[iCycle];
				// PRINT(cycle);
				if (cycle == null) {
					// PRINT(__LINE__);
					continue; // already used this cycle
				}
				while (cycle.length != 1) {
					if (cycle.length == 2) {
						// Mingle with another cycle of even length,
						// to get a 3-cycle and a cycle of odd length.
						// XXX this can get n^2, should
						int other[][] = null;
						for (int jCycle = iCycle + 1; jCycle < cycles.length; ++jCycle) {
							// PRINT(jCycle);
							// PRINT(cycles[jCycle]);
							if (cycles[jCycle] != null
									&& cycles[jCycle].length % 2 == 0) {
								other = cycles[jCycle];
								cycles[jCycle] = null;
								break;
							}
						}
						Assert(other != null);
						// PRINT(other);
						// (a b) (c d e f g h) -> (a b c) (c a d e f g h)
						tricycles = Arrays.append(tricycles, Arrays.append(
								cycle, other[0]));
						cycle = Arrays.insert(other, 1, cycle[0]);
						// System.out.println("            join ->"+Arrays.toString(tricycles[tricycles.length-1])+Arrays.toString(cycle));
					} else {
						// (a b c d e f g) -> (a b c) (a d e f g)
						// or, in particular,
						// (a b c) -> (a b c) (a)
						tricycles = Arrays.append(tricycles, Arrays.subArray(
								cycle, 0, 3));
						cycle = Arrays.deleteRange(cycle, 1, 2);
						// System.out.println("            split ->"+Arrays.toString(tricycles[tricycles.length-1])+Arrays.toString(cycle));
					}
				}
			}
			if (debugLevel >= 2)
				System.out.println("        tricycles="
						+ Arrays.toString(tricycles));

			//
			// Find a sequence of moves that accomplishes the desired 3-cycles.
			//
			java.util.ArrayList<int[]> solution = new java.util.ArrayList<int[]>();
			for (int i = 0; i < tricycles.length; ++i)
				Arrays.addAll(solution, cycle_3_ksticker_cubies(k, n, d,
						tricycles[i], debugLevel));

			if (debugLevel >= 2)
				System.out.println("        solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 1)
				System.out.println("    out position_" + k + "sticker_cubies");
			return solution;
		} // position_ksticker_cubies

		// Find a sequence of moves that cycles three k-sticker cubies,
		// without messing up any other cubies with k or fewer stickers,
		// but freely messing up cubies with more than k stickers.
		private static int[][] cycle_3_ksticker_cubies(int k, int n, int d,
				int tricycle[/* 3 */][/* d */], int debugLevel) {
			if (debugLevel >= 2)
				System.out
						.println("        in cycle_3_" + k + "sticker_cubies");
			Object toLsequence_and_Lcycle[] = take_tricycle_to_L_of_ksticker_cubies_INDICES(
					k, n, d, tricycle, debugLevel);
			int toLsequence[][] = (int[][]) toLsequence_and_Lcycle[0];
			int Lcycle[][] = (int[][]) toLsequence_and_Lcycle[1];

			int fromLsequence[][] = PuzzleManipulation
					.reverseMoves(toLsequence);
			int solution[][] = Arrays.concat3(toLsequence,
					cycle_L_of_ksticker_cubies_INDICES(k, n, d, Lcycle,
							debugLevel), fromLsequence);
			if (debugLevel >= 3)
				System.out.println("            solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out.println("        out cycle_3_" + k
						+ "sticker_cubies");
			return solution;
		} // cycle_3_ksticker_cubies

		// Find a sequence of moves that cycles three k-sticker cubies
		// that are in the shape of an L -- that is,
		// they all lie in a single 2-plane, and the middle one
		// is the knee of the L:
		// A B
		// C
		// Don't mess up any other cubies with k or fewer stickers,
		// but freely mess up cubies with more than k stickers.
		private static int[][] cycle_L_of_ksticker_cubies_INDICES(int k, int n,
				int d, int Lcycle[/* 3 */][/* d */], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("            in cycle_L_of_" + k
						+ "sticker_cubies");
			if (debugLevel >= 3)
				System.out.println("                Lcycle="
						+ Arrays.toString(Lcycle));

			// argh, we are translating back and forth a lot
			int A[] = Utils.indexToCoords(n, Lcycle[0]);
			int B[] = Utils.indexToCoords(n, Lcycle[1]);
			int C[] = (Lcycle.length == 2 ? null : Utils.indexToCoords(n,
					Lcycle[2]));
			// PRINT(A);
			// PRINT(B);
			// PRINT(C);
			Assert(Arrays.normSqrd(A) == Arrays.normSqrd(B));
			Assert(Arrays.normSqrd(B) == Arrays.normSqrd(C));

			int solution[][];

			Assert(k >= 2); // impossible to move the 1-sticker cubies
			if (k == 2) {
				// Special case... our cycle-slabs function
				// doesn't know how to cycle an L of (d-2)-slabs
				// since it's awkward, but we don't need to.
				// Cycling an L of 2-sticker cubies is easy,
				// since we are allowed to mess up the higher-sticker
				// cubies...
				// On the standard Rubik's cube:
				// (BU FU FD): U U F F U U F F
				// Come to think of it, this does actually cycle
				// the (d-2)-slabs
				// (i.e. it cycles the 3 edges on the Rubik's cube)
				// however it flips two of them, which would violate
				// our cycle-slabs function's contract.
				// 

				// ABface = the face containing A,B (and not C)
				int ABfaceCenter[] = Arrays.average(A, B);
				int ABfaceAxis = 0;
				while (ABfaceCenter[ABfaceAxis] == 0)
					ABfaceAxis++;
				int ABfaceSign = (A[ABfaceAxis] < 0 ? -1 : 1);

				// BCface = the face containing B,C (and not A)
				int BCfaceCenter[] = Arrays.average(B, C);
				int BCfaceAxis = 0;
				while (BCfaceCenter[BCfaceAxis] == 0)
					BCfaceAxis++;
				int BCfaceSign = (B[BCfaceAxis] < 0 ? -1 : 1);

				// PRINT(ABfaceCenter);
				// PRINT(ABfaceAxis);
				// PRINT(ABfaceSign);
				// PRINT(BCfaceCenter);
				// PRINT(BCfaceAxis);
				// PRINT(BCfaceSign);

				int AB[][] = find_oneface_twist_sequence_taking_these_coords_to_those_coords(
						ABfaceAxis, ABfaceSign, new int[][] { A },
						new int[][] { B }, 1, debugLevel);
				Assert(AB.length == 2); // it's a 180 degree twist
				int BC[][] = find_oneface_twist_sequence_taking_these_coords_to_those_coords(
						BCfaceAxis, BCfaceSign, new int[][] { B },
						new int[][] { C }, 1, debugLevel);
				Assert(BC.length == 2); // it's a 180 degree twist

				solution = (int[][]) Arrays.concat4(AB, BC, AB, BC);
			} else // k >= 3
			{
				solution = cycle_L_of_kslabs(d - k, n, d, A, B, C, debugLevel);
			}

			if (debugLevel >= 3)
				System.out.println("                solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out.println("            out cycle_L_of_" + k
						+ "sticker_cubies");
			return solution;
		} // cycle_L_of_ksticker_cubies

		//
		// A 0-slab is a corner cubie,
		// a 1-slab is the set of n cubies along some edge,
		// a 2-slab is the set of n^2 cubies on some 2-face, etc.
		// a (d-1)-slab is a face,
		// a d-slab is the whole puzzle.
		// Three k-slabs are said to form an L
		// if they are the extrusion (in some consistent set of k dimensions)
		// of three corner cubies that form an L (see above).
		// This function finds a sequence of moves
		// that moves the k-slabs a to b, b to c, and c to a,
		// keeping the orientations of the slabs consistent
		// in the slab directions,
		// and without messing up the rest of the puzzle.
		// Only works for k <= d-3
		// (cycling three (d-2)-slabs while keeping their orientations
		// consistent is more awkward, so we do something else
		// in that case instead, in the calling function).
		//
		// We represent a k-slab by the coordinates of its center
		// (which will be zero in k coordinates
		// and nonzero in d-k coordinates).
		//
		private static int[][] cycle_L_of_kslabs(int k, int n, int nDims,
				int a[], int b[], int c[], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("                in cycle_L_of_" + k
						+ "slabs");
			Assert(k <= nDims - 3); // assumption, see above
			int solution[][];
			if (k == nDims - 3) {
				//
				// Base case of the recursion--
				// this is essentially the case
				// of cycling three corner (3-sticker) cubies
				// on a standard Rubik's cube, which can be done in 8 moves.
				// From Tom Davis's "Permutation Groups and Rubik's Cube":
				// http://mathcircle.berkeley.edu/BMC3/perm/node15.html:
				// (LUF RUB LUB): U R U' L' U R' U' L
				// (where unprimed means clockwise,
				// primed means counterclockwise)
				//
				// Looking top-down at the cube:
				//            
				// b = LUB<-RUB = a
				// | ^
				// | /
				// v /
				// c = LUF
				//

				// The face U is the unique face containing a,b,c...
				int UfaceAxis = 0;
				while (a[UfaceAxis] == 0 || a[UfaceAxis] != b[UfaceAxis]
						|| b[UfaceAxis] != c[UfaceAxis])
					UfaceAxis++;
				int UfaceSign = (a[UfaceAxis] < 0 ? -1 : 1);

				// The face L is the unique face containing b,c but not a.
				int LfaceAxis = 0;
				while (a[LfaceAxis] == 0 || a[LfaceAxis] == b[LfaceAxis]
						|| b[LfaceAxis] != c[LfaceAxis])
					LfaceAxis++;
				int LfaceSign = (b[LfaceAxis] < 0 ? -1 : 1);

				// The face F is the unique face containing c but not a,b.
				int FfaceAxis = 0;
				while (a[FfaceAxis] == 0 || a[FfaceAxis] != b[FfaceAxis]
						|| b[FfaceAxis] == c[FfaceAxis])
					FfaceAxis++;
				int FfaceSign = (c[FfaceAxis] < 0 ? -1 : 1);

				// The face R is the face opposite L.
				int RfaceAxis = LfaceAxis;
				int RfaceSign = -LfaceSign;

				int U[] = PuzzleManipulation.makeTwist90(UfaceAxis, UfaceSign,
						FfaceAxis, FfaceSign, LfaceAxis, LfaceSign, 1);
				int L[] = PuzzleManipulation.makeTwist90(LfaceAxis, LfaceSign,
						UfaceAxis, UfaceSign, FfaceAxis, FfaceSign, 1);
				int R[] = PuzzleManipulation.makeTwist90(RfaceAxis, RfaceSign,
						FfaceAxis, FfaceSign, UfaceAxis, UfaceSign, 1);

				int Uprime[] = PuzzleManipulation.reverseMove(U);
				int Lprime[] = PuzzleManipulation.reverseMove(L);
				int Rprime[] = PuzzleManipulation.reverseMove(R);

				solution = new int[][] { U, R, Uprime, Lprime, U, Rprime,
						Uprime, L };
			} else // k < nDims-2
			{
				//
				// Assume (inductive step) that we know how
				// to cycle any L of (k+1)-slabs.
				// We want to cycle the L of k-slabs a,b,c.
				// Let d = a-b+c (i.e. the 4th corner of the square);
				// d will be used as a helper k-slab.
				//
				// Take any axis orthogonal to these k-slabs,
				// and extrude a,b,c,d along that axis
				// to form (k+1)-slabs A,B,C,D.
				//
				// Observe that the L-tricycle (a b c)
				// can be expressed as the composition
				// of two L-tricycles (d a b) (a d c)
				// (the first in the original direction,
				// the second in the opposite direction).
				// Since the second one (a d c) is in the opposite direction
				// from the first one (d a b),
				// it is the conjugation of (a d c)^-1 by a rotation
				// of the square, namely:
				// (a d c) = (a b c d) (d a b)^-1 (a b c d)^-1
				// Putting these facts together,
				// we get a useful decomposition of the desired cycle:
				// (a b c) = (d a b) (a b c d) (d a b)^-1 (a b c d)^-1
				//
				// We will do the (d a b) and (d a b)^-1 parts
				// as (D A B) and (D A B)^-1 (which we know
				// how to do recursively), and we will do
				// the (a b c d) and (a b c d)^-1 parts
				// by simply twisting the face
				// that contains a,b,c,d but none of the rest
				// of A,B,C,D.
				// The result: (a b c) get cycled as desired
				// and the rest of the puzzle is back to the way it was.
				// Woo hoo!
				//
				// So:
				// foo = cycle_L_of_kslabs(D,A,B)
				// bar = twist of the face containing a,b,c,d
				// but not any of the rest of A,B,C,D,
				// that cycles (a b c d).
				// and the answer is foo bar foo^-1 bar^-1.
				//

				int d[] = Arrays.plus(Arrays.minus(a, b), c);

				// faceAxis = any axis in which a,b,c,d are nonzero
				// and have the same coordinate
				int faceAxis = 0;
				while (a[faceAxis] == 0 || a[faceAxis] != b[faceAxis]
						|| b[faceAxis] != c[faceAxis])
					faceAxis++;
				int faceSign = (a[faceAxis] < 0 ? -1 : 1);

				// Extrude the k-slabs a,b,c,d
				// in the direction normal to faceAxis
				// to get the (k+1)-slabs A,B,C,D...
				int A[] = Arrays.copy(a);
				A[faceAxis] = 0;
				int B[] = Arrays.copy(b);
				B[faceAxis] = 0;
				int C[] = Arrays.copy(c);
				C[faceAxis] = 0;
				int D[] = Arrays.copy(d);
				D[faceAxis] = 0;

				// foo is the sequence of moves that cycles
				// the L of (k+1)-slabs (D A B)
				int foo[][] = cycle_L_of_kslabs(k + 1, n, nDims, D, A, B,
						debugLevel);

				// bar is the single 90 degree twist of this face that takes
				// a to b and b to c (and c to d and d to a)...
				int bar[][] = find_oneface_twist_sequence_taking_these_coords_to_those_coords(
						faceAxis, faceSign, new int[][] { a, b }, new int[][] {
								b, c }, 1, debugLevel);
				Assert(bar.length == 1);

				// Assert that bar does indeed cycle (a b c d)...
				Assert(Arrays.equals(PuzzleManipulation.twist90coords(n,
						bar[0], a), b));
				Assert(Arrays.equals(PuzzleManipulation.twist90coords(n,
						bar[0], b), c));
				Assert(Arrays.equals(PuzzleManipulation.twist90coords(n,
						bar[0], c), d));
				Assert(Arrays.equals(PuzzleManipulation.twist90coords(n,
						bar[0], d), a));

				solution = (int[][]) Arrays.concat4(foo, bar,
						PuzzleManipulation.reverseMoves(foo),
						PuzzleManipulation.reverseMoves(bar));
			} // k < nDims-2 inductive case
			if (debugLevel >= 2)
				System.out.println("                out cycle_L_of_" + k
						+ "slabs");
			return solution;
		} // cycle_L_of_kslabs

		// Take the given three cubie centers to an L,
		// freely messing up everything else.
		// Tricycle is allowed to be of length 2 as well, meaning
		// just take them to an I.
		private static Object[] take_tricycle_to_L_of_ksticker_cubies_INDICES(
				int k, int n, int d, int tricycle[][], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("                 in take_tricycle_to_L_of_"
						+ k + "sticker_cubies");

			int A[] = Utils.indexToCoords(n, tricycle[0]);
			int B[] = Utils.indexToCoords(n, tricycle[1]);
			int C[] = (tricycle.length == 2 ? null : Utils.indexToCoords(n,
					tricycle[2]));

			if (debugLevel >= 3) {
				System.out.println("                    tricycle = "
						+ Arrays.toString(tricycle));
				System.out.println("                    A = "
						+ Arrays.toString(A));
				System.out.println("                    B = "
						+ Arrays.toString(B));
				System.out.println("                    C = "
						+ Arrays.toString(C));
			}

			Assert(!Arrays.equals(A, B));
			if (C != null) {
				Assert(!Arrays.equals(B, C));
				Assert(!Arrays.equals(C, A));
			}

			int solution[][] = {};

			// PRINT(__LINE__);
			// Move B so it's next to A, without perturbing A
			{
				// Goal is to make B differ from A
				// in exactly one coordinate axis.
				int nIndicesDifferent = Arrays.nIndicesDifferent(A, B);
				// PRINT(nIndicesDifferent);
				Assert(nIndicesDifferent >= 1);
				if (nIndicesDifferent > 1) {
					// BtargetFaceAxis = any axis in which A is nonzero
					// (doesn't matter what B is along that axis)
					int BtargetFaceAxis = 0;
					while (A[BtargetFaceAxis] == 0)
						BtargetFaceAxis++;
					int BtargetFaceSign = (A[BtargetFaceAxis] < 0 ? 1 : -1); // opposite
																				// A

					int Btarget[] = Arrays.copy(A);
					Btarget[BtargetFaceAxis] = -A[BtargetFaceAxis];

					// PRINT(BtargetFaceAxis);
					// PRINT(BtargetFaceSign);
					// PRINT(Btarget);

					// Get B onto BtargetFace, if it's not already there...
					if (B[BtargetFaceAxis] != Btarget[BtargetFaceAxis]) {
						// BhelperFace = any face that contains B but not A
						int BhelperFaceAxis = 0;
						while (B[BhelperFaceAxis] == 0
								|| A[BhelperFaceAxis] == B[BhelperFaceAxis])
							BhelperFaceAxis++;
						int BhelperFaceSign = (B[BhelperFaceAxis] < 0 ? -1 : 1);

						// PRINT(BhelperFaceAxis);
						// PRINT(BhelperFaceSign);

						Assert(BhelperFaceAxis != BtargetFaceAxis);

						// Bwaystation = a point of the right type
						// that is on both BhelperFace and BtargetFace
						// and is as close as possible to B.
						// Start by shooting straight from B to BtargetFace...
						int Bwaystation[] = Arrays.copy(B);
						Bwaystation[BtargetFaceAxis] = Btarget[BtargetFaceAxis];
						// PRINT(Bwaystation);
						// But if we changed a coordinate
						// from zero to nonzero, we destroyed the type...
						// to get it back, we need to change some other
						// coordinate from nonzero (in fact the same
						// absolute value) to zero.
						// But be careful not to leave BhelperFace
						// when doing this; i.e. don't choose BhelperFaceAxis
						// as the axis to zero out.
						if (B[BtargetFaceAxis] == 0) {
							int axisToZeroOut = 0;
							while (axisToZeroOut == BhelperFaceAxis
									|| axisToZeroOut == BtargetFaceAxis
									|| Bwaystation[axisToZeroOut] == 0
									|| Math.abs(Bwaystation[axisToZeroOut]) != Math
											.abs(Bwaystation[BtargetFaceAxis]))
								axisToZeroOut++;
							Bwaystation[axisToZeroOut] = 0;
						}
						// PRINT(Bwaystation);
						solution = Arrays
								.concat(
										solution,
										find_oneface_twist_sequence_taking_these_coords_to_those_coords(
												BhelperFaceAxis,
												BhelperFaceSign,
												new int[][] { B },
												new int[][] { Bwaystation }, 1,
												debugLevel));
						B = Bwaystation;
					}

					// Okay, now B is on BtargetFace...
					// twist that face as necessary to move it to Btarget.
					solution = Arrays
							.concat(
									solution,
									find_oneface_twist_sequence_taking_these_coords_to_those_coords(
											BtargetFaceAxis, BtargetFaceSign,
											new int[][] { B },
											new int[][] { Btarget }, 1,
											debugLevel));
					B = Btarget;
				}
				Assert(Arrays.nIndicesDifferent(A, B) == 1);
				Assert(Arrays.normSqrd(A) == Arrays.normSqrd(B));
			}

			if (C != null) {
				// Apply what we've got so far to C...
				for (int i = 0; i < solution.length; ++i) {
					// System.out.print("    "+Arrays.toString(solution[i])+" takes "+Arrays.toString(C)+" to ");
					C = PuzzleManipulation.twist90coords(n, solution[i], C);
					// System.out.println(Arrays.toString(C));
				}

				// PRINT(__LINE__);
				// PRINT(solution);
				// PRINT(A);
				// PRINT(B);
				// PRINT(C);

				Assert(Arrays.nIndicesDifferent(A, B) == 1);
				Assert(!Arrays.equals(B, C));
				Assert(!Arrays.equals(C, A));

				// A and B are good;
				// now move C so it's next to B, without perturbing A or B.
				// XXX this is very similar to the above, should think about how
				// to combine into one section of code to be cleaner
				{
					// Goal is to make C differ from B
					// in exactly one coordinate axis
					// (and that axis can't be the axis in which A and B
					// differ).
					int nIndicesDifferent = Arrays.nIndicesDifferent(B, C);
					// PRINT(nIndicesDifferent);
					Assert(nIndicesDifferent >= 1);
					if (nIndicesDifferent > 1) {
						// CtargetFaceAxis = any axis in which B is nonzero
						// and equal to A
						// (doesn't matter what C is along that axis)
						int CtargetFaceAxis = 0;
						while (B[CtargetFaceAxis] == 0
								|| B[CtargetFaceAxis] != A[CtargetFaceAxis])
							CtargetFaceAxis++;
						int CtargetFaceSign = (B[CtargetFaceAxis] < 0 ? 1 : -1); // opposite
																					// B

						int Ctarget[] = Arrays.copy(B);
						Ctarget[CtargetFaceAxis] = -B[CtargetFaceAxis];

						// PRINT(CtargetFaceAxis);
						// PRINT(CtargetFaceSign);
						// PRINT(Ctarget);

						// Get C onto CtargetFace, if it's not already there...
						if (C[CtargetFaceAxis] != Ctarget[CtargetFaceAxis]) {
							// ChelperFace = any face that contains C but not B
							// or A
							int ChelperFaceAxis = 0;
							while (C[ChelperFaceAxis] == 0
									|| B[ChelperFaceAxis] == C[ChelperFaceAxis]
									|| A[ChelperFaceAxis] == C[ChelperFaceAxis])
								ChelperFaceAxis++;
							int ChelperFaceSign = (C[ChelperFaceAxis] < 0 ? -1
									: 1);

							// PRINT(ChelperFaceAxis);
							// PRINT(ChelperFaceSign);

							Assert(ChelperFaceAxis != CtargetFaceAxis);

							// Cwaystation = a point of the right type
							// that is on both ChelperFace and CtargetFace
							// and is as close as possible to C.
							// Start by shooting straight from C to
							// CtargetFace...
							int Cwaystation[] = Arrays.copy(C);
							Cwaystation[CtargetFaceAxis] = Ctarget[CtargetFaceAxis];
							// PRINT(Cwaystation);
							// But if we changed a coordinate
							// from zero to nonzero, we destroyed the type...
							// to get it back, we need to change some other
							// coordinate from nonzero (in fact the same
							// absolute value) to zero.
							// But be careful not to leave ChelperFace
							// when doing this; i.e. don't choose
							// ChelperFaceAxis
							// as the axis to zero out.
							if (C[CtargetFaceAxis] == 0) {
								int axisToZeroOut = 0;
								while (axisToZeroOut == ChelperFaceAxis
										|| axisToZeroOut == CtargetFaceAxis
										|| Cwaystation[axisToZeroOut] == 0
										|| Math.abs(Cwaystation[axisToZeroOut]) != Math
												.abs(Cwaystation[CtargetFaceAxis]))
									axisToZeroOut++;
								Cwaystation[axisToZeroOut] = 0;
							}
							// PRINT(Cwaystation);
							solution = Arrays
									.concat(
											solution,
											find_oneface_twist_sequence_taking_these_coords_to_those_coords(
													ChelperFaceAxis,
													ChelperFaceSign,
													new int[][] { C },
													new int[][] { Cwaystation },
													1, debugLevel));
							C = Cwaystation;
						}

						// Okay, now C is on CtargetFace...
						// twist that face as necessary to move it to Ctarget.
						solution = Arrays
								.concat(
										solution,
										find_oneface_twist_sequence_taking_these_coords_to_those_coords(
												CtargetFaceAxis,
												CtargetFaceSign,
												new int[][] { C },
												new int[][] { Ctarget }, 1,
												debugLevel));
						C = Ctarget;
					}
				}

				// PRINT(__LINE__);

				Assert(Arrays.nIndicesDifferent(A, B) == 1);
				Assert(Arrays.normSqrd(A) == Arrays.normSqrd(B));
				Assert(Arrays.nIndicesDifferent(B, C) == 1);
				Assert(Arrays.nIndicesDifferent(C, A) == 2);
				Assert(Arrays.normSqrd(B) == Arrays.normSqrd(C));
			}

			int Lcycle[][] = { Utils.coordsToIndex(n, A),
					Utils.coordsToIndex(n, B), };
			if (C != null)
				Lcycle = Arrays.append(Lcycle, Utils.coordsToIndex(n, C));

			if (debugLevel >= 3)
				System.out.println("                    solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out
						.println("                 out take_tricycle_to_L_of_"
								+ k + "sticker_cubies");
			return new Object[] { solution, Lcycle };
		} // take_tricycle_to_L_of_ksticker_cubies

		// A 90 degree rotation of the whole puzzle is expressed
		// simply as a {fromAxis,toAxis} pair. Return a list of such.
		// Throws an exception on failure (i.e. don't do that!),
		// unless it's a mirror image and
		// returnNullOnIfFailedButIsMirrorImage is true,
		// in which case it returns null.
		private static int[][] find_rotation_sequence_taking_these_coords_to_those_coords(
				int theseCoords[][], int thoseCoords[][],
				boolean returnNullIfFailedButIsMirrorImage, int debugLevel) {
			if (debugLevel >= 2)
				System.out
						.println("                    in find_rotation_sequence_taking_these_coords_to_those_coords");
			if (debugLevel >= 3)
				System.out.println("                        theseCoords = "
						+ Arrays.toString(theseCoords));
			if (debugLevel >= 3)
				System.out.println("                        thoseCoords = "
						+ Arrays.toString(thoseCoords));

			int originalTheseCoords[][] = theseCoords;
			theseCoords = Arrays.shallowCopy(theseCoords); // so we can modify
															// them

			Assert(theseCoords.length == thoseCoords.length);
			int nCoords = theseCoords.length;
			int d = (nCoords == 0 ? 0 : theseCoords[0].length);

			// Simple (though not complete) sanity check on these and those--
			// assert they are equal distances from the origin.
			for (int i = 0; i < nCoords; ++i)
				Assert(Arrays.normSqrd(theseCoords[i]) == Arrays
						.normSqrd(thoseCoords[i]));

			// Keep track of which axes are happy
			// and anti-happy...
			boolean happy[] = new boolean[d];
			boolean antihappy[] = new boolean[d];
			for (int i = 0; i < d; ++i) {
				happy[i] = Arrays.columnEquals(theseCoords, i, thoseCoords, i,
						1);
				antihappy[i] = Arrays.columnEquals(theseCoords, i, thoseCoords,
						i, -1);
			}

			int solution[][] = {}; // quadratic time to build by appending
									// repeatedly to an array, but solution
									// length is bounded by bounded by at most d
									// (or so) (I think) so we're it's okay

			//
			// First pass: get all columns happy-or-antihappy
			// using 90 degree rotations (column swaps negating one of them).
			//
			if (debugLevel >= 4)
				System.out.println("                        first pass");
			for (int i = 0; i < d; ++i) {
				if (debugLevel >= 4)
					System.out
							.println("                                theseCoords = "
									+ Arrays.toString(theseCoords));
				if (debugLevel >= 4)
					System.out
							.println("                                thoseCoords = "
									+ Arrays.toString(thoseCoords));
				if (debugLevel >= 4)
					System.out
							.println("                                happy = "
									+ Arrays.toString(happy));
				if (debugLevel >= 4)
					System.out
							.println("                                antihappy = "
									+ Arrays.toString(antihappy));
				// if (debugLevel >= 4)
				// System.out.println("                            happy["+i+"] = "+happy[i]+", antihappy["+i+"] = "+antihappy[i]);
				if (debugLevel >= 4)
					System.out.println("                            happy[" + i
							+ "] = " + happy[i]);
				if (debugLevel >= 4)
					System.out.println("                            antihappy["
							+ i + "] = " + antihappy[i]);

				// axis permutation is now right for 0..i-1.
				if (happy[i] || antihappy[i])
					continue;
				//
				// Get totally happy via a 90 degree rotation
				// with some other later neither-happy-nor-antihappy guy.
				// First preference is to make the other guy happy in the
				// process.
				// Second preference is to just make me happy
				// (which will make him a different
				// neither-happy-nor-antihappy).
				//
				for (int iPicky = 0; iPicky < 2; ++iPicky) {
					boolean picky = (iPicky == 0 ? true : false);
					if (debugLevel >= 4)
						System.out
								.println("                            picky = "
										+ picky);
					for (int j = i + 1; j < d; ++j) {
						if (happy[j] || antihappy[j])
							continue;
						if (Arrays.columnEquals(theseCoords, j, thoseCoords, i,
								1)
								&& (!picky || Arrays.columnEquals(theseCoords,
										i, thoseCoords, j, -1))) {
							// j->i
							if (debugLevel >= 4)
								System.out
										.println("                            "
												+ j + "->" + i);
							solution = Arrays.append(solution,
									new int[] { j, i });
							theseCoords = PuzzleManipulation.rot90coordss(j, i,
									theseCoords);
							happy[i] = true;
							antihappy[i] = Arrays.columnEquals(theseCoords, i,
									thoseCoords, i, -1);
							if (picky)
								happy[j] = true;
							antihappy[j] = Arrays.columnEquals(theseCoords, j,
									thoseCoords, j, -1);
							break;
						} else if (Arrays.columnEquals(theseCoords, j,
								thoseCoords, i, -1)
								&& (!picky || Arrays.columnEquals(theseCoords,
										i, thoseCoords, j, 1))) {
							// i->j
							if (debugLevel >= 4)
								System.out
										.println("                            "
												+ i + "->" + j);
							solution = Arrays.append(solution,
									new int[] { i, j });
							theseCoords = PuzzleManipulation.rot90coordss(i, j,
									theseCoords);
							happy[i] = true;
							antihappy[i] = Arrays.columnEquals(theseCoords, i,
									thoseCoords, i, -1);
							if (picky)
								happy[j] = true;
							antihappy[j] = Arrays.columnEquals(theseCoords, j,
									thoseCoords, j, -1);
							break;
						}
					}
					if (happy[i])
						break;
				}
				// We definitely made i happy (not antihappy)...
				if (!happy[i])
					throw new Error("theseCoords = "
							+ Arrays.toString(originalTheseCoords)
							+ " can't be rotated to thoseCoords = "
							+ Arrays.toString(thoseCoords));
			}

			// Second pass: all columns are now happy or antihappy
			// (or both, if zero).
			// Fix pairs of antihappy-but-not-happy ones
			// with 180 degree rotations.
			if (debugLevel >= 4)
				System.out.println("                        second pass");
			int remainingUnhappyAxis = -1;
			for (int i = 0; i < d; ++i) {
				if (happy[i])
					continue;
				for (int j = i + 1; j < d; ++j) {
					if (happy[j])
						continue;
					// i->j twice
					if (debugLevel >= 4)
						System.out.println("                            " + i
								+ "->" + j + " twice");
					for (int iTwice = 0; iTwice < 2; ++iTwice) {
						solution = Arrays.append(solution, new int[] { i, j });
						theseCoords = PuzzleManipulation.rot90coordss(i, j,
								theseCoords);
					}
					happy[i] = true;
					antihappy[i] = false;
					happy[j] = true;
					antihappy[j] = false;
					break;
				}
				if (!happy[i])
					remainingUnhappyAxis = i;
			}

			// Third pass: there is at most one remaining unhappy column,
			// and it's antihappy.
			// There are three possible tricks for fixing
			// this last antihappy-but-not-happy column,
			// requiring 1, 2, or 3 moves respectively.

			if (remainingUnhappyAxis != -1) {
				if (debugLevel >= 4)
					System.out.println("                        "
							+ remainingUnhappyAxis + " is still not right");
				int i = remainingUnhappyAxis;
				//
				// Trick #1 (1 move):
				// Find another column j that's equal or anti-equal
				// to the unhappy column i.
				// Then the 90-degree rotation i->j or j->i
				// makes i happy and keeps j happy.
				//
				Assert(!happy[i]);
				{
					if (debugLevel >= 3)
						System.out
								.println("                        Trying trick #1");
					for (int j = 0; j < d; ++j) {
						if (j == i)
							continue;
						int sign;
						if (Arrays.columnEquals(theseCoords, i, theseCoords, j,
								sign = 1)
								|| Arrays.columnEquals(theseCoords, i,
										theseCoords, j, sign = -1)) {
							int fromAxis = sign == 1 ? i : j;
							int toAxis = sign == 1 ? j : i;
							// fromAxis->toAxis
							if (debugLevel >= 4)
								System.out
										.println("                            "
												+ fromAxis + "->" + toAxis + "");
							solution = Arrays.append(solution, new int[] {
									fromAxis, toAxis });
							theseCoords = PuzzleManipulation.rot90coordss(
									fromAxis, toAxis, theseCoords);
							happy[i] = true;
							antihappy[i] = false;
							break;
						}
					}
				}

				//
				// Trick #2 (2 moves):
				// Find a happy-and-antihappy (i.e. zero) column j
				// and do a 180-degree rotation of i,j to make i happy
				// while keeping j happy (and antihappy).
				//
				if (!happy[i]) {
					if (debugLevel >= 3)
						System.out
								.println("                        Trying trick #2");
					for (int j = 0; j < d; ++j) {
						if (j == i)
							continue;
						if (Arrays.columnEquals(theseCoords, j, theseCoords, j,
								-1)) {
							// i->j twice
							if (debugLevel >= 4)
								System.out
										.println("                            "
												+ i + "->" + j + " twice");
							for (int iTwice = 0; iTwice < 2; ++iTwice) {
								solution = Arrays.append(solution, new int[] {
										i, j });
								theseCoords = PuzzleManipulation.rot90coordss(
										i, j, theseCoords);
							}
							happy[i] = true;
							antihappy[i] = false;
							break;
						}
					}
				}

				//
				// Trick #3 (3 moves):
				// Find two other columns j,k which are equal
				// or anti-equal to each other (and happy of course).
				// Rotate j->k, which makes one of j,k antihappy,
				// and then 180-degree rotate that now-antihappy one
				// with i.
				//
				if (!happy[i]) {
					if (debugLevel >= 3)
						System.out
								.println("                        Trying trick #3");
					for (int j = 0; j < d; ++j) {
						if (j == i)
							continue;
						for (int k = 0; k < d; ++k) {
							if (k == i || k == j)
								continue;
							int sign;
							if (Arrays.columnEquals(theseCoords, j,
									theseCoords, k, sign = 1)
									|| Arrays.columnEquals(theseCoords, j,
											theseCoords, k, sign = -1)) {
								// j->k
								if (debugLevel >= 4)
									System.out
											.println("                            "
													+ j + "->" + k + "");
								solution = Arrays.append(solution, new int[] {
										j, k });
								theseCoords = PuzzleManipulation.rot90coordss(
										j, k, theseCoords);
								int otherAntihappyAxis = (sign == 1 ? j : k);
								// i -> otherAntihappyAxis twice
								if (debugLevel >= 4)
									System.out
											.println("                            "
													+ i
													+ "->"
													+ otherAntihappyAxis
													+ " twice");
								for (int iTwice = 0; iTwice < 2; ++iTwice) {
									solution = Arrays
											.append(solution, new int[] { i,
													otherAntihappyAxis });
									theseCoords = PuzzleManipulation
											.rot90coordss(i,
													otherAntihappyAxis,
													theseCoords);
								}
								happy[i] = true;
								antihappy[i] = false;
								break;
							}
						}
						if (happy[i])
							break;
					}
				}

				if (!happy[i]) {
					// All the tricks for reversing this axis failed--
					// it's inside out.
					if (returnNullIfFailedButIsMirrorImage) {
						if (debugLevel >= 2)
							System.out
									.println("                    out find_rotation_sequence_taking_these_coords_to_those_coords (returning null because it's inside out)");
						return null;
					} else
						throw new Error("theseCoords = "
								+ Arrays.toString(originalTheseCoords)
								+ " can't be rotated to thoseCoords = "
								+ Arrays.toString(thoseCoords)
								+ " (inside out!)");
				}
			}

			// Assert that we kept track of happy and antihappy right
			// and that everyone is now happy...
			for (int i = 0; i < d; ++i) {
				Assert(happy[i] == Arrays.columnEquals(theseCoords, i,
						thoseCoords, i, 1));
				Assert(antihappy[i] == Arrays.columnEquals(theseCoords, i,
						thoseCoords, i, -1));
				Assert(happy[i]);
			}

			if (debugLevel >= 3)
				System.out.println("                        solution = "
						+ Arrays.toString(solution));
			//
			// Assert that solution applied to originalTheseCoords
			// are in fact thoseCoords (i.e. that we did
			// what we were contracted to do)...
			//
			for (int iThese = 0; iThese < theseCoords.length; ++iThese)
				Assert(Arrays.equals(PuzzleManipulation.rot90sCoords(solution,
						originalTheseCoords[iThese]), thoseCoords[iThese]));

			if (debugLevel >= 2)
				System.out
						.println("                    out find_rotation_sequence_taking_these_coords_to_those_coords");
			return solution;
		} // find_rotation_sequence_taking_these_coords_to_those_coords

		// Finds a sequence of twists of a single face
		// that take these coords to those coords.
		// This is accomplished by using the generic rot function
		// on the original constraints with one more constraint added:
		// don't move the face center.
		// Note, we do NOT sanity-check whether the coords in question
		// are actually within slicesMask
		// 
		private static int[][] find_oneface_twist_sequence_taking_these_coords_to_those_coords(
				int faceAxis, int faceSign, int theseCoords[][],
				int thoseCoords[][], int slicesMask, int debugLevel) {
			// System.out.println("                in find_oneface_twist_sequence_taking_these_coords_to_those_coords");
			// PRINT(faceAxis);
			// PRINT(faceSign);
			// PRINT(theseCoords);
			// PRINT(thoseCoords);
			// PRINT(slicesMask);
			if (theseCoords.length == 0)
				return new int[0][5]; // protects the d calculation
			int d = theseCoords[0].length;
			int faceCenter[] = Arrays.repeat(0, d);
			faceCenter[faceAxis] = faceSign; // actually not necessarily the
												// face center but it's in the
												// right direction which is what
												// matters

			int theseCoordsAndFaceCenter[][] = Arrays.append(theseCoords,
					faceCenter);
			int thoseCoordsAndFaceCenter[][] = Arrays.append(thoseCoords,
					faceCenter);
			int rots[][] = find_rotation_sequence_taking_these_coords_to_those_coords(
					theseCoordsAndFaceCenter, thoseCoordsAndFaceCenter, false,
					debugLevel);
			int twists[][] = new int[rots.length][];
			for (int i = 0; i < rots.length; ++i)
				twists[i] = new int[] { faceAxis, faceSign, rots[i][0],
						rots[i][1], slicesMask };
			// PRINT(twists);
			// System.out.println("                out find_oneface_twist_sequence_taking_these_coords_to_those_coords");
			return twists;
		} // find_oneface_twist_sequence_taking_these_coords_to_those_coords

		//
		// Assuming the k-sticker cubies are positioned correctly,
		// find a sequence of moves that orients them correctly,
		// without messing up the fewer-sticker cubies,
		// but freely messing up the more-sticker cubies.
		//
		private static java.util.ArrayList<int[]> orient_ksticker_cubies(int k, int n,
				int d, Object puzzleIndices, int debugLevel) {
			if (debugLevel >= 1)
				System.out.println("    in orient_" + k + "sticker_cubies");
			java.util.ArrayList<int[]> solution = new java.util.ArrayList<int[]>();

			//
			// Decompose the permutation on the k-sticker cubie stickers
			// into cycles, omitting cycles of length 1.
			//
			java.util.ArrayList<int[][]> cyclesArrayList = new java.util.ArrayList<int[][]>();
			Object scratch = Arrays.repeat(new Object(), n + 2, d); // set
																	// entries
																	// in
																	// scratch
																	// to null
																	// as we've
																	// seen them
																	// during
																	// position
																	// parity
																	// checking
			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				// It's a sticker of a k-sticker cubie
				// iff it's a sticker index (i.e. it's 0 or n+1 in exactly 1
				// axis)
				// and <=1 or >=n in exactly k axis directions.
				int hist[] = Utils.histogram(n + 2, index);
				boolean isKStickerCubieSticker = (hist[0] + hist[n + 1] == 1 && hist[0]
						+ hist[1] + hist[n] + hist[n + 1] == k);
				if (isKStickerCubieSticker) {
					int cycle[][] = {}; // quadratic time to build by appending
										// repeatedly to an array, but grows to
										// at most d so it's fine
					while (Arrays.get(scratch, index) != null) {
						cycle = Arrays.append(cycle, index);
						Arrays.set(scratch, index, null);
						index = (int[]) Arrays.get(puzzleIndices, index);
					}
					if (cycle.length > 1) // omit cycles of length 1
						cyclesArrayList.add(cycle);
				}
			}
			int cycles[][][] = cyclesArrayList.toArray(new int[0][][]);
			if (debugLevel >= 2)
				System.out.println("        cycles=" + Arrays.toString(cycles));

			// Assert that each cycle stayed within a single cubie...
			for (int i = 0; i < cycles.length; ++i)
				for (int j = 1; j < cycles[i].length; ++j)
					Assert(Utils.areOnSameCubie(n, cycles[i][j], cycles[i][0]));

			//
			// The primitives we will use for orienting are;
			// For corners:
			// twirl (i.e. cycle 3 stickers on) two corner cubies
			// in opposite directions
			// (although for d>=5 twirls of corners don't have a sign, so the
			// "in opposite directions" clause is only meaningful
			// for d==3 and d==4)
			// For non-corners:
			// flip (i.e. swap 2 stickers on) two non-corner cubies
			// So grind up the cycles until they are of the appropriate
			// form, depending on whether we are working on corners or not.
			//        

			if (k < d) // non-corners
			{
				// Need all sticker cycles to occur
				// in pairs of flips (a b) (c d)
				// where stickers a,b are on one (non-corner) cubie
				// and stickers c,d are on a different one.
				java.util.ArrayList<int[][][]> flipPairs = new java.util.ArrayList<int[][][]>(); // int
																			// flipPairs[][/*2*/][/*2*/][/*d*/];
				int happyHelperFlip[][] = null;
				for (int iCycle = 0; iCycle < cycles.length; ++iCycle) {
					int cycle[][] = cycles[iCycle];
					int cubieCenter[] = Arrays.clamp(cycle[0], 1, n);
					while (cycle.length != 1) {
						if (happyHelperFlip != null) {
							// Extract one swap from cycle
							// and pair it with happyHelperFlip.
							// Before: (a b c d e)
							// After: (a b) (a c d e)
							flipPairs.add(new int[][][] {
									{ cycle[0], cycle[1] }, happyHelperFlip });
							cycle = Arrays.delete(cycle, 1);
						} else {
							// Try to find a helper cycle, that is,
							// a cycle that needs to be done that's
							// on some other cubie.
							// If there is none, then we will have to
							// recruit some happy other cubie as a helper
							// and mess it up temporarily.
							int iHelperCycle = -1;
							for (int iHelperCycleMaybe = iCycle + 1; iHelperCycleMaybe < cycles.length; iHelperCycleMaybe++) {
								if (cycles[iHelperCycleMaybe].length == 1)
									continue; // it's tired of helping
								if (!Utils.areOnSameCubie(n,
										cycles[iHelperCycleMaybe][0], cycle[0])) {
									iHelperCycle = iHelperCycleMaybe;
									break;
								}
							}
							if (iHelperCycle != -1) {
								int helperCycle[][] = cycles[iHelperCycle];
								// Extract one swap from cycle
								// and one swap from helpercycle.
								// Before: (a b c d e) (f g h i j)
								// After: (a b) (f g) (a c d e) (f h i j)
								flipPairs.add(new int[][][] {
										{ cycle[0], cycle[1] },
										{ helperCycle[0], helperCycle[1] }, });
								cycle = Arrays.delete(cycle, 1);
								helperCycle = Arrays.delete(helperCycle, 1);
								cycles[iHelperCycle] = helperCycle;
							} else {
								// There was no other cycle to help;
								// need to take a happy cubie
								// and create a flip on it temporarily.
								// If this happens, it means this cycle
								// and ALL other remaining cycles
								// are on the same single cubie,
								// so we choose some other flip
								// on some other cubie,
								// call it the happyHelperFlip,
								// and use it for all the rest of the cycles.
								//
								// We arbitrarily choose the cubie of
								// happyHelperFlip to be any cubie of
								// the same type as this cubie.
								// We find such a cubie by rotating cubieCenter
								// using fromAxis,toAxis directions
								// such that cubieCenter is nonzero
								// in at least one of the two axes
								// (so that we don't rotate it to itself).
								//
								int fromAxis = 0, toAxis = 1;
								while (cubieCenter[fromAxis] == 0
										&& cubieCenter[toAxis] == 0)
									toAxis++;
								// Actually we don't even need to know the happy
								// helper cubie's center,
								// we just need two stickers on it... so use
								// the rotated images of the first two stickers
								// on cycle. Simple!
								happyHelperFlip = new int[][] {
										PuzzleManipulation.rot90index(n,
												fromAxis, toAxis, cycle[0]),
										PuzzleManipulation.rot90index(n,
												fromAxis, toAxis, cycle[1]), };
								// Now when we get back to the top of the loop
								// we will hit the happyHelperFlip!=null case
								// and be able to proceed.
							} // if there was no other cycle to help
						} // if happyHelperFlip was null beforehand
					} // while cycle.length != 1
				} // for iCycle
				if (debugLevel >= 2)
					System.out.println("        flipPairs="
							+ Arrays.toString(flipPairs));

				int nFlipPairs = flipPairs.size();
				for (int i = 0; i < nFlipPairs; ++i) {
					int flipPair[][][] = flipPairs.get(i);
					int A[][] = (int[][]) Utils.indexsToCoordss(n, flipPair[0]);
					int B[][] = (int[][]) Utils.indexsToCoordss(n, flipPair[1]);
					solution.addAll(flip_two_noncorner_cubies_COORDS(k, n, d,
							A, B, debugLevel));
				}
			} // non-corners case
			else // k == d, i.e. corners case
			{
				Assert(k == d);

				// Need all sticker cycles to occur
				// in pairs of twirls (a b c) (d e f)
				// where stickers a,b,c are on one (corner) cubie
				// and stickers d,e,f are on a different one.
				// Furthermore if d == 3 or 4, then (a b c) and (d e f)
				// must be in opposite directions (a meaningless concept when d
				// >= 5).

				// Even-length cycles are a pain in the ass here,
				// so get rid of them by doing a first pass
				// in which we replace pairs of even-length cycles
				// on the same cubie with an equivalent pair of odd-length
				// cycles
				// (which means the cycles will no longer be disjoint
				// after this pass).
				// (I don't think we could have done the same thing
				// back in the positioning code where I went through contortions
				// to deal with 2-cycles when trying to make 3-cycles-- because
				// in that case the cycles not being disjoint could really mess
				// us up when we look for helpers later (see "subtlety" below))
				for (int iCycle = 0; iCycle < cycles.length; ++iCycle) {
					if (cycles[iCycle].length % 2 == 0) // if this one's even
						for (int jCycle = iCycle + 1; jCycle < cycles.length; ++jCycle)
							if (cycles[jCycle].length % 2 == 0 // if that one's
																// even
									&& Utils.areOnSameCubie(n,
											cycles[iCycle][0],
											cycles[jCycle][0])) {
								// Before: (a b c d) (e f g h i j)
								// After: (a b c d e) (e a f g h i j)
								// in particular:
								// Before: (a b) (c d e f)
								// After: (a b c) (c a d e f)
								cycles[iCycle] = Arrays.append(cycles[iCycle],
										cycles[jCycle][0]);
								cycles[jCycle] = Arrays.insert(cycles[jCycle],
										1, cycles[iCycle][0]);
								break;
							}
					Assert(cycles[iCycle].length % 2 == 1);
				}

				java.util.ArrayList<int[][][]> twirlPairs = new java.util.ArrayList<int[][][]>(); // int
																			// twirlPairs[][/*2*/][/*2*/][/*d*/];
				int happyHelperTwirl[][] = null;
				int happyHelperUnTwirl[][] = null;
				int timesHappyHelperTwirlHelped = 0;
				for (int iCycle = 0; iCycle < cycles.length; ++iCycle) {
					int cycle[][] = cycles[iCycle];
					int cubieCenter[] = Arrays.clamp(cycle[0], 1, n);
					while (cycle.length != 1) {
						Assert(cycle.length >= 3);

						if (happyHelperTwirl != null) {
							// Extract one twirl from cycle
							// and pair it with happyHelperTwirl
							// either forwards or backwards,
							// whichever is in the opposite direction
							// from the extracted twirl.
							//
							// If d >= 5 then it doesn't matter;
							// there is no concept of twirl direction,
							// since any 3 stickers on one cubie
							// can always be rotated to any 3 stickers
							// on any other cubie of the same type.
							// But we check anyway for sanity.
							//

							int abc[][] = { cycle[0], cycle[1], cycle[2] };
							boolean cycleIsSameOrientationAsHappyHelperTwirl = (find_rotation_sequence_taking_these_coords_to_those_coords(
									(int[][]) Utils.indexsToCoordss(n, abc),
									(int[][]) Utils.indexsToCoordss(n,
											happyHelperTwirl), true, debugLevel) != null);
							boolean cycleIsSameOrientationAsHappyHelperUnTwirl = (find_rotation_sequence_taking_these_coords_to_those_coords(
									(int[][]) Utils.indexsToCoordss(n, abc),
									(int[][]) Utils.indexsToCoordss(n,
											happyHelperUnTwirl), true,
									debugLevel) != null);
							if (d == 3 || d == 4) {
								// One way works and the other doesn't
								Assert(cycleIsSameOrientationAsHappyHelperTwirl != cycleIsSameOrientationAsHappyHelperUnTwirl);
							} else // d >= 5, so the rotation should always
									// succeed
							{
								// Both ways work.
								Assert(cycleIsSameOrientationAsHappyHelperTwirl
										&& cycleIsSameOrientationAsHappyHelperUnTwirl);
							}

							int happyHelperTwirlOrUnTwirl[][];
							if (cycleIsSameOrientationAsHappyHelperTwirl) {
								happyHelperTwirlOrUnTwirl = happyHelperUnTwirl;
								timesHappyHelperTwirlHelped--;
							} else {
								happyHelperTwirlOrUnTwirl = happyHelperTwirl;
								timesHappyHelperTwirlHelped++;
							}
							twirlPairs.add(new int[][][] {
									{ cycle[0], cycle[1], cycle[2] },
									happyHelperTwirlOrUnTwirl, });
							// (a b c d e f g) -> (a b c) (a d e f g)
							cycle = Arrays.deleteRange(cycle, 1, 2);
						} else {
							// Try to find a helper cycle, that is,
							// a cycle that needs to be done that's
							// on some other cubie.
							// If there is none, then we will have to
							// recruit some happy other cubie as a helper
							// and mess it up temporarily.
							int iHelperCycle = -1;
							for (int iHelperCycleMaybe = iCycle + 1; iHelperCycleMaybe < cycles.length; iHelperCycleMaybe++) {
								if (cycles[iHelperCycleMaybe].length == 1)
									continue; // it's tired of helping
								if (!Utils.areOnSameCubie(n,
										cycles[iHelperCycleMaybe][0], cycle[0])) {
									iHelperCycle = iHelperCycleMaybe;
									break;
								}
							}
							if (iHelperCycle != -1) {
								// Extract one twirl from cycle
								// and one anti-twirl from helpercycle.
								// this might end up simply reversing
								// helpercycle,
								// which is sort of lame (if we
								// were being ambitious, we should really
								// try to get a helper that we actually
								// help in the process of being helped,
								// instead),
								// but whatever.
								int helperCycle[][] = cycles[iHelperCycle];
								// Extract one twirl from cycle
								// and one anti-twirl from helpercycle.
								// This will be one of the following two cases,
								// depending on whether (h i j) is a twirl
								// or an anti-twirl with respect to (a b c):
								//
								// Case 1: (a b c) is opposite (h i j)
								// Before: (a b c d e f g) (h i j k l m n)
								// After: (a b c) (a d e f g) (h i j) (h k l m
								// n)
								// Case 2: (a b c) is opposite (j i h)
								// Before: (a b c d e f g) (h i j k l m n)
								// After: (a b c) (a d e f g) (j i h) (h j i k l
								// m n)
								//
								// Subtlety: can't it be the case that
								// cycle and helpercycle are not disjoint,
								// due to the first pass where we mingled
								// even cycles to form odd ones?
								// NO-- we only mingled within a cubie,
								// whereas cycle and helperCycle are on
								// different
								// cubies (phew!)

								// Check the orientation
								// of the twirls-- we always need to pair
								// with a twirl of the opposite direction.
								// See the comments above for an explanation
								// of this, where we did the same thing
								// for happyHelperCycle.
								int abc[][] = { cycle[0], cycle[1], cycle[2] };
								int hij[][] = { helperCycle[0], helperCycle[1],
										helperCycle[2] };
								int jih[][] = { helperCycle[2], helperCycle[1],
										helperCycle[0] };
								boolean abc_isSameOrientationAs_hij = (find_rotation_sequence_taking_these_coords_to_those_coords(
										(int[][]) Utils.indexsToCoordss(n, abc),
										(int[][]) Utils.indexsToCoordss(n, hij),
										true, debugLevel) != null);
								boolean abc_isSameOrientationAs_jih = (find_rotation_sequence_taking_these_coords_to_those_coords(
										(int[][]) Utils.indexsToCoordss(n, abc),
										(int[][]) Utils.indexsToCoordss(n, jih),
										true, debugLevel) != null);
								if (d == 3 || d == 4) {
									// One way works and the other doesn't
									Assert(abc_isSameOrientationAs_hij != abc_isSameOrientationAs_jih);
								} else // d >= 5, so the rotation should always
										// succeed
								{
									// Both ways work.
									Assert(abc_isSameOrientationAs_hij
											&& abc_isSameOrientationAs_jih);
								}

								if (abc_isSameOrientationAs_jih) {
									// case 1 as described above:
									// Before: (a b c d e f g) (h i j k l m n)
									// After: (a b c) (a d e f g) (h i j) (h k l
									// m n)
									twirlPairs.add(new int[][][] { abc, hij });
									cycle = Arrays.deleteRange(cycle, 1, 2);
									helperCycle = Arrays.deleteRange(
											helperCycle, 1, 2);
								} else {
									// case 2 as described above:
									// Before: (a b c d e f g) (h i j k l m n)
									// After: (a b c) (a d e f g) (j i h) (h j i
									// k l m n)
									twirlPairs.add(new int[][][] { abc, jih });
									cycle = Arrays.deleteRange(cycle, 1, 2);
									// just swap helperCycle[1] and
									// helperCycle[2] in place (it's not shared)
									int temp[] = helperCycle[1];
									helperCycle[1] = helperCycle[2];
									helperCycle[2] = temp;
								}
								cycles[iHelperCycle] = helperCycle;
							} else {
								// There was no other cycle to help;
								// need to take a happy cubie
								// and create a twirl on it temporarily.
								// If this happens, it means this cycle
								// and ALL other remaining cycles
								// are on the same single cubie,
								// so we choose some other twirl
								// on some other cubie,
								// call it the happyHelperTwirl,
								// and use it for all the rest of the cycles.
								//
								// We arbitrarily choose the cubie of
								// happyHelperTwirl to be any cubie of
								// the same type as this cubie.
								// We find such a cubie by rotating cubieCenter
								// using fromAxis,toAxis directions
								// such that cubieCenter is nonzero
								// in at least one of the two axes
								// (so that we don't rotate it to itself).
								int fromAxis = 0, toAxis = 1;
								while (cubieCenter[fromAxis] == 0
										&& cubieCenter[toAxis] == 0)
									toAxis++;
								// Actually we don't even need to know the happy
								// helper cubie's center,
								// we just need two stickers on it... so use
								// the rotated images of the first three
								// stickers on cycle. Simple!
								happyHelperTwirl = new int[][] {
										PuzzleManipulation.rot90index(n,
												fromAxis, toAxis, cycle[0]),
										PuzzleManipulation.rot90index(n,
												fromAxis, toAxis, cycle[1]),
										PuzzleManipulation.rot90index(n,
												fromAxis, toAxis, cycle[2]), };
								happyHelperUnTwirl = new int[][] {
										happyHelperTwirl[2],
										happyHelperTwirl[1],
										happyHelperTwirl[0], };
								// Now when we get back to the top of the loop
								// we will hit the happyHelperTwirl!=null case
								// and be able to proceed.
							} // if there was no other cycle to help
						} // if happyHelperTwirl was null beforehand
					} // while cycle.length != 1
				} // for iCycle

				//
				// If d>=5, it may now be the case
				// that we used happyHelperTwirl
				// a nonzero-mod-3 number of times,
				// in which case we now need to fix it.
				//
				// PRINT(timesHappyHelperTwirlHelped);
				if (timesHappyHelperTwirlHelped % 3 != 0) {
					Assert(d >= 5); // for 3 or 4, parity keeps this from
									// happening
					// System.out.println("        WHOA! Twirling happyHelperTwirl in isolation!");
					if (((timesHappyHelperTwirlHelped % 3) + 3) % 3 == 1) {
						// Swap happyHelperTwirl with happyHelperUnTwirl,
						// so we only have to deal with the case
						// that it helped 2 times mod 3.
						int temp[][] = happyHelperTwirl;
						happyHelperTwirl = happyHelperUnTwirl;
						happyHelperUnTwirl = temp;
					}
					// Now we want to effectively do one happyHelperTwirl
					// in isolation. Recruit a helper...
					// Probably most effective would be
					// to get a helper who differs in only one coord direction,
					// but it's easier to just take the guy
					// exactly opposite across the puzzle.
					//
					int otherTwirl[][] = {
							Utils.coordsToIndex(n, Arrays.minus(Utils
									.indexToCoords(n, happyHelperTwirl[0]))),
							Utils.coordsToIndex(n, Arrays.minus(Utils
									.indexToCoords(n, happyHelperTwirl[1]))),
							Utils.coordsToIndex(n, Arrays.minus(Utils
									.indexToCoords(n, happyHelperTwirl[2]))), };
					int otherUnTwirl[][] = { otherTwirl[2], otherTwirl[1],
							otherTwirl[0], };
					twirlPairs.add(new int[][][] { happyHelperUnTwirl,
							otherTwirl, });
					twirlPairs.add(new int[][][] { happyHelperUnTwirl,
							otherUnTwirl, });
				}

				if (debugLevel >= 2)
					System.out.println("        twirlPairs="
							+ Arrays.toString(twirlPairs));

				int nTwirlPairs = twirlPairs.size();
				for (int i = 0; i < nTwirlPairs; ++i) {
					int twirlPair[][][] = twirlPairs.get(i);
					int A[][] = (int[][]) Utils
							.indexsToCoordss(n, twirlPair[0]);
					int B[][] = (int[][]) Utils
							.indexsToCoordss(n, twirlPair[1]);
					solution.addAll(twirl_two_corner_cubies_COORDS(n, d, A, B,
							debugLevel));
				}
			} // corners case

			if (debugLevel >= 2)
				System.out.println("        solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 1)
				System.out.println("    out orient_" + k + "sticker_cubies");
			return solution;
		} // orient_ksticker_cubies

		// Find a sequence of moves that flips
		// (i.e swaps two stickers on each of)
		// two k-sticker non-corner cubies (i.e. k < d),
		// without messing up the positions or orientations of any
		// other same-or-fewer-sticker cubies,
		// but freely messing up more-sticker cubies.
		private static java.util.ArrayList<int[]> flip_two_noncorner_cubies_COORDS(
				int k, int n, int d, int A[][], int B[][], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("        in flip_two_noncorner_cubies");
			if (debugLevel >= 3)
				System.out.println("            A = " + Arrays.toString(A)
						+ ", B = " + Arrays.toString(B));
			java.util.ArrayList<int[]> solution = new java.util.ArrayList<int[]>();
			if (k == 2) {
				// Special case-- can't do the general recursive
				// slab approach, but fortunately flipping
				// two pairs of 2-sticker cubies isn't too hard.
				// XXX this special case should be moved down a level to be
				// cleaner, I think
				int AcubieCenter[] = Arrays.clamp(A[0], -(n - 1), n - 1);
				int BcubieCenter[] = Arrays.clamp(B[0], -(n - 1), n - 1);
				int cubieCenters_INDICES[][] = {
						Utils.coordsToIndex(n, AcubieCenter),
						Utils.coordsToIndex(n, BcubieCenter) };
				Object toI_and_I[] = take_two_ksticker_cubies_to_I_INDICES(2,
						n, d, cubieCenters_INDICES, debugLevel);
				int toI[][] = (int[][]) toI_and_I[0];
				int I[][] = (int[][]) toI_and_I[1];

				int fromI[][] = PuzzleManipulation.reverseMoves(toI);
				Arrays.addAll(solution, toI);
				Arrays.addAll(solution, flip_I_of_2sticker_cubies_INDICES(n, d,
						I, debugLevel));
				Arrays.addAll(solution, fromI);
			} else // k >= 3
			{
				Assert(k >= 3 && k <= d - 1); // it's non-corner or we wouldn't
												// be here

				// foo = Do some twists so that the four stickers and two cubie
				// centers
				// all lie in a single 2-plane and the two cubies are in an I
				// bar = solve the simpler problem
				// (flip_two_canonical_noncorner_cubies)
				// return foo bar foo^-1

				if (debugLevel >= 3)
					System.out.println("            A = " + Arrays.toString(A)
							+ ", B = " + Arrays.toString(B));
				Object foo_and_whatever[] = flatten_flipPair_or_twirlPair_COORDS(
						k, n, d, A, B, debugLevel);
				int foo[][] = (int[][]) foo_and_whatever[0];
				int unfoo[][] = PuzzleManipulation.reverseMoves(foo);

				int A1[][] = PuzzleManipulation.twist90sCoordss(n, foo, A);
				int B1[][] = PuzzleManipulation.twist90sCoordss(n, foo, B);
				int bar[][] = flip_two_canonical_noncorner_cubies_COORDS(k, n,
						d, A1, B1, debugLevel);

				Arrays.addAll(solution, foo);
				Arrays.addAll(solution, bar);
				Arrays.addAll(solution, unfoo);

				// As sanity check, apply to A and B,
				// and make sure it does indeed swap them...
				{
					// System.out.println("before foo (flatten):");
					// PRINT(A);
					// PRINT(B);
					// System.out.println("after foo (flatten):");
					// PRINT(A1);
					// PRINT(B1);
					int A2[][] = PuzzleManipulation.twist90sCoordss(n, bar, A1);
					int B2[][] = PuzzleManipulation.twist90sCoordss(n, bar, B1);
					// System.out.println("after bar (flip canonical pairs):");
					// PRINT(A2);
					// PRINT(B2);
					Assert(Arrays.equals(A1[0], A2[1]));
					Assert(Arrays.equals(A1[1], A2[0]));
					Assert(Arrays.equals(B1[0], B2[1]));
					Assert(Arrays.equals(B1[1], B2[0]));
					int A3[][] = PuzzleManipulation.twist90sCoordss(n, unfoo,
							A2);
					int B3[][] = PuzzleManipulation.twist90sCoordss(n, unfoo,
							B2);
					// System.out.println("after unfoo (unflatten):");
					// PRINT(A3);
					// PRINT(B3);
					Assert(Arrays.equals(A3[0], A[1]));
					Assert(Arrays.equals(A3[1], A[0]));
					Assert(Arrays.equals(B3[0], B[1]));
					Assert(Arrays.equals(B3[1], B[0]));
				}
			}
			if (debugLevel >= 3)
				System.out.println("            solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out.println("        out flip_two_noncorner_cubies");
			return solution;
		} // flip_two_noncorner_cubies_COORDS

		// Like flip_two_noncorner_cubies
		// but assumes the desired flips are in canonical relationship
		// to each other:
		// that is, the stickers in question and the two cubie centers
		// span as few dimensions as possible.
		private static int[][] flip_two_canonical_noncorner_cubies_COORDS(
				int k, int n, int d, int A[][], int B[][], int debugLevel) {
			Assert(k > 2); // caller handles this XXX although I probably should
							// here
			int originalAxisThatWasZero = 0;
			while (A[0][originalAxisThatWasZero] != 0)
				++originalAxisThatWasZero;
			return flip_two_canonical_kslabs(d - k, n, d, A, B,
					originalAxisThatWasZero, debugLevel);
		} // flip_two_canonical_noncorner_cubies

		// for the recursion, need to be able to flip two not just when
		// in the shape of an I (e.g. FU and FD on standard Rubik's cube)
		// but also when in shape of a / (e.g. FU and BD on standard Rubik's
		// cube).
		// we call the I shape "canonical" and the / shape "almost canonical",
		// but this function handles both.
		private static int[][] flip_two_canonical_kslabs(int k, int n, int d,
				int a[][], int b[][], int originalAxisThatWasZero,
				int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("          in flip_two_canonical_" + k
						+ "slabs");
			if (debugLevel >= 3)
				System.out.println("              a = " + Arrays.toString(a)
						+ ", b = " + Arrays.toString(b));
			int aCenter[] = Arrays.clamp(a[0], -(n - 1), n - 1);
			int bCenter[] = Arrays.clamp(b[0], -(n - 1), n - 1);
			if (debugLevel >= 3)
				System.out.println("              aCenter = "
						+ Arrays.toString(aCenter) + ", bCenter = "
						+ Arrays.toString(bCenter));
			int nIndicesDifferent = Arrays.nIndicesDifferent(aCenter, bCenter);
			Assert(nIndicesDifferent == 1 || nIndicesDifferent == 2);

			int solution[][];

			int originalA[][] = a;
			int originalB[][] = b;

			Assert(k <= d - 3); // caller handles d-2 specially
			if (k == d - 3) {
				if (nIndicesDifferent == 1) {
					// They are in the shape of an I
					if (debugLevel >= 2)
						System.out
								.println("              base case shape I -- flipping two edges on the 3^4 puzzle");

					//
					// Base case-- this is flipping two 3-sticker cubies
					// (actually the whole 1-slabs, i.e. rows of 3)
					// on the 3^4 puzzle.
					// This is pretty much the only basic sequence we use
					// that requires thinking in more than 3 dimensions,
					// but it's really easy!
					//
					// If we want to flip the front-up and front-back edges
					// of the center face of the 3^4 puzzle,
					// we do the following:
					// foo = slide front-up edge to down face
					// using front face
					// bar = flip that edge using down face
					// foo^-1
					// baz = twist up face exchanging front-up edge
					// with front-back edge
					// foo
					// bar^-1
					// foo^-1
					// baz^-1
					// Done!
					//
					// So we just need to find how the faces map to
					// the corresponding faces of the 3^4 puzzle.
					// Call those faces L,R,F,B,U,D,I,O
					// ("Inner","Outer" for the latter).
					//
					// (WOOPS: the above doesn't uphold our contract
					// to not change anything in the slab dimensions--
					// it reverses the slabs in the R direction.
					// That's okay if d==4, since in that case
					// we are doing the 3-sticker cubies
					// and it's okay to mess up the 4-sticker ones...
					// but it's NOT okay if d>=5, since in that case
					// we are being called recursively and
					// the caller expects the slab-ness to hold.
					// No problem! If d>=5, just make sure
					// we pick an R face along whose axis
					// the original coords were zero; then no one cares
					// if we reverse things in that direction.
					// That's the purpose of that parameter.)

					// I = the face containing a,b that contains
					// none of the four stickers in question...
					int IfaceAxis = 0;
					while (aCenter[IfaceAxis] == 0
							|| aCenter[IfaceAxis] != bCenter[IfaceAxis]
							|| Math.abs(a[0][IfaceAxis]) == (n + 1)
							|| Math.abs(a[1][IfaceAxis]) == (n + 1)
							|| Math.abs(b[0][IfaceAxis]) == (n + 1)
							|| Math.abs(b[1][IfaceAxis]) == (n + 1))
						IfaceAxis++;
					int IfaceSign = (aCenter[IfaceAxis] < 0 ? -1 : 1);

					// U = the other face containing a,b
					int UfaceAxis = 0;
					while (UfaceAxis == IfaceAxis || aCenter[UfaceAxis] == 0
							|| aCenter[UfaceAxis] != bCenter[UfaceAxis])
						UfaceAxis++;
					int UfaceSign = (aCenter[UfaceAxis] < 0 ? -1 : 1);

					// F = the third face containing a
					int FfaceAxis = 0;
					while (FfaceAxis == IfaceAxis || FfaceAxis == UfaceAxis
							|| aCenter[FfaceAxis] == 0)
						FfaceAxis++;
					int FfaceSign = (aCenter[FfaceAxis] < 0 ? -1 : 1);

					// R = an axis that was zero
					// in the original caller,
					// so no one minds if we mess it up.
					int RfaceAxis = originalAxisThatWasZero;
					Assert(RfaceAxis != IfaceAxis);
					Assert(RfaceAxis != UfaceAxis);
					Assert(RfaceAxis != FfaceAxis);
					int RfaceSign = 1; // arbitrarily

					// D = opposite U
					int DfaceAxis = UfaceAxis;
					int DfaceSign = -UfaceSign;

					if (debugLevel >= 4) {
						System.out.println("                Iface = "
								+ (IfaceSign < 0 ? "-" : "+") + IfaceAxis);
						System.out.println("                Uface = "
								+ (UfaceSign < 0 ? "-" : "+") + UfaceAxis);
						System.out.println("                Fface = "
								+ (FfaceSign < 0 ? "-" : "+") + FfaceAxis);
						System.out.println("                Rface = "
								+ (RfaceSign < 0 ? "-" : "+") + RfaceAxis);
					}

					// foo = slide front-up edge to down face using Fface
					int foo[][] = { PuzzleManipulation.makeTwist90(FfaceAxis,
							FfaceSign, UfaceAxis, UfaceSign, IfaceAxis,
							IfaceSign, 1) };
					// bar = flip that edge using Dface.
					// In the 4d puzzle we'd just click on that edge,
					// but here we need to express it as three 90-degree twists.
					int bar[][] = {
							PuzzleManipulation.makeTwist90(DfaceAxis,
									DfaceSign, IfaceAxis, IfaceSign, RfaceAxis,
									RfaceSign, 1),
							PuzzleManipulation.makeTwist90(DfaceAxis,
									DfaceSign, FfaceAxis, FfaceSign, IfaceAxis,
									IfaceSign, 1),
							PuzzleManipulation.makeTwist90(DfaceAxis,
									DfaceSign, RfaceAxis, RfaceSign, FfaceAxis,
									FfaceSign, 1) };
					// baz = twist Uface exchanging front-up edge with
					// front-back edge
					int baz[][] = {
							PuzzleManipulation.makeTwist90(UfaceAxis,
									UfaceSign, FfaceAxis, FfaceSign, RfaceAxis,
									RfaceSign, 1),
							PuzzleManipulation.makeTwist90(UfaceAxis,
									UfaceSign, FfaceAxis, FfaceSign, RfaceAxis,
									RfaceSign, 1) };
					int unfoo[][] = PuzzleManipulation.reverseMoves(foo);
					int unbar[][] = PuzzleManipulation.reverseMoves(bar);
					int unbaz[][] = PuzzleManipulation.reverseMoves(baz);

					solution = (int[][]) Arrays.concat(new int[][][] { foo,
							bar, unfoo, baz, foo, unbar, unfoo, unbaz, });
				} else // nIndicesDifferent == 2
				{
					if (debugLevel >= 2)
						System.out.println("              base case shape /");
					Assert(d > 4); // XXX wtf? was getting this in 3^4 puzzle?
					// They are in the shape of a /.
					// I don't feel like dealing with this;
					// just do a 180 degree twist
					// to put them in the shape of an I.
					// The face to twist is bFace (any face containing B but not
					// A),
					// fromAxis is the sticker dir that's not bFaceAxis,
					// and toAxis is any axis orthogonal to the whole mess
					// in which all the coords are zero.
					int bFaceAxis = 0;
					while (bCenter[bFaceAxis] == aCenter[bFaceAxis])
						bFaceAxis++;
					int bFaceSign = (bCenter[bFaceAxis] < 0 ? -1 : 1);
					int fromAxis = 0;
					while (fromAxis == bFaceAxis
							|| (b[0][fromAxis] - bCenter[fromAxis] == 0 && b[1][fromAxis]
									- bCenter[fromAxis] == 0))
						fromAxis++;

					// System.out.println("====");
					// PRINT(a);
					// PRINT(b);
					// PRINT(aCenter);
					// PRINT(bCenter);
					// PRINT(bFaceAxis);
					// PRINT(fromAxis);

					int toAxis = -1;
					for (int toAxisMaybe = 0; toAxisMaybe < d; ++toAxisMaybe) {
						if (a[0][toAxisMaybe] == 0 && a[1][toAxisMaybe] == 0
								&& b[0][toAxisMaybe] == 0
								&& b[1][toAxisMaybe] == 0) {
							toAxis = toAxisMaybe;
							break;
						}
					}
					Assert(toAxis != -1);

					int twist90[] = { bFaceAxis, bFaceSign, fromAxis, toAxis, 1 };
					int twist180[][] = { twist90, twist90 };
					// PRINT(twist180);
					int bMoved[][] = PuzzleManipulation.twist90sCoordss(n,
							twist180, b);
					// PRINT(bMoved);
					Assert(Arrays.nIndicesDifferent(aCenter, Arrays.clamp(
							bMoved[0], -(n - 1), (n - 1))) == 1);
					solution = Arrays.concat3(twist180,
							flip_two_canonical_kslabs(k, n, d, a, bMoved,
									originalAxisThatWasZero, debugLevel),
							twist180);
				}
			} else // k < d-3 -- need to recurse to bigger slabs
			{
				if (debugLevel >= 2)
					System.out.println("              non-base case shape "
							+ (nIndicesDifferent == 1 ? "I" : "/"));
				// Find extrusion axis...
				// that's some axis in which all the participating
				// sticker coords are equal and nonzero.
				int extrusionAxis = -1;
				for (int extrusionAxisMaybe = 0; extrusionAxisMaybe < d; ++extrusionAxisMaybe) {
					if (a[0][extrusionAxisMaybe] != 0
							&& a[0][extrusionAxisMaybe] == a[1][extrusionAxisMaybe]
							&& a[0][extrusionAxisMaybe] == b[0][extrusionAxisMaybe]
							&& a[0][extrusionAxisMaybe] == b[1][extrusionAxisMaybe]) {
						extrusionAxis = extrusionAxisMaybe;
						break;
					}
				}
				Assert(extrusionAxis != -1);
				int abcSign = (a[0][extrusionAxis] < 0 ? -1 : 1);

				// aFace is a face containing a but not b
				// (the unique such face if I, one of the two if /)
				int aFaceAxis = 0;
				while (aCenter[aFaceAxis] == 0
						|| aCenter[aFaceAxis] != -bCenter[aFaceAxis])
					aFaceAxis++;
				int aFaceSign = (aCenter[aFaceAxis] < 0 ? -1 : 1);

				int foo[][] = {};
				if (nIndicesDifferent == 1) {
					// Twist b's face 180 degrees so that it a,b are in the
					// shape of a /.
					// then untwist it afterwards.
					// otherFaceAxis = direction of the other sticker
					// zeroFaceAxis = some direction in which all these stickers
					// are zero. (There must be at least one,
					// otherwise we'd be actual corner cubies
					// of the puzzle, but this doesn't get called on corners,
					// only non-corners.)
					int otherFaceAxis = 0;
					while (otherFaceAxis == aFaceAxis
							|| (b[0][otherFaceAxis] - bCenter[otherFaceAxis] == 0 && b[1][otherFaceAxis]
									- bCenter[otherFaceAxis] == 0))
						otherFaceAxis++;
					int zeroFaceAxis = 0;
					while (bCenter[zeroFaceAxis] != 0)
						zeroFaceAxis++;
					int twist90[] = PuzzleManipulation.makeTwist90(aFaceAxis,
							-aFaceSign, otherFaceAxis, 1, zeroFaceAxis, 1, 1);
					foo = new int[][] { twist90, twist90 };
					b = Arrays.deepCopy(b);
					b[0][otherFaceAxis] *= -1;
					b[1][otherFaceAxis] *= -1;
					bCenter[otherFaceAxis] *= -1;
					if (debugLevel >= 3)
						System.out
								.println("              after changing I to /: b = "
										+ Arrays.toString(b));
				}

				// a,b are in the shape of a /, hooray.
				// find c = the knee of an L-shape a,c,b,
				// keeping in the same 2-space.
				int bFaceAxis = 0;
				while (bFaceAxis == aFaceAxis || bCenter[bFaceAxis] == 0
						|| bCenter[bFaceAxis] != -aCenter[bFaceAxis])
					bFaceAxis++;
				int bFaceSign = (bCenter[bFaceAxis] < 0 ? -1 : 1);

				int c[][] = Arrays.deepCopy(b);
				c[0][bFaceAxis] *= -1;
				c[1][bFaceAxis] *= -1;

				// Extrude the k-slabs a,c,b
				// in extrusion direction
				// to get the (k+1)-slabs A,C,B...
				int A[][] = Arrays.deepCopy(a);
				A[0][extrusionAxis] = A[1][extrusionAxis] = 0;
				int C[][] = Arrays.deepCopy(c);
				C[0][extrusionAxis] = C[1][extrusionAxis] = 0;
				int B[][] = Arrays.deepCopy(b);
				B[0][extrusionAxis] = B[1][extrusionAxis] = 0;

				int bar[][] = flip_two_canonical_kslabs(k + 1, n, d, A, C,
						originalAxisThatWasZero, debugLevel);
				// baz = twist b -> c -> a
				int baz[][] = { PuzzleManipulation.makeTwist90(extrusionAxis,
						abcSign, aFaceAxis, -aFaceSign, bFaceAxis, -bFaceSign,
						1) };

				// PRINT(foo);
				// PRINT(bar);
				// PRINT(baz);
				solution = (int[][]) Arrays.concat(new int[][][] { foo, bar,
						baz, PuzzleManipulation.reverseMoves(bar),
						PuzzleManipulation.reverseMoves(baz),
						PuzzleManipulation.reverseMoves(foo), });
			}

			if (debugLevel >= 3)
				System.out.println("              solution = "
						+ Arrays.toString(solution));

			// Make sure we do what we were hired to do...
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalA[0]), originalA[1]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalA[1]), originalA[0]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalB[0]), originalB[1]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalB[1]), originalB[0]));

			if (debugLevel >= 2)
				System.out.println("          out flip_two_canonical_" + k
						+ "slabs");
			return solution;
		} // flip_two_canonical_kslabs

		// Find a sequence of moves that flips two 2-sticker cubies
		// that are in the shape of an I -- that is,
		// they differ in exactly one coordinate
		// and they are opposite in that coordinate.
		// Don't mess up any other 2-sticker cubies,
		// but freely mess up cubies with more than 2 stickers.
		private static int[][] flip_I_of_2sticker_cubies_INDICES(int n, int d,
				int ItoFlip[/* 2 */][/* d */], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("            in flip_I_of_2sticker_cubies");
			if (debugLevel >= 3)
				System.out.println("                ItoFlip="
						+ Arrays.toString(ItoFlip));

			// argh, we are translating back and forth a lot
			int a[] = Utils.indexToCoords(n, ItoFlip[0]);
			int b[] = Utils.indexToCoords(n, ItoFlip[1]);
			// PRINT(a);
			// PRINT(b);
			Assert(Arrays.normSqrd(a) == Arrays.normSqrd(b));

			// On a standard Rubik's cube,
			// to flip the edges cubies UF and UB,
			// we can use the sequence from wikipedia.
			// it refrains from messing with the rest of the cube,
			// so it's overkill (since we are allowed to mess up
			// anything with > 2 stickers), but whatever... (XXX should ask
			// around)
			// http://en.wikipedia.org/wiki/Rubik's_Cube_as_a_mathematical_group
			// R U D B2 U2 B' U B U B2 D' R' U'
			// (where unprimed means clockwise and primed means
			// counterclockwise)
			// That flips UR and UB.
			// Unfortunately those are not in the shape of an "I" like we
			// want...
			// But (F R) take UF to UR, so we can flip the I-shaped pair UF and
			// UB by:
			// F R (above sequence) R' F'.
			//
			// We express twists as (face,fromAxis,toAxis),
			// so we express F, R, and U as:
			// F = (Faxis,Uaxis,Raxis)
			// R = (Raxis,Faxis,Uaxis)
			// U = (Uaxis,Raxis,Faxis)
			// except we need to get the signs right too.

			// Uface is the unique face containing A and B.
			int UfaceAxis = 0;
			while (a[UfaceAxis] == 0 || a[UfaceAxis] != b[UfaceAxis])
				UfaceAxis++;
			int UfaceSign = (a[UfaceAxis] < 0 ? -1 : 1);

			// Fface is the unique face containing A but not B.
			int FfaceAxis = 0;
			while (a[FfaceAxis] == b[FfaceAxis])
				FfaceAxis++;
			int FfaceSign = (a[FfaceAxis] < 0 ? -1 : 1);

			// Rface is any face adjacent to Uface and Fface.
			int RfaceAxis = 0;
			while (RfaceAxis == UfaceAxis || RfaceAxis == FfaceAxis)
				RfaceAxis++;
			int RfaceSign = 1; // arbitrary;

			int F[] = PuzzleManipulation.makeTwist90(FfaceAxis, FfaceSign,
					UfaceAxis, UfaceSign, RfaceAxis, RfaceSign, 1);
			int R[] = PuzzleManipulation.makeTwist90(RfaceAxis, RfaceSign,
					FfaceAxis, FfaceSign, UfaceAxis, UfaceSign, 1);
			int U[] = PuzzleManipulation.makeTwist90(UfaceAxis, UfaceSign,
					RfaceAxis, RfaceSign, FfaceAxis, FfaceSign, 1);
			int F_[] = PuzzleManipulation.reverseMove(F);
			int R_[] = PuzzleManipulation.reverseMove(R);
			int U_[] = PuzzleManipulation.reverseMove(U);

			int B[] = Arrays.copy(F_);
			B[1] *= -1;
			int D[] = Arrays.copy(U_);
			D[1] *= -1;
			int L[] = Arrays.copy(R_);
			L[1] *= -1;
			int B_[] = PuzzleManipulation.reverseMove(B);
			int D_[] = PuzzleManipulation.reverseMove(D);
			//int L_[] = PuzzleManipulation.reverseMove(L);

			// PRINT(a);
			// PRINT(b);
			// PRINT(F);
			// PRINT(R);
			// PRINT(U);
			// PRINT(B);
			// PRINT(D);
			// PRINT(L);
			// PRINT(F_);
			// PRINT(R_);
			// PRINT(U_);
			// PRINT(B_);
			// PRINT(D_);
			// PRINT(L_);

			int solution[][] = { F, R, R, U, D, B, B, U, U, B_, U, B, U, B, B,
					D_, R_, U_, R_, F_ };

			if (debugLevel >= 3)
				System.out.println("                solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out.println("            out flip_I_of_2sticker_cubies");
			return solution;
		} // flip_I_of_2sticker_cubies

		// Take the given two cubies to an I shape
		// (i.e. opposite and extreme in one axis, equal in all others),
		// freely messing up everything else.
		// Returns the move sequence and the transformed twocubies.
		// XXX would be cleaner to just return the move sequence... is returning
		// both
		// XXX really saving that much work? I think not
		private static Object[] take_two_ksticker_cubies_to_I_INDICES(int k,
				int n, int d, int twocubies[][], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("                in take_two_" + k
						+ "sticker_cubies_to_I");

			Object solution_and_Ipair[] = take_tricycle_to_L_of_ksticker_cubies_INDICES(
					k, n, d, twocubies, debugLevel);

			if (debugLevel >= 3)
				System.out.println("                    solution = "
						+ Arrays.toString(solution_and_Ipair[0]));
			if (debugLevel >= 2)
				System.out.println("                out take_two_" + k
						+ "sticker_cubies_to_I");
			return solution_and_Ipair;
		} // take_two_ksticker_cubies_to_I

		// Find a sequence of moves that twirls
		// (i.e cycles three stickers on each of)
		// two corner cubies,
		// without messing up the positions or orientations of any
		// other cubies.
		private static java.util.ArrayList<int[]> twirl_two_corner_cubies_COORDS(
				int n, int d, int a[][], int b[][], int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("        in twirl_two_corner_cubies");
			if (debugLevel >= 3)
				System.out.println("            a = " + Arrays.toString(a)
						+ ", b = " + Arrays.toString(b));

			java.util.ArrayList<int[]> solution = new java.util.ArrayList<int[]>();

			// foo = Do some twists so that the six stickers and two cubie
			// centers
			// all lie in a single 3-space and the cubies are in an I
			// bar = solve the simpler problem
			// (twirl_two_canonical_corner_kslabs)
			// return foo bar foo^-1

			Object foo_and_whatever[] = flatten_flipPair_or_twirlPair_COORDS(d,
					n, d, a, b, debugLevel);
			int foo[][] = (int[][]) foo_and_whatever[0];

			a = PuzzleManipulation.twist90sCoordss(n, foo, a);
			b = PuzzleManipulation.twist90sCoordss(n, foo, b);

			Arrays.addAll(solution, foo);
			Arrays.addAll(solution, twirl_two_canonical_corner_kslabs_COORDS(0,
					n, d, a, b, debugLevel));
			Arrays.addAll(solution, PuzzleManipulation.reverseMoves(foo));

			if (debugLevel >= 3)
				System.out.println("            solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out.println("        out twirl_two_corner_cubies");
			return solution;
		} // twirl_two_corner_cubies

		// Like twirl_two_corner_cubies
		// but assumes the desired twirls are in canonical relationship
		// to each other:
		// that is, the stickers in question and the two cubie centers
		// span as few dimensions as possible.
		//
		// Actually doesn't assume the stickers are corner cubies;
		// in each axis direction in which the coord plane
		// is non-extreme, all this stuff is extruded into (extreme) slabs
		// and the operations are actually done to the slabs.
		//
		private static int[][] twirl_two_canonical_corner_kslabs_COORDS(int k,
				int n, int d, int a[][], int b[][], int debugLevel) {
			if (debugLevel >= 2)
				System.out
						.println("            in twirl_two_canonical_corner_kslabs_COORDS");
			if (debugLevel >= 3)
				System.out.println("                a = " + Arrays.toString(a)
						+ ", b = " + Arrays.toString(b));

			int originalA[][] = a;
			int originalB[][] = b;

			int aCubieCenter[] = Arrays.clamp(a[0], -(n - 1), n - 1);
			int bCubieCenter[] = Arrays.clamp(b[0], -(n - 1), n - 1);
			int aLegsSum[] = Arrays.repeat(0, d);
			int bLegsSum[] = Arrays.repeat(0, d);
			for (int i = 0; i < a.length; ++i) {
				aLegsSum = Arrays.plus(aLegsSum, Arrays.minus(aCubieCenter,
						a[i]));
				bLegsSum = Arrays.plus(bLegsSum, Arrays.minus(bCubieCenter,
						b[i]));
			}

			int solution[][] = {};
			if (k == d - 3) {
				if (debugLevel >= 2)
					System.out
							.println("                base case: twirl-two-corners on rubik's cube");
				// Solve using the standard Rubik's cube
				// algorithm.
				// From Tom Davis's "Permutation Groups and Rubik's Cube":
				// http://mathcircle.berkeley.edu/BMC3/perm/node15.html:
				// URB,URF -> RBU,RFU: F D D F' R' D D R U R' D D R F D D F' U'
				// (where unprimed means clockwise,
				// primed means counterclockwise)
				// In other words, twirl URB counterclockwise (U->R->B)
				// and URF clockwise (U->R->B).
				//
				// B
				// +-------b
				// | |
				// | U |R
				// | |
				// +-------a
				// F
				//
				// To feed this in as moves for experimenting:
				// F = ACB
				// R = BAC
				// U = CBA
				// D = cBA
				// F' = ABC
				// R' = BCA
				// U' = CAB
				// So run it with args:
				// 3 3 -exec
				// "ACB;cBA;cBA;ABC;BCA;cBA;cBA;BAC;CBA;BCA;cBA;cBA;BAC;ACB;cBA;cBA;ABC;CAB"
				//    

				// Fface = the face containing a but not b
				int FfaceAxis = 0;
				while (aCubieCenter[FfaceAxis] == bCubieCenter[FfaceAxis])
					FfaceAxis++;
				int FfaceSign = (aCubieCenter[FfaceAxis] < 0 ? -1 : 1);

				// permute a and b
				// so that the opposing stickers
				// (the ones in the direction of Fface and opposite it)
				// are listed first.
				// XXX maybe shoulda done this earlier as part of
				// canonicalization?
				{
					for (int i = 0; i < 3; ++i)
						if (a[i][FfaceAxis] - aCubieCenter[FfaceAxis] != 0) {
							a = new int[][] { a[i], a[(i + 1) % 3],
									a[(i + 2) % 3] };
							break;
						}
					for (int i = 0; i < 3; ++i)
						if (b[i][FfaceAxis] - bCubieCenter[FfaceAxis] != 0) {
							b = new int[][] { b[i], b[(i + 1) % 3],
									b[(i + 2) % 3] };
							break;
						}
				}

				// So now Fface is the direction of a[0] from aCubieCenter
				Assert(a[0][FfaceAxis] == FfaceSign * (n + 1));
				Assert(b[0][FfaceAxis] == -FfaceSign * (n + 1));

				// Make Uface the direction of a[1] from aCubiecenter
				int UfaceAxis = 0;
				while (a[1][UfaceAxis] - aCubieCenter[UfaceAxis] == 0)
					UfaceAxis++;
				int UfaceSign = (aCubieCenter[UfaceAxis] < 0 ? -1 : 1);
				Assert(a[1][UfaceAxis] == UfaceSign * (n + 1));
				Assert(b[1][UfaceAxis] == UfaceSign * (n + 1));

				// Make Rface the direction of a[2] from aCubieCenter
				int RfaceAxis = 0;
				while (a[2][RfaceAxis] - aCubieCenter[RfaceAxis] == 0)
					RfaceAxis++;
				int RfaceSign = (aCubieCenter[RfaceAxis] < 0 ? -1 : 1);
				Assert(a[2][RfaceAxis] == RfaceSign * (n + 1));
				Assert(b[2][RfaceAxis] == RfaceSign * (n + 1));

				// Dface = opposite Uface
				int DfaceAxis = UfaceAxis;
				int DfaceSign = -UfaceSign;

				int U[] = PuzzleManipulation.makeTwist90(UfaceAxis, UfaceSign,
						RfaceAxis, RfaceSign, FfaceAxis, FfaceSign, 1);
				int F[] = PuzzleManipulation.makeTwist90(FfaceAxis, FfaceSign,
						UfaceAxis, UfaceSign, RfaceAxis, RfaceSign, 1);
				int R[] = PuzzleManipulation.makeTwist90(RfaceAxis, RfaceSign,
						FfaceAxis, FfaceSign, UfaceAxis, UfaceSign, 1);
				int D[] = PuzzleManipulation.makeTwist90(DfaceAxis, DfaceSign,
						FfaceAxis, FfaceSign, RfaceAxis, RfaceSign, 1);

				int U_[] = PuzzleManipulation.reverseMove(U);
				int F_[] = PuzzleManipulation.reverseMove(F);
				int R_[] = PuzzleManipulation.reverseMove(R);
				//int D_[] = PuzzleManipulation.reverseMove(D);

				solution = new int[][] { F, D, D, F_, R_, D, D, R, U, R_, D, D,
						R, F, D, D, F_, U_ };
			} else // 0 <= k <= d-4
			{
				// Find extrusion axis...
				// that's some axis in which all the participating
				// sticker coords are equal and nonzero.
				int extrusionAxis = 0;
				while (a[0][extrusionAxis] == 0
						|| a[0][extrusionAxis] != a[1][extrusionAxis]
						|| a[0][extrusionAxis] != a[2][extrusionAxis]
						|| a[0][extrusionAxis] != b[0][extrusionAxis]
						|| a[0][extrusionAxis] != b[1][extrusionAxis]
						|| a[0][extrusionAxis] != b[2][extrusionAxis])
					extrusionAxis++;

				// bcFace = the face containing b (and c) but not a...
				int bcFaceAxis = 0;
				while (bCubieCenter[bcFaceAxis] == aCubieCenter[bcFaceAxis])
					bcFaceAxis++;
				int bcFaceSign = (bCubieCenter[bcFaceAxis] < 0 ? -1 : 1);

				// Choose c by reflecting b
				// in one of its sticker directions
				// other than towards a, and other than extrusionAxis.
				// cFace = the face containing c but not a,b
				int cFaceAxis = 0;
				while (cFaceAxis == bcFaceAxis || cFaceAxis == extrusionAxis
						|| bLegsSum[cFaceAxis] == 0)
					cFaceAxis++;
				int cFaceSign = (bLegsSum[cFaceAxis] < 0 ? -1 : 1); // one of
																	// b's
																	// "legs"
																	// points to
																	// c

				// abcFace = a sticker direction that is NOT
				// extrusionAxis, bcFaceAxis, or cFaceAxis
				int abcFaceAxis = 0;
				while (abcFaceAxis == extrusionAxis
						|| abcFaceAxis == bcFaceAxis
						|| abcFaceAxis == cFaceAxis
						|| aLegsSum[abcFaceAxis] == 0)
					abcFaceAxis++;
				int abcFaceSign = (aCubieCenter[abcFaceAxis] < 0 ? -1 : 1);

				int c[][] = Arrays.deepCopy(b);
				c[0][cFaceAxis] *= -1;
				c[1][cFaceAxis] *= -1;
				c[2][cFaceAxis] *= -1;

				// Extrude the k-slabs a,b,c
				// in extrusion direction
				// to get the (k+1)-slabs A,B,C...
				int A[][] = Arrays.deepCopy(a);
				A[0][extrusionAxis] = A[1][extrusionAxis] = A[2][extrusionAxis] = 0;
				int B[][] = Arrays.deepCopy(b);
				B[0][extrusionAxis] = B[1][extrusionAxis] = B[2][extrusionAxis] = 0;
				int C[][] = Arrays.deepCopy(c);
				C[0][extrusionAxis] = C[1][extrusionAxis] = C[2][extrusionAxis] = 0;

				//
				// The solution is the composition of the following six
				// sequences:
				// foo = twist bcFace so that b goes to c
				// bar = twirl A and untwirl B (recursively)
				// baz = twist extrusionAxis face containing a,b,c so that c
				// goes to a and b stays put
				// bar^-1 (untwirl A and twirl B)
				// baz^-1 = (untwist extrusionAxis face so that a goes to c and
				// b stays put)
				// foo^-1 = (untwist bcFace so that c goes to b)
				// where a,b,c always refers to world space points here
				// (rather than cubie space coords that are getting moved
				// around).
				//
				int foo[][] = { PuzzleManipulation.makeTwist90(bcFaceAxis,
						bcFaceSign, abcFaceAxis, abcFaceSign, cFaceAxis,
						cFaceSign, 1) };
				int bar[][] = twirl_two_canonical_corner_kslabs_COORDS(k + 1,
						n, d, A, B, debugLevel);
				// int baz[][] =
				// {PuzzleManipulation.makeTwist90(extrusionAxis,aCubieCenter[extrusionAxis]<0?-1:1,
				// cFaceAxis,cFaceSign, bcFaceAxis,bcFaceSign, 1)};
				int baz[][] = {
						PuzzleManipulation
								.makeTwist90(extrusionAxis,
										aCubieCenter[extrusionAxis] < 0 ? -1
												: 1, bcFaceAxis, bcFaceSign,
										cFaceAxis, cFaceSign, 1),
						PuzzleManipulation.makeTwist90(extrusionAxis,
								aCubieCenter[extrusionAxis] < 0 ? -1 : 1,
								cFaceAxis, cFaceSign, abcFaceAxis, abcFaceSign,
								1), };
				solution = (int[][]) Arrays.concat(new int[][][] { foo, bar,
						baz, PuzzleManipulation.reverseMoves(bar),
						PuzzleManipulation.reverseMoves(baz),
						PuzzleManipulation.reverseMoves(foo), });
			}

			if (debugLevel >= 3)
				System.out.println("                solution = "
						+ Arrays.toString(solution));

			// Make sure we do what we were hired to do...
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalA[0]), originalA[1]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalA[1]), originalA[2]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalA[2]), originalA[0]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalB[0]), originalB[1]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalB[1]), originalB[2]));
			Assert(Arrays.equals(PuzzleManipulation.twist90sCoords(n, solution,
					originalB[2]), originalB[0]));

			if (debugLevel >= 2)
				System.out
						.println("            out twirl_two_canonical_corner_kslabs_COORDS");
			return solution;
		} // twirl_two_canonical_corner_kslabs_COORDS

		// Find a sequence of twists
		// that puts the given two k-sticker cubies
		// in relation to each other such that
		// the cubie centers are in an I (i.e. only differ in one axis
		// and are opposite in that axis)
		// and such that the listed stickers
		// (2 on each cubie for flips or 3 on each cubie for twirls)
		// along with the two cubie centers
		// lie in as low-dimensional a space as possible--
		// 2-dimensional for a flipPair, or 3-dimensional for a twirlPair.
		//
		// Furthermore twists B's face 180 degrees if necessary
		// so that the direction of A's stickers
		// correspond to the direction of B's stickers.
		//
		// Returns the move sequence and the transformed indices.
		private static Object[] flatten_flipPair_or_twirlPair_COORDS(int k,
				int n, int d, int A[/* 2 or 3 */][/* d */],
				int B[/* 2 or 3 */][/* d */], int debugLevel) {
			if (debugLevel >= 2)
				System.out
						.println("            in flatten_flipPair_or_twirlPair");
			if (debugLevel >= 3)
				System.out.println("                A = " + Arrays.toString(A)
						+ ", B = " + Arrays.toString(B));
			int twoOrThree = A.length; // 2 if flips, 3 if twirls
			Assert(twoOrThree == 2 || twoOrThree == 3);
			Assert(B.length == twoOrThree);

			int AcubieCenter[] = Arrays.clamp(A[0], -(n - 1), n - 1);
			int BcubieCenter[] = Arrays.clamp(B[0], -(n - 1), n - 1);
			if (debugLevel >= 3)
				System.out.println("                AcubieCenter = "
						+ Arrays.toString(AcubieCenter) + ", BcubieCenter = "
						+ Arrays.toString(BcubieCenter));

			int cubieCenters_INDICES[][] = {
					Utils.coordsToIndex(n, AcubieCenter),
					Utils.coordsToIndex(n, BcubieCenter) };
			Object toI_and_I[] = take_two_ksticker_cubies_to_I_INDICES(k, n, d,
					cubieCenters_INDICES, debugLevel);
			int toI[][] = (int[][]) toI_and_I[0];
			//int I[][] = (int[][]) toI_and_I[1]; // transformed indices of the two cubie centers

			int solution[][] = toI; // and we will append more to it

			// Apply the moves so far to the sticker coords...
			// XXX although, the moves so far should only have moved B...
			// XXX but currently take_two_ksticker_cubies_to_I_INDICES does its
			// thing backwards so it moved A instead... bleah. just do them both
			// to be safe.
			A = PuzzleManipulation.twist90sCoordss(n, toI, A);
			B = PuzzleManipulation.twist90sCoordss(n, toI, B);
			AcubieCenter = Arrays.clamp(A[0], -(n - 1), n - 1);
			BcubieCenter = Arrays.clamp(B[0], -(n - 1), n - 1);

			// Bface = the face now containing B but not A
			int BfaceAxis = 0;
			while (AcubieCenter[BfaceAxis] == 0
					|| AcubieCenter[BfaceAxis] != -BcubieCenter[BfaceAxis])
				BfaceAxis++;
			int BfaceSign = (BcubieCenter[BfaceAxis] < 0 ? -1 : 1);

			int AlegsSum[] = Arrays.repeat(0, d);
			int BlegsSum[] = Arrays.repeat(0, d);
			for (int i = 0; i < twoOrThree; ++i) {
				AlegsSum = Arrays.plus(AlegsSum, Arrays.minus(AcubieCenter,
						A[i]));
				BlegsSum = Arrays.plus(BlegsSum, Arrays.minus(BcubieCenter,
						B[i]));
			}
			if (debugLevel >= 3)
				System.out
						.println("                applying moves so far to bring together into I");
			if (debugLevel >= 3)
				System.out.println("                A = " + Arrays.toString(A)
						+ ", B = " + Arrays.toString(B));
			if (debugLevel >= 3)
				System.out.println("                AcubieCenter = "
						+ Arrays.toString(AcubieCenter) + ", BcubieCenter = "
						+ Arrays.toString(BcubieCenter));
			if (debugLevel >= 3)
				System.out.println("                AlegsSum = "
						+ Arrays.toString(AlegsSum) + ", BlegsSum = "
						+ Arrays.toString(BlegsSum));

			//
			// Need both A and B to get a "leg"
			// (i.e. a direction directly opposite a sticker)
			// on the axis connecting them.
			//
			if (AlegsSum[BfaceAxis] == 0 || BlegsSum[BfaceAxis] == 0) {
				if (debugLevel >= 2)
					System.out
							.println("                    legs DID NOT already point to each other");
				// This is sort of intricate...
				// find legs Aleg and Bleg of A and B respectively
				// such that neither leg is aligned with the B-A direction
				// nor each other.
				// Twist Bface 90 degrees in such a way
				// that B ends up where B+Aleg was
				// and B+Bleg ends up where B was.
				// Then Bleg is aligned with Aleg,
				// and B is across a square diagonal from A,
				// i.e. B's cubie center is now different from A's
				// in two axis directions: the original Bface dir,
				// and AlegDir. Let newBface be the face containing B
				// along AlegDir. Twist newBface
				// in such a way that B goes to the end of Aleg,
				// and Bleg's end goes to A.
				// Now A,B are in the desired relation to each other.
				// XXX argh I think I screwed up, may need to take B and A in
				// opposite order if...

				// PRINT(A);
				// PRINT(B);
				// PRINT(AcubieCenter);
				// PRINT(BcubieCenter);
				// PRINT(AlegsSum);
				// PRINT(BlegsSum);

				// If A has a leg pointing towards B,
				// then that leg is ineligible
				// in which case we better let A choose first
				// so that B doesn't steal A's only
				// remaining eligible leg dir.
				// Likewise for B. (They can't both
				// have legs pointing towards each other already,
				// or we wouldn't be here trying to make that the case.)
				int AlegAxis, BlegAxis;
				if (AlegsSum[BfaceAxis] != 0) {
					// A has an ineligible leg (aligned with BfaceAxis),
					// so let it choose first...
					if (debugLevel >= 2)
						System.out
								.println("                    letting A choose first");
					AlegAxis = 0;
					while (AlegAxis == BfaceAxis || AlegsSum[AlegAxis] == 0)
						AlegAxis++;
					BlegAxis = 0;
					while (BlegAxis == BfaceAxis || BlegAxis == AlegAxis
							|| BlegsSum[BlegAxis] == 0)
						BlegAxis++;
				} else {
					// Let B choose first.
					if (debugLevel >= 2)
						System.out
								.println("                    letting B choose first");
					BlegAxis = 0;
					while (BlegAxis == BfaceAxis || BlegsSum[BlegAxis] == 0)
						BlegAxis++;
					AlegAxis = 0;
					while (AlegAxis == BfaceAxis || AlegAxis == BlegAxis
							|| AlegsSum[AlegAxis] == 0)
						AlegAxis++;
				}
				int AlegSign = (AlegsSum[AlegAxis] < 0 ? -1 : 1);
				int BlegSign = (BlegsSum[BlegAxis] < 0 ? -1 : 1);
				if (debugLevel >= 3)
					System.out.println("                BfaceAxis = "
							+ BfaceAxis + ", BfaceSign = " + BfaceSign);
				if (debugLevel >= 3)
					System.out.println("                AlegAxis = " + AlegAxis
							+ ", AlegSign = " + AlegSign);
				if (debugLevel >= 3)
					System.out.println("                BlegAxis = " + BlegAxis
							+ ", BlegSign = " + BlegSign);

				// Twist Bface 90 degrees in such a way that B ends up where
				// B+Aleg was
				// and B+Bleg ends up where B was.
				// We could do this using
				// find_oneface_twist_sequence_taking_these_coords_to_those_coords,
				// but it would just tell us that the answer is the single
				// twist:
				// makeTwist90(BfaceAxis, BfaceSign, AlegAxis,AlegSign,
				// BlegAxis,BlegSign)
				// and that the second twist we want, which aligns Aleg with
				// -Bleg, is:
				// makeTwist90(AlegAxis,AlegSign, BlegAxis,BlegSign,
				// BfaceAxis,BfaceSign)
				int moreTwists[][] = {
						PuzzleManipulation.makeTwist90(BfaceAxis, BfaceSign,
								AlegAxis, AlegSign, BlegAxis, BlegSign, 1),
						PuzzleManipulation.makeTwist90(AlegAxis, AlegSign,
								BlegAxis, BlegSign, BfaceAxis, BfaceSign, 1), };
				if (debugLevel >= 3)
					System.out.println("                to align legs = "
							+ Arrays.toString(moreTwists));

				solution = Arrays.concat(solution, moreTwists);

				// these two twists move B but not A, so we don't need to update
				// A.
				// XXX in fact could have just done makeRot90...
				B = PuzzleManipulation.twist90sCoordss(n, moreTwists, B);
				BcubieCenter = Arrays.clamp(B[0], -(n - 1), n - 1);

				if (debugLevel >= 3)
					System.out.println("                A = "
							+ Arrays.toString(A) + ", B = "
							+ Arrays.toString(B));
				if (debugLevel >= 3)
					System.out.println("                AcubieCenter = "
							+ Arrays.toString(AcubieCenter)
							+ ", BcubieCenter = "
							+ Arrays.toString(BcubieCenter));

				// Adjust Bface since B scooted around...
				// as before, Bface should be the unique face
				// containing B but not A.
				BfaceAxis = AlegAxis;
				BfaceSign = AlegSign;
				Assert(BcubieCenter[BfaceAxis] != 0);
				Assert(BcubieCenter[BfaceAxis] == -AcubieCenter[BfaceAxis]);
				if (debugLevel >= 3)
					System.out.println("                BfaceAxis = "
							+ BfaceAxis + ", BfaceSign = " + BfaceSign);

				// and Assert that it's still I-shaped
				Assert(Arrays.nIndicesDifferent(AcubieCenter, BcubieCenter) == 1);
			} else {
				if (debugLevel >= 2)
					System.out
							.println("                legs already pointed to each other");
			}

			//
			// Now should be able to twist B's face
			// (the unique face containing B but not A)
			// while keeping BcubieCenter fixed,
			// such that all the remaining legs of B
			// align with all the remaining legs of A.
			// This is equivalent to saying that the sum of B's
			// remaining leg directions should get rotated by these twists
			// to the sum of A's remaining leg directions.
			// 
			{
				int aRemainingLegsSum[] = Arrays.repeat(0, d);
				int bRemainingLegsSum[] = Arrays.repeat(0, d);
				for (int i = 0; i < twoOrThree; ++i) {
					aRemainingLegsSum = Arrays.plus(aRemainingLegsSum, Arrays
							.minus(AcubieCenter, A[i]));
					bRemainingLegsSum = Arrays.plus(bRemainingLegsSum, Arrays
							.minus(BcubieCenter, B[i]));
				}
				Assert(bRemainingLegsSum[BfaceAxis] != 0);
				Assert(bRemainingLegsSum[BfaceAxis] == -aRemainingLegsSum[BfaceAxis]);
				bRemainingLegsSum[BfaceAxis] = aRemainingLegsSum[BfaceAxis] = 0;

				int theseCoords[][] = { bRemainingLegsSum, BcubieCenter };
				int thoseCoords[][] = { aRemainingLegsSum, BcubieCenter };

				int finalTwistSequence[][] = find_oneface_twist_sequence_taking_these_coords_to_those_coords(
						BfaceAxis, BfaceSign, theseCoords, thoseCoords, 1,
						debugLevel);
				solution = Arrays.concat(solution, finalTwistSequence);

				// apply finalTwistSequence (it only moves B, not A)
				if (debugLevel >= 3)
					System.out
							.println("                applying final flattening twist sequence to B without moving its center");
				if (debugLevel >= 3)
					System.out.println("                A = "
							+ Arrays.toString(A) + ", B = "
							+ Arrays.toString(B));
				B = PuzzleManipulation
						.twist90sCoordss(n, finalTwistSequence, B);
				Assert(Arrays.equals(BcubieCenter, Arrays.clamp(B[0], -(n - 1),
						n - 1))); // didn't move BcubieCenter
			}
			if (debugLevel >= 3)
				System.out.println("                flattened A = "
						+ Arrays.toString(A) + ", flattened B = "
						+ Arrays.toString(B));

			// Assert that the dimensionality of the space
			// containing all of the stickers we were trying to align
			// as well as the cubie centers,
			// is 2 or 3 (the number of stickers we were given on each cubie).
			int subspaceDimensionality = 0;
			for (int iAxis = 0; iAxis < d; ++iAxis) {
				if (A[0][iAxis] - AcubieCenter[iAxis] != 0
						|| A[1][iAxis] - AcubieCenter[iAxis] != 0
						|| (A.length == 3 && A[2][iAxis] - AcubieCenter[iAxis] != 0)
						|| B[0][iAxis] - BcubieCenter[iAxis] != 0
						|| B[1][iAxis] - BcubieCenter[iAxis] != 0
						|| (B.length == 3 && B[2][iAxis] - BcubieCenter[iAxis] != 0))
					subspaceDimensionality++;
			}
			Assert(subspaceDimensionality == twoOrThree);

			// and Assert that it's I-shaped
			Assert(Arrays.nIndicesDifferent(AcubieCenter, BcubieCenter) == 1);

			//
			// Okay we have to do one last thing--
			// make it so that the direction of A's stickers, in order,
			// corresponds to B's, in order
			// (possibly permuted cyclically).
			// If not, flip B 180 degrees around until they do.
			// This should only happen if d >= 5 (it's impossible
			// to reverse the twirl in d <= 4).
			//
			if (twoOrThree == 3) {
				int Ai = 0;
				while (A[Ai][BfaceAxis] - AcubieCenter[BfaceAxis] == 0)
					Ai++;
				int Bi = 0;
				while (B[Bi][BfaceAxis] - BcubieCenter[BfaceAxis] == 0)
					Bi++;
				Assert(Arrays.equals(Arrays.minus(A[Ai], AcubieCenter), Arrays
						.minus(BcubieCenter, B[Bi])));
				// Ai and Bi are the indices of the stickers
				// that are opposite...
				if (!Arrays.equals(Arrays.minus(A[(Ai + 1) % 3], AcubieCenter),
						Arrays.minus(B[(Bi + 1) % 3], BcubieCenter))) {
					Assert(Arrays.equals(Arrays.minus(A[(Ai + 1) % 3],
							AcubieCenter), Arrays.minus(B[(Bi + 2) % 3],
							BcubieCenter)));
					// Need to add twists of Bface
					// that hold B[Bi] fixed
					// and swap B[(Bi+1)%3] with B[(Bi+2)%3]
					int theseCoords[][] = { B[Bi], B[(Bi + 1) % 3],
							B[(Bi + 2) % 3] };
					int thoseCoords[][] = { B[Bi], B[(Bi + 2) % 3],
							B[(Bi + 1) % 3] };
					Assert(d >= 5); // the following will fail in d<=4!
					int finalFinalTwistSequence[][] = find_oneface_twist_sequence_taking_these_coords_to_those_coords(
							BfaceAxis, BfaceSign, theseCoords, thoseCoords, 1,
							debugLevel);

					solution = Arrays.concat(solution, finalFinalTwistSequence);

					// apply finalTwistSequence (it only moves B, not A)
					if (debugLevel >= 3)
						System.out
								.println("                applying final final correcting twist sequence to B without moving its center");
					if (debugLevel >= 3)
						System.out.println("                A = "
								+ Arrays.toString(A) + ", B = "
								+ Arrays.toString(B));
					B = PuzzleManipulation.twist90sCoordss(n,
							finalFinalTwistSequence, B);
					Assert(Arrays.equals(BcubieCenter, Arrays.clamp(B[0],
							-(n - 1), n - 1))); // didn't move BcubieCenter
				}
			}
			if (debugLevel >= 3)
				System.out.println("                flattened A = "
						+ Arrays.toString(A) + ", flattened and corrected B = "
						+ Arrays.toString(B));

			if (debugLevel >= 3)
				System.out.println("                solution = "
						+ Arrays.toString(solution));
			if (debugLevel >= 2)
				System.out
						.println("            out flatten_flipPair_or_twirlPair");
			return new Object[] { solution, null };
		} // flatten_flipPair_or_twirlPair

		//
		// Figure out whether the puzzle state is odd.
		// It's odd iff it's an odd permutation on the 2-sticker cubies.
		//
		private static boolean puzzleStateIsOdd(int n, int d,
				Object puzzleIndices) {
			// print "    in puzzleStateIsOdd"
			// Gradually set entries in scratch to null as we've seen them
			Object scratch = Arrays.repeat(new Object(), n + 2, d);

			boolean isOdd = false;
			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				// It's the center of a 2-sticker cubie
				// iff all coords are in [1..n]
				// and it's 1 or n in exactly 2 axis directions.
				int hist[] = Utils.histogram(n + 2, index);
				boolean is2stickerCubieCenter = (hist[0] == 0
						&& hist[n + 1] == 0 && hist[1] + hist[n] == (n == 2 ? d
						: 2)); // fudge: if n==2 then we are really looking for
								// d-sticker cubies. XXX hey! this is wrong for
								// d >= 4! how to tell whether the puzzle state
								// is odd in that case??? or... does it matter?
								// well in that case I think if it's an odd
								// permutation on the corners, then it's
								// unsolvable... and if it's an even permutation
								// on the corners, then it might be an even or
								// odd number of twists but... does it not
								// matter? can we get an odd number of twists
								// going back to original state? YES!
								// interesting!
								// "java NdSolve 2 4 -exec abc -solve" gives a
								// 288-move solution!
				if (is2stickerCubieCenter) {
					int cycleSize = 0;
					while (Arrays.get(scratch, index) != null) {
						cycleSize++;
						Arrays.set(scratch, index, null);
						index = (int[]) Arrays.get(puzzleIndices, index);
					}
					if (cycleSize != 0 && cycleSize % 2 == 0) // if cycle size
																// is even,
																// reverse
																// parity
						isOdd = !isOdd;
				}
			}
			// print "    out puzzleStateIsOdd, returning ",isOdd
			return isOdd;
		} // puzzleStateIsOdd

		private static boolean is_positioned_up_to(int whichToCheck,
				int maxToCheck, int n, int d, Object puz, int debugLevel) {
			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				if (!PuzzleManipulation.isCubieIndex(n, d, index))
					continue; // we're checking cubie centers, not stickers

				// number of stickers = number of positions
				// in which index is <= 1 or >= n...
				int nStickersOnThisCubie = 0;
				for (int i = 0; i < d; ++i)
					if (index[i] <= 1 || index[i] >= n)
						nStickersOnThisCubie++;

				if (nStickersOnThisCubie <= maxToCheck
						&& ((whichToCheck >> nStickersOnThisCubie) & 1) == 1) {
					if (!Arrays.equals((int[]) Arrays.get(puz, index), index)) {
						if (debugLevel >= 2)
							System.out
									.println("    puzzleState is not positioned up to "
											+ maxToCheck
											+ "-sticker cubies: found a "
											+ nStickersOnThisCubie
											+ "-sticker cubie that's in the wrong position: found "
											+ Arrays.toString(Arrays.get(puz,
													index))
											+ " at "
											+ Arrays.toString(index) + "");
						return false;
					}
				}
			}
			return true;
		} // is_positioned

		private static boolean is_oriented_up_to(int whichToCheck,
				int maxToCheck, int n, int d, Object puz, int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("    in is_oriented_up_to(" + maxToCheck
						+ ")");

			// The algorithm below will get confused if n=1
			// because the cubie has more stickers than usual,
			// so in this case just return true-- a 1^ puzzle
			// is always solved anyway
			if (n <= 1) {
				if (debugLevel >= 2)
					System.out.println("    out is_oriented_up_to("
							+ maxToCheck + ") (n==1 so returning true early)");
				return true;
			}

			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				if (!PuzzleManipulation.isStickerIndex(n, d, index))
					continue; // we're checking stickers, not cubie centers

				// number of stickers = number of positions
				// in which index is <= 1 or >= n...
				int nStickersOnThisCubie = 0;
				for (int i = 0; i < d; ++i)
					if (index[i] <= 1 || index[i] >= n)
						nStickersOnThisCubie++;

				if (nStickersOnThisCubie <= maxToCheck
						&& ((whichToCheck >> nStickersOnThisCubie) & 1) == 1) {
					if (!Arrays.equals((int[]) Arrays.get(puz, index), index)) {
						if (debugLevel >= 2)
							System.out
									.println("    puzzleState is not oriented up to "
											+ maxToCheck
											+ "-sticker cubies: found a "
											+ nStickersOnThisCubie
											+ "-sticker cubie that's oriented wrong: found "
											+ Arrays.toString(Arrays.get(puz,
													index))
											+ " at "
											+ Arrays.toString(index) + "");
						// PRINT(puz);
						return false;
					}
				}
			}
			if (debugLevel >= 2)
				System.out.println("    out is_oriented_up_to(" + maxToCheck
						+ ") (returning true)");
			return true;
		} // is_oriented

		public static boolean isSolvable(int n, int d, Object puz,
				// XXX param for checking inside-outedness of corner cubies?
				int whichToCheckPositions, int whichToCheckOrientations,
				java.io.PrintWriter progressWriter, int debugLevel) {
			if (debugLevel >= 1)
				System.out.println("in isSolvable");

			// so that we flush after every newline,
			// to guarantee sanity in case interspersed with debugging output...
			// Note, without this, we aren't guaranteed to flush at the end
			// either.
			// XXX since we are going to do this,
			// XXX should we even require progressWriter to be
			// XXX a printWriter to begin with?
			if (progressWriter != null)
				progressWriter = new java.io.PrintWriter(progressWriter, true);

			boolean solvableSoFar = true;

			// XXX should this be part of isSane too? I think so, since its doc
			// implies that if this fails it should be considered insane
			if (progressWriter != null) {
				progressWriter
						.print("    Figuring out where cubies want to be... ");
				progressWriter.flush();
			}
			Object puzzleIndices = figureOutWhereIndicesWantToBe(n, d, puz);
			if (progressWriter != null)
				progressWriter.println("done.");

			// XXX hack for n==1: if we got this far, concede,
			// XXX rather than digging ourselves into a hole
			// XXX assuming there are at most d stickers per cubie.
			if (n == 1)
				return true;

			// stuff to check orientations on
			// must be a subset of stuff to check positions on...
			// XXX should we just set whichToPosition |= whichToOrient?
			Assert((whichToCheckOrientations & ~whichToCheckPositions) == 0);

			int nIndices = Arrays.intpow(n + 2, d);

			//
			// Check whether all the corners are right-side-out.
			// A corner is right-side-out iff there's
			// a rotation taking it to where it wants to be.
			//
			int nInsideOut = 0;
			{
				if (progressWriter != null) {
					progressWriter
							.print("    Checking for inside-outedness of corner cubies... ");
					progressWriter.flush();
				}
				for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
					int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
					int hist[] = Utils.histogram(n + 2, index);
					boolean isCornerCubieCenter = (hist[1] + hist[n] == d);
					if (!isCornerCubieCenter)
						continue;

					int theseCoords[][] = new int[d][];
					int thoseCoords[][] = new int[d][];

					for (int i = 0; i < d; ++i) {
						int fromStickerIndex[] = Arrays.copy(index);
						fromStickerIndex[i] = (fromStickerIndex[i] == 1 ? 0
								: n + 1);
						int toStickerIndex[] = (int[]) Arrays.get(
								puzzleIndices, fromStickerIndex);
						theseCoords[i] = Utils.indexToCoords(n,
								fromStickerIndex);
						thoseCoords[i] = Utils.indexToCoords(n, toStickerIndex);
					}
					int rots[][] = find_rotation_sequence_taking_these_coords_to_those_coords(
							theseCoords, thoseCoords, true, debugLevel);
					if (rots == null) {
						solvableSoFar = false;
						nInsideOut++;
						break;
					}
				}
				if (progressWriter != null) {
					// XXX think about the 1x puzzle... if we can get here in
					// that case, the message might be wrong
					progressWriter.println("" + nInsideOut + "/"
							+ Arrays.intpow(2, d) + " corners inside-out"
							+ (nInsideOut == 0 ? "." : "!"));
				}
			}

			//
			// If it's an odd permutation on the 2-sticker
			// cubies, then apply a single arbitrary twist-- that will
			// make it much easier to analyze.
			// XXX but what if they didn't include 1<<2 in
			// whichToCheckPositions? this gets subtle
			//
			if (progressWriter != null) {
				progressWriter.print("    Checking permutation parity on "
						+ (n == 2 ? d : 2) + "-sticker cubies... ");
				progressWriter.flush();
			}
			boolean wasOdd = puzzleStateIsOdd(n, d, puzzleIndices);
			if (wasOdd) {
				int twist[] = { 0, +1, 1, 2, 1 };
				puzzleIndices = PuzzleManipulation.twist90(n, d, puzzleIndices,
						twist);
				if (progressWriter != null)
					progressWriter.println("odd; applying one twist");
			} else {
				if (progressWriter != null)
					progressWriter.println("even.");
			}

			Object scratch = Arrays.repeat(new Object(), n + 2, d); // set
																	// entries
																	// in
																	// scratch
																	// to null
																	// as we've
																	// seen them
																	// during
																	// position
																	// parity
																	// checking

			for (int k = 2; k <= d; ++k) {
				if (n < 3 && k < d)
					continue; // there are no k-sticker cubies
				boolean doCheckPositions = ((whichToCheckPositions & (1 << k)) != 0);
				String flipOrTwirl = (k == d ? "twirl" : "flip");
				if (!doCheckPositions) {
					if (progressWriter != null) {
						progressWriter
								.println("    Checking permutation parity on "
										+ k + "-sticker cubies... NOT!");
						progressWriter
								.println("    Checking " + flipOrTwirl
										+ " parity on " + k
										+ "-sticker cubies... NOT!");
					}
					continue;
				}
				boolean doCheckOrientations = ((whichToCheckOrientations & (1 << k)) != 0);

				//
				// Check position parity on the k-sticker cubies
				//
				if (k != 2 || wasOdd) // avoid showing two identical
										// "Checking permutation parity on 2-sticker cubies... even."
										// messages
				{
					if (progressWriter != null) {
						progressWriter
								.print("    Checking permutation parity on "
										+ k + "-sticker cubies... ");
						progressWriter.flush();
					}
					// XXX the following loop is too similar to puzzleStateIsOdd
					// ... should combine into a single function (if we're
					// willing to suffer the overhead of a new scratch array for
					// each k, which is fine I think)
					boolean isOdd = false;
					for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
						int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
						// It's the center of a k-sticker cubie
						// iff all coords are in [1..n]
						// and it's 1 or n in exactly k axis directions.
						int hist[] = Utils.histogram(n + 2, index);
						boolean isKStickerCubieCenter = (hist[0] == 0
								&& hist[n + 1] == 0 && hist[1] + hist[n] == k);
						if (!isKStickerCubieCenter)
							continue;
						int cycleLength = 0;
						while (Arrays.get(scratch, index) != null) {
							cycleLength++;
							Arrays.set(scratch, index, null);
							index = (int[]) Arrays.get(puzzleIndices, index);
						}
						if (cycleLength > 0 // if not seen already
								&& cycleLength % 2 == 0) // if cycle is of even
															// length
							isOdd = !isOdd;
					}
					if (isOdd) {
						solvableSoFar = false;
						if (progressWriter != null)
							progressWriter.println("odd!");
					} else {
						if (progressWriter != null)
							progressWriter.println("even.");
					}
				}
				//
				// Check orientation parity on the k-sticker cubies
				// XXX does this make sense if position parity is odd? if not,
				// then skip it
				//
				if (doCheckOrientations) {
					if (progressWriter != null) {
						progressWriter.print("    Checking " + flipOrTwirl
								+ " parity on " + k + "-sticker cubies... ");
						progressWriter.flush();
					}

					if (k == d && nInsideOut > 0) {
						if (progressWriter != null)
							progressWriter.println(" NOT (it's pointless)");
					} else {

						// First do cubie position swaps until
						// the k-sticker cubie positions are all right,
						// to make it easier to analyze.
						// Since this is the last thing we are doing
						// with the k-sticker cubies,
						// it's okay if we re-order the puzzleIndices array.
						// XXX is this right for corners? seems like it might
						// turn some inside out, which would be bad
						if (true) // XXX actually I think this might be
									// unnecessary for the flip parity analysis
									// (but it's necessary for the twirl
									// analysis), but it doesn't hurt
						{
							for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
								int index[] = Arrays.unFlatIndex(iIndex, n + 2,
										d);
								// It's the center of a k-sticker cubie
								// iff all coords are in [1..n]
								// and it's 1 or n in exactly k axis directions.
								int hist[] = Utils.histogram(n + 2, index);
								boolean isKStickerCubieCenter = (hist[0] == 0
										&& hist[n + 1] == 0 && hist[1]
										+ hist[n] == k);
								if (!isKStickerCubieCenter)
									continue;

								int target[];
								while (!Arrays.equals(target = (int[]) Arrays
										.get(puzzleIndices, index), index)) {
									// PRINT(index);
									// PRINT(target);
									Arrays.set(puzzleIndices, index, Arrays
											.get(puzzleIndices, target));
									Arrays.set(puzzleIndices, target, target);
									for (int i = 0; i < d; ++i) {
										// PRINT(i);
										if (index[i] != 1 && index[i] != n)
											continue; // no sticker in this
														// direction
										int stickerIndex[] = Arrays.copy(index);
										stickerIndex[i] = (index[i] == 1 ? 0
												: n + 1);
										int stickerTarget[] = (int[]) Arrays
												.get(puzzleIndices,
														stickerIndex);
										// PRINT(stickerIndex);
										// PRINT(stickerTarget);
										Arrays.set(puzzleIndices, stickerIndex,
												Arrays.get(puzzleIndices,
														stickerTarget));
										Arrays.set(puzzleIndices,
												stickerTarget, stickerTarget);
									}
								}
							}
						}

						if (k == d) // XXX only do this if nInsideOut > 0?
						{
							//
							// Corner cubies.
							// Twirl modulus check is only necessary
							// when d is 3 or 4,
							// since for d>=5 it's possible to twirl a single
							// corner in isolation.
							//
							if (d == 3 || d == 4) {
								int twirlModulus = 0;
								for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
									int index[] = Arrays.unFlatIndex(iIndex,
											n + 2, d);
									// It's the center of a k-sticker cubie
									// iff all coords are in [1..n]
									// and it's 1 or n in exactly k axis
									// directions.
									int hist[] = Utils.histogram(n + 2, index);
									boolean isKStickerCubieSticker = (hist[0]
											+ hist[n + 1] == 1 && hist[0]
											+ hist[1] + hist[n] + hist[n + 1] == k);
									if (!isKStickerCubieSticker)
										continue;

									int cycle[][] = {};
									while (Arrays.get(scratch, index) != null) {
										cycle = (int[][]) Arrays.append(cycle,
												index);
										Arrays.set(scratch, index, null);
										index = (int[]) Arrays.get(
												puzzleIndices, index);
									}
									if (d == 3)
										Assert(cycle.length != 2); // no swaps
																	// within a
																	// corner
																	// cubie in
																	// 3d, since
																	// we know
																	// nothing's
																	// inside
																	// out
									if (cycle.length != 0 // if not seen already
											&& cycle.length != 1 // and not
																	// trivial
											&& cycle.length != 2) // and not
																	// swaps
																	// (swaps
																	// are fine,
																	// if d==4,
																	// since we
																	// know they
																	// occur in
																	// pairs
																	// since we
																	// checked
																	// for
																	// inside
																	// outedness)
									{
										Assert(cycle.length == 3);
										if (d == 4) {
											int cubieCenter[] = Arrays.clamp(
													index, 1, n);
											cycle = Arrays.append(cycle,
													cubieCenter);
										}
										int coordss[][] = (int[][]) Utils
												.indexsToCoordss(n, cycle);
										int det = Arrays.intdet(coordss);
										Assert(det != 0);
										if (det < 0)
											twirlModulus--;
										else
											twirlModulus++;
									}
								}
								if ((twirlModulus % 3) != 0) {
									solvableSoFar = false;
									if (progressWriter != null)
										progressWriter
												.println("nonzero mod 3!");
								} else {
									if (progressWriter != null)
										progressWriter.println("zero mod 3.");
								}
							} else {
								if (progressWriter != null)
									progressWriter
											.println("not necessary (d>=5)");
							}
						} else {
							//
							// Non-corner cubies.
							// Check whether the number of flips needed is odd.
							//
							boolean isOdd = false;
							// XXX hey, was the cubie reordering even necessary?
							// think about it
							for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
								int index[] = Arrays.unFlatIndex(iIndex, n + 2,
										d);
								// It's the center of a k-sticker cubie
								// iff all coords are in [1..n]
								// and it's 1 or n in exactly k axis directions.
								int hist[] = Utils.histogram(n + 2, index);
								boolean isKStickerCubieSticker = (hist[0]
										+ hist[n + 1] == 1 && hist[0] + hist[1]
										+ hist[n] + hist[n + 1] == k);
								if (!isKStickerCubieSticker)
									continue;
								int cycleLength = 0;
								while (Arrays.get(scratch, index) != null) {
									cycleLength++;
									Arrays.set(scratch, index, null);
									index = (int[]) Arrays.get(puzzleIndices,
											index);
								}
								if (cycleLength > 0 // if not seen already
										&& cycleLength % 2 == 0) // if cycle is
																	// of even
																	// length
									isOdd = !isOdd;
							}
							if (isOdd) {
								solvableSoFar = false;
								if (progressWriter != null)
									progressWriter.println("odd!");
							} else {
								if (progressWriter != null)
									progressWriter.println("even.");
							}
						}
					}
				} else {
					if (progressWriter != null)
						progressWriter
								.println("    Checking " + flipOrTwirl
										+ " parity on " + k
										+ "-sticker cubies... NOT!");
				}
			}

			if (progressWriter != null) {
				if (solvableSoFar)
					progressWriter.println("    Puzzle is solvable.");
				else
					progressWriter.println("    Puzzle is NOT solvable.");
				progressWriter.println();
			}

			if (debugLevel >= 1)
				System.out.println("out isSolvable");
			return solvableSoFar;
		} // isSolvable

		public static boolean isSolved(int n, int d, Object puz) {
			// XXX should maybe have this be simpler, since
			// figureOutWhereIndicesWantToBe will fail
			// XXX if n>=4 but it should be easy to check this anyway
			Object puzzleIndices = figureOutWhereIndicesWantToBe(n, d, puz);
			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				if (PuzzleManipulation.isStickerIndex(n, d, index)
						&& !Arrays.equals((int[]) Arrays.get(puzzleIndices,
								index), index))
					return false;
			}
			return true;
		}

	} // private static class SolveStuff

	// =========================================================================

	//
	// Puzzle i/o utility functions:
	// puzzleToString
	// puzzleFromString(String s) (returns a triple {n,d,puz})
	// puzzleFromString(int n, int d, String s)
	// puzzleFromReader (keeps reading lines until puzzleFromString succeeds)
	//
	private static class PuzzleIO {
		@SuppressWarnings("unused")
		public static String puzzleToString(int n, int d, Object puz,
				String template) {
			// This logic is kinda backwards... could really use the template
			// in the inner core of _puzzleToString, but whatever, that's
			// already written.
			String string0 = puzzleToString(n, d, puz); // the non-template
														// version
			string0 = string0.replaceAll("\\s", ""); // throw away its
														// formatting
			int nStickers = string0.length();
			int numNonSpacesInTemplate = template.replaceAll("\\s", "")
					.length();
			if (numNonSpacesInTemplate != nStickers) {
				throw new Error("Template number of non-spaces is "
						+ numNonSpacesInTemplate + ", required number is "
						+ nStickers + " for this " + n + "^" + d + " puzzle");
			}

			StringBuffer sb = new StringBuffer();

			int iSticker = 0;
			for (int i = 0; i < template.length(); ++i) {
				char c = template.charAt(i);
				if (Character.isWhitespace(c))
					sb.append(c);
				else
					sb.append(string0.charAt(iSticker++));
			}
			Assert(iSticker == nStickers);

			return sb.toString();
		}

		public static String puzzleToString(int n, int d, Object puz) {
			StringBuffer sb = new StringBuffer();
			_puzzleToString(n, d, d, puz, Arrays.repeat(0, d), sb);
			return sb.toString();
		}

		// recursive work function, prints a d-dimensional layer
		// of the D-dimensional puzzle.
		private static void _puzzleToString(int n, int d, int D, Object puz,
				int offset[], StringBuffer sb) {
			if (d == 0) {
				Object item = Arrays.get(puz, offset);
				if (item == null)
					sb.append(' ');
				else if (item.getClass().isArray()) // it's an array: assume
													// it's a multidimensional
													// target index.
				{
					int targetIndex[] = (int[]) item;
					// See what letter we are.
					// Look at the item here, it is the index
					// where this sticker goes.
					// Determine its color from the index.
					int extremeAxis = -1;
					int nExtremes = 0;
					for (int iDim = 0; iDim < D; ++iDim) {
						if (targetIndex[iDim] == 0
								|| targetIndex[iDim] == n + 1) {
							nExtremes += 1;
							extremeAxis = iDim;
						}
					}
					if (nExtremes != 1) {
						// sb.append('.');
						sb.append(' ');
					} else {
						sb.append((char) ((targetIndex[extremeAxis] == 0 ? 'A'
								: 'a') + extremeAxis));
					}
				} else
					// it's a primitive or object: assume the substitution has
					// already been done and it's a letter.
					sb.append(item);
			} else {
				for (offset[D - d] = 0; offset[D - d] < n + 2; ++offset[D - d])
					_puzzleToString(n, d - 1, D, puz, offset, sb);
				if (d == (D + 1) / 2)
					sb.append('\n'); // XXX should use system notion?
			}
		} // private _puzzleToString

		// In this version, n and d are known beforehand.
		// and the spacing in the string is ignored.
		// Only puz is returned (not a triple {n,d,puz} like the other version).
		public static Object puzzleFromString(int n, int d, String s,
				int debugLevel) {
			int nNonSpacesExpected = 2 * d * Arrays.intpow(n, d - 1);
			if (debugLevel >= 2)
				System.out.println("    nNonSpacesExpected = "
						+ nNonSpacesExpected);

			String nonSpaces = s.replaceAll("\\s", "");

			if (nonSpaces.length() != nNonSpacesExpected)
				throw new Error("expected " + nNonSpacesExpected
						+ " non-spaces, got " + nonSpaces.length()); // XXX what
																		// to do

			if (debugLevel >= 2)
				System.out.println("    nonSpaces = "
						+ Arrays.toString(nonSpaces));

			// Make a multidimensional array, and for every
			// sticker index in it, replace the entry with the next
			// sticker color (letter) from the string,
			// and replace the non-sticker entries with spaces.

			Object puz = Arrays.repeat(null, n + 2, d);
			// PRINT(puz);
			int iNonSpace = 0;
			int nIndices = Arrays.intpow(n + 2, d);
			for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
				int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
				if (PuzzleManipulation.isStickerIndex(n, d, index))
					Arrays.set(puz, index, new Character(nonSpaces
							.charAt(iNonSpace++)));
				else
					Arrays.set(puz, index, new Character(' '));
			}
			Assert(iNonSpace == nNonSpacesExpected);
			return puz;
		} // puzzleFromString knowing n and d beforehand

		// Returns a triple {Integer(n),Integer(d),puz}.
		public static Object[/* 3 */] puzzleFromString(String s, int debugLevel) {
			if (debugLevel >= 2)
				System.out.println("    in puzzleFromString");
			s = s.replaceAll("\\s", ""); // remove whitespace

			java.util.HashMap<Character, int[]> colorCounts = new java.util.HashMap<Character, int[]>();
			for (int i = 0; i < s.length(); ++i) {
				Character c = new Character(s.charAt(i));
				if (!colorCounts.containsKey(c))
					colorCounts.put(c, new int[] { 0 });
				((int[]) colorCounts.get(c))[0]++;
			}
			int nColors = colorCounts.size();
			if (debugLevel >= 2)
				System.out.println("        nColors = " + nColors);
			if (nColors % 2 != 0)
				throw new Error("odd number of colors " + nColors
						+ " in puzzleFromString");
			int d = nColors / 2;
			if (debugLevel >= 2)
				System.out.println("        d = " + d);
			double N = d - 1 <= 0 ? 1 // arbitrary
					: Math.pow(s.length() / (2 * d), 1. / (d - 1));
			int n = (int) Math.round(N);
			int faceSize = Arrays.intpow(n, d - 1);
			if (2 * d * faceSize != s.length())
				throw new Error("nNonSpaces = " + s.length() + ", d = " + d
						+ ", n = " + N + " in puzzleFromString");
			if (debugLevel >= 2)
				System.out.println("        n = " + n);

			if (!_allCountsAre(faceSize, colorCounts))
				throw new Error("ERROR: this is a no good puzzle state! n=" + n
						+ ", d=" + d
						+ ", but color counts are not all n^(d-1)=" + faceSize
						+ ""); // XXX be more helpful and tell which color is
								// wrong?

			// now call the version where we know n and d.
			// XXX need to do more sanity checking on colorCounts!
			return new Object[] { new Integer(n), new Integer(d),
					puzzleFromString(n, d, s, debugLevel) };
		} // puzzleFromString

		// Keeps reading lines until what it's read is a valid puzzle.
		// (Likes to live dangerously.)
		// Actually, rejects puzzles of dimension <= 1 that are
		// jammed together and all on one line,
		// so that, for example, it doesn't
		// mistake the first line of a formatted scrambled 4^3 puzzle
		// for a 1^2 puzzle.
		// XXX actually doesn't, yet...

		// Returns a triple {Integer(n),Integer(d),puz}.
		// XXX this isn't very robust, it can go south and will hang
		// XXX forever if it gets bad input...
		// XXX should use the number
		// XXX of initial non-space chars as a clue to what n should be,
		// XXX unless all on one line.
		public static Object[/* 3 */] puzzleFromReader(
				java.io.BufferedReader reader, int debugLevel) {
			StringBuffer sb = new StringBuffer();
			java.util.HashMap<Character, int[]> colorCounts = new java.util.HashMap<Character, int[]>();
			int nLinesGot = 0;
			int nWordsGot = 0;
			while (true) {
				{
					String line;
					try {
						line = reader.readLine();
					} catch (java.io.IOException e) {
						e.printStackTrace();
						continue; // XXX not sure what this entails, never seen
									// it happen I don't think
					}
					if (line == null)
						throw new Error("premature EOF scanning puzzle after "
								+ nLinesGot + " line"
								+ (nLinesGot == 1 ? "" : "s") + ", "
								+ sb.length() + " nonspaces, "
								+ colorCounts.size() + " colors!");
					nLinesGot++;
					{
						String words[] = line.split("\\s");
						if (words.length == 1 && words[0].equals(""))
							words = new String[0]; // lame that I have to do
													// this, but "".split("\\s")
													// returns {""} instead of
													// {}
						nWordsGot += words.length;
					}
					line = line.replaceAll("\\s", "");
					if (line.length() == 0)
						continue;
					for (int i = 0; i < line.length(); ++i) {
						Character c = new Character(line.charAt(i));
						if (!colorCounts.containsKey(c))
							colorCounts.put(c, new int[] { 0 });
						(colorCounts.get(c))[0]++;
					}
					sb.append(line);
				}

				int nColors = colorCounts.size();
				if (nColors % 2 != 0)
					continue; // not a valid puzzle yet; need to read more
				int d = nColors / 2;
				double N = d - 1 <= 0 ? 1 // arbitrary
						: Math.pow(sb.length() / (2 * d), 1. / (d - 1));
				int n = (int) Math.round(N);
				int faceSize = Arrays.intpow(n, d - 1);
				if (2 * d * faceSize != sb.length())
					continue; // not a valid puzzle yet; need to read more
				if (!_allCountsAre(faceSize, colorCounts))
					continue; // not a valid puzzle yet; need to read more

				// "asdf" - 1^2 or first line of 4^3
				// "abcdef" - 1^3 or first line of 6^3
				// "aabbccdd" - 2^2 or first line of 8^3
				// XXX You know this is really dangerous... maybe
				// shouldn't accept puzzles of dimension <= 2 here at all?
				if ((n <= 1 || d <= 2) && nWordsGot <= 1) {
					if (debugLevel >= 1)
						System.out
								.println("NOT returning a "
										+ n
										+ "^"
										+ d
										+ " puzzle that was all jammed together, since it could be part of a larger puzzle");
					continue;
				}

				// now call the version where we know n and d.
				if (debugLevel >= 2)
					System.out.println("    n = " + n);
				if (debugLevel >= 2)
					System.out.println("    d = " + d);
				return new Object[] { new Integer(n), new Integer(d),
						puzzleFromString(n, d, sb.toString(), debugLevel) };
			}
		} // puzzleFromReader

		private static boolean _allCountsAre(int desiredCount,
				java.util.HashMap<Character, int[]> counts) {
			for (java.util.Iterator<Character> it = counts.keySet().iterator(); it
					.hasNext();) {
				Object key = it.next();
				int count = ((int[]) counts.get(key))[0];
				if (count != desiredCount)
					return false;
			}
			return true;
		} // private _allCountsAre

		private static char[][] getSignedAxisColors(int n, int d, Object puz) {
			char colors[][] = new char[d][2];
			{
				for (int iDim = 0; iDim < d; ++iDim)
					for (int iSign = 0; iSign < 2; ++iSign) {
						int sign = iSign == 0 ? -1 : 1;
						int coords[] = Arrays.repeat(n % 2 == 0 ? sign
								* (n - 1) : 0, d);
						coords[iDim] = sign * (n + 1);
						int index[] = Utils.coordsToIndex(n, coords);
						colors[iDim][iSign] = ((Character) Arrays.get(puz, index)).charValue();
					}

				java.util.HashMap<Character, char[]> colorToPossibleOpposites = new java.util.HashMap<Character, char[]>();
				{
					int nColorsSeen = 0;
					char allColors[] = new char[2 * d];

					int nIndices = Arrays.intpow(n + 2, d);
					for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
						int index[] = Arrays.unFlatIndex(iIndex, n + 2, d);
						if (PuzzleManipulation.isStickerIndex(n, d, index)) {
							Character Color = (Character) Arrays
									.get(puz, index);
							if (!colorToPossibleOpposites.containsKey(Color))
								allColors[nColorsSeen++] = Color.charValue();
							colorToPossibleOpposites.put(Color, allColors);
						}
					}
					Assert(nColorsSeen == 2 * d);

					int cubieCenterIndex[] = new int[d];
					Character colorsOnThisCubie[] = new Character[d];
					for (int iCorner = 0; iCorner < (1 << d); ++iCorner) {
						for (int iDim = 0; iDim < d; ++iDim)
							if (((iCorner >> iDim) & 1) == 1)
								cubieCenterIndex[iDim] = n;
							else
								cubieCenterIndex[iDim] = 1;
						for (int iDim = 0; iDim < d; ++iDim) {
							int stickerIndex[] = Arrays.copy(cubieCenterIndex);
							stickerIndex[iDim] += (((iCorner >> iDim) & 1) == 1 ? 1
									: -1);
							colorsOnThisCubie[iDim] = (Character) Arrays.get(
									puz, stickerIndex);
						}
						for (int iDim = 0; iDim < d; ++iDim)
							for (int jDim = 0; jDim < d; ++jDim) {
								Character iColor = colorsOnThisCubie[iDim];
								Character jColor = colorsOnThisCubie[jDim];
								char possibleOpposites[] = colorToPossibleOpposites.get(iColor);
								int found = Arrays.find(possibleOpposites, jColor.charValue());
								if (found != -1) {
									possibleOpposites = Arrays.delete(possibleOpposites, found);
									colorToPossibleOpposites.put(iColor, possibleOpposites);
								}
							}
					}
					//
					// If we did that right,
					// colorToPossibleOpposites should
					// now be pruned down to the actual opposite
					// of each color.
					//
					// PRINT(colorToPossibleOpposites);
					for (java.util.Iterator<Character> it = colorToPossibleOpposites.keySet().iterator(); it.hasNext();) {
						Character key = it.next();
						char possibleOpposites[] = colorToPossibleOpposites.get(key);
						Assert(possibleOpposites.length == 1);
					}
				}
				for (int iDim = 0; iDim < d; ++iDim) {
					char color = colors[iDim][0];
					char oppositeColor = (colorToPossibleOpposites.get(new Character(color)))[0];
					if (n % 2 == 1) {
						Assert(colors[iDim][1] == oppositeColor);
					} else {
						colors[iDim][1] = oppositeColor;
					}
				}
			}
			// PRINT(colors);
			// PRINT(PuzzleIO.puzzleToString(n,d,puz));
			return colors;
		} // getSignedAxisColors

		// Tricky... twisting the center slice
		// can change the meaning of subsequent moves.
		// Easiest way to deal with this
		// would be to keep a puzzle that we update as we go,
		// but that would be really expensive...
		// So we have to keep track of the colors explicitly.
		// XXX hey!!!! I marked this private by mistake... but how come solve()
		// XXX can still get at it???????? That's not right!
		private static String movesToString(int moves[][], int n, int d,
				Object puz) {
			char colors[][] = getSignedAxisColors(n, d, puz);

			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < moves.length; ++i) {
				int faceAxis = moves[i][0];
				int faceSign = moves[i][1];
				int fromAxis = moves[i][2];
				int fromSign = -1; // so we favor the letters that come first in
									// the input string
				int toAxis = moves[i][3];
				int toSign = -1; // so we favor the letters that come first in
									// the input string
				int slicesMask = moves[i][4];
				char faceColor = colors[faceAxis][(faceSign + 1) / 2];
				char fromColor = colors[fromAxis][(fromSign + 1) / 2];
				char toColor = colors[toAxis][(toSign + 1) / 2];

				if (n % 2 == 0) {
					//
					// Make sure it doesn't move the first cubie...
					// if it did, we'd have to update colors,
					// but that never happens since we only get called
					// from solve and scramble, which are careful
					// to never move the first cubie when n is even.
					//
					int bit = (faceSign < 0 ? 0 : n - 1);
					if (((slicesMask >> bit) & 1) == 1) {
						throw new Error(
								"internal error: PuzzleIO::movesToString called on a move that moves the first cubie!");
					}
				}

				sb.append(faceColor);
				sb.append(fromColor);
				sb.append(toColor);
				if (slicesMask != 1)
					sb.append(":" + slicesMask);
				sb.append(" ");
			}
			return sb.toString().trim();
		} // movesToString

		public static int[][] movesFromStrings(String moveStrings[], int n,
				int d, Object puz) {
			char colors[][] = getSignedAxisColors(n, d, puz);

			boolean needToKeepPuzUpdated = false;
			if (n % 2 == 0) {
				// n is even,
				// so if any move except the last
				// moves the first-listed cubie,
				// then we need to keep the puzzle updated
				// throughout, so that we can figure
				// out the new axis labels when cubie 0 is moved.
				// This is kinda unfortunate.
				// Fortunately this never happens for move strings
				// generated by apply() or solve().
				// XXX really only need to keep track of the corners,
				// XXX that might be less expensive
				for (int iMove = 0; iMove < moveStrings.length - 1; ++iMove) // don't
																				// worry
																				// about
																				// last
																				// one
				{
					char c = moveStrings[iMove].charAt(0);
					for (int iDim = 0; iDim < d; ++iDim)
						if (colors[iDim][0] == c) {
							needToKeepPuzUpdated = true;
							break;
						}
					if (needToKeepPuzUpdated)
						break;
				}
			} else {
				// n is odd,
				// so we need to keep the puzzle updated
				// if any move except the last moves a middle slice.
				// Actually we don't keep the puzzle updated,
				// we just update the face center colors,
				// which is much much much much cheaper.
				for (int iMove = 0; iMove < moveStrings.length - 1; ++iMove) // don't
																				// worry
																				// about
																				// last
																				// one
				{
					String moveString = moveStrings[iMove];
					String tokens[] = moveString.split(":");
					Assert(tokens.length == 1 || tokens.length == 2);
					int slicesMask = (tokens.length == 2 ? Integer
							.parseInt(tokens[1]) : 1);
					int bit = (n - 1) / 2;
					if (((slicesMask >> bit) & 1) == 1) {
						needToKeepPuzUpdated = true;
						break;
					}
				}
			}
			// PRINT(needToKeepPuzUpdated);

			int moves[][] = new int[moveStrings.length][];
			for (int iMove = 0; iMove < moveStrings.length; ++iMove) {
				String moveString = moveStrings[iMove];
				String tokens[] = moveString.split(":");
				Assert(tokens.length == 1 || tokens.length == 2);
				int slicesMask = (tokens.length == 2 ? Integer
						.parseInt(tokens[1]) : 1);

				int axes[] = { -1, -1, -1 };
				int signs[] = { 0, 0, 0 };
				for (int i = 0; i < 3; ++i) {
					char c = tokens[0].charAt(i);
					for (int iDim = 0; iDim < d; ++iDim) {
						for (int iSign = 0; iSign < 2; ++iSign) {
							if (colors[iDim][iSign] == c) {
								axes[i] = iDim;
								signs[i] = iSign * 2 - 1;
								break;
							}
						}
						if (axes[i] != -1)
							break;
					}
					if (axes[i] == -1)
						throw new Error("Couldn't find face with color '" + c
								+ "'!");
				}
				int faceAxis = axes[0];
				int faceSign = signs[0];
				int fromAxis = axes[1];
				int fromSign = signs[1];
				int toAxis = axes[2];
				int toSign = signs[2];
				moves[iMove] = PuzzleManipulation.makeTwist90(faceAxis,
						faceSign, fromAxis, fromSign, toAxis, toSign,
						slicesMask);
				if (needToKeepPuzUpdated && iMove + 1 < moveStrings.length) // XXX
																			// actually
																			// could
																			// be
																			// smarter
																			// and
																			// only
																			// keep
																			// it
																			// updated
																			// until
																			// the
																			// last
																			// twist
																			// that
																			// moves
																			// cubie
																			// 0
				{
					if (n % 2 == 0) {
						// XXX Really expensive, and silly,
						// since we are probably going to apply all these moves
						// to the puzzle again right after this
						puz = PuzzleManipulation.twist90(n, d, puz,
								moves[iMove]);
						int bit = (faceSign < 0 ? 0 : n - 1);
						if (((slicesMask >> bit) & 1) == 1) {
							colors = getSignedAxisColors(n, d, puz);
						}
					} else {
						int bit = (n - 1) / 2;
						if (((slicesMask >> bit) & 1) == 1) {
							// Just rotate the colors
							// +fromAxis -> +toAxis -> -fromAxis -> -toAxis ->
							// +fromAxis
							char temp = colors[fromAxis][1];
							colors[fromAxis][1] = colors[toAxis][0];
							colors[toAxis][0] = colors[fromAxis][0];
							colors[fromAxis][0] = colors[toAxis][1];
							colors[toAxis][1] = temp;
						}
					}
				}
			}
			return moves;
		} // movesFromStrings

	} // private static class PuzzleIO

	// ========================================================================
	// THIS IS THE PUBLIC STUFF
	//

	/**
	 * Returns a string representing a pristine n<sup>d</sup> puzzle, formatted
	 * nicely with A,B,C,... for the first face colors and a,b,c,... for the
	 * respective opposite face colors.
	 * <p>
	 * Throws an Error if n,d are ridiculous.
	 */
	public static String newPuzzle(int n, int d) {
		// XXX think about whether these are exactly right
		if (n < 1 || d < 1 || Math.pow(n + 2, d) > 1e9)
			throw new Error("newPuzzle(n=" + n + ", d=" + d
					+ ") called-- yeah, right.");

		// The way to get a letters array version of a puzzle
		// is a bit roundabout--
		// we do it by stringifying and unstringifying.
		// This works because both the indices array
		// and the letters array
		// have the same string representation-- it shows the letters
		// but not the indices.
		Object puzzleIndices = Arrays.makeIndexArray(n + 2, d);
		return PuzzleIO.puzzleToString(n, d, puzzleIndices);
	} // newPuzzle

	/**
	 * Reformats the non-space chars of puzzleString to look like template.
	 * <p>
	 * Throws an Error if puzzleString and template have a different number of
	 * non-space chars; does no other sanity checking on puzzleString.
	 */
	public static String reformatPuzzleString(String puzzleString,
			String template) {
		puzzleString = puzzleString.replaceAll("\\s", ""); // remove spaces
		int nStickers = puzzleString.length();
		int nNonSpacesInTemplate = template.replaceAll("\\s", "").length();
		if (nNonSpacesInTemplate != nStickers)
			throw new Error("Tried to format a " + nStickers
					+ "-sticker puzzle with a " + nNonSpacesInTemplate
					+ "-sticker template");

		StringBuffer sb = new StringBuffer();

		int iSticker = 0;
		for (int i = 0; i < template.length(); ++i) {
			char c = template.charAt(i);
			if (Character.isWhitespace(c))
				sb.append(c);
			else
				sb.append(puzzleString.charAt(iSticker++));
		}
		Assert(iSticker == nStickers);

		return sb.toString();
	} // reformatPuzzleString

	/**
	 * Tells whether the given string represents an n<sup>d</sup> puzzle for
	 * some n, d (possibly taken apart and put back together wrong, possibly
	 * with some corners inside out).
	 */
	public static boolean isSane(String puzzleString) {
		try {
			Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString,
					0);
			int n = ((Integer) n_d_puz[0]).intValue();
			int d = ((Integer) n_d_puz[1]).intValue();
			Object puz = n_d_puz[2];
			if (n <= 3) {
				SolveStuff.figureOutWhereIndicesWantToBe(n, d, puz);
			} else {
				// figureOutWhereIndicesWantToBe will fail in this case...
				// but it doesn't matter, isSolvable and solve are documented
				// to throw an error in that case anyway.
			}
			// If nothing got thrown by either operation, we're sane.
			return true;
		} catch (Throwable e) {
			// e.printStackTrace();
			return false;
		}
	} // isSane

	/**
	 * Tells whether the given (assumed to be sane) puzzle string is solvable.
	 * <p>
	 * Equivalent to {@link #isSolvable(String,int,int,java.io.PrintWriter,int)
	 * isSolvable}(puzzleString, ~0, ~0, null, 0).
	 */
	public static boolean isSolvable(String puzzleString) {
		return isSolvable(puzzleString, ~0, ~0, null, 0);
	} // isSolvable with default args

	/**
	 * Tells whether the given (assumed to be sane) puzzle string is solvable.
	 * <p>
	 * whichToCheckPositions and whichToCheckOrientations should be bit masks
	 * representing which cubie types (i.e. number of stickers per cubie) to
	 * check permution parity of and which to check orientation parity of,
	 * respectively; whichToOrient must be a subset of whichToPosition.
	 * <p>
	 * Throws an Error if the puzzle string is insane (see {@link #isSane
	 * isSane}) or if there are any bits set in whichToCheckPositions which are
	 * not also set in whichToCheckOrientations. Currently also throws an Error
	 * if n > 3.
	 */
	public static boolean isSolvable(String puzzleString,
			int whichToCheckPositions, int whichToCheckOrientations,
			java.io.PrintWriter progressWriter, int debugLevel) {
		if (!isSane(puzzleString))
			throw new Error("isSolvable called on insane puzzle");
		Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString, 0);
		int n = ((Integer) n_d_puz[0]).intValue();
		int d = ((Integer) n_d_puz[1]).intValue();
		Object puz = n_d_puz[2];
		if (n > 3)
			throw new Error(
					"isSolvable called on a "
							+ n(puzzleString)
							+ "^"
							+ d(puzzleString)
							+ " puzzle, only implemented for lengths n <= 3.  How the hell should I know?  What do you think I am, a supergenius?");
		return SolveStuff.isSolvable(n, d, puz, whichToCheckPositions,
				whichToCheckOrientations, progressWriter, debugLevel);
	} // isSolvable

	/**
	 * Tells whether the given (assumed to be solvable) puzzle string is already
	 * solved.
	 * <p>
	 * Throws an Error if the puzzle is insane (see {@link #isSane isSane}).
	 * <p>
	 * If n > 3 it may return a result (which may be taken as definitive) or may
	 * throw an Error, since it may not be completely implemented.
	 */
	public static boolean isSolved(String puzzleString) {
		Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString, 0); // throws
																				// if
																				// insane,
																				// which
																				// is
																				// what
																				// we
																				// want
																				// XXX
																				// although
																				// maybe
																				// should
																				// explicitly
																				// catch
																				// it
																				// and
																				// throw
																				// something
																				// definite
		int n = ((Integer) n_d_puz[0]).intValue();
		int d = ((Integer) n_d_puz[1]).intValue();
		Object puz = n_d_puz[2];
		return SolveStuff.isSolved(n, d, puz);
	} // isSolved

	/**
	 * Solves the puzzle.
	 * <p>
	 * Equivalent to {@link #solve(String,int,int,java.io.PrintWriter,int)
	 * solve}(puzzleString, ~0, ~0, null, 0).
	 */
	public static String solve(String puzzleString) {
		return solve(puzzleString, ~0, ~0, null, 0);
	} // solve with no masks or debugLevel params

	/**
	 * Solves the puzzle. The format of the returned solution string is
	 * described in the class description at the beginning of this document.
	 * <p>
	 * whichToPosition and whichToOrient should be bit masks representing which
	 * cubie types (i.e. number of stickers per cubie) to position and which to
	 * orient, respectively; whichToOrient must be a subset of whichToPosition.
	 * <p>
	 * Progress messages and stats will be written to progressWriter if it is
	 * not null.
	 * <p>
	 * If the puzzle length n is even, the twists in the solution will not move
	 * the first-listed cubie.
	 * <p>
	 * Throws an Error if puzzleString is insane or unsolvable (see
	 * {@link #isSane isSane}, {@link #isSolvable isSolvable}) or if there are
	 * any bits set in whichToOrient which are not also set in whichToPosition.
	 * <br>
	 * Currently also throws an Error if n > 3 (the solve algorithm is only
	 * implemented for n &le; 3).
	 * <p>
	 * debugLevel specifies how much debugging output to send to System.out;
	 * this is probably not useful to the general public, unless you are curious
	 * to see how much thinking it's doing:
	 * 
	 * <pre>
	 *      0 - none (default)
	 *      1 - print messages on entry/exit of top-level functions
	 *      2 - print messages on entry/exit of most functions
	 *      3 - print most function arguments and return values
	 *      4 - severe debug-- dump variables that I found useful at the time.
	 * </pre>
	 */
	public static String solve(String puzzleString, int whichToPosition,
			int whichToOrient, java.io.PrintWriter progressWriter,
			int debugLevel) {
		// Check sanity
		// and solvability of the specified parts; if they fail, throw.
		if (!isSane(puzzleString))
			throw new Error("solve called on insane puzzle");

		// Check that puzzle size is something we know how to solve...
		// XXX this isn't being tested in TestPlayer, since isSolvable bombs out
		// first
		if (n(puzzleString) > 3)
			throw new Error(
					"solve called on a "
							+ n(puzzleString)
							+ "^"
							+ d(puzzleString)
							+ " puzzle, only implemented for lengths n <= 3.  What do you think I am, a supergenius?");

		boolean IThinkItsSolvable = isSolvable(puzzleString, whichToPosition,
				whichToOrient, null, debugLevel);
		if (!IThinkItsSolvable) {
			if (progressWriter != null) {
				progressWriter
						.println("UH OH... I don't think it's solvable, but trying anyway...");
				progressWriter.flush();
			}
		}

		// If puzzle is sane and solvable,
		// and of a size we can solve,
		// then from here on out it should be IMPOSSIBLE
		// to get an Exception or Error.
		// If we do, appeal for a bug report.
		int solution[][] = null;
		Throwable throwable = null;

		try {
			Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString,
					debugLevel);

			int n = ((Integer) n_d_puz[0]).intValue();
			int d = ((Integer) n_d_puz[1]).intValue();
			Object puz = n_d_puz[2];
			solution = SolveStuff.solve(n, d, puz, whichToPosition,
					whichToOrient, progressWriter, debugLevel);
			if (n % 2 == 0)
				solution = PuzzleManipulation.makeSoDoesntMoveFirstCubie(n, d,
						solution, debugLevel);
			if (IThinkItsSolvable)
				return PuzzleIO.movesToString(solution, n, d, puz);
		} catch (Throwable t) {
			if (t instanceof OutOfMemoryError) {
				System.err.print(" OH NOOOOOOOOOOOO!!!!! ");
				throw (OutOfMemoryError) t;
			}
			throwable = t;
		}
		boolean solvedIt = (throwable == null);

		if (!IThinkItsSolvable && !solvedIt) {
			// The Exception or Error was expected.
			if (progressWriter != null) {
				progressWriter.println("    nope, couldn't do it. Told ya so.");
				progressWriter.flush();
			}
			throw new Error("solve called on unsolvable puzzle");
		}

		// Either we expected to solve it but didn't,
		// or we didn't expect to solve it but did.
		// Either way, it's a bug and we'd like a bug report.
		Assert(IThinkItsSolvable != solvedIt);

		String coreDump = puzzleString;
		coreDump = puzzleString.replaceAll("\\s", ""); // remove spaces
		// coreDump = coreDump.replaceAll("..........","$0\n"); // newline every
		// 10 chars
		coreDump = coreDump.replaceAll(
				"......................................................",
				"$0\n"); // newline every 54 chars-- nice size that divides
							// evenly into 3^d puzzle sizes

		if (!IThinkItsSolvable) {
			//
			// I thought it was unsolvable but solved it.
			//
			System.err.println();
			System.err.println();
			System.err
					.println("    HEY: I thought that puzzle was unsolvable but then I solved it (I think).");
			System.err
					.println("    There's a bug in my program.  I hate that.");
		} else {
			//
			// I thought it was solvable but failed to solve it
			// (something was thrown).
			//
			System.err.print(" OH NOOOOOOOOOOOO!!!!! ");
			throwable.printStackTrace();
			System.err.println();
			System.err.println();
			System.err
					.println("    HEY: you made my program core dump. I hate that!");
		}
		System.err
				.println("    If you would be so kind, please report this bug");
		System.err.println("    to Don Hatch <hatch@plunk.org>");
		System.err
				.println("    and please include the following string in your message:");
		System.err.println();
		/*
		 * AAAAAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBCCCDdDdDdcccCCCDdD
		 * dDdcccCCCDdDdDdcccbbbbbbbbbBBBBBBBBBCCCDdDdDdcccCCCDdD
		 * dDdcccCCCDdDdDdcccbbbbbbbbbBBBBBBBBBCCCDdDdDdcccCCCDdD
		 * dDdcccCCCDdDdDdcccbbbbbbbbbaaaaaaaaaaaaaaaaaaaaaaaaaaa
		 */
		System.err.println(coreDump);
		System.err.println("    Thank you so much!!");
		System.err.println();
		// XXX include the above message as part of the Error?
		throw new Error("Programmer is not very bright :-(");
	} // solve with which, progressWriter, and debugLevel params

	/**
	 * Applies the given move sequence to the puzzle, returning a new puzzle
	 * string with the same formatting as the input puzzleString.
	 * <p>
	 * movesString should be a spaces-separarated list of moves in the same form
	 * as that returned by solve()-- that is, each move consists of 3 letters,
	 * representing the color of the face being twisted, the from axis, and the
	 * to axis, respectively.
	 * <p>
	 * Each move may optionally be followed by a colon and a slicesMask,
	 * specifying which slices of the puzzle this twist should be applied to.
	 * For example, "abc:1" means the first slice (same as "abc"), "abc:2" means
	 * the second slice, "abc:5" means the first and third slice, "abc:-1" means
	 * all the slices regardless of puzzle size (i.e. a rotation of the entire
	 * puzzle at once).
	 * <p>
	 * Throws an Error if movesString does not represent a valid sequence of
	 * moves or if puzzleString is insane (see {@link #isSane isSane}). It
	 * doesn't matter whether puzzleString is solvable or not.
	 */
	public static String apply(String movesString, String puzzleString) {
		Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString, 0);
		int n = ((Integer) n_d_puz[0]).intValue();
		int d = ((Integer) n_d_puz[1]).intValue();
		Object puz = n_d_puz[2];

		String moveStrings[] = movesString.split("\\s+");
		if (moveStrings.length == 1 && moveStrings[0].equals(""))
			moveStrings = new String[0]; // lame that I have to do this, but
											// "".split("\\s") returns {""}
											// instead of {}

		int moves[][] = PuzzleIO.movesFromStrings(moveStrings, n, d, puz);
		puz = PuzzleManipulation.twist90s(n, d, puz, moves);

		return reformatPuzzleString(PuzzleIO.puzzleToString(n, d, puz),
				puzzleString); // reformat to original string
	} // apply

	/**
	 * Returns a random number from minScrambleChen to maxScrambleChen of random
	 * 90-degree twists that can be applied to the given puzzle (see
	 * {@link #apply apply}).
	 * <p>
	 * To scramble a puzzle:
	 * 
	 * <pre>
	 * String scrambledPuzzleString = apply(scramble(puzzleString, minScrambleChen,
	 * 		maxScrambleChen, random, debugLevel), puzzleString);
	 * </pre>
	 * 
	 * If the puzzle length n is even, the twists will not move the first-listed
	 * cubie.
	 * <p>
	 * Throws an Error if puzzleString is insane (see {@link #isSane isSane}) or
	 * if not 0 &le; minScrambleChen &le; maxScrambleChen or if d &lt; 3.
	 * <p>
	 * NOTE: currently can return a sequence in which consecutive moves cancel
	 * each other. If you want to get more sophisticated, write your own
	 * scramble function :-)
	 */
	public static String scramble(String puzzleString, int minScrambleChen,
			int maxScrambleChen, java.util.Random random, int debugLevel) {
		if (!(0 <= minScrambleChen && minScrambleChen <= maxScrambleChen))
			throw new Error("scramble called with minScrambleChen="
					+ minScrambleChen + ", maxScrambleChen=" + maxScrambleChen);

		Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString, 0);
		int n = ((Integer) n_d_puz[0]).intValue();
		int d = ((Integer) n_d_puz[1]).intValue();
		Object puz = n_d_puz[2];
		if (d < 3)
			throw new Error("scramble called with d=" + d + " < 3");
		int moves[][] = PuzzleManipulation.scramble(n, d, minScrambleChen,
				maxScrambleChen, random, debugLevel);
		if (n % 2 == 0)
			moves = PuzzleManipulation.makeSoDoesntMoveFirstCubie(n, d, moves,
					debugLevel);
		return PuzzleIO.movesToString(moves, n, d, puz);
	} // scramble

	/**
	 * Reads a puzzle string.
	 * <p>
	 * Implemented by reading and accumulating input lines from reader until the
	 * string is a valid puzzle string, and reading no further.
	 * <p>
	 * Throws an Error if end-of-file is reached without having seen a valid
	 * puzzle.
	 * 
	 * <pre>
	 * XXX Document debugLevel if I leave it in, otherwise provide
	 * XXX     a way to access the functionality.
	 * XXX Should we throw an IOException (allowing that to be passed up?)
	 * XXX Should we throw some sort of exception on EOF instead of an error?
	 * XXX go over PuzzleIO.puzzleFromReader and make sure its behavior is suitable for public use
	 * </pre>
	 */
	public static String readPuzzle(java.io.BufferedReader reader,
			int debugLevel) {
		Object n_d_puz[/* 3 */] = PuzzleIO
				.puzzleFromReader(reader, debugLevel);
		int n = ((Integer) n_d_puz[0]).intValue();
		int d = ((Integer) n_d_puz[1]).intValue();
		Object puz = n_d_puz[2];
		return PuzzleIO.puzzleToString(n, d, puz);
	} // readPuzzle

	/**
	 * Returns the length n of the n<sup>d</sup> puzzle. Throws an Error if
	 * puzzleString is insane (see {@link #isSane isSane}).
	 */
	public static int n(String puzzleString) {
		Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString, 0);
		return ((Integer) n_d_puz[0]).intValue();
	} // n()

	/**
	 * Returns the number of dimensions d of the n<sup>d</sup> puzzle. Throws an
	 * Error if puzzleString is insane (see {@link #isSane isSane}).
	 */
	public static int d(String puzzleString) {
		Object n_d_puz[/* 3 */] = PuzzleIO.puzzleFromString(puzzleString, 0);
		return ((Integer) n_d_puz[1]).intValue();
	} // d()

	/**
	 * Full featured test/demo program.
	 * <p>
	 * Has many command line options and interactive shell commands. For
	 * example, to scramble a 3<sup>5</sup> puzzle with 9 or 10 twists and then
	 * solve it:
	 * 
	 * <pre>
	 *    java NdSolve 3 5 -scramble 9+ -solve
	 * </pre>
	 * 
	 * To load a puzzle from the beginning of the file MYPUZZLE.mcnd, then apply
	 * two twists (the second of which is an exotic middle-slice twist), and
	 * then solve:
	 * 
	 * <pre>
	 *    java NdSolve -load MYPUZZLE.mcnd -exec ABc -exec cBa:2 -solve
	 * </pre>
	 * 
	 * There are many more options. To see a usage message, run the program with
	 * no arguments. Type "help" at the prompt to see the list of available
	 * interactive commands.
	 */
	public static void main(String[] args) {
		_MagicCubeNdSolve_TestPuzzlePlayer.main(args);
	}

	/**
	 * Simple test/demo program.
	 */
	public static void simple_main(String[] args) {
		_MagicCubeNdSolve_TestPuzzlePlayer.simple_main(args);
	}

	/**
	 * Trivial test/demo program.
	 */
	public static void trivial_main(String[] args) {
		_MagicCubeNdSolve_TestPuzzlePlayer.simple_main(args);
	}

} // public class NdSolve

//
// Multidimensional array utility functions.
// Used by both other classes in this file
// (NdSolve and TestPuzzlePlayer).
// XXX needs to be moved out of the common space, it's polluting by creating
// Arrays.class
// XXX whose name is too generic
//
class Arrays {
	// uncomment this if not using the C preprocessor...
	static private void Assert(boolean condition) {
		if (!condition)
			throw new Error("Assertion failed");
	}

	private Arrays() {
	} // non-instantiatable, only has static member functions

	//
	// Multidimensional array indexing.
	// The index is listed from major to minor.
	//
	public static Object get(Object array, int index[]) {
		for (int i = 0; i < index.length; ++i)
			array = java.lang.reflect.Array.get(array, index[i]);
		return array;
	}

	public static void set(Object array, int index[], Object val) {
		Assert(index.length > 0); // XXX throw illegalargumentexception or
									// something!
		for (int i = 0; i < index.length - 1; ++i)
			array = java.lang.reflect.Array.get(array, index[i]);
		java.lang.reflect.Array.set(array, index[index.length - 1], val);
	}

	// faster less convenient in-place version, for inner loop of twist90
	// which is the bottleneck (hmm, didn't make much difference)
	public static void unFlatIndex(int index[], int i, int n, int d) {
		for (int j = 0; j < d; ++j) {
			index[d - 1 - j] = i % n;
			i /= n;
		}
	}

	// more convenient slower version, for everywhere else
	public static int[] unFlatIndex(int i, int n, int d) {
		int index[] = new int[d];
		for (int j = 0; j < d; ++j) {
			index[d - 1 - j] = i % n;
			i /= n;
		}
		return index;
	}

	public static int flatIndex(int index[], int n, int d) {
		int i = 0;
		for (int j = 0; j < d; ++j)
			i = i * n + index[j];
		return i;
	}

	// Make a d-dimensional array of size n in each dimension,
	// all of whose entries are x.
	public static Object repeat(Object x, int n, int d) {
		if (d == 0)
			return x;
		else {
			Object result[] = new Object[n];
			for (int i = 0; i < n; ++i)
				result[i] = repeat(x, n, d - 1);
			return result;
		}
	}

	public static Object[] repeat(Object x, int n) {
		return (Object[]) repeat(x, n, 1);
	}

	public static int intpow(int n, int d) {
		// XXX think about whether to do something like the following... nicer
		// for big numbers, but we don't really use big numbers in this program
		// return d == 0 ? intpow(n, d>>1) squared times (d&1)==1 ? n : 1;

		int result = 1;
		for (int i = 0; i < d; ++i)
			result *= n;
		return result;
	}

	// determinant
	public static int intdet(int M[][]) {
		// These are machine-generated
		// (from the C preprocessor output from vec.h)
		// so they are probably right.
		if (M.length == 3)
			return M[0][0] * (M[1][1] * M[2][2] - M[1][2] * M[2][1]) - M[0][1]
					* (M[1][0] * M[2][2] - M[1][2] * M[2][0]) + M[0][2]
					* (M[1][0] * M[2][1] - M[1][1] * M[2][0]);
		else if (M.length == 4)
			return M[0][0]
					* (M[1][1] * (M[2][2] * M[3][3] - M[2][3] * M[3][2])
							- M[1][2] * (M[2][1] * M[3][3] - M[2][3] * M[3][1]) + M[1][3]
							* (M[2][1] * M[3][2] - M[2][2] * M[3][1]))
					- M[0][1]
					* (M[1][0] * (M[2][2] * M[3][3] - M[2][3] * M[3][2])
							- M[1][2] * (M[2][0] * M[3][3] - M[2][3] * M[3][0]) + M[1][3]
							* (M[2][0] * M[3][2] - M[2][2] * M[3][0]))
					+ M[0][2]
					* (M[1][0] * (M[2][1] * M[3][3] - M[2][3] * M[3][1])
							- M[1][1] * (M[2][0] * M[3][3] - M[2][3] * M[3][0]) + M[1][3]
							* (M[2][0] * M[3][1] - M[2][1] * M[3][0]))
					- M[0][3]
					* (M[1][0] * (M[2][1] * M[3][2] - M[2][2] * M[3][1])
							- M[1][1] * (M[2][0] * M[3][2] - M[2][2] * M[3][0]) + M[1][2]
							* (M[2][0] * M[3][1] - M[2][1] * M[3][0]));
		else {
			Assert(false); // unimplemented
			return 0;
		}
	} // intdet

	// Make a uniform d-dimensional array of length n in each dimension,
	// each entry is the index.
	// For example, makeIndexArray(3,2) returns:
	// {{{0,0},{0,1},{0,2}},
	// {{1,0},{1,1},{1,2}},
	// {{2,0},{2,1},{2,2}}}
	public static Object makeIndexArray(int n, int d) {
		Object result = repeat(null, n, d);
		int nIndices = Arrays.intpow(n, d);
		for (int iIndex = 0; iIndex < nIndices; ++iIndex) {
			int index[] = unFlatIndex(iIndex, n, d);
			set(result, index, index);
		}
		return result;
	} // makeIndexArray

	public static String toString(Object array) {
		// uncomment this to dump where every rogue print is coming from
		// try {throw new Error();} catch (Error e) {e.printStackTrace();}

		StringBuffer sb = new StringBuffer();
		_toString(array, sb);
		return sb.toString();
	}

	private static void _toString(Object array, StringBuffer sb) {
		if (array == null)
			sb.append("null");
		else if (array.getClass() == String.class)
			sb.append('"' + array.toString() + '"'); // XXX should escapify
		else if (array.getClass() == Character.class)
			sb.append('\'' + array.toString() + '\''); // XXX should escapify
		else if (array.getClass().isArray()) {
			sb.append("{");
			int n = java.lang.reflect.Array.getLength(array);
			for (int i = 0; i < n; ++i) {
				_toString(java.lang.reflect.Array.get(array, i), sb);
				if (i + 1 < n)
					sb.append(",");
			}
			sb.append("}");
		} else if (array instanceof java.util.ArrayList<?>) {
			java.util.ArrayList<?> arrayList = (java.util.ArrayList<?>) array;
			sb.append("[ ");
			int n = arrayList.size();
			for (int i = 0; i < n; ++i) {
				_toString(arrayList.get(i), sb);
				if (i + 1 < n)
					sb.append(",");
			}
			sb.append(" ]");
		} else if (array instanceof java.util.Map<?,?>) {
			java.util.Map<?,?> map = (java.util.Map<?,?>) array;
			sb.append("{ ");
			for (java.util.Iterator<?> it = map.keySet().iterator();
			it.hasNext();) {
				Object key = it.next();
				Object value = map.get(key);
				_toString(key, sb);
				sb.append("->");
				_toString(value, sb);
				if (it.hasNext())
					sb.append(", ");
			}
			sb.append(" }");
		} else
			sb.append(array.toString());
	} // private _toString

	// so that callers can blindly call toString(whatever)...
	public static String toString(byte x) {
		return "" + x;
	}

	public static String toString(double x) {
		return "" + x;
	}

	public static String toString(float x) {
		return "" + x;
	}

	public static String toString(int x) {
		return "" + x;
	}

	public static String toString(long x) {
		return "" + x;
	}

	public static String toString(short x) {
		return "" + x;
	}

	public static String toString(boolean x) {
		return "" + x;
	}

	public static String toString(char x) {
		return "" + x;
	}

	// XXX weird that this isn't a method on arrayList? hmm, could make a
	// subclass
	public static void addAll(java.util.ArrayList<int[]> arrayList, Object array) {
		int n = java.lang.reflect.Array.getLength(array);
		for (int i = 0; i < n; ++i)
			arrayList.add((int[])java.lang.reflect.Array.get(array, i));
	}

	// XXX weird that this isn't one of the arrayList constructors? hmm, could
	// make a subclass
	public static java.util.ArrayList<int[]> toArrayList(Object array) {
		java.util.ArrayList<int[]> arrayList = new java.util.ArrayList<int[]>();
		addAll(arrayList, array);
		return arrayList;
	}

	//
	// Instances of convenience functions... I'm just adding them as I need
	// them.
	// XXX I'm being wishy-washy about what goes in Utils and what goes here...
	// XXX really shouldn't this class just be
	// XXX for hairy generic recursive multidimensional stuff?
	//

	// faster less convenient in-place version, for inner loop of twist90
	public static void copy(int toArray[], int fromArray[]) {
		for (int i = 0; i < toArray.length; ++i)
			toArray[i] = fromArray[i];
	}

	// more convenient slower version, for everywhere else
	public static int[] copy(int array[]) {
		int result[] = new int[array.length];
		for (int i = 0; i < result.length; ++i)
			result[i] = array[i];
		return result;
	}

	// copies one level
	public static int[][] shallowCopy(int array[][]) {
		int result[][] = new int[array.length][];
		for (int i = 0; i < result.length; ++i)
			result[i] = array[i]; // shallow
		return result;
	}

	// copies both levels
	public static int[][] deepCopy(int array[][]) {
		int result[][] = new int[array.length][];
		for (int i = 0; i < result.length; ++i)
			result[i] = copy(array[i]); // deep
		return result;
	}

	public static int[] repeat(int x, int n) {
		int result[] = new int[n];
		for (int i = 0; i < result.length; ++i)
			result[i] = x;
		return result;
	}

	public static char[] repeat(char x, int n) {
		char result[] = new char[n];
		for (int i = 0; i < result.length; ++i)
			result[i] = x;
		return result;
	}

	public static int[] identityperm(int n) {
		int result[] = new int[n];
		for (int i = 0; i < n; ++i)
			result[i] = i;
		return result;
	}

	public static int nIndicesDifferent(int a[], int b[]) {
		int nDifferent = 0;
		for (int i = 0; i < a.length; ++i)
			if (a[i] != b[i])
				nDifferent++;
		return nDifferent;
	}

	// concatenate a bunch of arrays.
	public static Object concat(Object arrays[]) {
		if (arrays.length == 0)
			return new Object[0]; // avoids looking at arrays[0] which we do
									// below
		int totalLength = 0;
		for (int i = 0; i < arrays.length; ++i)
			totalLength += java.lang.reflect.Array.getLength(arrays[i]);
		Object result = java.lang.reflect.Array.newInstance(arrays[0]
				.getClass().getComponentType(), totalLength);
		int offset = 0;
		for (int i = 0; i < arrays.length; ++i) {
			int thisLength = java.lang.reflect.Array.getLength(arrays[i]);
			System.arraycopy(arrays[i], 0, result, offset, thisLength);
			offset += thisLength;
		}
		return result;
	} // concat arrays

	public static Object concat(Object array0, Object array1) {
		return concat(new Object[] { array0, array1 });
	} // concat

	public static Object concat3(Object array0, Object array1, Object array2) {
		return concat(new Object[] { array0, array1, array2 });
	} // concat3

	public static Object concat4(Object array0, Object array1, Object array2,
			Object array3) {
		return concat(new Object[] { array0, array1, array2, array3 });
	} // concat4

	public static Object insert(Object array, int index, Object itemToInsert) {
		int n = java.lang.reflect.Array.getLength(array);
		Object result = java.lang.reflect.Array.newInstance(array.getClass()
				.getComponentType(), n + 1);
		System.arraycopy(array, 0, result, 0, index);
		java.lang.reflect.Array.set(result, index, itemToInsert);
		System.arraycopy(array, index, result, index + 1, n - index);
		return result;
	} // insert

	public static Object deleteRange(Object array, int index, int nToDelete) {
		int n = java.lang.reflect.Array.getLength(array);
		Object result = java.lang.reflect.Array.newInstance(array.getClass()
				.getComponentType(), n - nToDelete);
		System.arraycopy(array, 0, result, 0, index);
		System.arraycopy(array, index + nToDelete, result, index, n - index
				- nToDelete);
		return result;
	} // deleteRange

	public static Object delete(Object array, int index) {
		return deleteRange(array, index, 1);
	} // delete

	public static Object append(Object array, Object itemToAppend) {
		return insert(array, java.lang.reflect.Array.getLength(array),
				itemToAppend);
	} // append

	public static Object subArray(Object array, int start, int n) {
		Object result = java.lang.reflect.Array.newInstance(array.getClass()
				.getComponentType(), n);
		System.arraycopy(array, start, result, 0, n);
		return result;
	} // subArray

	public static Object subArray(Object array, int start) {
		return subArray(array, start, java.lang.reflect.Array.getLength(array)
				- start);
	} // subArray to end

	public static int find(char array[], char item) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == item)
				return i;
		return -1;
	} // find

	//
	// Specializations so the caller doesn't have to cast the result
	// all the time...
	//
	public static int[] insert(int array[], int index, int itemToInsert) {
		return (int[]) insert(array, index, (Object) new Integer(itemToInsert));
	}

	public static int[][] insert(int array[][], int index, int itemToInsert[]) {
		return (int[][]) insert(array, index, (Object) itemToInsert);
	}

	public static int[][][] insert(int array[][][], int index,
			int itemToInsert[][]) {
		return (int[][][]) insert(array, index, (Object) itemToInsert);
	}

	public static String[] insert(String array[], int index, String itemToInsert) {
		return (String[]) insert(array, index, (Object) itemToInsert);
	}

	public static int[] append(int array[], int itemToAppend) {
		return (int[]) append(array, (Object) new Integer(itemToAppend));
	}

	public static int[][] append(int array[][], int itemToAppend[]) {
		return (int[][]) append(array, (Object) itemToAppend);
	}

	public static int[][][] append(int array[][][], int itemToAppend[][]) {
		return (int[][][]) append(array, (Object) itemToAppend);
	}

	public static String[] append(String array[], String itemToAppend) {
		return (String[]) append(array, (Object) itemToAppend);
	}

	public static String[] concat(String a[], String b[]) {
		return (String[]) concat((Object) a, (Object) b);
	}

	public static int[] concat(int a[], int b[]) {
		return (int[]) concat((Object) a, (Object) b);
	}

	public static int[][] concat(int a[][], int b[][]) {
		return (int[][]) concat((Object) a, (Object) b);
	}

	public static int[][] concat3(int a[][], int b[][], int c[][]) {
		return (int[][]) concat3((Object) a, (Object) b, (Object) c);
	}

	public static String[] subArray(String array[], int start, int n) {
		return (String[]) subArray((Object) array, start, n);
	}

	public static int[] subArray(int array[], int start, int n) {
		return (int[]) subArray((Object) array, start, n);
	}

	public static int[][] subArray(int array[][], int start, int n) {
		return (int[][]) subArray((Object) array, start, n);
	}

	public static String[] subArray(String array[], int start) {
		return (String[]) subArray((Object) array, start);
	}

	public static int[] subArray(int array[], int start) {
		return (int[]) subArray((Object) array, start);
	}

	public static int[][] subArray(int array[][], int start) {
		return (int[][]) subArray((Object) array, start);
	}

	public static char[] delete(char[] array, int index) {
		return (char[]) delete((Object) array, index);
	}

	public static int[] delete(int[] array, int index) {
		return (int[]) delete((Object) array, index);
	}

	public static int[][] delete(int[][] array, int index) {
		return (int[][]) delete((Object) array, index);
	}

	public static int[][][] delete(int[][][] array, int index) {
		return (int[][][]) delete((Object) array, index);
	}

	public static int[] deleteRange(int[] array, int index, int nToDelete) {
		return (int[]) deleteRange((Object) array, index, nToDelete);
	}

	public static int[][] deleteRange(int[][] array, int index, int nToDelete) {
		return (int[][]) deleteRange((Object) array, index, nToDelete);
	}

	public static int[] clamp(int array[], int a, int b) {
		int result[] = new int[array.length];
		for (int i = 0; i < array.length; ++i)
			result[i] = (array[i] <= a ? a : array[i] >= b ? b : array[i]);
		return result;
	}

	public static int[] plus(int a[], int b[]) {
		Assert(a.length == b.length); // XXX throw illegalargumentexception or
										// something!
		int result[] = new int[a.length];
		for (int i = 0; i < result.length; ++i)
			result[i] = a[i] + b[i];
		return result;
	}

	public static int[] minus(int a[], int b[]) {
		Assert(a.length == b.length); // XXX throw illegalargumentexception or
										// something!
		int result[] = new int[a.length];
		for (int i = 0; i < result.length; ++i)
			result[i] = a[i] - b[i];
		return result;
	}

	public static int[] minus(int x[]) {
		int result[] = new int[x.length];
		for (int i = 0; i < result.length; ++i)
			result[i] = -x[i];
		return result;
	}

	public static int[] average(int a[], int b[]) {
		Assert(a.length == b.length); // XXX throw illegalargumentexception or
										// something!
		int result[] = new int[a.length];
		for (int i = 0; i < result.length; ++i) {
			Assert((a[i] + b[i]) % 2 == 0); // don't ever call this otherwise //
											// XXX throw
											// illegalargumentexception or
											// something!
			result[i] = (a[i] + b[i]) / 2;
		}
		return result;
	}

	public static int normSqrd(int array[]) {
		int result = 0;
		for (int i = 0; i < array.length; ++i)
			result += array[i] * array[i];
		return result;
	}

	public static boolean isAll(int array[], int x) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] != x)
				return false;
		return true;
	}

	// a,b assumed to be same length
	public static boolean equals(int a[], int b[]) {
		for (int i = 0; i < a.length; ++i)
			if (a[i] != b[i])
				return false;
		return true;
	}

	// a,b,c assumed to be same length
	public static boolean equals(int a[], int b[], int c[]) {
		for (int i = 0; i < a.length; ++i)
			if (a[i] != b[i] || b[i] != c[i])
				return false;
		return true;
	}

	// tell whether the given column of A is equal to sign times the
	// the given column of B.
	public static boolean columnEquals(int A[][], int i, int B[][], int j,
			int sign) {
		for (int iRow = 0; iRow < A.length; ++iRow)
			if (A[iRow][i] != sign * B[iRow][j])
				return false;
		return true;
	}

} // private static class Arrays

//
// Test/demo player object, with a method for each available command,
// and a play() method that lets the user enter commands.
// This class is really private to NdSolve.main(),
// but it is separated out to be sure that it only uses
// NdSolve's public interface.
// Unfortunately I think the only way to do this and keep it in the same file
// is to make the class have package protection, which means
// it can be seen by other classes in this package, which sucks.
// Anyway, that's why the name starts with _MagicCubeNdSolve_;
// it's not intended to be used by classes outside this file.
//
class _MagicCubeNdSolve_TestPuzzlePlayer {
	// uncomment this if not using the C preprocessor...
	static private void Assert(boolean condition) {
		if (!condition)
			throw new Error("Assertion failed");
	}

	// XXX shouldn't store n,d,puz-- should only store puzzleString!
	private int n, d;
	private String puzzleString; // current state of the puzzle
	private java.util.Random random = new java.util.Random();
	private int debugLevel = 0; // be slightly debugLevel about solve steps, by
								// default
	private java.io.PrintWriter progressWriter = new java.io.PrintWriter(
			new java.io.BufferedWriter(new java.io.OutputStreamWriter(
					System.out)));
	private int whichToPosition = ~0;
	private int whichToOrient = ~0;

	public _MagicCubeNdSolve_TestPuzzlePlayer(int n, int d) {
		if (debugLevel >= 2)
			System.out.println("    in TestPuzzlePlayer(n=" + n + ", d=" + d
					+ ")");
		new_(3, 3); // initialize to something sane in case new_(n,d) fails
		new_(n, d);
		seed();
		if (debugLevel >= 2)
			System.out.println("    out TestPuzzlePlayer(n=" + n + ", d=" + d
					+ ")");
	}

	public void new_(int n, int d) {
		if (debugLevel >= 2)
			System.out.println("    in new(n=" + n + ", d=" + d + ")");
		this.puzzleString = NdSolve.newPuzzle(n, d);
		this.n = n;
		this.d = d;
		if (debugLevel >= 2)
			System.out.println("    out new(n=" + n + ", d=" + d + ")");
	}

	public void load(String fileName) {
		if (debugLevel >= 2)
			System.out.println("    in load(" + fileName + ")");
		java.io.BufferedReader in = null;
		try {
			in = new java.io.BufferedReader(new java.io.FileReader(fileName));
		} catch (Exception e) {
			System.out.println("couldn't open file " + fileName
					+ " for reading: " + e);
		}
		if (in != null)
			load(in);

		if (debugLevel >= 2)
			System.out.println("    out load(" + fileName + ")");
	}

	public void load(java.io.BufferedReader reader) {
		if (debugLevel >= 2)
			System.out.println("    in load(BufferedReader)");
		this.puzzleString = NdSolve.readPuzzle(reader, debugLevel);
		this.n = NdSolve.n(this.puzzleString);
		this.d = NdSolve.d(this.puzzleString);
		System.out.println("    Puzzle is"
				+ (NdSolve.isSolvable(this.puzzleString, whichToPosition,
						whichToOrient, progressWriter, debugLevel) ? " "
						: " NOT ") + "solvable.");
		System.out.println("    Puzzle is"
				+ (NdSolve.isSolved(this.puzzleString) ? " " : " NOT ")
				+ "solved.");
		if (debugLevel >= 2)
			System.out.println("    out load(BufferedReader)");
	}

	public void positionMask(int mask) {
		if (debugLevel >= 2)
			System.out.println("    in positionMask(" + mask + ")");
		// XXX should probably take a comma-separated list of numbers instead
		System.out.println("Setting which to position mask = " + mask);
		this.whichToPosition = mask;
		if (debugLevel >= 2)
			System.out.println("    out positionMask(" + mask + ")");
	}

	public void state(String newPuzzleStateString) {
		if (debugLevel >= 2)
			System.out.println("    in state(" + newPuzzleStateString + ")");
		if (!NdSolve.isSane(newPuzzleStateString)) {
			System.out.println("    That puzzle state was INSANE!!!");
		} else {
			this.n = NdSolve.n(newPuzzleStateString);
			this.d = NdSolve.d(newPuzzleStateString);
			this.puzzleString = NdSolve.reformatPuzzleString(
					newPuzzleStateString, NdSolve.newPuzzle(n, d));
			System.out.println("    Puzzle is"
					+ (NdSolve.isSolvable(newPuzzleStateString,
							whichToPosition, whichToOrient, progressWriter,
							debugLevel) ? " " : " NOT ") + "solvable.");
			System.out.println("    Puzzle is"
					+ (NdSolve.isSolved(newPuzzleStateString) ? " " : " NOT ")
					+ "solved.");
		}
		if (debugLevel >= 2)
			System.out.println("    out state(" + newPuzzleStateString + ")");
	}

	public void orientMask(int mask) {
		if (debugLevel >= 2)
			System.out.println("    in orientMask(" + mask + ")");
		// XXX should probably take a comma-separated list of numbers instead
		System.out.println("Setting which to orient mask = " + mask);
		this.whichToOrient = mask;
		if (debugLevel >= 2)
			System.out.println("    out orientMask(" + mask + ")");
	}

	public void move(String movesString) {
		if (debugLevel >= 2)
			System.out.println("    in move(\"" + movesString + "\")");

		System.out.println("Applying move \"" + movesString + "\"");
		System.out.println();

		this.puzzleString = NdSolve.apply(movesString, this.puzzleString);
		if (debugLevel >= 2)
			System.out.println("    out move(\"" + movesString + "\")");
	}

	public void scramble(int minScrambleChen, int maxScrambleChen) {
		if (debugLevel >= 2)
			System.out.println("    in scramble");
		String moves = NdSolve.scramble(this.puzzleString, minScrambleChen,
				maxScrambleChen, this.random, debugLevel); // XXX don't expose
															// this
		if (debugLevel >= 1)
			System.out.println("        moves = " + moves);
		this.puzzleString = NdSolve.apply(moves, this.puzzleString);
		if (debugLevel >= 2)
			System.out.println("    out scramble");
	}

	public void solve() {
		if (debugLevel >= 2)
			System.out.println("    in solve");

		System.out.println("Checking solvability...");
		System.out.flush();
		if (!NdSolve.isSolvable(puzzleString, whichToPosition, whichToOrient,
				progressWriter, debugLevel)) {
			System.out.println("That puzzle is unsolvable!!");
			System.out.println("But trying anyway (expect an error)...");
		}

		System.out.println("Solving...");
		System.out.flush();
		// XXX should spew a warning if masks are not complete?
		String solution = NdSolve.solve(puzzleString, whichToPosition,
				whichToOrient, debugLevel < 0 ? null : progressWriter,
				debugLevel);
		String moves[] = solution.split("\\s");
		if (moves.length == 1 && moves[0].equals("")) // XXX weird that this
														// happens?
			moves = new String[0];
		System.out.print("Solution has " + moves.length + " move"
				+ (moves.length == 1 ? "" : "s") + ": \"" + solution + "\"");
		System.out.println();
		if (debugLevel >= 2)
			System.out.println("    out solve");
	}

	// XXX figure out where to put this
	// XXX actually it's not used any more
	@SuppressWarnings("unused")
	private static String _twistToString(int twist[]) {
		int faceAxis = twist[0];
		int faceSign = twist[1];
		int fromAxis = twist[2];
		int toAxis = twist[3];
		int slicesMask = twist[4];
		Assert(faceSign == -1 || faceSign == 1);
		return (faceSign == -1 ? "-" : "+") + faceAxis + "(" + fromAxis + "->"
				+ toAxis + ")"
				// +" "
				+ (slicesMask != 1 ? ":" + slicesMask : "");
	}

	public void cheat() {
		if (debugLevel >= 2)
			System.out.println("    in cheat");
		new_(n, d);
		if (debugLevel >= 2)
			System.out.println("    out cheat");
	}

	public void seed(long seedValue) {
		if (debugLevel >= 2)
			System.out.println("    in seed(" + seedValue + ")");
		random.setSeed(seedValue);
		System.out.println("Initializing random number generator with seed "
				+ seedValue);
		if (debugLevel >= 2)
			System.out.println("    out seed(" + seedValue + ")");
	}

	public void seed() {
		if (debugLevel >= 2)
			System.out.println("    in seed");
		seed(System.currentTimeMillis());
		if (debugLevel >= 2)
			System.out.println("    out seed");
	}

	public void debug(int debugLevel) {
		if (debugLevel >= 2)
			System.out.println("    in debug(" + debugLevel + ")");
		System.out.println("debugLevel " + this.debugLevel + " -> "
				+ debugLevel);
		this.debugLevel = debugLevel;
		if (debugLevel >= 2)
			System.out.println("    out debug(" + debugLevel + ")");
	}

	public void debug() {
		if (debugLevel >= 2)
			System.out.println("    in debug");
		System.out.println("debugLevel is " + this.debugLevel);
		if (debugLevel >= 2)
			System.out.println("    out debug");
	}

	// XXX not sure if this is a command or not-- if it is then it should be in
	// the help menu.
	public void print() {
		if (debugLevel >= 2)
			System.out.println("    in print");
		System.out.println(puzzleString);
		if (debugLevel >= 2)
			System.out.println("    out print");
	}

	public void help() {
		System.out.println();
		System.out.println("Available commands:");
		System.out.println("        help");
		System.out
				.println("        Abc               (or any other twist-- 3 letters: face fromAxis toAxis)");
		System.out
				.println("        Abc:<slicesMask>  (twist with a slicesMask number)");
		System.out
				.println("        [0-99]            (scrambles that many twists)");
		System.out
				.println("        [0-99]+           (scrambles that many twists and maybe one more)");
		System.out
				.println("        solve             (only implemented for 2^d and 3^d puzzles) (XXX actually only 3^d but 2^d is soon, I swear!)");
		System.out.println("        cheat");
		System.out.println("        save <fileName>   (- for stdout)");
		System.out.println("        load <fileName>   (- for stdin)");
		System.out
				.println("        state <puzzleStateString>     (all on one line)");
		System.out.println("        new <n> <d>");
		System.out
				.println("        seed <seedValue>  (initializes random number generator)");
		System.out
				.println("        seed              (initializes using current time)");
		System.out
				.println("        debugLevel <newDebugLevel>   (current value = "
						+ debugLevel + ")");
		System.out.println("        print");
		System.out.println("        quit");
		System.out.println();
	}

	// XXX maybe pass in out instead of isInteractive?
	public void play(java.io.BufferedReader in, boolean isInteractive) {
		if (debugLevel >= 2)
			System.out.println("    in play");
		String prompt = "Enter command (help for help): ";
		boolean done = false;
		while (!done) {
			if (isInteractive) {
				System.out.print(prompt);
				System.out.flush();
			}

			String line;
			try {
				line = in.readLine();
			} catch (java.io.IOException e) {
				e.printStackTrace();
				continue; // XXX not sure what this entails, never seen it
							// happen I don't think
			}

			if (debugLevel >= 2)
				System.out.println("    line = " + Arrays.toString(line));

			if (line == null) // EOF
				line = "quit";

			String commands[] = line.split(";");

			for (int iCommand = 0; iCommand < commands.length; ++iCommand) {
				String command = commands[iCommand];
				command = command.trim();
				try {
					if (command == null || command.matches("^(quit|q)$")) {
						if (isInteractive)
							System.out.println("Byeeee!");
						done = true;
						break;
					} else if (command.matches("^h(elp)?$"))
						help();
					else if (command.matches("^p(rint)?$"))
						print();
					else if (command.matches("^s(olve)?$")) {
						solve();
						print();
					}
					// cheat and new are the same, really
					else if (command.matches("^(c(heat)?|n(ew)?)$")) {
						new_(n, d);
						print();
					} else if (command.matches("^state\\s+.*$")) {
						String newPuzzleString = command.substring(5);
						state(newPuzzleString);
					} else if (command.matches("^positionMask\\s+-?\\d+$")) {
						String tokens[] = command.split("\\s+");
						int mask = Integer.parseInt(tokens[1]);
						positionMask(mask);
					} else if (command.matches("^orientMask\\s+-?\\d+$")) {
						String tokens[] = command.split("\\s+");
						int mask = Integer.parseInt(tokens[1]);
						orientMask(mask);
					} else if (command.equals("l")) {
						load(in);
						print();
					} else if (command.matches("^load\\s+\\S+$")) {
						String tokens[] = command.split("\\s+");
						String fileName = tokens[1];
						if (fileName.equals("-")) {
							System.out.println("Enter puzzle state:");
							load(in);
							print();
						} else {
							load(fileName);
							print();
						}
					} else if (command.matches("^save\\s+\\S+$")) {
						String tokens[] = command.split("\\s+");
						String fileName = tokens[1];
						if (fileName.equals("-"))
							print();
						else
							System.err
									.println("Sorry, only saving to stdout is supported because I am a coward. (but you can cut and paste)");
					} else if (command
							.matches("^(c(heat)?|n(ew)?)\\s+\\d+\\s+\\d+$")) {
						String tokens[] = command.split("\\s+");
						int newN = Integer.parseInt(tokens[1]);
						int newD = Integer.parseInt(tokens[2]);
						new_(newN, newD);
						print();
					} else if (command.length() != 3 // so we don't get confused
														// with a move XXX could
														// relax this when the
														// puzzle letters aren't
														// digits, would need to
														// check
							&& command.matches("^(scramble\\s+)?\\d+$")) {
						command = command.replaceAll("^scramble\\s+", "");
						int nScrambleChen = Integer.parseInt(command);
						System.out.println();
						System.out.println("Scrambling " + nScrambleChen
								+ "...");
						scramble(nScrambleChen, nScrambleChen);
						System.out.println();
						print();
					} else if (command.matches("^(scramble\\s+)?\\d+\\+$")) // relaxing
																			// the
																			// 3-char
																			// prohibition
																			// for
																			// this--
																			// it
																			// would
																			// be
																			// pretty
																			// weird
																			// to
																			// have
																			// numbers
																			// and
																			// plus
																			// sign
																			// as
																			// colors
					{
						command = command.replaceAll("^scramble\\s+", "");
						int nScrambleChen = Integer.parseInt(command.substring(
								0, command.length() - 1));
						System.out.println();
						System.out.println("Scrambling " + nScrambleChen
								+ " and maybe one more...");
						scramble(nScrambleChen, nScrambleChen + 1);
						System.out.println();
						print();
					} else if (command
							.matches("^\\S\\S\\S(:-?\\d+)?(\\s+\\S\\S\\S(:-?\\d+)?)*$")) {
						move(command);
						print();
					} else if (command.equals("seed"))
						seed();
					else if (command.matches("^seed\\s+-?\\d+$")) {
						String tokens[] = command.split("\\s+");
						long seedValue = Long.parseLong(tokens[1]);
						seed(seedValue);
					} else if (command.matches("^d(ebug)?$")) {
						debug();
					} else if (command.matches("^d(ebug)?\\s+-?\\d+$")) {
						String tokens[] = command.split("\\s+");
						int newDebugLevel = Integer.parseInt(tokens[1]);
						debug(newDebugLevel);
					} else if (command.equals(""))
						; // nothing
					else {
						System.out.println("Unknown command "
								+ Arrays.toString(command)
								+ " (type help for help)");
					}
				}
				// Get this straight... null pointers are Expections, my Asserts
				// are Errors... I think I want both?
				// catch (Exception e)
				// catch (Error e)
				catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		if (debugLevel >= 2)
			System.out.println("    out play");
	} // play

	/**
	 * Full featured test/demo program.
	 */
	public static void main(String[] args) {
		System.err.println("in NdSolve.main");

		if (args.length == 1 && args[0].equals("-trivial")) {
			trivial_main(new String[0]);
			return;
		}
		if (args.length == 1 && args[0].equals("-simple")) {
			simple_main(new String[0]);
			return;
		}

		// Strip off and save final command-line commands,
		// and turn them into player commands.
		// XXX pretty tedius
		String preCommands[] = {};
		while (true) {
			//
			// 1-arg commands
			//
			if (args.length >= 2 && args[args.length - 2].equals("-exec")) {
				preCommands = Arrays.insert(preCommands, 0,
						args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-seed")) {
				preCommands = Arrays.insert(preCommands, 0, "seed "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-load")) {
				preCommands = Arrays.insert(preCommands, 0, "load "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-state")) {
				preCommands = Arrays.insert(preCommands, 0, "state "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-scramble")) {
				preCommands = Arrays.insert(preCommands, 0, "scramble "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-debug")) {
				preCommands = Arrays.insert(preCommands, 0, "debug "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-positionMask")) {
				preCommands = Arrays.insert(preCommands, 0, "positionMask "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			} else if (args.length >= 2
					&& args[args.length - 2].equals("-orientMask")) {
				preCommands = Arrays.insert(preCommands, 0, "orientMask "
						+ args[args.length - 1]);
				args = Arrays.subArray(args, 0, args.length - 2);
			}
			//
			// no-arg commands
			//
			else if (args.length >= 1 && args[args.length - 1].equals("-solve")) {
				preCommands = Arrays.insert(preCommands, 0, "solve");
				args = Arrays.subArray(args, 0, args.length - 1);
			} else
				break;
		}

		if (!(args.length == 2 && args[0].matches("^\\d+$")
				&& args[1].matches("^\\d+$") || args.length == 0
				&& preCommands.length > 0)) {
			System.err.println("Usage:");
			System.err.println("    NdSolve -trivial");
			System.err.println("    NdSolve -simple");
			System.err.println("    NdSolve [<n> <d>] <options>");
			System.err.println("where <options> are one or more of:");
			System.err.println("        -seed <seed>");
			System.err.println("        -load <puzzleFileName>");
			System.err.println("        -state <puzzleString>");
			System.err.println("        -scramble <n or n+>");
			System.err.println("        -exec \"command0;command1;...\"");
			System.err.println("        -positionMask <mask>");
			System.err.println("        -orientMask <mask>");
			System.err.println("        -solve");

			System.exit(1);
		}
		int n = args.length > 0 ? Integer.parseInt(args[0]) : 3;
		int d = args.length > 1 ? Integer.parseInt(args[1]) : 3;

		System.out.println("n = " + n);
		System.out.println("d = " + d);

		if (n < 2)
			System.err.println("WARNING: n < 2, expect insanity");
		if (d < 2)
			System.err.println("WARNING: d < 1, expect insanity");

		if (Math.pow(n + 2, d) > 1e9) {
			System.err.println("Yeah, right.");
			System.exit(1);
		}

		_MagicCubeNdSolve_TestPuzzlePlayer player = new _MagicCubeNdSolve_TestPuzzlePlayer(
				n, d);

		player.help();

		player.print();

		//
		// Execute commands from each -exec arg...
		//
		for (int iPreCommand = 0; iPreCommand < preCommands.length; ++iPreCommand) {
			java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.StringReader(preCommands[iPreCommand]));
			player.play(in, false);
		}

		//
		// Execute commands from stdin.
		//
		{
			java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.InputStreamReader(System.in));

			player.play(in, true);
		}

		System.err.println("out NdSolve.main");
	} // main

	/**
	 * Simple test/demo program.
	 */
	public static void simple_main(String[] args) {
		// if (System.in.isatty()) // dang!
		System.out.println("Enter puzzle state:");

		java.io.BufferedReader in = new java.io.BufferedReader(
				new java.io.InputStreamReader(System.in));

		int readPuzzle_debugLevel = 1;
		String puzzleString = NdSolve.readPuzzle(in, readPuzzle_debugLevel);
		Assert(NdSolve.isSane(puzzleString)); // readPuzzle woulda thrown
												// otherwise
		int n = NdSolve.n(puzzleString);
		int d = NdSolve.d(puzzleString);
		System.out.println("n = " + n);
		System.out.println("d = " + d);

		java.io.PrintWriter progressWriter = new java.io.PrintWriter(
				new java.io.BufferedWriter(new java.io.OutputStreamWriter(
						System.out)));

		int isSolvable_debugLevel = 0; // not of interest to general public
		if (!NdSolve.isSolvable(puzzleString, ~0, ~0, progressWriter,
				isSolvable_debugLevel)) {
			System.out.println("That puzzle is unsolvable!!");
			System.out.println("But trying anyway (expect an error)...");
		}

		int solve_debugLevel = 0; // not of interest to general public
		String solution = NdSolve.solve(puzzleString, ~0, ~0, progressWriter,
				solve_debugLevel);
		System.out.println("Solution = \"" + solution + "\"");
	}

	/**
	 * Trivial test/demo program.
	 */
	public static void trivial_main(String[] args) {
		String solution = NdSolve.solve(
		// "        AAa   AAa   AAa        "
				// + "   bBB c   C c   C c   C Bbb   "
				// + "   BBB C   c C   c C   c bbb   "
				// + "   bBB C   c C   c C   c Bbb   "
				// + "        Aaa   Aaa   Aaa        "

				"                                                 "
						+ "                1 1           3 3                "
						+ "                1 1           3 3                "
						+ "                                                 "
						+ "                2 2           2 2                "
						+ "         0 0   3   4         6   1   7 7         "
						+ "         0 0   3   4         6   1   7 7         "
						+ "                5 5           5 5                "
						+ "                                                 "
						+ "                2 2           2 2                "
						+ "         4 1   0   7         0   7   3 6         "
						+ "         4 1   0   7         0   7   3 6         "
						+ "                5 5           5 5                "
						+ "                                                 "
						+ "                6 4           6 4                "
						+ "                6 4           6 4                "
						+ "                                                 ");
		System.out.println("Solution = \"" + solution + "\"");
	}

} // MagicCubeNdSolve_TestPuzzlePlayer
