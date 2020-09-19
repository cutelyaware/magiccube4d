package com.superliminal.magiccube4d;

/*
 RELNOTES:
 =========
 This version has the following enhancements:
 - Lots of new puzzle types available from the Puzzle menu.
 These are called "generic puzzles" and they are a work
 in progress.
 Generic puzzles have the following limitations currently:
 - can't twist (or undo or redo)
 while a twist or cheat is in progress
 (which means you can't double-click to do a move twice)
 - only 8 colors still, even when more than 8 faces
 - some of the even-length puzzles have spurious extra
 very thin stickers at the halfway planes
 - trying to make a puzzle based on triangles
 (e.g. simplex, triangular prism)
 will probably produce something ugly at this point, if anything
 - sometimes the highlighted sticker fails to get updated correctly
 at the end of a twist (jiggle the mouse to fix it)
 - no save/load (menus are probably misleading)
 - no real solve
 - scramble only affects outer or 2nd slices (you'll
 only notice this if your puzzle length is >= 6)
 - no macros (menus are probably misleading)
 - contiguous cubies not implemented (even if gui says otherwise)
 - exceptions everywhere if you try to do unimplemented stuff
 And the following enhancements:
 - you can rotate any *cubie* of the length-3 puzzle
 to the center with the middle mouse
 (not just hyperface centers to the center).

 ISSUES:
 =======
 - Contiguous cubies.  I would like to do the following:
 1. Get rid of the "Contiguous Cubies" checkbox;
 there will be no magical half-broken
 slider-following-other-slider state any more.
 2. Replace it with either:
 a) a selector:
 Stickers Shrink Towards: [Sticker Centers / Face Boundaries]
 or b) a "Stickers shrink towards face boundaries" checkbox
 Then Contiguous Cubies can be obtained
 by turning on "Stickers shrink towards face boundaries"
 and setting "Face Shrink" to 1, and sliding stickerShrink
 up and down.
 3. (Optional): There could be a Button (NOT a checkbox)
 called Contiguous Cubies that:
 turns on "Stickers shrink towards face boundaries" if
 not already on, and sets "Face Shrink" to 1.
 I think we can do without it though; it's easy enough
 to do by hand.
 4. Once the above is done, there will be no reason
 to let them go above 1 on either faceshrink or stickershrink
 any more, so both of those sliders's maxes can be set to 1.
 5. (Optional) Actually "Shrink towards face boundaries"
 doesn't need to be boolean, it can be a slider value
 between 0 and 1.
 - Possible rot-element-to-center behaviors,
 from least to most restrictive
 - don't do it
 - only do it if enabled via checkbox or esoteric modifier combo
 - only do it on stickers that are already on the center face,
 or if there is no center face; 
 if there is a center face, clicking anywhere on a diff face
 just centers that face, you have to click a non-center
 sticker on that face again once the face is in the center
 to focus it
 - do it everywhere-- on one hand this is nice and clean
 and powerful, but on the other hand sometimes it's
 hard to click on the face-center sticker which is
 what is most often wanted
 - It would be nice to have "face shrink 4d", "sticker shrink 4d",
 "face shrink 3d", "sticker shrink 3d".  The 4d and 4d versions
 do qualitatively different things (I was surprised when
 I first say the 120-cell, until I realized this--
 I was expecting 3d sticker shrink which preserves 3d shape,
 but instead the program does 4d sticker shrink which
 regularizes the 3d shape as it gets smaller).

 BUGS / URGENT TODOS:
 ===================

 - bleah, dodecahedron is not face first! (noticable in {}x{5,3})

 - 2x2x2x2 does corner twists, should do face (I think)
 - why is scale different before I touch the slider??
 - scale doesn't quite match original
 - it's not oriented correctly at the end-- so I had to make orientDeep
 public and have the caller call it-- lame! need to send in all planes at once so it can do that automatically with some hope of being efficient
 - need more colors!



 NOT HAVING TO DO WITH THIS GENERIC STUFF:
 =====================================================
 - fucking twisting after I let up on the mouse sucks! fix it!!!
 - fucking gui lies... not acceptable.
 - disallow spin dragging gives me the original orientation-- argh.
 - but sometimes that's what I want... bleah.
 need a way to save and restore my "favorite" orientation.
 - hey, I think clicking on a sticker shouldn't kill spin dragging--
 only clicking on boundary should stop it.
 that way can solve while it's spinning!  fun drinking game!
 ooh and make it speed up and slow down and tumble randomly
 while you are trying to solve it!
 - just noticed, Esc doesn't work to cancel immediately, have to click
 at least one thing first

 TODO:
 =====
 SPECIFICATION:
 - be able to specify initial orientation
 (using which elts to which axes)
 - be able to specify slice thicknesses,
 orthogonal to puzzle length spec,
 and allow different for different faces
 (we don't get slice thicknesses reasonable for anything
 with triangles in it yet)
 (and we want the 2.5 thing to work on only the pents,
 not the squares, of a {5}x{4} and the {5,3}x{})
 MISC:
 - can't twist while twist is in progress yet-- sucks for usability
 - the cool rotate-arbitrary-element-to-center thing
 should be undoable
 - faceExplode / stickerExplode sliders?
 I'm really confused on what the relationship is
 between those and viewScale, faceShrink, stickerShrink
 - history compression

 POLYTOPE STUFF:
 - getAllIncidences would be faster
 if I did it in two passes, counting first and then filling,
 instead of using a gzillion Vectors one for each element

 NON-IMMEDIATE:
 - 3 level cascading menus for {3..12}x{3..12}?
 - nframes proportional to angle actually kind of sucks...
 should be proportionally less frames when rot angle is big,
 otherwise very small rotations get large acceleration
 which looks lame.  maybe the way to think about it is
 that slider should control max acceleration? i.e.
 the acceleration at the start and finish, if we make it
 a cubic function (or even if we leave it sine, I think)
 - nframes should ALWAYS be odd!  even means sometimes
 we get flat faces!
 - ooh, make more slices proportionally slower, would feel more massive!
 - completely general solve?
 - general uniform polytopes! yeah!

 PIE IN THE SKY:
 - figure out how to do "contiguous cubies" generically-- is it possible in terms of the others?  probably not... unless I modify the shrink behavior so it always likes up the outer ones?  Hmm, I think this would be a different "shrink mode" that shrinks stickers towards the face boundaries?  YES!  DO IT!

 - hmm... wireframe around the non-shrunk sliced geometry would be nice!
 In fact, it would be nice to have separate buttons for:
 - wirefame around unshrunk faces
 - wireframe around shrunk faces (separate faceShrink for it?)
 - wireframe around unshrunk stickers (separate stickerShrink for it?)
 - wireframe around stickers (that's the current button)

 polygon shrink?
 several different wireframes at once with different styles?
 okay I think this is where I went insane last time I was
 implementing a polytope viewer
 - fade out to transparent instead of suddenly turning inside out?
 This would nicely light up the center,
 And would also help mask the sorting failures
 on faces that are very close to flat
 - ha, for the {5}x{4}, it could be fudged so the cubical facets
 behave like they have full symmetry-- it would allow stickers
 to slide off of the pentprism face and onto a cube face.
 In general this will make the symmetry of a twist
 be dependent on the symmetry of the face,
 which can be more than the symmetry of the whole puzzle.
 */


import java.util.HashMap;
import java.util.Map;

import com.donhatchsw.util.*; // XXX get rid
import com.superliminal.util.PropertyManager;

public class PolytopePuzzleDescription implements PuzzleDescription {
    private CSG.SPolytope originalPolytope;
    private CSG.SPolytope slicedPolytope;

    private String schlafliProduct;
    private int intLength;
    private double doubleLength;
    private String lengthString;

    private String topologicalFingerprintHumanReadable;
    private String topologicalFingerprintDigest;

    private float _circumRadius;
    private float _inRadius;
    private int _nCubies;

    private float vertsMinusStickerCenters[][];
    private float vertStickerCentersMinusFaceCenters[][];
    private float vertFaceCenters[][];
    private int stickerInds[/* nStickers */][/* nPolygonsThisSticker */][/* nVertsThisPolygon */];

    private int face2OppositeFace[/* nFaces */];
    private int sticker2face[/* nStickers */];
    private int sticker2faceShadow[/* nStickers */]; // so we can detect nefariousness
    private int sticker2cubie[/* nStickers */];

