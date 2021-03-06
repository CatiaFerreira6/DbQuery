package com.bingzer.android.dbv;

import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;

import com.bingzer.android.dbv.queries.Average;
import com.bingzer.android.dbv.queries.InsertInto;
import com.bingzer.android.dbv.queries.Max;
import com.bingzer.android.dbv.queries.Min;
import com.bingzer.android.dbv.queries.Sum;
import com.bingzer.android.dbv.queries.Total;

/**
 * Created by Ricky Tobing on 8/19/13.
 */
public class ViewTest extends AndroidTestCase {

    static boolean populated = false;
    IDatabase db;

    @Override
    public void setUp(){
        db = DbQuery.getDatabase("ViewTest");
        db.open(1, new SQLiteBuilder() {
            @Override
            public Context getContext() {
                return ViewTest.this.getContext();
            }

            @Override
            public void onModelCreate(IDatabase database, IDatabase.Modeling modeling) {
                modeling.add("Orders")
                        .addPrimaryKey("Id")
                        .add("Quantity", "INTEGER")
                        .add("ProductId", "INTEGER")
                        .add("CustomerId", "INTEGER");

                modeling.add("Products")
                        .addPrimaryKey("Id")
                        .add("Name", "TEXT")
                        .add("Price", "FLOAT");

                modeling.add("Customers")
                        .addPrimaryKey("Id")
                        .add("Name", "TEXT")
                        .add("Address", "TEXT");

                modeling.addView("CustomerView")
                        .as("SELECT * FROM Customers")
                        .ifNotExists();

                modeling.addView("OrderView")
                        .as("SELECT O.Id, O.Quantity, P.Name AS ProductName, P.Price AS ProductPrice, C.Name AS CustomerName")
                        .append("FROM Orders O")
                        .append("JOIN Products P ON P.Id = O.ProductId")
                        .append("JOIN Customers C ON C.Id = O.CustomerId")
                        .ifNotExists();

                modeling.addView("ViewToDrop")
                        .as("SELECT * FROM Products")
                        .ifNotExists();
            }
        });

        if(!populated){
            db.get("Orders").delete();
            db.get("Products").delete();
            db.get("Customers").delete();

            // initial value
            InsertInto insert = db.get("Customers").insertInto("Name", "Address");
            insert.val("Baloteli", "Italy");
            insert.val("Pirlo", "Italy");
            insert.val("Ronaldo", "Portugal");
            insert.val("Messi", "Argentina");

            insert = db.get("Products").insertInto("Name", "Price");
            insert.val("Computer", 600);
            insert.val("Smartphone", 450);
            insert.val("Car", 20000);
            insert.val("House", 500000);
            insert.val("Monitor", 120);

            insert = db.get("Orders").insertInto("Quantity", "ProductId", "CustomerId");
            insert.val(2, db.get("Products").selectId("Name = ?", "Computer"), db.get("Customers").selectId("Name = ?", "Baloteli"));
            insert.val(1, db.get("Products").selectId("Name = ?", "House"), db.get("Customers").selectId("Name = ?", "Ronaldo"));
            insert.val(5, db.get("Products").selectId("Name = ?", "Monitor"), db.get("Customers").selectId("Name = ?", "Messi"));
            insert.val(1, db.get("Products").selectId("Name = ?", "Computer"), db.get("Customers").selectId("Name = ?", "Messi"));
            populated = true;
        }
    }

    public void testTableNull(){
        assertNotNull(db.getView("CustomerView"));
        assertNotNull(db.getView("OrderView"));
        assertNull(db.getView("ProductView"));
    }

