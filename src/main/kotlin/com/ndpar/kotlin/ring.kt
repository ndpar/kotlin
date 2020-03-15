package com.ndpar.kotlin

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

private data class Message(
    val count: Int,
    val message: String
)

private fun process() = Channel<Message>()

/**
 * Usage:   java -jar target/kotlin-1.0-SNAPSHOT-jar-with-dependencies.jar <N> <M> <T>
 * Example: java -jar target/kotlin-1.0-SNAPSHOT-jar-with-dependencies.jar 200000 2000 hello
 *
 * Parameters:
 * - N number of coroutines/channels in the ring
 * - M number of messages/cycles
 * - T text message
 */
fun main(args: Array<String>) = runBlocking {
    val (n, m, message) = args
    val time = measureTimeMillis {
        val first = process()
        val last = (1..n.toInt()).fold(first) { to, _ ->
            process().also { from ->
                launch {
                    relay(from, to)
                }
            }
        }
        val process = launch {
            nextMessage(first, last)
        }
        last.send(Message(m.toInt(), message))
        process.join()
    }
    println("Done in $time ms. Message passing speed ${time / n.toDouble() / m.toDouble() / 1000} s")
}

private suspend fun relay(from: Channel<Message>, to: Channel<Message>) {
    for (m in from) {
        to.send(m)
        if (m.count == 0) {
            break
        }
    }
    from.close()
}

private suspend fun nextMessage(from: Channel<Message>, to: Channel<Message>) {
    for (m in from) {
        if (m.count == 0) {
            break
        } else {
            to.send(m.copy(count = m.count - 1))
        }
    }
    from.close()
}
