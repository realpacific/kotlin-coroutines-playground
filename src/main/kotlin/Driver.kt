import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.Calendar


fun main(args: Array<String>) {
    Driver().channelDemo2()
}

class Driver {

    fun cancellableDemo() = kotlinx.coroutines.runBlocking {
        val job = launch(Dispatchers.Default) {
            try {
                var i = 0
                while (isActive) {
                    println("Running ${i++} ${Thread.currentThread().name}")
                }
            } finally {
                println("I'm closing")
            }
        }
        delay(1000L)
        println("cancelling ${Thread.currentThread().name}")
        job.cancelAndJoin()
        println("cancelled ${Thread.currentThread().name}")
        Thread.sleep(4000L)
    }

    fun channelDemo2() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5) {
                println("Sent to channel ${time()}")
                delay(1000)
                channel.send(x * x)
            }
            channel.close()
        }
        repeat(5) {
            println("Received to channel ${time()}")
            println(channel.receive())
        }
        println("Done!")
    }

    fun channelDemo() {
        val channel = Channel<Int>(capacity = 5)
        GlobalScope.launch {
            for (i in channel) {
                println(i)
            }
        }
        GlobalScope.launch {
            repeat(10) {
                delay(500)
                channel.send(it)
            }
            channel.close()
        }

        Thread.sleep(10000)
    }

    fun run() {
        GlobalScope.launch {
            printDelayed("#1 `${Thread.currentThread().name}`", time = 5)
        }
        println("#7 `${Thread.currentThread().name}`")
        runBlocking {
            println("#2 ${Thread.currentThread().name}")
            printDelayed("#3 `${Thread.currentThread().name}`")
            println("#5 ${Thread.currentThread().name}")
        }
        GlobalScope.launch {
            println("#8 ${Thread.currentThread().name}")
            printDelayed("#9 `${Thread.currentThread().name}`", time = 5)
        }
        println("#6 `${Thread.currentThread().name}`")
    }

    fun runBlocking() = kotlinx.coroutines.runBlocking {
        printDecorated("runBlocking")
        GlobalScope.launch {
            printDelayed("Called from launch in ${Thread.currentThread().name}")
        }
        println("Called from outside in ${Thread.currentThread().name}")
        delay(2000L)
    }

    fun jobJoin() = runBlocking {
        printDecorated("jobJoin")
        val job = GlobalScope.launch {
            printDelayed("Called from launch in ${Thread.currentThread().name}")
        }
        println("Called from outside in ${Thread.currentThread().name}")
        job.join()
    }

    fun scopeBuilder() = runBlocking {
        printDecorated("scopeBuilder")
        launch {
            printDelayed("Called from launch in ${Thread.currentThread().name}")
        }
        println("Called from outside in ${Thread.currentThread().name}")
        printDelayed("Called from outside in ${Thread.currentThread().name}", time = 2000L)


        coroutineScope {
            launch {
                printDelayed("Called from coroutineScope in ${Thread.currentThread().name}")
            }
            println("Called from outside coroutineScope in ${Thread.currentThread().name}")
            printDelayed("Called from outside coroutineScope in ${Thread.currentThread().name}")
        }
        printDecorated("The end")
    }

    fun scopeBuilderWithoutRunBlocking() {
        printDecorated("scopeBuilder w/o run blocking")
        GlobalScope.launch {
            async {
                println("#97 ${Thread.currentThread().name}")
                printDelayed("#98 ${Thread.currentThread().name}")
                println("#99 ${Thread.currentThread().name}")
            }

            launch {
                printDelayed("#1 ${Thread.currentThread().name}")
                launch {
                    printDelayed("#2 ${Thread.currentThread().name}")
                    launch {
                        printDelayed("#3 ${Thread.currentThread().name}")
                    }
                }
            }
            launch {
                printDelayed("#11 ${Thread.currentThread().name}")
                launch {
                    printDelayed("#12 ${Thread.currentThread().name}")
                    launch {
                        printDelayed("#13 ${Thread.currentThread().name}")
                    }
                }
            }
            println("#4 ${Thread.currentThread().name}")
            printDelayed("#41 ${Thread.currentThread().name}")
            coroutineScope {
                launch {
                    printDelayed("#51 ${Thread.currentThread().name}")
                }
                launch {
                    printDelayed("#52 ${Thread.currentThread().name}")
                }
                println("#6 ${Thread.currentThread().name}")
            }
        }
        println("#z Here")
        Thread.sleep(28000L)
        printDecorated("The end")
    }
}

suspend fun printDelayed(msg: String, time: Long = Random().nextInt(7).toLong() * 1000) {
    delay(time)
    println(msg + " delay of ${time}ms")
}

fun printDecorated(msg: String) {
    print("-----------")
    print(msg)
    print("----------")
    println("")
}

fun printImp(msg: String) {
    print("****")
    print(msg)
    println("")
}

fun printDetails(msg: String) {
    print(msg + " ${Thread.currentThread().name}")
    println("")
}


fun time(): String {
    val now = Calendar.getInstance()
    return now.get(Calendar.HOUR_OF_DAY).toString() + ":" + now.get(Calendar.MINUTE).toString() + ":" + now.get(Calendar.SECOND) + ":" + now.get(
        Calendar.MILLISECOND
    )
}
