package com.example.ktor

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class User(val name: String, val pass: String)
data class UserR(val pass: String, val name: String)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    val db = SQLite("base.sqlite")

    install(ContentNegotiation) {
        gson {
        }
    }

    routing {

        get("/") {
            call.respondText("""
                GET /users
                GET /user?id=1
                POST /users2 : JSON [id?, id?, id?]
                POST /users3 : JSON [{name=?, pass=?}, {name=?, pass=?}]
                POST /login : JSON {name=?, pass=?}
                POST /adduser : name=?
                POST /adduser2 : JSON {name=?, pass=?}
                POST /addusers : JSON [name?, name?, name?]
                POST /addusers2 : JSON [{name=?, pass=?}, {name=?, pass=?}]
                POST /edituser : id=?, name=?
                POST /passwd : JSON {name=?, pass=?}
                POST /deluser : id=?
                POST /delusers : JSON [name?, name?, name?]
            """.trimIndent(), contentType = ContentType.Text.Plain)
        }

        get("/users") {
            Respond(call, db).select("select * from users")
        }

        get("/user") {
            Respond(call, db).get("id").select("select * from users where id=?")
        }

        post("/users2") {
            Respond(call, db).json<Array<String>>().select("select * from users where id=?", "id=?")
        }

        post("/users3") {
            Respond(call, db).json<Array<User>>().select("select * from users where name=? and pass=?", "name=? and pass=?")
        }

        post("/login") {
            Respond(call, db).json<User>().select("select * from users where name=? and pass=?")
        }

        post("/adduser") {
            Respond(call, db).post("name").insert("insert into users (name) values (?)")
        }

        post("/adduser2") {
            Respond(call, db).json<User>().insert("insert into users (name, pass) values (?,?)")
        }

        post("/addusers") {
            Respond(call, db).json<Array<String>>().insert("insert into users (name) values (?)")
        }

        post("/addusers2") {
            Respond(call, db).json<Array<User>>().insert("insert into users (name, pass) values (?,?)")
        }

        post("/edituser") {
            Respond(call, db).post("name", "id").update("update users set name=? where id=?")
        }

        post("/passwd") {
            Respond(call, db).json<UserR>().update("update users set pass=? where name=?")
        }

        post("/deluser") {
            Respond(call, db).post("id").update("delete from users where id=?")
        }

        post("/delusers") {
            Respond(call, db).json<Array<String>>().update("delete from users where name=?")
        }

        get("/reset") {
            db.create("users", "id integer primary key autoincrement, name text, pass text")
            db.insert("users", "id, name")("1, 'Andrew'")("2, 'Андрей'")
            call.respondText("Data Reset - OK")
        }
    }
}

