import java.io.*

interface Continuation<in T> {
    fun resumeWith(result: SuccessOrFailure<T>)
}


class Deferred<T> {
    suspend fun await(): T = TODO()
}

fun async(block: () -> Unit): Deferred<Int> = TODO()

val deferreds: List<Deferred<Int>> = List(10) {
    async {
        /* Do something that produces T or fails */
    }
}

suspend fun <T> Deferred<T>.awaitCatching(): SuccessOrFailure<T> =
    runCatching { await() }

suspend fun asyncMapUseCase() {
    val outcomes3: List<SuccessOrFailure<Int>> = deferreds.map { it.awaitCatching() } // !!! <= THIS IS THE ONE WE WANT
}

data class Data(val s: String)

fun readFileData(file: File): Data = Data(file.toString())

fun readFiles(files: List<File>): List<SuccessOrFailure<Data>> =
    files.map {
        runCatching {
            readFileData(it)
        }
    }

data class AnotherData(val s: String)

fun Data.doSomething(): AnotherData = AnotherData(s)

fun main(args: Array<String>) {
    val files = List(10) { File("file$it.txt") }

    readFiles(files).map { result ->
        result.map { it.doSomething() }
    }
}

fun doSomethingSync(): Data = TODO()

fun functionalHandingUseCase() {
    runCatching { doSomethingSync() }
        .onFailure { showErrorDialog(it) }
        .onSuccess { processData(it) }
}

fun processData(data: Data) {
    TODO("not implemented")
}

fun showErrorDialog(exception: Throwable) {
    TODO("not implemented")
}