    private float gripCentersF[/* nGrips */][];
    private int gripDims[/* nGrips */];
    private int grip2face[/* nGrips */];
    private int gripSymmetryOrders[/* nGrips */];
    private double gripUsefulMats[/* nGrips */][/* nDims */][/* nDims */]; // weird name
    private double faceInwardNormals[/* nFaces */][/* nDims */];
    private double faceCutOffsets[/* nFaces */][/* nCutsThisFace */]; // slice 0 is bounded by -infinity and offset[0], slice i+1 is bounded by offset[i],offset[i+1], ... slice[nSlices-1] is bounded by offset[nSlices-2]..infinity

    private float nicePointsToRotateToCenter[][/* nDims */];
    private float faceCenters[/* nFaces */][/* nDims */];

    private double stickerCentersD[][];
    private FuzzyPointHashTable<Integer> stickerCentersHashTable;

    private static void Assert(boolean condition) {
        if(!condition)
            throw new Error("Assertion failed");
    }
    @SuppressWarnings("unused")
    private static void Assumpt(boolean condition) {
        if(!condition)
            throw new Error("Assumption failed");
    }

    // split into lines, indent each line, and re-join.
    private static String indented(String indent, String text) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; ++i) {
            sb.append(indent);
            sb.append(lines[i]);
            if (i != lines.length-1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * The following schlafli product symbols are supported;
     * note that they are all uniform, and every vertex has exactly
     * nDims incident facets and edges (things would go crazy otherwise).
     * 
     * 3-dimensional:
     * {3,3}
     * {4,3}
     * {5,3}
     * {3}x{} or {}x{3} (triangular prism)
     * {4}x{} or {}x{4} (cube, same as {4,3})
     * {5}x{} or {}x{5} (pentagonal prism)
     * {6}x{} or {}x{6} (hexagonal prism)
     * ...
     * 
     * 4-dimensional:
     * {3,3,3}
     * {4,3,3}
     * {5,3,3}
     * 
     * {3}x{3} {3}x{4} {3}x{5} {3}x{6} ...
     * {4}x{3} {4}x{4} {4}x{5} {4}x{6} ...
     * {5}x{3} {5}x{4} {5}x{5} {5}x{6} ...
     * {6}x{3} {6}x{4} {6}x{5} {6}x{6} ...
     * ...
     * 
     * {3,3}x{} or {}x{3,3} (tetrahedral prism)
     * {4,3}x{} or {}x{4,3} (hypercube, same as {4,3,3})
     * {5,3}x{} or {}x{5,3} (dodecahedral prism)
     * 
     * Note that {4} can also be expressed as {}x{} in any of the above.
     * 
     * XXX would also be cool to support other uniform polyhedra or polychora
     * with simplex vertex figures, e.g.
     * truncated regular
     * omnitruncated regular
     */

    public PolytopePuzzleDescription(String prescription, ProgressCallbacks progress)
    {
        // Caller may have passed us "Grand Antiprism" from a menu,
        // but the lower level function we're going go call doesn't allow spaces,
        // and understands Grand_Antiprism as a special case, so convert.
        prescription = prescription.replaceAll("Grand Antiprism",
                                               "Grand_Antiprism");
        java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(
            "\\s*([^ ]+)\\s+((\\d+)(\\((.*)\\))?)"
        ).matcher(prescription);
        if (!matcher.matches())
            throw new IllegalArgumentException("PolytopePuzzleDescription didn't understand prescription string "+com.donhatchsw.util.Arrays.toStringCompact(prescription)+"");
        String schlafliProductString = matcher.group(1);
        String lengthString = matcher.group(2);
        String intLengthString = matcher.group(3);
        String doubleLengthString = matcher.group(5);

        int intLength = Integer.parseInt(intLengthString);
        double doubleLength = (double)intLength; // but to be overridden by doubleLengthString if there is one
        if (doubleLengthString != null)
        {
            // Allow fractions
            int slashIndex = doubleLengthString.lastIndexOf('/');
            if (slashIndex != -1)
            {
                String numeratorString = doubleLengthString.substring(0, slashIndex);
                String denominatorString = doubleLengthString.substring(slashIndex+1);
                doubleLength = Double.valueOf(numeratorString).doubleValue()
                             / Double.valueOf(denominatorString).doubleValue();
            }
            else
                doubleLength = Double.valueOf(doubleLengthString).doubleValue(); //  XXX should catch parse error and throw illegal arg exception
        }

        this.schlafliProduct = schlafliProductString;
        this.lengthString = lengthString;
        this.intLength = intLength;
        this.doubleLength = doubleLength;

        if (intLength < 1)
            throw new IllegalArgumentException("PolytopePuzzleDescription called with intLength="+intLength+", min legal intLength is 1");
        if (doubleLength <= 0)
            throw new IllegalArgumentException("PolytopePuzzleDescription called with doubleLength="+doubleLength+", doubleLength must be positive");

        if(progress != null)
            if (!progress.subtaskInit("Constructing polytope"))
                return;

        originalPolytope = CSG.makeRegularStarPolytopeProductJoinFromString(schlafliProduct);
        CSG.orientDeepFunctional(originalPolytope); // XXX shouldn't be necessary!!!!
        CSG.orientDeepCosmetic(originalPolytope); // XXX shouldn't be necessary!!!!

        int nDims = originalPolytope.p.dim; // == originalPolytope.fullDim

        CSG.Polytope originalElements[][] = originalPolytope.p.getAllElements();
        CSG.Polytope originalVerts[] = originalElements[0];
        CSG.Polytope originalFaces[] = originalElements[nDims - 1];
        final int nFaces = originalFaces.length;
        int originalIncidences[][][][] = originalPolytope.p.getAllIncidences();

        // Mark each original face with its face index.
        // These marks will persist even aver we slice up into stickers,
        // so that will give us the sticker-to-original-face-index mapping.
        // Also mark each vertex with its vertex index... etc.
        {
            for(int iDim = 0; iDim < originalElements.length; ++iDim)
                for(int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
                    originalElements[iDim][iElt].setAux(Integer.valueOf(iElt));
        }

        //
        // Figure out the face inward normals and offsets;
        // these will be used for computing where cuts should go.
        //
        faceInwardNormals = new double[nFaces][nDims];
        double faceOffsets[] = new double[nFaces];
        for(int iFace = 0; iFace < nFaces; ++iFace)
        {
            CSG.Polytope face = originalFaces[iFace];
            CSG.Hyperplane plane = face.contributingHyperplanes[0];
            VecMath.vxs(faceInwardNormals[iFace], plane.normal, -1);
            faceOffsets[iFace] = -plane.offset;
            Assert(faceOffsets[iFace] < 0.);
            double invNormalLength = 1. / VecMath.norm(faceInwardNormals[iFace]);
            VecMath.vxs(faceInwardNormals[iFace], faceInwardNormals[iFace], invNormalLength);
            faceOffsets[iFace] *= invNormalLength;
        }

        //
        // Figure out the circumRadius (farthest vertex from origin)
        // and inRadius (closest face plane to origin)
        // of the original polytope...
        //
        {
            double farthestVertexDistSqrd = 0.;
            for(int iVert = 0; iVert < originalVerts.length; ++iVert)
            {
                double thisDistSqrd = VecMath.normsqrd(originalVerts[iVert].getCoords());
                if(thisDistSqrd > farthestVertexDistSqrd)
                    farthestVertexDistSqrd = thisDistSqrd;
            }
            _circumRadius = (float) Math.sqrt(farthestVertexDistSqrd);

            double nearestFaceDist = 0.;
            for(int iFace = 0; iFace < originalFaces.length; ++iFace)
            {
                double thisFaceDist = -faceOffsets[iFace];
                if(thisFaceDist < nearestFaceDist)
                    nearestFaceDist = thisFaceDist;
            }
            _inRadius = (float) nearestFaceDist;
        }


        //
        // So we can easily find the opposite face of a given face...
        //
        face2OppositeFace = new int[nFaces];
        {
            FuzzyPointHashTable<CSG.Polytope> table = new FuzzyPointHashTable<CSG.Polytope>(1e-9, 1e-8, 1. / 128);
            for(int iFace = 0; iFace < nFaces; ++iFace)
                table.put(faceInwardNormals[iFace], originalFaces[iFace]);
            double oppositeNormalScratch[] = new double[nDims];
            //System.err.print("opposites:");
            for(int iFace = 0; iFace < nFaces; ++iFace)
            {
                VecMath.vxs(oppositeNormalScratch, faceInwardNormals[iFace], -1.);
                CSG.Polytope opposite = table.get(oppositeNormalScratch);
                face2OppositeFace[iFace] = opposite == null ? -1 : ((Integer) opposite.getAux()).intValue();
                //System.err.print("("+iFace+":"+face2OppositeFace[iFace]+")");
            }
        }

        //
        // Figure out exactly what cuts are wanted
        // for each face.  Cuts parallel to two opposite faces
        // will appear in both faces' cut lists.
        //
        // Note, we store face inward normals rather than outward ones,
        // so that, as we iterate through the slicemask bit indices later,
        // the corresponding cut offsets will be in increasing order,
        // for sanity.
        //
        faceCutOffsets = new double[nFaces][];
        {
            for(int iFace = 0; iFace < nFaces; ++iFace)
            {
                CSG.Polytope face = originalFaces[iFace];

                double fullThickness = 0.;
                {
                    // In case of nonuniformity (e.g. pseudorhombicuboctahedron or frucht),
                    // we need to do check *all* vertices on this facet, not just one.
                    for (int iVert : originalIncidences[nDims-1][iFace][0]) {
                        // iVertEdges = indices of all edges incident on vert iVert
                        int iVertsEdges[] = originalIncidences[0][iVert][1];
                        // Find an edge incident on vertex iVert
                        // that is NOT incident on face iFace..
                        for(int i = 0; i < iVertsEdges.length; ++i)
                        {
                            int iEdge = iVertsEdges[i];
                            int iEdgesFaces[] = originalIncidences[1][iEdge][nDims - 1];
                            int j;
                            for(j = 0; j < iEdgesFaces.length; ++j)
                                if(iEdgesFaces[j] == iFace)
                                    break; // iEdge is incident on iFace-- no good
                            if(j == iEdgesFaces.length)
                            {
                                // iEdge is not incident on iFace-- good!
                                int jVert0 = originalIncidences[1][iEdge][0][0];
                                int jVert1 = originalIncidences[1][iEdge][0][1];
                                Assert((jVert0 == iVert) != (jVert1 == iVert));

                                double edgeVec[] = VecMath.vmv(
                                    originalVerts[jVert1].getCoords(),
                                    originalVerts[jVert0].getCoords());
                                double thisThickness = VecMath.dot(edgeVec, faceInwardNormals[iFace]);
                                if(thisThickness < 0.)
                                    thisThickness *= -1.;

                                // If there are more than one neighbor vertex
                                // that's not on this face, pick one that's
                                // closest to the face plane.  This can only
                                // happen if the vertex figure is NOT a simplex
                                // (e.g. it happens for the icosahedron).
                                if(thisThickness > 1e-6
                                    && (fullThickness == 0. || thisThickness < fullThickness))
                                    fullThickness = thisThickness;
                            }
                        }
                    }
                }
                Assert(fullThickness != 0.); // XXX actually this fails if puzzle dimension <= 1, maybe should disallow

                double sliceThickness = fullThickness / doubleLength;

                int nNearCuts = intLength / 2;  // (n-1)/2 if odd, n/2 if even
                int nFarCuts = face2OppositeFace[iFace]==-1 ? 0 : nNearCuts;

                // In some cases, notably an even length prism or antiprism of this face,
                // the middle cut is the same from either side: it goes through the origin.
                // In that case, we'd like to do it only as a near cut, so nFarCuts
                // should be nNearCuts-1 instead of nNearCuts.
                //
                // HOWEVER, there are other cases where the middle cut is the same from either side,
                // notably truncated tetrahedron with carefully chosen cut depth: "(1)3(1)3(0) 2(1)"
                // (and truncated simplex in 4d, too, probably), and there's no way
                // of predicting when this will happen.
                // In fact, we can cause similar problems even for odd numbers of slices on standard puzzles:
                //      "{4,3,3} 7(6)"
                //      "{4,3,3} 7(4)"
                // Given that, what we do is as follows: make the list of cuts (even though nFarCuts may
                // may be too many at this point), then sort and remove dups (and near dups) at the end.

                faceCutOffsets[iFace] = new double[nNearCuts + nFarCuts];
                for (int iNearCut = 0; iNearCut < nNearCuts; ++iNearCut)
                    faceCutOffsets[iFace][iNearCut] = faceOffsets[iFace] + (iNearCut+1)*sliceThickness;
                // We'll fill in the far cuts in another pass
            }  // for iFace

            // Fill in far cuts of each face,
            // from near cuts of the opposite face.
            // Note the opposite face may have a different
            // offset from the origin, and different slice thickness
            // (e.g. the truncated simplex in 3 or 4 dimensions).
            for(int iFace = 0; iFace < nFaces; ++iFace)
            {
                int iOppositeFace = face2OppositeFace[iFace];
                if (iOppositeFace != -1) {
                  int nNearCuts = intLength / 2;  // same reason in previous pass
                  int nFarCuts = faceCutOffsets[iFace].length - nNearCuts;
                  for (int iFarCut = 0; iFarCut < nFarCuts; ++iFarCut)
                      faceCutOffsets[iFace][nNearCuts+nFarCuts-1-iFarCut] = -faceCutOffsets[iOppositeFace][iFarCut];
                }
            }
            // Finally, make sure opposite cut sets are *exactly* opposite.  They might not be,
            // if de-duping made different choices from the two directions.
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                int iOppositeFace = face2OppositeFace[iFace];
                if (iOppositeFace > iFace)  // so we do this only once per pair, and only if there is an opposite
                {
                    int nCuts = faceCutOffsets[iFace].length;
                    Assert(nCuts == faceCutOffsets[iOppositeFace].length); // careful analysis of the deduping probably would show this can't fail, but check anyway
                    for (int iCut = 0; iCut < nCuts; ++iCut) {
                        Assert(Math.abs(faceCutOffsets[iOppositeFace][nCuts-1-iCut] - -faceCutOffsets[iFace][iCut]) < 1e-3);  // rough sanity check, much coarser than the dedup tolerance that was used
                        faceCutOffsets[iOppositeFace][nCuts-1-iCut] = -faceCutOffsets[iFace][iCut];
                    }
                }
            }
        }

        if(progress != null)
            if (!progress.subtaskDone())  // "Constructing polytope"
                return;

        //System.out.println("face inward normals = "+com.donhatchsw.util.Arrays.toStringCompact(faceInwardNormals));
        //System.out.println("cut offsets = "+com.donhatchsw.util.Arrays.toStringCompact(faceCutOffsets));

        if(progress != null)
            if (!progress.subtaskInit("Computing fingerprint of original polytope"))
                return;
        String originalPolytopeTopologicalFingerprintHumanReadable = CSG.computeHumanReadableTopologicalFingerprint(originalPolytope.p);
        if(progress != null)
            if (!progress.subtaskDone())  // "Computing fingerprint of original polytope"
                return;

        //
        // Slice!
        //
        {
            slicedPolytope = originalPolytope;
            // Count the number of cuts we'll be making and initialize the progress manager.
            {
                int totalCuts = 0;
                for(int iFace = 0; iFace < nFaces; ++iFace) {
                    if(face2OppositeFace[iFace] != -1 && face2OppositeFace[iFace] < iFace)
                        continue; // already saw opposite face
                    totalCuts += faceCutOffsets[iFace].length;
                }

                if(progress != null)
                    if (!progress.subtaskInit("Slicing", totalCuts))
                        return;
            }
            // Now do the actual work, updating the progress worker as we go.
            {
                int cut = 0;
                for(int iFace = 0; iFace < nFaces; ++iFace)
                {
                    if(face2OppositeFace[iFace] != -1 && face2OppositeFace[iFace] < iFace)
                        continue; // already saw opposite face and made the cuts
                    //System.out.println("REALLY doing facet "+iFace);
                    for(int iCut = 0; iCut < faceCutOffsets[iFace].length; ++iCut)
                    {
                        CSG.Hyperplane cutHyperplane = new CSG.Hyperplane(
                            faceInwardNormals[iFace],
                            faceCutOffsets[iFace][iCut]);
                        Object auxOfCut = null; // we don't set any aux on the cut for now
                        slicedPolytope = CSG.sliceElements(slicedPolytope, slicedPolytope.p.dim-1, cutHyperplane, auxOfCut, /*sizes=*/null);

                        if(progress != null)
                            if (!progress.updateProgress(cut))
                                return;
                        cut++;
                    }
                }
            }
            if(progress != null)
                if (!progress.subtaskDone())  // "Slicing"
                    return;
        }

        if(progress != null)
            if (!progress.subtaskInit("Fixing orientations"))
                return;
        CSG.orientDeepFunctional(slicedPolytope); // XXX shouldn't be necessary!!!!
        CSG.orientDeepCosmetic(slicedPolytope); // XXX shouldn't be necessary!!!!
        if(progress != null)
            if (!progress.subtaskDone())  // "Fixing orientations"
                return;

        CSG.Polytope stickers[] = slicedPolytope.p.getAllElements()[nDims - 1];
        int nStickers = stickers.length;

        //
        // Figure out the mapping from sticker to face.
        //
        sticker2face = new int[nStickers];
        {
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2face[iSticker] = ((Integer) stickers[iSticker].getAux()).intValue();
        }
        sticker2faceShadow = VecMath.copyvec(sticker2face);

        //
        // Figure out the mapping from sticker to cubie.
        // Cubie indices are arbitrary and not used for anything else;
        // all that is guaranteed is that two stickers are on the same
        // cubie iff they have the same cubie index.
        //
        sticker2cubie = new int[nStickers];
        {
            MergeFind mf = new MergeFind(nStickers);
            // The 4d case:
            //     for each polygon in the sliced puzzle
            //         if it's part of an original polygon (not a cut)
            //             merge the two incident stickers

            CSG.Polytope slicedRidges[] = slicedPolytope.p.getAllElements()[nDims - 2];
            int allSlicedIncidences[][][][] = slicedPolytope.p.getAllIncidences();
            for(int iSlicedRidge = 0; iSlicedRidge < slicedRidges.length; ++iSlicedRidge)
            {
                CSG.Polytope ridge = slicedRidges[iSlicedRidge];
                boolean ridgeIsFromOriginal = (ridge.getAux() != null);
                if(ridgeIsFromOriginal) // if it's not a cut
                {
                    // Find the two stickers that meet at this ridge...
                    int indsOfStickersContainingThisRidge[] = allSlicedIncidences[nDims - 2][iSlicedRidge][nDims - 1];

                    // NOTE: This assert was a check until we started filtering out slivers.
                    //       Slivers will just make cubies with other slivers though,
                    //       so this should be ok.
                    //Assert(indsOfStickersContainingThisRidge.length == 2);
                    if(indsOfStickersContainingThisRidge.length == 2)
                    {
                        int iSticker0 = indsOfStickersContainingThisRidge[0];
                        int iSticker1 = indsOfStickersContainingThisRidge[1];
                        mf.merge(iSticker0, iSticker1);
                    }
                }
            }
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2cubie[iSticker] = mf.find(iSticker);

            _nCubies = 0;
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
                if(sticker2cubie[iSticker] == iSticker)
                    _nCubies++;
            if(PropertyManager.getBoolean("debug", false))
                System.out.println("    There seem to be " + _nCubies + " accessible cubie(s).");
            // XXX note, we could easily collapse the cubie indicies
            // XXX so that they are consecutive, if we cared
        }

        //
        // Find the face centers and sticker centers.
        // The center of mass of the vertices is probably
        // as good as anything, for this
        // (when we get shrinking right, it won't
        // actually use centers, I don't think)
        //
        double faceCentersD[][] = new double[nFaces][nDims];
        {
            for(int iFace = 0; iFace < nFaces; ++iFace)
                CSG.cgOfVerts(faceCentersD[iFace], originalFaces[iFace]);
        }
        stickerCentersD = new double[nStickers][nDims];
        stickerCentersHashTable = new FuzzyPointHashTable<Integer>(1e-9, 1e-8, 1. / 128);
        {
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                CSG.cgOfVerts(stickerCentersD[iSticker], stickers[iSticker]);
                stickerCentersHashTable.put(stickerCentersD[iSticker], Integer.valueOf(iSticker));
            }
        }

        faceCenters = VecMath.doubleToFloat(faceCentersD);
        float stickerCentersMinusFaceCentersF[][] = new float[nStickers][];
        {
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
                stickerCentersMinusFaceCentersF[iSticker] = VecMath.doubleToFloat(
                    VecMath.vmv(stickerCentersD[iSticker], faceCentersD[sticker2face[iSticker]]));
        }

        if (progress != null && !progress.subtaskInit("Computing fingerprint of polytope")) return;
        this.topologicalFingerprintHumanReadable = computeTopologicalFingerprintHumanReadable(
          this.originalPolytope, originalPolytopeTopologicalFingerprintHumanReadable, new int[] {intLength}, nStickers, this.faceInwardNormals, this.faceCutOffsets, this.stickerCentersD);
        this.topologicalFingerprintDigest =
            this.topologicalFingerprintHumanReadable.contains("NOT FINGERPRINTABLE")
              ? null
              : CSG.sha1(this.topologicalFingerprintHumanReadable);
        if (progress != null && !progress.subtaskDone()) return;  // "Computing puzzle fingerprint"

        System.out.println("topologicalFingerprintHumanReadable = \n"+indented("    ", this.topologicalFingerprintHumanReadable));
        System.out.println("topologicalFingerprintDigest = "+this.topologicalFingerprintDigest);


        //
        // PolyFromPolytope doesn't seem to like the fact that
        // some elements have an aux and some don't... so clear all the vertex
        // auxs.
        // XXX why does this seem to be a problem for nonregular cross products but not for regulars?  figure this out
        //
        if(true)
        {
            CSG.Polytope allElements[][] = slicedPolytope.p.getAllElements();
            for(int iDim = 0; iDim < allElements.length; ++iDim)
                for(int iElt = 0; iElt < allElements[iDim].length; ++iElt)
                    allElements[iDim][iElt].setAux(null);
        }

        //
        // Get the rest verts (with no shrinkage)
        // and the sticker polygon indices.
        // This is dimension-specific.
        //
        double restVerts[][];
        if(nDims == 3)
        {
            Poly slicedPoly = PolyCSG.PolyFromPolytope(slicedPolytope.p);
            restVerts = (double[][]) slicedPoly.verts;

            // slicedPoly.inds is a list of faces, each of which
            // is a list of contours.  We assume there is always
            // one contour per face (i.e. no holes),
            // so we can now just re-interpret the one contour per face
            // as one face per sticker (instead of flattening
            // and re-expanding which would just give us back
            // what we started with).
            stickerInds = (int[][][]) slicedPoly.inds;
        }
        else if(nDims == 4)
        {
            // Start by making a completely separate Poly
            // out of each sticker.  There will be no
            // vertex sharing between stickers.
            Poly stickerPolys[] = new Poly[nStickers];
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                stickerPolys[iSticker] = PolyCSG.PolyFromPolytope(stickers[iSticker]);
                // So it gets grouped when we concatenate...
                stickerPolys[iSticker].inds = new int[][][][]{(int[][][]) stickerPolys[iSticker].inds};
            }
            //
            // Now concatenate them all together (the verts
            // and the inds).
            //
            Poly slicedPoly = Poly.concat(stickerPolys);

            restVerts = (double[][]) slicedPoly.verts;
            // We assume there is only 1 contour per polygon,
            // so we can flatten out the contours part.
            stickerInds = (int[][][]) com.donhatchsw.util.Arrays.flatten(slicedPoly.inds, 2, 2);

            //
            // Fix up the indices on each sticker so that
            // the first vertex on the second face
            // does not occur on the first face;
            // that will guarantee that [0][0], [0][1], [0][2], [1][0]
            // form a non-degenerate simplex, as required.
            //
            {
                for(int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
                {
                    int polygon0[] = stickerInds[iSticker][0];
                    int polygon1[] = stickerInds[iSticker][1];
                    int i;
                    for(i = 0; i < polygon1.length; ++i)
                    {
                        int j;
                        for(j = 0; j < polygon0.length; ++j)
                        {
                            if(polygon1[i] == polygon0[j])
                                break; // this i is no good
                        }
                        if(j == polygon0.length)
                            break; // this i is good
                    }
                    // Cyclic permute polygon1
                    // to put its [i] at [0]
                    if(i != 0)
                    {
                        int cycled[] = new int[polygon1.length];
                        for(int ii = 0; ii < cycled.length; ++ii)
                            cycled[ii] = polygon1[(i + ii) % cycled.length];
                        stickerInds[iSticker][1] = cycled;
                    }
                }
            }
        }
        else // nDims is something other than 3 or 4
        {
            // Make a vertex array of the right size,
            // just so nVerts() will return something sane for curiosity
            int nVerts = 0;
            for(int iSticker = 0; iSticker < nStickers; ++iSticker)
                nVerts += stickers[iSticker].getAllElements()[0].length;
            restVerts = new double[nVerts][nDims]; // zeros
            stickerInds = new int[nStickers][0][];
        }


        //
        // Calculate the three arrays 
        // that will let us quickly calculate the sticker verts
        // at rest for any faceShrink and stickerShrink.
        // Note that vertFaceCenters and vertStickerCentersMinusFaceCenters
        // contain lots of duplicates; these dups all point
        // to the same float[] however, so it's not as horrid as it seems.
        //
        {
            int nVerts = restVerts.length;
            vertsMinusStickerCenters = new float[nVerts][];
            vertStickerCentersMinusFaceCenters = new float[nVerts][];
            vertFaceCenters = new float[nVerts][];
            {
                for(int iSticker = 0; iSticker < nStickers; ++iSticker)
                    for(int j = 0; j < stickerInds[iSticker].length; ++j)
                        for(int k = 0; k < stickerInds[iSticker][j].length; ++k)
                        {
                            int iFace = sticker2face[iSticker];
                            int iVert = stickerInds[iSticker][j][k];
                            if(vertsMinusStickerCenters[iVert] == null)
                            {
                                vertsMinusStickerCenters[iVert] = VecMath.doubleToFloat(VecMath.vmv(restVerts[iVert], stickerCentersD[iSticker]));
                                vertStickerCentersMinusFaceCenters[iVert] = stickerCentersMinusFaceCentersF[iSticker];
                                vertFaceCenters[iVert] = faceCenters[iFace];
                            }
                        }
            }
        }

        if(PropertyManager.getBoolean("debug", false)) {
            double maxSqrdVertDist4 = 0;
            for(double[] vert : restVerts) {
                double distSqrd4 = 0;
                for(double c : vert)
                    distSqrd4 += c * c;
                maxSqrdVertDist4 = Math.max(distSqrd4, maxSqrdVertDist4);
            }
            System.out.println("4D radius: " + Math.sqrt(maxSqrdVertDist4));
        }

        //
        // Now think about the twist grips.
        // There will be one grip at each vertex,edge,face center
        // of the original polytope (if 3d)
        // or of each cell of the original polytope (if 4d).
        // XXX woops, I'm retarded, 3d doesn't have that...
        // XXX but actually it wouldn't hurt, could just make that
        // XXX rotate the whole puzzle.
        //
        if(nDims == 4)
        {
            // Count the number of grips we'll be generating and initialize the progress manager.
            int nGrips = 0;
            for(int iFace = 0; iFace < nFaces; ++iFace)
            {
                CSG.Polytope cell = originalFaces[iFace];
                CSG.Polytope[][] allElementsOfCell = cell.getAllElements();
                for(int iDim = 0; iDim <= 3; ++iDim)
                    // yes, even for cell center, which doesn't do anything
                    nGrips += allElementsOfCell[iDim].length;
            }
            if(progress != null)
                if (!progress.subtaskInit("Calculating possible twists", nGrips))
                    return;

            // Now do the actual work, updating the progress manager as we go.
            gripSymmetryOrders = new int[nGrips];
            gripUsefulMats = new double[nGrips][nDims][nDims];
            gripCentersF = new float[nGrips][];
            gripDims = new int[nGrips];
            grip2face = new int[nGrips];
            double gripCenterD[] = new double[nDims];
            int iGrip = 0;
            for(int iFace = 0; iFace < nFaces; ++iFace)
            {
                CSG.Polytope cell = originalFaces[iFace];
                CSG.Polytope[][] allElementsOfCell = cell.getAllElements();
                for(int iDim = 0; iDim <= 3; ++iDim) // XXX should we have a grip for the cell center, which doesn't do anything? maybe!
                {
                    for(int iElt = 0; iElt < allElementsOfCell[iDim].length; ++iElt)
                    {
                        CSG.Polytope elt = allElementsOfCell[iDim][iElt];
                        gripSymmetryOrders[iGrip] = CSG.calcRotationGroupOrderDEPRECATED(
                            originalPolytope.p, cell, elt,
                            gripUsefulMats[iGrip]);

                        CSG.cgOfVerts(gripCenterD, elt);
                        // !! We can't use the element center,
                        // that will end up having the same center
                        // for different stickers on the same cubie!
                        // So fudge it a little towards the cell center.
                        // XXX should try to be more scientific...
                        VecMath.lerp(gripCenterD, gripCenterD, faceCentersD[iFace], .01);

                        gripCentersF[iGrip] = VecMath.doubleToFloat(gripCenterD);
                        gripDims[iGrip] = iDim;
                        grip2face[iGrip] = iFace;

                        if(progress != null)
                            if (!progress.updateProgress(iGrip))
                                return;
                        //System.out.println("("+iDim+":"+gripSymmetryOrders[iGrip]+")");

                        iGrip++;
                    }
                }
            }
            if(progress != null)
                if (!progress.subtaskDone())  // "Calculating possible twists"
                    return;
            Assert(iGrip == nGrips);

            /*
             * want to know, for each grip:
             * - the grip center coords
             * - its face center coords
             * - its period
             * for each slice using this grip (i.e. iterate through the slices parallel to the face the grip is on):
             * - from indices of a CCW twist of this slice
             * - to indices of a CCW twist of this slice
             */

        } // nDims == 4

        //
        // Select points worthy of being rotated to the center (-W axis).
        //
        {
            int nNicePoints = 0;
            for(int iDim = 0; iDim < originalElements.length; ++iDim)
                nNicePoints += originalElements[iDim].length;
            nicePointsToRotateToCenter = new float[nNicePoints][nDims];
            double eltCenter[] = new double[nDims];
            int iNicePoint = 0;
            for(int iDim = 0; iDim < originalElements.length; ++iDim)
                for(int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
                {
                    CSG.cgOfVerts(eltCenter, originalElements[iDim][iElt]);
                    nicePointsToRotateToCenter[iNicePoint++] = VecMath.doubleToFloat(eltCenter);

                }
            Assert(iNicePoint == nNicePoints);
        }
    } // ctor from schlafli and length

    @Override
    public String toString()
    {
        String nl = System.getProperty("line.separator");
        CSG.Polytope[][] allElements = slicedPolytope.p.getAllElements();
        int sizes[] = new int[allElements.length];
        for(int iDim = 0; iDim < sizes.length; ++iDim)
            sizes[iDim] = allElements[iDim].length;
        String answer = "{polytope counts per dim = "
            + com.donhatchsw.util.Arrays.toStringCompact(sizes)
            + ", " + nl + "  nDims = " + nDims()
            + ", " + nl + "  nStickers = " + nStickers()
            + ", " + nl + "  nGrips = " + nGrips()
            + ", " + nl + "  slicedPolytope = " + slicedPolytope.toString(true)
            + ", " + nl + "  vertsMinusStickerCenters = " + com.donhatchsw.util.Arrays.toStringNonCompact(vertsMinusStickerCenters, "    ", "    ")
            + ", " + nl + "  vertStickerCentersMinusFaceCenters = " + com.donhatchsw.util.Arrays.toStringNonCompact(vertStickerCentersMinusFaceCenters, "    ", "    ")
            + ", " + nl + "  vertFaceCenters = " + com.donhatchsw.util.Arrays.toStringNonCompact(vertFaceCenters, "    ", "    ")
            + ", " + nl + "  stickerInds = " + com.donhatchsw.util.Arrays.toStringNonCompact(stickerInds, "    ", "    ")
            + ", " + nl + "  sticker2face = " + com.donhatchsw.util.Arrays.toStringNonCompact(sticker2face, "    ", "    ")
            + "}";
        return answer;
    } // toString


    //
    // Utilities...
    //

    // magic crap used in a couple of methods below
    private double[][] getTwistMat(int gripIndex, int dir, double frac)
    {
        int order = gripSymmetryOrders[gripIndex];
        double angle = dir * (2 * Math.PI / order) * frac;
        int nDims = slicedPolytope.p.fullDim;
        return VecMath.mxmxm(VecMath.transpose(gripUsefulMats[gripIndex]),
            VecMath.makeRowRotMat(nDims, nDims - 2, nDims - 1, angle),
            gripUsefulMats[gripIndex]);
    } // getTwistMat


    // Make the topological fingerprint, consisting of the following:
    // - fingerprint of original polytope
    // - "number of cuts" that is, floor(intLength/2).
    // - nStickers
    // - for each different type of facet, a histogram of slice-to-number-of-stickers for slices parallel to a facet of that type.
    // Note that, in particular, this correctly recognizes (even without counts):
    //     "{5,3,3} 2" == "{5,3,3} 3"
    //     "{4,3,3} 2" != "{4,3,3} 3"
    // And furthermore, the face-to-slice-sticker-counts mapping allows correctly recognizing:
    //     "{3}x{4} 3(2.75)" != "{3}x{4} 3(3.25}"
    // Note: if ultimately not fingerprintable, this function
    // still returns a summary of much of the structure,
    // but it will contain the substring "NOT FINGERPRINTABLE".
    // In this case the caller should not make the digest of it.
    private static String computeTopologicalFingerprintHumanReadable(
        CSG.SPolytope originalPolytope,
        String originalPolytopeHumanReadableTopologicalFingerprint,
        int[] intLengths,
        int nStickers,
        double[][] facetInwardNormals,
        double[][] facetCutOffsets,
        double[][] stickerCentersD)
    {
        StringBuilder topologicalFingerprintHumanReadableBuilder = new StringBuilder();
        topologicalFingerprintHumanReadableBuilder.append("original polytope:\n");
        topologicalFingerprintHumanReadableBuilder.append(indented("    ", originalPolytopeHumanReadableTopologicalFingerprint) + "\n");
        topologicalFingerprintHumanReadableBuilder.append("floor(intLength/2) = ");
        for (int i = 0; i < intLengths.length; ++i) {
          if (i > 0) topologicalFingerprintHumanReadableBuilder.append(",");
          topologicalFingerprintHumanReadableBuilder.append(intLengths[i]/2);
        }
        topologicalFingerprintHumanReadableBuilder.append("\n");
        topologicalFingerprintHumanReadableBuilder.append("number of stickers: " + nStickers);

        if (true) {
          // We need more information,
          // so that we don't mistakenly think "{3}x{4} 3(2.75)" is the same as "{3}x{4} 3(3.25)".
          // So, make a mapping from face type to slice sticker counts.

          // First of all, check whether all edges in the original polytope
          // are the same length. If not, this method won't work, in which case
          // we just mark the whole thing "NOT FINGERPRINTABLE".
          // Examples of non-uniform edge lengths:
          //    "(1.0)3(0)3(2.0) 3"  (3d)
          //    "(1.0)3(0)3(0)3(2.0) 3"  (4d)
          //    "frucht 3"  (3d)
          //    "frucht*{} 3"  (4d)
          //    "(1.2)x(1.3) 3"  (2d)
          // Note, it's probably still possible to fool it,
          // by making a nonuniform polytope with uniform edge lengths.
          boolean edgeLengthsAreUniform;
          double minEdgeLength;
          double maxEdgeLength;
          {
            CSG.Polytope[] originalEdges = originalPolytope.p.getAllElements()[1];
            double minEdgeLength2 = Double.POSITIVE_INFINITY;
            double maxEdgeLength2 = 0.;
            for (int iEdge = 0; iEdge < originalEdges.length; ++iEdge) {
              double[] v0 = originalEdges[iEdge].facets[0].p.getCoords();
              double[] v1 = originalEdges[iEdge].facets[1].p.getCoords();
              double thisEdgeLength2 = VecMath.distsqrd(v0, v1);
              if (thisEdgeLength2 < minEdgeLength2) minEdgeLength2 = thisEdgeLength2;
              if (thisEdgeLength2 > maxEdgeLength2) maxEdgeLength2 = thisEdgeLength2;
            }
            minEdgeLength = Math.sqrt(minEdgeLength2);
            maxEdgeLength = Math.sqrt(maxEdgeLength2);
            edgeLengthsAreUniform = (maxEdgeLength <= minEdgeLength * (1. + 1e-12));
          }

          if (intLengths.length > 1) {
            topologicalFingerprintHumanReadableBuilder.append("\nface type to slice sticker counts: UNKNOWN because intLengths are not uniform: "+com.donhatchsw.util.Arrays.toStringCompact(intLengths));
            topologicalFingerprintHumanReadableBuilder.append("\nNOT FINGERPRINTABLE!");
          } else if (!edgeLengthsAreUniform) {
            topologicalFingerprintHumanReadableBuilder.append("\nface type to slice sticker counts: UNKNOWN because edge lengths are nonuniform: min "+minEdgeLength+", max "+maxEdgeLength);
            topologicalFingerprintHumanReadableBuilder.append("\nNOT FINGERPRINTABLE!");
          } else {
            // TreeMap rather than HashMap, so iterating comes out in sorted order
            java.util.TreeMap<String,int[]> facetType2Counts = new java.util.TreeMap<String,int[]>();
            {
              // CBB: this re-performs all the analysis done already when computing
              // originalPolytopeHumanReadableTopologicalFingerprint.
              // At least this part isn't too slow.
              String[][] allElementTypes = CSG.computeAllElementTopologicalishSummaries(originalPolytope.p,
                                                                                        /*mainSeparator=*/",\n",
                                                                                        /*isVertexFigure=*/false);
              String[] allFacetTypes = allElementTypes[originalPolytope.p.dim-1];
              for (int iFacet = 0; iFacet < allFacetTypes.length; ++iFacet) {
                // NOTE: this assumes our facet ordering is the same as the original polytope's internal ordering.
                // This seems to be the case, for now, but we may want to canonicalize it
                // so that we aren't at the mercy of whatever arbitrary order the CSG module produces.
                String facetType = allFacetTypes[iFacet];
                int[] oldCounts = facetType2Counts.get(facetType);

                boolean sanityCheckMode = false;  // set this to true to confirm that facets of the same type do indeed have the same counts. expensive.
                // E.g. for "(1)5(1)3(1)3(1) 3":
                //     sanityCheckMode=false: 0.038760642s
                //     sanityCheckMode=true: 19.608136629s

                // CBB: could do at least a half-hearted sanity check mode-- that is, take a small handful (maybe 2) of each type and make sure we get the same answer for each

                if (oldCounts == null || sanityCheckMode) {
                   double[] thisFacetInwardNormal = facetInwardNormals[iFacet];
                   double[] thisFacetCutOffsets = facetCutOffsets[iFacet];
                   int[] counts = new int[thisFacetCutOffsets.length+1];  // all zeros initially
                   for (int iSticker = 0; iSticker < nStickers; ++iSticker) {
                     int whichSlice = whichSlice(stickerCentersD[iSticker],
                                                 thisFacetInwardNormal,
                                                 thisFacetCutOffsets);
                     counts[whichSlice]++;
                   }
                   //System.out.println(com.donhatchsw.util.Arrays.toStringCompact(facetType)+" -> "+com.donhatchsw.util.Arrays.toStringCompact(counts));
                   if (oldCounts == null) {
                     facetType2Counts.put(facetType, counts);
                   } else {
                     Assert(VecMath.equals(counts, oldCounts));
                   }
                }
              }
            }
            topologicalFingerprintHumanReadableBuilder.append("\nface type to slice sticker counts:");
            for (java.util.Map.Entry<String,int[]> kv : facetType2Counts.entrySet()) {
              topologicalFingerprintHumanReadableBuilder.append("\n    ");
              topologicalFingerprintHumanReadableBuilder.append(com.donhatchsw.util.Arrays.toStringCompact(kv.getKey()));
              topologicalFingerprintHumanReadableBuilder.append(": ");
              int value[] = kv.getValue();
              for (int i = 0; i < value.length; ++i) {
                if (i > 0) topologicalFingerprintHumanReadableBuilder.append(",");
                topologicalFingerprintHumanReadableBuilder.append(value[i]);
              }
            }
          }  // edge lengths are uniform
        }
        return topologicalFingerprintHumanReadableBuilder.toString();
    }  // computeTopologicalFingerprintHumanReadable

    //======================================================================
    // BEGIN GENERICPUZZLEDESCRIPTION INTERFACE METHODS
    //

    @Override
    public String getTopologicalFingerprintHumanReadable()
    {
        return topologicalFingerprintHumanReadable;
    }
    @Override
    public String getTopologicalFingerprintDigest()
    {
        return topologicalFingerprintDigest;
    }
    @Override
    public String getSchlafliProduct()
    {
        return schlafliProduct;
    }
    @Override
    public String getLengthString() {
        return lengthString;
    }
    @Override
    public int getIntLength() {
        return intLength;
    }
    @Override
    public double getDoubleLength() {
        return doubleLength;
    }
    @Override
    public String getFullPuzzleString() {
        return schlafliProduct + " " + lengthString;
    }
    @Override
    public int nDims()
    {
        return slicedPolytope.p.fullDim;
    }
    @Override
    public int nVerts()
    {
        return vertsMinusStickerCenters.length;
    }
    @Override
    public int nFaces()
    {
        return originalPolytope.p.facets.length;
    }
    @Override
    public int nCubies()
    {
        return _nCubies;
    }
    @Override
    public int nStickers()
    {
        return slicedPolytope.p.facets.length;
    }
    @Override
    public int nGrips()
    {
        return grip2face.length;
    }
    @Override
    public float circumRadius()
    {
        return _circumRadius;
    }
    @Override
    public float inRadius()
    {
        return _inRadius;
    }

    @Override
    public void computeStickerVertsAtRest(float verts[/* nVerts */][/* nDims */],
        float faceShrink,
        float stickerShrink)
    {
        Assert(verts.length == vertsMinusStickerCenters.length);
        for(int iVert = 0; iVert < verts.length; ++iVert)
        {
            float faceCenter[] = vertFaceCenters[iVert];
            float stickerCenterMinusFaceCenter[] = vertStickerCentersMinusFaceCenters[iVert];
            float vertMinusStickerCenter[] = vertsMinusStickerCenters[iVert];
            float vert[] = verts[iVert];
            Assert(vert.length == vertMinusStickerCenter.length);
            for(int j = 0; j < vert.length; ++j)
                vert[j] = (vertMinusStickerCenter[j] * stickerShrink
                    + stickerCenterMinusFaceCenter[j]) * faceShrink
                    + faceCenter[j];
        }
    }

    @Override
    public float[][] getStandardStickerVertsAtRest()
    {
        // Return the cached values if we've already calculated them.
        if(standardStickerVertsAtRest != null)
            return standardStickerVertsAtRest;

        standardStickerVertsAtRest = new float[nVerts()][nDims()];
        computeStickerVertsAtRest(standardStickerVertsAtRest,
            1.f, // faceShrink
            1.f); // stickerShrink

        return standardStickerVertsAtRest;
    }
    private float[][] standardStickerVertsAtRest;

    @Override
    public int[/* nStickers */][/* nPolygonsThisSticker */][/* nVertsThisPolygon */]
        getStickerInds()
    {
        return stickerInds;
    }
    @Override
    public void computeGripVertsAtRest(float verts[/* nVerts */][/* nDims */],
        float faceShrink,
        float stickerShrink)
    {
        throw new RuntimeException("unimplemented");
    }
    @Override
    public int[/* nGrips */][/* nPolygonsThisGrip */][/* nVertsThisPolygon */]
        getGripInds()
    {
        throw new RuntimeException("unimplemented");
    }
    @Override
    public int[/* nGrips */]
        getGripSymmetryOrders()
    {
        return gripSymmetryOrders;
    }

    @Override
    public int getNumColorsForCubie(int cubie)
    {
        if(numColorsForCubie != null)
            return numColorsForCubie.get(cubie);

        // We need to calc this.
        numColorsForCubie = new java.util.HashMap<Integer, Integer>();
        for(int s = 0; s < nStickers(); s++)
        {
            int c = getSticker2Cubie()[s];
            Integer temp = numColorsForCubie.get(c);
            if(temp == null)
                numColorsForCubie.put(c, 1);
            else
                numColorsForCubie.put(c, temp + 1);
        }

        return numColorsForCubie.get(cubie);
    }

    /**
     * Answers the question, how many cubies have exactly n colors?
     */
    @Override
    public int getNumCubiesWithNumColors(int n_colors) {
        if(numCubiesForNumColors == null) {
            numCubiesForNumColors = new HashMap<Integer, Integer>();
            for(int s = 0; s < nStickers(); s++)
            {
                int c = getSticker2Cubie()[s];
                int num_colors_for_c = getNumColorsForCubie(c);
                Integer temp = numCubiesForNumColors.get(num_colors_for_c);
                if(temp == null)
                    numCubiesForNumColors.put(num_colors_for_c, 1);
                else
                    numCubiesForNumColors.put(num_colors_for_c, temp + 1);
            }
        }
        Integer n = numCubiesForNumColors.get(n_colors);
        return n == null ? 0 : n.intValue();
    }

    // Map from cubie id to num colors.
    // See notes when sticker2cubie values are calculated,
    // for why this needs to be a map.
    private Map<Integer, Integer> numColorsForCubie;
    private Map<Integer, Integer> numCubiesForNumColors; // The inverse of the above.

    @Override
    public int getClosestGrip(float pickCoords[/* 4 */])
    {
        return getClosestGrip(pickCoords, -1, -1, false);
    }

    @Override
    public int getClosestGrip(float pickCoords[/* 4 */], int faceIndex, int stickerIndex, boolean is2x2x2Cell)
    {
        boolean faceValid = -1 != faceIndex;
        boolean stickerValid = -1 != stickerIndex;

        // Find the dimension of the grips we want to consider.
        int gripDim = -1;
        if(stickerValid)
        {
            if(is2x2x2Cell)
                gripDim = 2;
            else
            {
                /*
                 * NOTE
                 * There is a fundamental problem with the approach in this block, which
                 * is that the number-of-colors isn't a discriminating enough property to
                 * classify piece types. There will be situations on some puzzles where
                 * different piece types have the same number of colors (simplex puzzles),
                 * and situations where we want those different piece types to do different
                 * things even though they have the same number of colors (tetrahedral prism).
                 * Another example is the 600 cell, where vertexes will have 20C pieces!
                 * 
                 * A better long term solution will be to have some step at puzzle build
                 * time where the various piece types are matched up with grip types in
                 * some other fashion. One thought is to check incidences with the parent
                 * polytope. Pieces touching vertices would get marked as such, pieces
                 * touching edges would get marked as such, etc., irrespective of
                 * number-of-colors (of course, pieces touching vertices also touch edges,
                 * but we would always choose the lowest-dimension element we were incident with).
                 * 
                 * This looks to be ok for the 4.0 puzzles we are supporting, but we'll have
                 * to address this to push out some of the upcoming puzzles.
                 */

                int cubie = getSticker2Cubie()[stickerIndex];
                int numColors = getNumColorsForCubie(cubie);
                gripDim = nDims() - numColors;

                // This is necessary in some rare situations,
                // e.g. the length-2 simplex has a 5C central piece.
                if(gripDim < 0)
                    gripDim = nDims() - 1;
            }
        }

        int bestIndex = -1;
        float bestDistSqrd = Float.MAX_VALUE;
        for(int i = 0; i < gripCentersF.length; ++i)
        {
            // Only consider grips on this face.
            // This helped some poor behavior on 2x2x2 cells,
            // and is a good check in general I think.
            if(faceValid && this.getGrip2Face()[i] != faceIndex)
                continue;

            // Restrict the type of grips we search.
            if(stickerValid && gripDims[i] != gripDim)
                continue;

            float thisDistSqrd = VecMath.distsqrd(gripCentersF[i],
                pickCoords);
            if(thisDistSqrd < bestDistSqrd)
            {
                bestDistSqrd = thisDistSqrd;
                bestIndex = i;
            }
        }

        Assert(-1 != bestIndex);
        return bestIndex;
    }
    @Override
    public float[/* nDims */] getClosestNicePointToRotateToCenter(float pickCoords[])
    {
        int bestIndex = -1;
        float bestDistSqrd = Float.MAX_VALUE;
        for(int i = 0; i < nicePointsToRotateToCenter.length; ++i)
        {
            float thisDistSqrd = VecMath.distsqrd(nicePointsToRotateToCenter[i],
                pickCoords);
            if(thisDistSqrd < bestDistSqrd)
            {
                bestDistSqrd = thisDistSqrd;
                bestIndex = i;
            }
        }
        return nicePointsToRotateToCenter[bestIndex];
    }
    @Override
    public void
        computeStickerVertsPartiallyTwisted(
            float verts[/* nVerts */][/* nDims */],
            float faceShrink,
            float stickerShrink,
            int gripIndex,
            int dir,
            int slicemask,
            float frac)
    {
        // Note, we purposely go through all the calculation
        // even if dir*frac is 0; we get more consistent timing that way.
        if(gripIndex < 0 || gripIndex >= nGrips())
            throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on bad gripIndex " + gripIndex + ", there are " + nGrips() + " grips!");
        if(gripSymmetryOrders[gripIndex] == 0)
            throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on gripIndex " + gripIndex + " which does not rotate!");

        if(slicemask == 0)
            slicemask = 1; // XXX is this the right place for this? lower and it might be time consuming, higher and too many callers will have to remember to do it

        double matD[][] = getTwistMat(gripIndex, dir, frac);
        float matF[][] = VecMath.doubleToFloat(matD);

        float restVerts[][] = new float[nVerts()][nDims()];
        computeStickerVertsAtRest(restVerts, faceShrink, stickerShrink);
        boolean whichVertsGetMoved[] = new boolean[restVerts.length]; // false initially
        int iFace = grip2face[gripIndex];
        double thisFaceInwardNormal[] = faceInwardNormals[iFace];
        double thisFaceCutOffsets[] = faceCutOffsets[iFace];
        for(int iSticker = 0; iSticker < stickerCentersD.length; ++iSticker)
        {
            if(pointIsInSliceMask(stickerCentersD[iSticker],
                slicemask,
                thisFaceInwardNormal,
                thisFaceCutOffsets))
            {
                for(int i = 0; i < stickerInds[iSticker].length; ++i)
                    for(int j = 0; j < stickerInds[iSticker][i].length; ++j)
                        whichVertsGetMoved[stickerInds[iSticker][i][j]] = true;
            }
        }

        for(int iVert = 0; iVert < verts.length; ++iVert)
        {
            if(whichVertsGetMoved[iVert])
                verts[iVert] = VecMath.vxm(restVerts[iVert], matF);
            else
                verts[iVert] = restVerts[iVert];
        }
    } // getStickerVertsPartiallyTwisted
    @Override
    public int[/* nStickers */] getSticker2Face()
    {
        // Make sure caller didn't mess it up from last time!!
        if(!VecMath.equals(sticker2face, sticker2faceShadow))
            throw new RuntimeException("PolytopePuzzleDescription.getSticker2Face: caller modified previously returned sticker2face! BAD! BAD! BAD!");
        return sticker2face;
    }
    @Override
    public int[/* nStickers */] getSticker2Cubie()
    {
        return sticker2cubie;
    }

    @Override
    public int[/* nGrips */] getGrip2Face()
    {
        return grip2face;
    }

    @Override
    public int getNumSlicesForGrip(int gripIndex)
    {
        if(gripIndex < 0 || gripIndex >= nGrips())
            throw new IllegalArgumentException("getNumSlicesForGrip called with bad grip index " + gripIndex + ", there are " + nGrips() + " grips!");

        int iFace = grip2face[gripIndex];
        double thisFaceCutOffsets[] = faceCutOffsets[iFace];
        int numSlices = thisFaceCutOffsets.length + 1;
        //System.out.println( "number of slices available for twist = " + numSlices );
        return numSlices;
    }

    @Override
    public float[/* nDims */] getFaceCenter(int faceIndex)
    {
        if(faceIndex < 0 || faceIndex >= nFaces())
            throw new IllegalArgumentException("getFaceCenter called with bad face index " + faceIndex + ", there are " + nFaces() + " faces!");

        return faceCenters[faceIndex];
    }

    @Override
    public float[/* nDims */] getGripCoords(int gripIndex)
    {
        if(gripIndex < 0 || gripIndex >= nGrips())
            throw new IllegalArgumentException("getGripCoords called with bad grip index " + gripIndex + ", there are " + nGrips() + " grips!");

        return gripCentersF[gripIndex];
    }

    @Override
    public int[/* nFaces */] getFace2OppositeFace()
    {
        return face2OppositeFace;
    }
    @Override
    public void applyTwistToState(int state[/* nStickers */],
        int gripIndex,
        int dir,
        int slicemask)
    {
        if(gripIndex < 0 || gripIndex >= nGrips())
            throw new IllegalArgumentException("applyTwistToState called on bad gripIndex " + gripIndex + ", there are " + nGrips() + " grips!");
        if(gripSymmetryOrders[gripIndex] == 0)
            throw new IllegalArgumentException("applyTwistToState called on gripIndex " + gripIndex + " which does not rotate!");
        if(state.length != stickerCentersD.length)
            throw new IllegalArgumentException("applyTwistToState called with wrong size state " + state.length + ", expected " + stickerCentersD.length + "!");

        if(slicemask == 0)
            slicemask = 1; // XXX is this the right place for this? lower and it might be time consuming, higher and too many callers will have to remember to do it

        double scratchVert[] = new double[nDims()];
        double matD[][] = getTwistMat(gripIndex, dir, 1.);
        int newState[] = new int[state.length];
        int iFace = grip2face[gripIndex];
        double thisFaceInwardNormal[] = faceInwardNormals[iFace];
        double thisFaceCutOffsets[] = faceCutOffsets[iFace];
        for(int iSticker = 0; iSticker < state.length; ++iSticker)
        {
            if(pointIsInSliceMask(stickerCentersD[iSticker],
                slicemask,
                thisFaceInwardNormal,
                thisFaceCutOffsets))
            {
                VecMath.vxm(scratchVert, stickerCentersD[iSticker], matD);
                Integer whereIstickerGoes = stickerCentersHashTable.get(scratchVert);
                Assert(whereIstickerGoes != null);
                newState[whereIstickerGoes.intValue()] = state[iSticker];
            }
            else
                newState[iSticker] = state[iSticker];
        }
        VecMath.copyvec(state, newState);
        newState = null;
    } // applyTwistToState

    //
    // END OF GENERICPUZZLEDESCRIPTION INTERFACE METHODS
    //======================================================================

    private static int whichSlice(double point[],
                                  double cutNormal[],
                                  double cutOffsets[])
    {
        // XXX a binary search would work better if num cuts is big.
        // XXX really need to check offsets only between differing
        // XXX bits of slicmask.
        double pointHeight = VecMath.dot(point, cutNormal);
        int iSlice = 0;
        while (iSlice < cutOffsets.length
            && pointHeight > cutOffsets[iSlice])
            iSlice++;
        return iSlice;
    }
    // does NOT do the slicemask 0->1 correction
    private static boolean pointIsInSliceMask(double point[],
                                              int slicemask,
                                              double cutNormal[],
                                              double cutOffsets[])
    {
        int whichSlice = whichSlice(point, cutNormal, cutOffsets);
        boolean answer = (slicemask & (1<<whichSlice)) != 0;
        return answer;
    }


    //
    // Example program
    //
    public static void main(String args[])
    {
        if(args.length != 1)
        {
            System.err.println();
            System.err.println("    Usage: PolytopePuzzleDescription \"<schlafliProduct> <length>[(<doubleLength>)]\"");
            System.err.println("    Example: PolytopePuzzleDescription \"{4,3,3} 3\"");
            System.err.println("    Example: PolytopePuzzleDescription \"{3,3,3} 3(5.0)\"");
            System.err.println("    Example: PolytopePuzzleDescription \"{5}x{4} 3\"");
            System.err.println();
            System.exit(1);
        }
        System.out.println("in main");

        //CSG.verboseLevel = 2;

        String prescription = args[0];

        final boolean[] cancelledHolder = {false};
        PuzzleDescription descr = new PolytopePuzzleDescription(prescription,
            new ProgressCallbacks() {
                private long initTimeNanos = 0;
                @Override public boolean subtaskInit(String string, int max) {
                    System.out.print(string+" ("+max+") ...");
                    initTimeNanos = System.nanoTime();
                    return true;  // keep going
                }
                @Override public boolean subtaskInit(String string) {
                    System.out.println(string+"...");
                    initTimeNanos = System.nanoTime();
                    return true;  // keep going
                }
                @Override public boolean updateProgress(int progress) {
                    System.out.print("..."+progress);
                    System.out.flush();
                    // Silly and disruptive exercise of the cancellation feature
                    if (progress == 1000) {
                        System.out.println();
                        System.out.print("This is taking a while.  Want to keep going? (y/n)[Y] ");
                        System.out.flush();
                        try {
                            char c = (char)System.in.read();
                            if (c == 'n') {
                                cancelledHolder[0] = true;
                                return false;  // cancel
                            }
                        } catch (java.io.IOException e) {
                            System.out.println("Caught: "+e);
                            cancelledHolder[0] = true;
                            return false;  // cancel
                        }
                    }
                    return true;  // keep going
                }
                @Override public boolean subtaskDone() {
                    long doneTimeNanos = System.nanoTime();
                    System.out.printf("  done (%.4gs).\n", (doneTimeNanos-initTimeNanos)/1e9);
                    return true;  // keep going (done with subtask)
                }
            }
            );
        if (cancelledHolder[0]) {
            System.out.println("Cancelled!");
        } else {
            System.out.println("description = " + descr);
        }

        System.out.println("out main");
    } // main

} // class PolytopePuzzleDescription
