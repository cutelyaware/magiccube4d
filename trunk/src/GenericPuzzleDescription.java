/**
 * Description of a generic Rubik's-cube-like puzzle,
 * in any number of dimensions
 * (although the grip concept probably doesn't make sense in higher
 * than 4 dimensions).
 *
 * Note, even derived classes will carry no scrambled state!
 * NOTE: All returned arrays are immutable!!!!!
 * (They would be const if java had a way to express that.)
 */
interface GenericPuzzleDescription {
	
    public String getSchlafliProduct();
    public double getEdgeLength();
    public int nDims();
    public int nVerts();
    public int nFaces();
    public int nCubies();
    public int nStickers();
    public int nGrips();
    public float circumRadius(); // distance of farthest vertex from origin
    public float inRadius();     // distance of closest face hyperplane to origin

    /**
    * Get the vertices of the geometry that gets drawn
    * (or picked when selecting a sticker rather than a grip) at rest.
    */
    public void computeStickerVertsAtRest(float verts[/*nVerts*/][/*nDims*/],
                                          float faceShrink,
                                          float stickerShrink);
    /**
    * Get the indices (into the vertices returned by getDrawVertsAtRest()
    * or getDrawVertsPartiallyTwisted())
    * of the polygons which make up the stickers.
    * If 4-dimensional,
    * it is guaranteed that the following form a non-degenerate
    * simplex, the sign of whose projected volume can be tested
    * for inside-out cell culling:
    *       verts[inds[iSticker][0][0]]
    *       verts[inds[iSticker][0][1]]
    *       verts[inds[iSticker][0][2]]
    *       verts[inds[iSticker][1][0]]
    */
    public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/]
        getStickerInds();

    /**
    * Get the vertices of the geometry to be picked
    * when selecting a grip for twisting.
    */
    public void computeGripVertsAtRest(float verts[/*nVerts*/][/*nDims*/],
                                       float faceShrink,
                                       float stickerShrink);
    /**
    * Get the indices (into the vertices returned by getPickVertsAtRest())
    * of the geometry to be picked when selecting a grip for twisting.
    * XXX argh, this won't work, because culling needs to be done
    * XXX on the stickers.
    * XXX so the caller needs to be able to associate the grip geometry
    * XXX with corresponding sticker geometry.  Maybe gripPolygon2Sticker?
    * XXX No, I think the way to do it is... use the sticker
    * XXX geometry for picking.  If it needs to be subdivided for picking,
    * XXX then that's the way it will be drawn.
    * XXX and then have a stickerPolygon2Grip array.
    * XXX Bleah, maybe that will show cracks :-(
    */
    public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/]
        getGripInds();

    /**
    * Get the rotational symmetry order for each grip.
    * The twist angle associated with each grip
    * will be 2*pi/order.
    */
    public int[/*nGrips*/]
        getGripSymmetryOrders();

    /**
    * XXX floundering here... closest in what sense? normalized vectors on a sphere?
    * The is2x2x2x2Cell is used to control only looking at a subset of the grips.
    */
    public int getClosestGrip(float pickCoords[/*4*/], int faceIndex, boolean is2x2x2Cell);

    /**
    * Get the vertices of the geometry that gets drawn
    * partway through a twist.
    * Frac is the fraction of the total angle (which is not the same
    * as the fraction of the total time, if a smoothing function is used).
    */
    public void
        computeStickerVertsPartiallyTwisted(float verts[/*nVerts*/][/*nDims*/],
                                            float faceShrink,
                                            float stickerShrink,
                                            int gripIndex,
                                            int dir,
                                            int slicemask,
                                            float frac);

    /**
    * Get a nearby point that would be a nice point to rotate to the center.
    * Nice points are vertices, edge centers, cell centers, ...
    * XXX closest in what sense? do we normalize one or the other? should we? would be the consequences?
    */
    public float[/*nDims*/] getClosestNicePointToRotateToCenter(float point[]);

    /**
    * Get a table mapping sticker index to face index.
    * !!!A COPY OF!!! resulting array can also be used as the initial puzzle state,
    */
    public int[/*nStickers*/] getSticker2Face();
    /**
    * Get a table mapping sticker index to cubie.
    * This can be used to highlight all the stickers on a given cubie.
    */
    public int[/*nStickers*/] getSticker2Cubie();

    /**
    * Get a table mapping face to opposite face (if any).
    */
    public int[/*nFaces*/] getFace2OppositeFace();

    /**
    * Get a table mapping grip index to face index.
    */
    public int[/*nGrips*/] getGrip2Face();

    /**
     * Get a particular face center.
     */
    public float[/*nDims*/] getFaceCenter( int faceIndex );

    /**
     * Get the coordinates for a particular grip.
     */
    public float[/*nDims*/] getGripCoords( int gripIndex );
    
    /**
    * Apply a move to an array of colors (face indices)
    * representing the current puzzle state.
    */
    public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                int gripIndex,
                                                int dir,
                                                int slicemask);
} // interface GenericPuzzleDescription
