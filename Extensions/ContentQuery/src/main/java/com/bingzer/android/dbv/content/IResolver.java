/**
 * Copyright 2013 Ricky Tobing
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
package com.bingzer.android.dbv.content;

import com.bingzer.android.dbv.IConfig;
import com.bingzer.android.dbv.queries.Countable;
import com.bingzer.android.dbv.queries.Deletable;
import com.bingzer.android.dbv.queries.Insertable;
import com.bingzer.android.dbv.queries.Selectable;
import com.bingzer.android.dbv.queries.Tangible;
import com.bingzer.android.dbv.queries.Updatable;

/**
 * Created by Ricky Tobing on 8/20/13.
 */
public interface IResolver
        extends Selectable, Insertable, Updatable, Deletable /*,Tangible , Countable*/{

    IConfig getConfig();

    void setReturnedColumns(String... columns);
}
