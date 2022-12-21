package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.stereotype.Component

@Component
class Listener {
    fun listener(message: String) {
        val (i,url) = message.split("\\")
        print(url)
        // Primera
        if(i.toInt() == 0){
            println("Primera")
        }
    }
}