    public void testTableAliases(){
        IView view = db.getView("CustomerView C");
        assertNotNull(view);
        assertEquals(view.getName(), "CustomerView");
        assertEquals(view.getAlias(), "C");
        assertEquals(view.toString(), "CustomerView C");

        view = db.getView("OrderView O");
        assertNotNull(view);
        assertEquals(view.getName(), "OrderView");
        assertEquals(view.getAlias(), "O");
        assertEquals(view.toString(), "OrderView O");
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

    public void testSelect_Id(){
        Cursor c = db.getView("CustomerView").select("Name = ?", "Baloteli").columns("Id").query();
        assertEquals(c.getCount(), 1);
        c.moveToFirst();
        int id = c.getInt(0);
        c.close();

        ////
        c = db.getView("CustomerView").select(id).columns("Id", "Name").query();
        assertEquals(c.getCount(), 1);
        if(c.moveToNext()){
            assertEquals(c.getInt(0), id);
            assertEquals(c.getString(1), "Baloteli");
        }
        c.close();
    }

    public void testSelect_Top_Condition(){
        Cursor c = db.getView("OrderView").select(3, "CustomerName not null").query();
        assertTrue(c.getCount() == 3);
        if(c.moveToNext()) assertEquals(c.getString(c.getColumnIndex("CustomerName")), "Baloteli");
        if(c.moveToNext()) assertEquals(c.getString(c.getColumnIndex("CustomerName")), "Ronaldo");
        if(c.moveToNext()) assertEquals(c.getString(c.getColumnIndex("CustomerName")), "Messi");
        c.close();
    }

    public void testSelect_Top_WhereClause(){
        Cursor c = db.getView("OrderView").select(1, "CustomerName = ?", "Messi").query();
        assertTrue(c.getCount() == 1);
        while(c.moveToNext()){
            assertEquals(c.getString(c.getColumnIndex("CustomerName")), "Messi");
        }
        c.close();
    }

    public void testSelect_Ids(){
        long messiId = db.getView("CustomerView").selectId("Name = ?", "Messi");
        long pirloId = db.getView("CustomerView").selectId("Name = ?", "Pirlo");
        long[] ids = new long[]{messiId, pirloId};

        Cursor c = db.getView("CustomerView").select(ids).orderBy("Name").query();
        assertEquals(c.getCount(), 2);

        assertTrue(c.moveToNext());
        assertEquals(c.getInt(c.getColumnIndex("Id")), messiId);
        assertEquals(c.getString(c.getColumnIndex("Name")), "Messi");

        assertTrue(c.moveToNext());
        assertEquals(c.getInt(c.getColumnIndex("Id")), pirloId);
        assertEquals(c.getString(c.getColumnIndex("Name")), "Pirlo");

        c.close();
    }

    public void testSelect_Condition(){
        Cursor c = db.getView("OrderView").select("ProductName not null").query();
        assertEquals(c.getCount(), 4);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Computer");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "House");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Monitor");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Computer");

        c.close();
    }

    public void testSelect_WhereClause(){
        Cursor c = db.getView("OrderView").select("ProductName <> ?", "Computer").query();
        assertEquals(c.getCount(), 2);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "House");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Monitor");

