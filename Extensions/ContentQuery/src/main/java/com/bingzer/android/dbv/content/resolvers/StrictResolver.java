/**
 * Copyright 2014 Ricky Tobing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bingzer.android.dbv.content.resolvers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.bingzer.android.dbv.IEntity;
import com.bingzer.android.dbv.IEntityList;
import com.bingzer.android.dbv.content.IStrictResolver;
import com.bingzer.android.dbv.content.queries.Config;
import com.bingzer.android.dbv.content.queries.StrictSelectImpl;
import com.bingzer.android.dbv.utils.EntityUtils;

/**
 * Created by Ricky Tobing on 8/23/13.
 */
public final class StrictResolver extends BaseResolver implements IStrictResolver {

    public StrictResolver(Config config, Uri uri, Context context){
        super(config, uri, context);
    }

    @Override
    public long selectId(String whereClause, Object... args) {
        long id = -1;
        Cursor cursor = null;
        try{
            cursor = select(whereClause, args).query();
            if(cursor.moveToNext()){
                int index = cursor.getColumnIndex(getPrimaryKeyColumn());
                id = cursor.getLong(index);
            }
        }
        finally {
            if(cursor != null) cursor.close();
        }

        return id;
    }

    @Override
    public boolean has(String whereClause, Object... whereArgs) {
        Cursor cursor = null;
        try{
            cursor = select(whereClause, whereArgs).columns(getPrimaryKeyColumn()).query();
            return cursor.getCount() > 0;
        }
        finally {
            if(cursor != null) cursor.close();
        }
    }

    @Override
    public int count(String whereClause, Object... whereArgs) {
        int count = -1;
        Cursor cursor = null;
        try{
            cursor = select(whereClause, whereArgs).columns(getPrimaryKeyColumn()).query();
            count = cursor.getCount();
        }
        finally {
            if(cursor != null)
                cursor.close();
        }

        return count;
    }

    ///////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Select select(long id) {
        return select(generateParamId(id));
    }

    @Override
    public Select select(String condition) {
        return select(condition, (Object)null);
    }

    @Override
    public Select select(long... ids) {
        if(ids != null && ids.length > 0){
            StringBuilder whereClause = new StringBuilder();
            whereClause.append(getPrimaryKeyColumn()).append(" ");
            whereClause.append(" IN (");
            for(int i = 0; i < ids.length; i++){
                whereClause.append(ids[i]);
                if(i < ids.length - 1){
                    whereClause.append(",");
                }
            }
            whereClause.append(")");

            return select(whereClause.toString());
        }
        else{
            // select all
            return select((String)null);
        }
    }

    @Override
    public Select select(String whereClause, Object... args) {
        return new StrictSelectImpl(this) {
            @Override
            public Cursor query() {
                String[] projections = getProjections();
                String selection = getSelection();
                String[] selectionArgs = getSelectionArgs();
                String sortOrder = getSortingOrder();
                return contentResolver.query(uri, projections, selection, selectionArgs, sortOrder);
            }

            @Override
            public void query(IEntity entity) {
                final Cursor cursor = query();
                if(cursor.moveToNext()){
                    EntityUtils.mapEntityFromCursor(StrictResolver.this, entity, cursor);
                }

                cursor.close();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <E extends IEntity> void query(IEntityList<E> entityList) {
                final Cursor cursor = query();

                EntityUtils.mapEntityListFromCursor(StrictResolver.this, entityList, cursor);

                cursor.close();
            }
        }.where(whereClause, args);
    }
}
