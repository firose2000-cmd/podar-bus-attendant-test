package org.podar.busattendant

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.room.Room
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity:Activity() {
    private lateinit var db:LocalStore
    private val worker=Executors.newSingleThreadExecutor()
    private lateinit var root:LinearLayout
    private lateinit var api:ServerApi
    private var route=""
    private var selected:TripRow?=null
    private val prefs by lazy{getSharedPreferences("device",MODE_PRIVATE)}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        try {
            SQLiteDatabase.loadLibs(applicationContext)
            val passphrase = EncryptedDbKey.passphrase(applicationContext)
            db = Room.databaseBuilder(applicationContext, LocalStore::class.java, "attendant_v15_encrypted.db")
                .openHelperFactory(SupportFactory(passphrase))
                .build()
        } catch (ex: Exception) {
            // Fail closed: never fall back to an unencrypted database or erase unsynced events.
            setContentView(TextView(this).apply { text = "Offline attendance database locked. Do not clear app data. Contact administrator.\n${ex.message}" })
            return
        }
        route=prefs.getString("route","") ?: ""
        api=ServerApi(prefs.getString("server","https://school-bus-server.example.invalid")!!)
        try { SecureSession.read(this)?.let { api.restore(it) } } catch(ex:Exception) { android.util.Log.w("BusAttendant","Stored login unavailable",ex) }
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,18,18,18)}
        setContentView(ScrollView(this).apply{addView(root)})
        home()
    }
    private fun label(s:String){root.addView(TextView(this).apply{text=s;textSize=17f;setPadding(0,12,0,10)})}
    private fun button(s:String,action:()->Unit){root.addView(Button(this).apply{text=s;setOnClickListener{action()}})}
    private fun edit(hint:String,secret:Boolean=false):EditText=EditText(this).apply{this.hint=hint;if(secret)inputType=129;root.addView(this)}
    private fun clear(){root.removeAllViews()}
    private fun job(work:()->Unit){worker.execute{try{work()}catch(e:Exception){notice("Error: ${e.message}")}}}
    private fun notice(msg:String){runOnUiThread{Toast.makeText(this,msg,Toast.LENGTH_LONG).show()}}
    private fun home(){clear();label("Podar Bus Attendant — V2.8 TEST")
        label("Offline-first · Local records survive app and phone restarts")
        if(route.isNotBlank()){
            label("Saved route: $route")
            button("Open downloaded trips (works offline)"){showTrips()}
            button("Synchronize pending attendance"){sync()}
        }
        button("Sign in / Download next 3 working days"){loginPage()}
        job{val p=db.dao().pendingCount();val r=db.dao().reviewCount();runOnUiThread{
            // Do not append stale counts to a different screen after navigation.
            if (root.childCount > 0 && (root.getChildAt(0) as? TextView)?.text?.contains("V2.8 TEST") == true)
                label("Pending: $p · Needs review: $r")
        }}
    }
    private fun loginPage(){clear();label("Connect to school Wi-Fi for sign-in and download")
        val server=edit("School server address").apply{setText(prefs.getString("server","https://school-bus-server.example.invalid"))}
        val user=edit("Attendant username");val pass=edit("Password",true)
        button("Sign in and download"){val address=server.text.toString().trim();val u=user.text.toString().trim();val p=pass.text.toString()
            job{
                val next=ServerApi(address);val newRoute=next.login(u,p)
                // Keep any unsent or review events attached to their original route.
                val old=route
                if(old.isNotBlank()&&old!=newRoute&&(db.dao().pendingCount()>0||db.dao().reviewCount()>0))
                    throw IllegalStateException("Unresolved attendance exists for $old. Complete reconciliation before switching routes.")
                SecureSession.save(applicationContext,next.token())
                api=next;route=newRoute
                prefs.edit().putString("route",route).putString("server",address).apply()
                download()
            }
        }
        button("Back"){home()}
    }
    private fun download(){
        val json=api.bootstrap(LocalDate.now().toString())
        if(json.getString("route")!=route)throw IllegalStateException("Server route mismatch")
        if (!json.optBoolean("complete")) throw IllegalStateException("Server could not prepare all requested working days; existing offline trips preserved")
        val days=json.getJSONArray("working_days")
        if (days.length() != 3) throw IllegalStateException("Expected 3 complete working days; existing manifests preserved")
        // Parse the entire response before changing local data.
        data class Batch(val trip:TripRow,val students:List<StudentRow>)
        val batches=mutableListOf<Batch>()
        for(i in 0 until days.length()){
            val d=days.getJSONObject(i);val day=d.getString("day");val version=d.optInt("schedule_version")
            val trips=d.getJSONArray("trips")
            if (trips.length() == 0) throw IllegalStateException("Empty trip manifest for $day; existing manifests preserved")
            for(j in 0 until trips.length()){
                val t=trips.getJSONObject(j);val trip=t.getString("trip");val arr=t.getJSONArray("students")
                val students=mutableListOf<StudentRow>()
                for(k in 0 until arr.length()){
                    val s=arr.getJSONObject(k)
                    students.add(StudentRow(route,day,trip,s.getString("student_id"),s.optString("name"),s.optString("grade"),s.optString("stop"),s.optString("name_kn"),s.optString("stop_kn")))
                }
                batches.add(Batch(TripRow(route,day,trip,version,LocalDateTime.now().toString()),students))
            }
        }
        db.runInTransaction{
            for(b in batches){
                // Never replace a manifest with pending attendance, as students could disappear.
                if(db.dao().protectedCount(route,b.trip.day,b.trip.trip)>0)continue
                db.dao().deleteStudents(route,b.trip.day,b.trip.trip)
                db.dao().putTrip(b.trip);db.dao().putStudents(b.students)
            }
        }
        notice("Saved ${batches.size} trip manifests for offline use")
        runOnUiThread{showTrips()}
    }
    private fun showTrips(){clear();label("Saved trips · $route")
        job{val trips=db.dao().trips(route);runOnUiThread{
            if(trips.isEmpty())label("No saved manifests. Sign in on school Wi-Fi first.")
            trips.forEach{t->button("${t.day} · ${t.trip} · Schedule v${t.scheduleVersion}"){showStudents(t)}}
        }}
        button("Sync pending"){sync()};button("Home"){home()}
    }
    private fun showStudents(t:TripRow){selected=t;clear();label("${t.day} · ${t.trip} · $route")
        label("Every selection is saved immediately on this phone.")
        job{val students=db.dao().students(route,t.day,t.trip)
            val statuses=students.associate{it.studentId to db.dao().latest(route,t.day,t.trip,it.studentId)}
            runOnUiThread{
                students.forEach{s->
                    label("${s.name} · ${s.grade} · ${s.stop}\n${s.nameKn}")
                    val latest=statuses[s.studentId];if(latest!=null)label("Saved: ${latest.status} · ${latest.state}")
                    val options=if(t.trip=="Morning Pickup")listOf("On Bus","Not On Bus") else listOf("Boarded Bus","Dropped","Not On Bus")
                    val spinner=Spinner(this);spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options);root.addView(spinner)
                    val reason=Spinner(this);val reasons=listOf("Absent — Parent Informed","Absent — Not Informed","Sick","On Leave","Own Transport","Late / Missed Bus","School Activity / Not Travelling","Transport Discontinued","Other — Supervisor Review")
                    reason.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,reasons);root.addView(reason)
                    button("Save attendance for ${s.name}"){
                        val status=spinner.selectedItem.toString();val why=if(status=="Not On Bus")reason.selectedItem.toString() else ""
                        val e=EventRow(UUID.randomUUID().toString(),route,t.day,t.trip,s.studentId,status,why,LocalDateTime.now().toString())
                        job{db.dao().addEvent(e);notice("Saved offline: ${s.name} — $status");runOnUiThread{showStudents(t)}}
                    }
                }
            }
        }
        button("Back to trips"){showTrips()}
    }
    private fun sync(){if(!api.hasSession()){notice("Sign in on school Wi-Fi first. Offline records are safe.");return}
        job{
            // Snapshot the durable outbox in submission order. Earlier selections must
            // still reach the server for audit, but only the final selection for a
            // student/trip may be reported as effective attendance.
            val pending=db.dao().pending()
            val lastByStudent=pending.withIndex().associate { indexed ->
                val e=indexed.value
                listOf(e.route,e.day,e.trip,e.studentId).joinToString("|") to indexed.index
            }
            var confirmed=0;var review=0;var superseded=0
            for((position,e) in pending.withIndex()){
                try{
                    val ack=api.postEvent(e,db.dao().localSequence(e.eventId))
                    if(!ack.optBoolean("ok")||ack.optString("event_id")!=e.eventId)throw IllegalStateException("Receipt mismatch")
                    val receipt=api.eventStatus(e.eventId)
                    if(!receipt.optBoolean("saved")||receipt.optString("status")!=e.status||receipt.optString("student_id")!=e.studentId)throw IllegalStateException("Stored event mismatch")
                    val key=listOf(e.route,e.day,e.trip,e.studentId).joinToString("|")
                    if(lastByStudent[key]!=position){
                        // Server receipt verified, but this event is not the final
                        // locally recorded choice. Never claim effective confirmation.
                        db.dao().mark(e.eventId,"SUPERSEDED","Stored on server; later local selection awaits verification")
                        superseded++
                        continue
                    }
                    val effective=api.effective(e)
                    val expected=mapOf("On Bus" to "ON_BUS","Not On Bus" to "NOT_ON_BUS","Boarded Bus" to "BOARDED","Dropped" to "DROPPED")[e.status]
                    if(effective.optBoolean("found")&&effective.optString("status")==expected){
                        db.dao().mark(e.eventId,"CONFIRMED","Server receipt and effective status verified");confirmed++
                    } else {
                        db.dao().mark(e.eventId,"REVIEW","Server effective status differs from latest local selection");review++
                    }
                }catch(ex:Exception){
                    db.dao().mark(e.eventId,"RETRY",ex.message.orEmpty().take(180))
                    notice("Sync paused; unsent events retained. ${ex.message}")
                    break
                }
            }
            notice("Sync: $confirmed verified, $superseded superseded, $review need review")
            runOnUiThread{home()}
        }
    }
}
