/* -*- c++ -*-
FILE: MacroManager.h
RCS REVISION: $Revision: 1.12 $

COPYRIGHT: (c) 1999 -- 2003 Melinda Green, Don Hatch, and Jay Berkenbilt - Superliminal Software

LICENSE: Free to use and modify for non-commercial purposes as long as the
    following conditions are adhered to:
    1) Obvious credit for the source of this code and the designs it embodies
       are clearly made, and
    2) Ports and derived versions of 4D Magic Cube programs are not distributed
       without the express written permission of the authors.

DESCRIPTION:
    This class provides the primary interface for working with
    macros.  It keeps track of which macros are currently in use and
    serves as a proxy for communicating with the Macro class.  This
    is done so that additional state required when interacting with
    Macros can be stored in one place and not made the concern of the
    clients.
*/

#ifndef MACROMANAGER_H
#define MACROMANAGER_H

#include "MagicCube.h"
#include "Macro.h"
#include <stdio.h>

class Preferences;
class PolygonManager4D;

class MacroManager
{
public:
    enum mode_e { m_reading, m_writing };

    MacroManager(Preferences&, PolygonManager4D*);
    ~MacroManager();

    void* destroy(int which);
    void create(char *name, int nrefs, int refs[MAXREFS][4]);
    void setUIData(int which, void* ui_data);
    void* getUIData(int which);
    int findWithUIData(void* ui_data);

    // Convenience functions
    void setMacroName(int which, char *name);
    char *getMacroName(int i);
    void setMacroRefs(int which, int nrefs, int refs[MAXREFS][4]);

    // Returns true on success, false if reading and it doesn't exist
    // or the reference stickers don't match the macro.
    bool open(int which, int nrefs, int refs[MAXREFS][4],
              mode_e mode, int direction);
    void close();

    void setRefs(int which, int nrefs, int refs[MAXREFS][4]);

    // Return true on success, false if not opened properly.  Return
    // true and do nothing if no macro is open.
    bool addMove(struct stickerspec *sticker, int dir, int slicesmask);
    // Return true on success, false if at end of the macro
    bool getMove(struct stickerspec* grip, int* direction, int* slicesmask);

    void dump(FILE *fp);
    // Returns true on success
    bool read(FILE *fp);
    int getNMacros();

private:
    Preferences& prefs;
    PolygonManager4D* polymgr;

    int nmacros;
    int nmacros_allocated;
    Macro** the_macros;

    Macro* the_writing_macro;
    Macro* the_reading_macro;
    int the_reading_macro_direction;
    int the_reading_mat[4][4];
    int the_reading_mat_det;
};

#endif

// Local Variables:
// c-basic-offset: 4
// c-comment-only-line-offset: 0
// c-file-offsets: ((defun-block-intro . +) (block-open . 0) (substatement-open . 0) (statement-cont . +) (statement-case-open . +4) (arglist-intro . +) (arglist-close . +) (inline-open . 0))
// indent-tabs-mode: nil
// End:
