package me.ellenhp.aprsbackend

import com.google.cloud.kms.v1.KeyManagementServiceClient
import com.google.cloud.kms.v1.KeyManagementServiceSettings
import com.google.protobuf.ByteString
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.response.ApplicationResponse
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import java.util.*

fun Application.main() {

    val config = environment.config.config("sql")
    val dbConnectionString = config.property("dbConnectionString").getString()
    val dbUser = config.property("dbUser").getString()
    val dbPasswordEncrypted = config.property("dbPasswordEncrypted").getString()
            .replace("\n", "")

    val client = KeyManagementServiceClient.create()
    val passwordResponse = client.decrypt("database password",
            ByteString.copyFrom(Base64.getDecoder().decode(dbPasswordEncrypted)))

    val database = DatabaseLayer(dbConnectionString, dbUser, passwordResponse.plaintext.toStringUtf8())

    install(DefaultHeaders)
    install(Compression)

    routing {
        get("/") {
            val packets = database.getAllFrom("KI7UKU")
            call.respondText("I have ${packets.count()} packets!",
                    contentType = io.ktor.http.ContentType.Text.Plain)
        }
    }
}