        c.close();
    }

    public void testSelect_Distinct(){
        Cursor c = db.getView("OrderView").selectDistinct().columns("ProductName").query();
        assertEquals(c.getCount(), 3);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Computer");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "House");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Monitor");

        c.close();
    }

    public void testSelect_Distinct_Top(){
        Cursor c = db.getView("OrderView").selectDistinct(2).columns("ProductName").query();
        assertEquals(c.getCount(), 2);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Computer");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "House");

        c.close();
    }

    public void testSelect_Distinct_Top_Condition(){
        Cursor c = db.getView("OrderView")
                .selectDistinct(1, "ProductName <> 'Computer'")
                .columns("ProductName")
                .query();
        assertEquals(c.getCount(), 1);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "House");

        c.close();
    }

    public void testSelect_Distinct_Top_WhereClause(){
        Cursor c = db.getView("OrderView")
                .selectDistinct(1, "ProductName <> ?", "Computer")
                .columns("ProductName")
                .orderBy("ProductName DESC")
                .query();
        assertEquals(c.getCount(), 1);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Monitor");

        c.close();
    }


    public void testSelect_Distinct_Condition(){
        Cursor c = db.getView("OrderView")
                .selectDistinct("ProductName <> 'House'")
                .columns("ProductName")
                .orderBy("ProductName DESC")
                .query();
        assertEquals(c.getCount(), 2);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Monitor");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Computer");

        c.close();
    }

    public void testSelect_Distinct_WhereClause(){
        Cursor c = db.getView("OrderView")
                .selectDistinct("ProductName <> ?", "Monitor")
                .columns("ProductName")
                .query();
        assertEquals(c.getCount(), 2);

        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "Computer");
        assertTrue(c.moveToNext());
        assertEquals(c.getString(c.getColumnIndex("ProductName")), "House");

        c.close();
    }


    public void testSelect_Simple(){
        Cursor cursor = db.getView("CustomerView").select("Address = ?", "Italy").orderBy("Id").query();
        assertTrue(cursor.getCount() == 2);
        if(cursor.moveToNext()){
            assertTrue(cursor.getString(1).equals("Baloteli"));
            assertTrue(cursor.getString(2).equals("Italy"));
        }
        if(cursor.moveToNext()){
            assertTrue(cursor.getString(1).equals("Pirlo"));
            assertTrue(cursor.getString(2).equals("Italy"));
        }
        cursor.close();
    }

    public void testSelect_Simple_OrderBy(){
        Cursor cursor = db.getView("CustomerView").select("Address = ?", "Italy").orderBy("Name DESC").query();
        assertTrue(cursor.getCount() == 2);
        if(cursor.moveToNext()){
            assertTrue(cursor.getString(1).equals("Pirlo"));
            assertTrue(cursor.getString(2).equals("Italy"));
        }
        if(cursor.moveToNext()){
            assertTrue(cursor.getString(1).equals("Baloteli"));
            assertTrue(cursor.getString(2).equals("Italy"));
        }
        cursor.close();
    }

    public void testSelect_Complex(){
        Cursor cursor = db.getView("OrderView").select("CustomerName IN (?,?)", "Messi", "Ronaldo").orderBy("CustomerName").query();

        assertTrue(cursor.getCount() == 3);
        if(cursor.moveToNext()){
            assertEquals(cursor.getString(cursor.getColumnIndex("CustomerName")), ("Messi"));
            assertEquals(cursor.getString(cursor.getColumnIndex("ProductName")), ("Monitor"));
        }
        if(cursor.moveToNext()){
            assertEquals(cursor.getString(cursor.getColumnIndex("CustomerName")), ("Messi"));
            assertEquals(cursor.getString(cursor.getColumnIndex("ProductName")), ("Computer"));
        }
        if(cursor.moveToNext()){
            assertEquals(cursor.getString(cursor.getColumnIndex("CustomerName")), ("Ronaldo"));
            assertEquals(cursor.getString(cursor.getColumnIndex("ProductName")), ("House"));
        }
        cursor.close();
    }

    public void testSelectId_Condition(){
        Cursor cursor = db.getView("OrderView").select("CustomerName = ? AND ProductName = ?", "Ronaldo", "House").query();
        assertTrue(cursor.getCount() > 0);
        assertTrue(cursor.moveToNext());
        int id = cursor.getInt(cursor.getColumnIndex("O.Id"));
        cursor.close();

        assertTrue(id > 0);

        // select id
        assertEquals(id, db.getView("OrderView").selectId("CustomerName = 'Ronaldo' AND ProductName = 'House'"));
    }

    public void testSelectId_WhereClause(){
        Cursor cursor = db.getView("OrderView").select("CustomerName = ? AND ProductName = ?", "Ronaldo", "House").query();
        assertTrue(cursor.getCount() > 0);
        assertTrue(cursor.moveToNext());
        int id = cursor.getInt(cursor.getColumnIndex("O.Id"));
        cursor.close();

        assertTrue(id > 0);

        // select id
        assertEquals(id, db.getView("OrderView").selectId("CustomerName = ? AND ProductName = ?", "Ronaldo", "House"));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    public void testCount(){
        assertEquals(db.getView("OrderView").count(), 4);
        assertEquals(db.getView("CustomerView").count(), 4);
    }

    public void testCount_Condition(){
        assertEquals(db.getView("OrderView").count("CustomerName = 'Messi'"), 2);
        assertEquals(db.getView("CustomerView").count("Name IN ('Messi','Baloteli','Pirlo')"), 3);
    }

    public void testCount_WhereClause(){
        assertEquals(db.getView("OrderView").count("CustomerName = ? OR CustomerName = ?", "Messi", "Baloteli"), 3);
        assertEquals(db.getView("CustomerView").count("Name = ?", "Ronaldo"), 1);
    }

    public void testHas_Id(){
        long id = db.getView("OrderView").selectId("CustomerName = ?", "Ronaldo");
        assertTrue(id > 0);
        assertTrue(db.getView("OrderView").has(id));
    }

    public void testHas_Condition(){
        assertTrue(db.getView("OrderView").has("CustomerName is not null And ProductName not null"));
        assertTrue(db.getView("CustomerView").has("Name = 'Ronaldo'"));
    }

    public void testHas_WhereClause(){
        assertTrue(db.getView("OrderView").has("CustomerName = ?", "Baloteli"));
        assertTrue(db.getView("CustomerView").has("Name is not null OR Name = ?", "Ronaldo"));
    }

    public void testDrop(){
        if(db.getView("ViewToDrop") != null){
            assertTrue(db.getView("ViewToDrop").drop().query());
            assertNull(db.getView("ViewToDrop"));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // raw
    public void testRaw(){
        Cursor c = db.getView("CustomerView").raw("SELECT * FROM Customers").query();
        assertTrue(c.getCount() > 0);
        c.close();
    }

    public void testRaw_Args(){
        Cursor c = db.getView("CustomerView").raw("SELECT * FROM Customers WHERE Name = ?", "Messi").query();
        assertTrue(c.getCount() > 0);
        c.close();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    // functiosn
    public void testAvg(){
        Average avg = db.getView("OrderView").avg("ProductPrice");
        assertEquals(125330, (Double) avg.value(), 0.01);

        avg = db.get("OrderView").avg("ProductPrice", "ProductName = 'Computer'");
        assertEquals(600, (Double) avg.value(), 0.1);

        avg = db.get("OrderView").avg("ProductPrice", "ProductName = ?", "Computer");
        assertEquals(600, (Double)avg.value(), 0.1);
    }

    public void testSum(){
        Sum sum = db.getView("OrderView").sum("ProductPrice");
        assertEquals(501320.0, (Double) sum.value(), 0.01);

        sum = db.get("OrderView").sum("ProductPrice", "ProductName IN ('Car', 'Computer')");
        assertEquals(1200.0, (Double) sum.value(), 0.1);

        sum = db.get("OrderView").sum("ProductPrice", "ProductName IN (?,?)", "Car", "Computer");
        assertEquals(1200.0, (Double) sum.value(), 0.1);
    }

    public void testTotal(){
        Total total = db.getView("OrderView").total("ProductPrice");
        assertEquals(501320.0, (Double) total.value(), 0.1);

        total = db.get("OrderView").total("ProductPrice", "ProductName IN ('Car', 'Computer')");
        assertEquals(1200.0, (Double) total.value(), 0.1);

        total = db.get("OrderView").total("ProductPrice", "ProductName IN (?,?)", "Car", "Computer");
        assertEquals(1200.0, (Double) total.value(), 0.1);
    }

    public void testMax(){
        Max max = db.getView("OrderView").max("ProductPrice");
        assertEquals(500000.0, (Double) max.value(), 0.1);

        max = db.getView("OrderView").max("ProductPrice", "ProductName Like 'M%' Or ProductName Like 'C%'");
        assertEquals(600.0, (Double) max.value(), 0.1);

        max = db.getView("OrderView").max("ProductPrice", "ProductName Like ? Or ProductName Like ?", "M%", "C%");
        assertEquals(600.0, (Double) max.value(), 0.1);
    }

    public void testMin(){
        Min min = db.getView("OrderView").min("ProductPrice");
        assertEquals(120.0,(Double) min.value(), 0.1);

        min = db.getView("OrderView").min("ProductPrice", "ProductPrice > 450");
        assertEquals(600.0, (Double) min.value(), 0.1);

        min = db.getView("OrderView").min("ProductPrice", "ProductPrice > ?", 450);
        assertEquals(600.0, (Double) min.value(), 0.1);
    }
}
