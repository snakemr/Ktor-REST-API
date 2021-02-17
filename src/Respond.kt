package com.example.ktor

import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond

class Respond(val call: ApplicationCall, val db: SQLite) {
    var parameters: Array<String?>? = null

    fun get(vararg params: String): Respond {
        parameters = params.map { call.parameters[it] }.toTypedArray()
        return this
    }

    suspend fun post(vararg params: String): Respond {
        val p = call.receiveParameters()
        parameters = params.map { p[it] }.toTypedArray()
        return this
    }

    suspend fun select(sql: String) {
        val result = parameters?.let {
            db.select(sql, *it)
        } ?: db.select(sql)
        call.respond(result)
    }

    suspend fun insert(sql: String) {
        val result = parameters?.let {
            db.insert(sql, *it)
        } ?: db.insert(sql)
        call.respond(mapOf("rows" to result.size, "keys" to result))
    }

    suspend fun update(sql: String) {
        val result = parameters?.let {
            db.update(sql, *it)
        } ?: db.update(sql)
        call.respond(mapOf("rows" to result))
    }

    suspend inline fun <reified T : Any> json(): FromJSON<T> {
        return FromJSON(call.receive())
    }

    fun <T : Any> fromJson(parameters: T) = FromJSON(parameters)

    inner class FromJSON<T: Any>(val param: T) {
        private fun insertObj(sql: String, param: Any, result: MutableList<Long>) {
            val par = param.javaClass.declaredFields.map {
                it.isAccessible = true
                it.get(param)
            }.toTypedArray()
            result.addAll(db.insert(sql, *par))
        }

        suspend fun insert(sql: String) {
            val result = mutableListOf<Long>()
            if (param is Array<*>)
                db.transaction().apply {
                    for (p in param)
                        if (p!=null && p::class.isData)
                            insertObj(sql, p, result)
                        else
                            result.addAll(db.insert(sql, p))
                    commit()
                }
            else if (param::class.isData)
                insertObj(sql, param, result)
            call.respond(mapOf("rows" to result.size, "keys" to result))
        }

        private fun updateObj(sql: String, param: Any) : Int {
            val par = param.javaClass.declaredFields.map {
                it.isAccessible = true
                it.get(param)
            }.toTypedArray()
            return db.update(sql, *par)
        }

        suspend fun update(sql: String) {
            var result = 0
            if (param is Array<*>)
                db.transaction().apply {
                    for (p in param)
                        if (p!=null && p::class.isData)
                            result += updateObj(sql, p)
                        else
                            result += db.update(sql, p)
                    commit()
                }
            else if (param::class.isData)
                result = updateObj(sql, param)
            call.respond(mapOf("rows" to result))
        }

        suspend fun select(sql: String, or: String = "") {
            var sql2 = sql
            var param2 = arrayOf<Any>()
            if (param is Array<*>) {
                sql2 = sql.replace(or, param.map { or }.joinToString(") or (", "((", "))"))
                param2 = param.flatMap { par ->
                    if (par!=null && par::class.isData)
                        par.javaClass.declaredFields.map {
                            it.isAccessible = true
                            it.get(par)
                        }
                    else listOf(par)
                }.toTypedArray()
            }
            else if (param::class.isData) {
                param2 = param.javaClass.declaredFields.map {
                    it.isAccessible = true
                    it.get(param)
                }.toTypedArray()
            }
            val result = db.select(sql2, *param2)
            call.respond(result)
        }
    }
}