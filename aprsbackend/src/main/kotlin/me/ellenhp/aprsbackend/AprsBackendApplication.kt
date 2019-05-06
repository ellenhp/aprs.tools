package me.ellenhp.aprsbackend

import com.zaxxer.hikari.HikariConfig
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.main() {

    val config = environment.config.config("sql")
    val dbConnectionString = config.property("dbConnectionString").getString()
    val dbUser = config.property("dbUser").getString()
    val dbName = config.property("dbName").getString()

    val database = DatabaseLayer(dbConnectionString, dbName, dbUser, "password goes here")

    install(DefaultHeaders)
    install(Compression)

    routing {
        get("/") {
            val packets = database.getAllFrom("KI7UKU")
            call.respondText("I have ${packets.size} packets!",
                    contentType = io.ktor.http.ContentType.Text.Plain)
        }
    }
}
