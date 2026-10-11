package org.podar.busattendant

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException

class ServerApi(private val base:String) {
    init {
        require(base.startsWith("https://", ignoreCase=true)) {
            "Secure HTTPS school server required. HTTP is disabled to protect student information."
        }
        require(URL(base).host.isNotBlank()) { "Invalid school server address" }
    }
    private var bearer:String=""
    fun restore(token:String){bearer=token}
    fun token():String=bearer
    fun hasSession()=bearer.isNotEmpty()
    private fun encode(s:String)=URLEncoder.encode(s,"UTF-8")
    private fun request(path:String,method:String="GET",payload:JSONObject?=null):JSONObject {
        val conn=(URL(base.trimEnd('/')+path).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod=method;conn.connectTimeout=6000;conn.readTimeout=10000
            conn.setRequestProperty("Accept","application/json")
            if(bearer.isNotEmpty())conn.setRequestProperty("Authorization","Bearer $bearer")
            if(payload!=null){conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");conn.outputStream.use{it.write(payload.toString().toByteArray(Charsets.UTF_8))}}
            val code=conn.responseCode
            val stream=if(code in 200..299)conn.inputStream else conn.errorStream
            val body=stream?.bufferedReader()?.use{it.readText()} ?: ""
            if(code !in 200..299)throw IOException("HTTP $code: ${body.take(180)}")
            return JSONObject(body)
        } finally {conn.disconnect()}
    }
    fun login(username:String,password:String):String {
        val j=request("/api/attendant/android/token-login","POST",JSONObject().put("username",username).put("password",password))
        if(!j.optBoolean("ok")||j.optString("access_token").isEmpty())throw IOException("Sign-in was not confirmed")
        bearer=j.getString("access_token")
        return j.getString("route")
    }
    fun logout():JSONObject=request("/api/attendant/android/logout","POST")
    fun bootstrap(day:String):JSONObject=request("/api/attendant/android/bootstrap?start_day=${encode(day)}&working_days=3")
    fun postEvent(e:EventRow,sequence:Long):JSONObject=request("/api/attendant/event","POST",JSONObject()
        .put("event_id",e.eventId).put("route",e.route).put("day",e.day).put("trip",e.trip)
        .put("student_id",e.studentId).put("client_sequence",sequence).put("status",e.status).put("reason",e.reason).put("created_at",e.createdAt))
    fun eventStatus(id:String):JSONObject=request("/api/attendant/event-status/${encode(id)}")
    fun effective(e:EventRow):JSONObject=request("/api/attendant/android/effective-attendance?day=${encode(e.day)}&trip=${encode(e.trip)}&student_id=${encode(e.studentId)}")
}
