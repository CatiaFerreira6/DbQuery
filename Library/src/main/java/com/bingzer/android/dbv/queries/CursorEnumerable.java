package com.bingzer.android.dbv.queries;

import android.database.Cursor;

import com.bingzer.android.dbv.IEnumerable;

public interface CursorEnumerable {

    void query(IEnumerable<Cursor> cursor);

}
