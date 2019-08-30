import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val runner = ExceptionDemo()
    runner.run()
}

class ExceptionDemo : Runner {
    override fun run() = runBlocking {
        //sampleStart
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                // the first child
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    withContext(NonCancellable) {
                        println("Children are cancelled, but exception is not handled until all children terminate")
                        delay(100)
                        println("The first child finished its non cancellable block")
                    }
                }
            }
            launch {
                // the second child
                delay(10)
                println("Second child throws an exception")
                throw ArithmeticException()
            }
        }
        job.join()
    }
}

class ThreadLocalDataDemo : Runner {
    val threadLocal = ThreadLocal<String?>()
    override fun run() = runBlocking {
        threadLocal.set("Hiiii")
        println("Pre-main, current thread: ${Thread.currentThread().name}, thread local value:'${threadLocal.get()}'")
        val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "NOOOO")) {
            threadLocal.asContextElement(value = "oooooo")
            println("#1 current thread: ${Thread.currentThread().name}, value:'${threadLocal.get()}'")
            yield()
            println("#2 current thread: ${Thread.currentThread().name}, value:'${threadLocal.get()}'")
            yield()
            println("#3 current thread: ${Thread.currentThread().name}, value:'${threadLocal.get()}'")
        }
        println("Starting....")
        job.join()
        println("Post-main, current thread: ${Thread.currentThread().name}, thread local value:'${threadLocal.get()}'")
    }
}

class JobCancellation : CoroutineScope, Runner {
    var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun run() = runBlocking {
        repeat(5) {
            //If not mentioned explicitly, launch will be inherited from *runBlocking*
            this@JobCancellation.launch {
                delay(1000L * (it + 1))
                println("#$it yo whassup")
            }
        }
        launch {
            delay(3000L)
            job.cancel()
            println("The end")
        }
        println("Starting...")
    }
}

class ContextJob : Runner {
    override fun run() = runBlocking {
        launch(Dispatchers.IO) {
            delay(2000L)
            printDetails("#1 " + coroutineContext[Job].toString())
        }
        launch(Dispatchers.Unconfined) {
            delay(100L)
            printDetails("#3 " + coroutineContext[Job].toString())
        }
        printDetails("#2 " + coroutineContext[Job].toString())
        delay(50L)
        println(this.coroutineContext[Job]?.children?.count())
    }
}

class JumpingBetweenThreads : Runner {
    override fun run() {
        newSingleThreadContext("ctx1").use { ctx1 ->
            newSingleThreadContext("ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    delay(2000L)
                    printDetails("#1 " + time())
                    withContext(ctx2) {
                        delay(1000L)
                        printDetails("#2 " + time())
                    }
                    withContext(ctx1) {
                        delay(200L)
                        printDetails("#4 " + time())
                    }
                    withContext(ctx2) {
                        delay(500L)
                        printDetails("#3 " + time())
                    }
                    printDetails("#6 " + time())

                }
            }
        }
    }
}

class StructuredConcurrencyAsync : Runner {
    override fun run() = runBlocking {
        coroutineScope {
            val i = async { doSomething(10, 4000L) }
            val j = async { doSomething(30, 1000L) }
            println("${i.await()} + ${j.await()} = ${i.await() + j.await()}")
        }
    }

    private suspend fun doSomething(value: Int, delay: Long = 500L): Int {
        delay(delay)
        return value.apply {
            printImp("Returned value of $value after delay of $delay @{${time()}")
        }
    }
}

class LazyDemo : Runner {
    override fun run() = runBlocking {
        printDecorated("LazyDemo")
        val time = measureTimeMillis {
            val i = async(start = CoroutineStart.LAZY) { doSomething1() }
            val j = async { doSomething2() }
            i.start()
            j.start()
            println("result = ${i.await() + j.await()}")
        }
        println("Completed in $time")
    }

    private suspend fun doSomething1(): Int {
        delay(500L)
        println("do something 1")
        return 3
    }

    private suspend fun doSomething2(): Int {
        delay(1500L)
        println("do something 2")
        return 5
    }
}

class ConcurrentDemo : Runner {
    override fun run() = runBlocking {
        printDecorated("ConcurrentDemo")
        val time = measureTimeMillis {
            val i = async { doSomething1() }
            val j = async { doSomething2() }
            println("result = ${i.await() + j.await()}")
        }
        println("Completed in $time")
    }

    private suspend fun doSomething1(): Int {
        delay(500L)
        println("do something 1")
        return 3
    }

    private suspend fun doSomething2(): Int {
        delay(1500L)
        println("do something 2")
        return 5
    }
}

class SequentialDemo : Runner {
    override fun run() = runBlocking {
        val i = doSomething1()
        val j = doSomething2()
        println("sum =  ${i + j}")
        println("inline =  ${doSomething1() + doSomething2()}")
    }

    private suspend fun doSomething1(): Int {
        delay(500L)
        println("do something 1")
        return 3
    }

    private suspend fun doSomething2(): Int {
        delay(1500L)
        println("do something 2")
        return 5
    }
}