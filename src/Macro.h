/* -*- c++ -*-
FILE: Macro.h
RCS REVISION: $Revision: 1.18 $

COPYRIGHT: (c) 1999 -- 2003 Melinda Green, Don Hatch, and Jay Berkenbilt - Superliminal Software

LICENSE: Free to use and modify for non-commercial purposes as long as the
    following conditions are adhered to:
    1) Obvious credit for the source of this code and the designs it embodies
       are clearly made, and
    2) Ports and derived versions of 4D Magic Cube programs are not distributed
       without the express written permission of the authors.

DESCRIPTION:
    This class implements the Macro itself.  Most interaction with
    Macros is done through the MacroManager class.
*/

#ifndef MACRO_H
#define MACRO_H

#include "MagicCube.h"
#include <stdio.h>

class History;
class MacroManager;
class Preferences;
class PolygonManager4D;

// MAXREFS would be better as a member of Macro, but this seems to
// cause compilation problems on some platforms.
const int MAXREFS = 3;

class Macro
{
public:


    Macro(char* name, int nrefs, int refs[MAXREFS][4],
          Preferences& prefs, PolygonManager4D* polymgr);
    ~Macro();

    void setUIData(void* ui_data);
    void* getUIData();
    void setName(char* name);
    char* getName();
    void setRefs(int nrefs, int refs[MAXREFS][4]);

    void addMove(struct stickerspec* sticker, int direction, int slicesmask);
    // Returns true on success, false if at end of the macro
    bool getMove(int dir_in, int det, int mat[4][4],
                 struct stickerspec* grip,
                 int* dir_out, int* slicesmask);

    void goToBeginning();
    void goToEnd();

    // Returns true if successful
    bool get4dMatThatRotatesThese3ToMy3(int ref0[4], int ref1[4], int ref2[4],
                                        int mat[4][4]);

    void dump(FILE* fp);
    bool readHistory(FILE* fp);

private:
    char *name;
    int nrefs;                  // number of reference stickerspec
    int refs[MAXREFS][NDIMS];
    PolygonManager4D* polymgr;
    History* moves;
    void* ui_data;
};

#endif

// Local Variables:
// c-basic-offset: 4
// c-comment-only-line-offset: 0
// c-file-offsets: ((defun-block-intro . +) (block-open . 0) (substatement-open . 0) (statement-cont . +) (statement-case-open . +4) (arglist-intro . +) (arglist-close . +) (inline-open . 0))
// indent-tabs-mode: nil
// End:
