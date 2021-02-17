package com.example.ktor

import java.sql.DriverManager

class SQLite(fileName: String){
    private val connection = DriverManager.getConnection("jdbc:sqlite:$fileName")

    private fun execute(sql: String) {
        connection.createStatement().apply {
            executeUpdate(sql)
            close()
        }
    }

    fun drop(table: String) = execute("drop table if exists $table")

    fun create(table: String, fields: String, dropIfExists: Boolean = true) {
        if (dropIfExists) drop(table)
        execute("create table $table ($fields)")
    }

    fun insert(into: String, fields: String, values: String) {
        execute("insert into $into ($fields) values ($values)")
    }
    inner class insert(val into: String, val fields: String) {
        operator fun invoke(values: String): insert {
            execute("insert into $into ($fields) values ($values)")
            return this
        }
    }

    fun select(sql: String) : List<Map<String, String?>> {
        connection.createStatement().apply {
            val res = executeQuery(sql)
            val names = mutableListOf<String>()
            val cnt = res.metaData.columnCount
            for (i in 1..cnt)
                names.add(res.metaData.getColumnName(i))
            val list = mutableListOf<Map<String, String?>>()
            while (res.next()) {
                val map = mutableMapOf<String, String?>()
                for (i in 1..cnt)
                    map.put(names[i-1], res.getString(i))
                list.add(map)
            }
            close()
            return list
        }
    }

    fun select(sql: String, vararg params: Any?) : List<Map<String, String?>> {
        connection.prepareStatement(sql).apply {
            for ((i, value) in params.withIndex())
                setString(i+1, value.toString())
            val res = executeQuery()
            val names = mutableListOf<String>()
            val cnt = res.metaData.columnCount
            for (i in 1..cnt)
                names.add(res.metaData.getColumnName(i))
            val list = mutableListOf<Map<String, String?>>()
            while (res.next()) {
                val map = mutableMapOf<String, String?>()
                for (i in 1..cnt)
                    map.put(names[i-1], res.getString(i))
                list.add(map)
            }
            close()
            return list
        }
    }

    fun update(sql: String, vararg params: Any?) : Int {
        connection.prepareStatement(sql).apply {
            for ((i, value) in params.withIndex())
                setString(i+1, value.toString())
            return executeUpdate().also { close() }
        }
    }

    fun insert(sql: String, vararg params: Any?) : List<Long> {
        connection.prepareStatement(sql).apply {
            for ((i, value) in params.withIndex())
                setString(i+1, value.toString())
            executeUpdate()
            val gkeys = generatedKeys
            val keys = mutableListOf<Long>()
            while (gkeys.next()) keys.add(gkeys.getLong(1))
            close()
            return keys
        }
    }

    inner class transaction() {
        init { connection.autoCommit = false }
        fun commit() = connection.commit().also { connection.autoCommit = true }
        fun rollback() = connection.rollback().also { connection.autoCommit = true }
    }
}