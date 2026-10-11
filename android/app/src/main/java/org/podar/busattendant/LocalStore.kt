package org.podar.busattendant

import androidx.room.*

@Entity(tableName="trips", primaryKeys=["route","day","trip"])
data class TripRow(val route:String, val day:String, val trip:String, val scheduleVersion:Int, val downloadedAt:String)

@Entity(tableName="students", primaryKeys=["route","day","trip","studentId"])
data class StudentRow(val route:String,val day:String,val trip:String,val studentId:String,val name:String,val grade:String,val stop:String,val nameKn:String,val stopKn:String)

@Entity(tableName="outbox", indices=[Index(value=["route","day","trip","studentId"])])
data class EventRow(@PrimaryKey val eventId:String,val route:String,val day:String,val trip:String,val studentId:String,val status:String,val reason:String,val createdAt:String,val state:String="PENDING",val detail:String="")

@Dao interface AttendantDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) fun putTrip(t:TripRow)
    @Insert(onConflict=OnConflictStrategy.REPLACE) fun putStudents(rows:List<StudentRow>)
    @Query("DELETE FROM students WHERE route=:route AND day=:day AND trip=:trip") fun deleteStudents(route:String,day:String,trip:String)
    @Query("SELECT * FROM trips WHERE route=:route ORDER BY day, CASE trip WHEN 'Morning Pickup' THEN 1 WHEN '12:30 Drop' THEN 2 ELSE 3 END") fun trips(route:String):List<TripRow>
    @Query("SELECT * FROM students WHERE route=:route AND day=:day AND trip=:trip ORDER BY name") fun students(route:String,day:String,trip:String):List<StudentRow>
    @Insert(onConflict=OnConflictStrategy.ABORT) fun addEvent(e:EventRow)
    @Query("SELECT * FROM outbox WHERE route=:route AND day=:day AND trip=:trip AND studentId=:studentId ORDER BY rowid DESC LIMIT 1") fun latest(route:String,day:String,trip:String,studentId:String):EventRow?
    @Query("SELECT rowid FROM outbox WHERE eventId=:id") fun localSequence(id:String):Long
    @Query("SELECT * FROM outbox WHERE state IN ('PENDING','RETRY') ORDER BY rowid") fun pending():List<EventRow>
    @Query("SELECT count(*) FROM outbox WHERE state IN ('PENDING','RETRY')") fun pendingCount():Int
    @Query("SELECT count(*) FROM outbox WHERE state='REVIEW'") fun reviewCount():Int
    @Query("UPDATE outbox SET state=:state,detail=:detail WHERE eventId=:id") fun mark(id:String,state:String,detail:String)
    @Query("SELECT count(*) FROM outbox WHERE route=:route AND day=:day AND trip=:trip AND studentId=:studentId AND eventId<>:eventId AND state IN ('PENDING','RETRY')") fun newerUnsent(route:String,day:String,trip:String,studentId:String,eventId:String):Int
    @Query("SELECT count(*) FROM outbox WHERE route=:route AND day=:day AND trip=:trip AND state IN ('PENDING','RETRY','REVIEW')") fun protectedCount(route:String,day:String,trip:String):Int
}

@Database(entities=[TripRow::class,StudentRow::class,EventRow::class],version=1,exportSchema=false)
abstract class LocalStore:RoomDatabase(){ abstract fun dao():AttendantDao }
