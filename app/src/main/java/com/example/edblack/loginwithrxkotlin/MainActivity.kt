package com.example.edblack.loginwithrxkotlin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RxTextView.afterTextChangeEvents(editText_email)
                .skipInitialValue()
                .map {
                    wrapper_email.error = null
                    it.view().text.toString()
                }
                .debounce(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                .compose(verifyEmailPattern)
                .compose(verifyEmailPattern)
                .compose(retryWhenError {
                    wrapper_email.error = it.message
                })
                .subscribe()

        RxTextView.afterTextChangeEvents(editText_password)
                .skipInitialValue()
                .map {
                    wrapper_password.error = null
                    it.view().text.toString()
                }
                .debounce(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                .compose(verifyPasswordLength)
                .compose(retryWhenError {
                    wrapper_password.error = it.message
                })
                .subscribe()


    }

    private val verifyPasswordLength = ObservableTransformer<String, String> { observable ->
        observable.flatMap {
            Observable.just(it).map { it.trim() } //removing white spaces on the string
                    .filter { it.length > 8 } //setting the length to 8
                    .singleOrError()
                    .onErrorResumeNext {
                        if (it is NoSuchElementException) {
                            Single.error(Exception("Password must be at least 8 characters")) //informing the user
                        } else {
                            Single.error(it)
                        }
                    }
                    .toObservable()
        }
    }

    private val verifyEmailPattern = ObservableTransformer<String, String> { observable ->
        observable.flatMap {
            Observable.just(it).map { it.trim() } //removing white spaces
                    .filter {
                        Patterns.EMAIL_ADDRESS.matcher(it).matches() // setting the email pattern - abc@def.com -
                    }
                    .singleOrError()
                    .onErrorResumeNext {
                        if (it is NoSuchElementException) {
                            Single.error(Exception("Invalid email format")) //informing the usher
                        } else {
                            Single.error(it)
                        }

                    }.toObservable()
        }
    }

    private inline fun retryWhenError(crossinline onError: (ex: Throwable) -> Unit): ObservableTransformer<String, String> = ObservableTransformer { observable ->
        observable.retryWhen { errors ->
            errors.flatMap {
                onError(it)
                Observable.just("")
            }
        }


    }


}
