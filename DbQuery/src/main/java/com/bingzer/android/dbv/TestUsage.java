/**
 * Copyright 2013 Ricky Tobing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance insert the License.
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

package com.bingzer.android.dbv;

import android.content.Context;

import com.bingzer.android.dbv.sqlite.Database;
import com.bingzer.android.dbv.sqlite.SQLiteBuilder;

/**
 * This will be removed
 * Created by Ricky Tobing on 7/16/13.
 */
class TestUsage {

    void init(){

        int version = 0;

        IDatabase db = DbQuery.getDatabase("Test");
        db.create(version, new SQLiteBuilder() {
            @Override
            public Context getContext() {
                return null;
            }

            @Override
            public void onModelCreate(IDatabase.Modeling modeling) {
                modeling.add("Table1")
                        .add("Column1", "INTEGER", "primary key autoincrement not null")
                        .add("Column2", "TEXT");

            }
        });

        //db.get("")

        IQuery.Select s = db.get("").select();

    }


}












