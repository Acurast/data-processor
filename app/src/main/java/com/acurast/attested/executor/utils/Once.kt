package com.acurast.attested.executor.utils

/**
 * A lambda wrapper which only allows the lambda to be called once.
 */
class Once<T>(private val lambda:(T) -> Unit): (T) -> Unit {
    var successCounter = 0

    override fun invoke(parameter: T) {
        if (successCounter==0) {
            successCounter++
            lambda(parameter)
        }
    }
}