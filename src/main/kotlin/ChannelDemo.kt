import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main(args: Array<String>) {
    val run: Runner = TickerDemo()
    run.run()
}

class TickerDemo : Runner {
    override fun run() = runBlocking<Unit> {
        val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // create ticker channel
        var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Initial element is available immediately: $nextElement") // initial delay hasn't passed yet

        nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements has 100ms delay
        println("Next element is not ready in 50 ms: $nextElement")

        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 100 ms: $nextElement")

        // Emulate large consumption delays
        println("Consumer pauses for 150ms")
        delay(150)
        // Next element is available immediately
        nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Next element is available immediately after large consumer delay: $nextElement")
        // Note that the pause between `receive` calls is taken into account and next element arrives faster
        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

        tickerChannel.cancel() // indicate that no more elements are needed
    }
}

class BufferedDemo : Runner {
    override fun run() = runBlocking<Unit> {
        val channel = Channel<Int>(4) // create buffered channel
        val sender = launch {
            // launch sender coroutine
            repeat(10) {
                println("Sending $it ${time()}") // print before sending each element
                channel.send(it) // will suspend when buffer is full
            }
        }
        // don't receive anything... just wait....
        delay(5000)
        for (i in channel) {
            println("Received $i")
        }
        sender.cancel() // cancel sender coroutine
    }
}

class FanOutDemo : Runner {
    override fun run() = runBlocking {
        val channel = Channel<String>()
        launch { sendString(channel, "foo", 200L) }
        launch { sendString(channel, "BAR!", 500L) }
        repeat(6) {
            // receive first six
            println(channel.receive())
        }
        coroutineContext.cancelChildren() // cancel all children to let main finish
    }

    private suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
        while (true) {
            delay(time)
            channel.send(s)
        }
    }
}

class FanInDemo : Runner {
    override fun run() = runBlocking<Unit> {
        val producer = produceNumbers()
        repeat(5) {
            launchProcessor(it, producer)
        }
        delay(950)
        producer.cancel() // cancel producer coroutine and thus kill them all
    }

    fun CoroutineScope.produceNumbers() = produce {
        var x = 1 // start from 1
        while (true) {

            println("Produced: $x")
            send(x) // produce next
            x += 1
            delay(100) // wait 0.1s
        }
    }

    fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
        printDecorated("Lauching processor #$id")
        for (msg in channel) {
            println("Processor #$id received item $msg ${time()}")
        }
        printDecorated("Finished process #$id")
//        println("Processor #$id received ${channel.receive()} ${time()}")
    }

}

class PipelinesPrimeDemo : Runner {

    override fun run() = runBlocking {
        var cur = numbersFrom(2)
        for (i in 1..10) {
            val prime = cur.receive()
            printDecorated(prime.toString())
            cur = filter(cur, prime)
        }
        coroutineContext.cancelChildren() // cancel all children to let main finish
    }

    private fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
        var x = start
        while (true) {
            send(x++)
        } // infinite stream of integers from start
    }

    private fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
        for (x in numbers) {
            println("$x % $prime = ${x % prime}")
            if (x % prime != 0)
                send(x)
        }
    }
}

class PipelinesDemo {
    fun run() = runBlocking {
        val producer = produceNumbers()
        val consumer = square(producer)
        for (i in 1..5)
            println(consumer.receive())
        coroutineContext.cancelChildren()
    }

    private fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
        for (i in numbers) {
            println("Squaring $i...")
            send(i * i)
        }
    }
}

class ChannelDemo {
    fun run() = runBlocking {
        printDecorated("producer demo")
        val channel = produceSquares()
        for (i in channel)
            println(i)
    }

    private fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
        for (i in 1..5) send(i * i)
    }
}

private fun CoroutineScope.produceNumbers(): ReceiveChannel<Int> = produce {
    var i = 1
    while (true) {
        println("Producing $i...")
        send(i++)
    }
}
